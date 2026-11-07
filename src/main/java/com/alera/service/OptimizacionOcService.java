package com.alera.service;

import com.alera.dto.OptimizacionOcDto;
import com.alera.dto.OptimizacionOcDto.ItemCompraDto;
import com.alera.model.ElaboracionPlanificada;
import com.alera.model.FacturaItem;
import com.alera.model.InsumoInventario;
import com.alera.model.enums.EstadoPlanificacion;
import com.alera.repository.FacturaItemRepository;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.TextBlockParam;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OptimizacionOcService {

    private static final Logger log = LoggerFactory.getLogger(OptimizacionOcService.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${app.anthropic.api-key:}")
    private String apiKey;

    private final PlanificacionService planificacionService;
    private final InsumoInventarioService insumoService;
    private final FacturaItemRepository facturaItemRepo;

    public OptimizacionOcService(PlanificacionService planificacionService,
                                  InsumoInventarioService insumoService,
                                  FacturaItemRepository facturaItemRepo) {
        this.planificacionService = planificacionService;
        this.insumoService = insumoService;
        this.facturaItemRepo = facturaItemRepo;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public OptimizacionOcDto analizar(int semanas) {
        // Recopilar datos necesarios antes de llamar a la IA
        DatosAnalisis datos = recopilarDatos(semanas);
        if (datos.elaboraciones.isEmpty()) {
            var dto = new OptimizacionOcDto();
            dto.setResumen("No hay elaboraciones planificadas en las próximas " + semanas + " semanas.");
            dto.setItemsSugeridos(List.of());
            dto.setAlertas(List.of());
            dto.setElaboracionesAnalizadas(0);
            return dto;
        }
        if (!isConfigured()) {
            return mockResponse(datos);
        }
        try {
            var client = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
            String prompt = construirPrompt(datos, semanas);
            ContentBlockParam bloque = ContentBlockParam.ofText(
                    TextBlockParam.builder().text(prompt).build());
            var response = client.messages().create(
                    MessageCreateParams.builder()
                            .model("claude-haiku-4-5")
                            .maxTokens(2048L)
                            .addUserMessageOfBlockParams(List.of(bloque))
                            .build());
            String texto = response.content().stream()
                    .filter(b -> b.isText())
                    .map(b -> b.asText().text())
                    .findFirst().orElse("{}");
            var dto = parsearRespuesta(texto, datos);
            dto.setElaboracionesAnalizadas(datos.elaboraciones.size());
            return dto;
        } catch (Exception ex) {
            log.error("Error al optimizar OC con IA", ex);
            var dto = new OptimizacionOcDto();
            dto.setError(ex.getMessage());
            return dto;
        }
    }

    // ── Recopilación de datos ──────────────────────────────────────────────

    private DatosAnalisis recopilarDatos(int semanas) {
        var datos = new DatosAnalisis();
        LocalDate hasta = LocalDate.now().plusWeeks(semanas);

        // Elaboraciones planificadas en el rango
        datos.elaboraciones = planificacionService.listarProximas().stream()
                .filter(e -> e.getEstado() == EstadoPlanificacion.PLANIFICADA)
                .filter(e -> e.getFechaPlaneada() == null || !e.getFechaPlaneada().isAfter(hasta))
                .filter(e -> e.getReceta() != null)
                .collect(Collectors.toList());

        // Agregar requerimientos de ingredientes (nombre → cantidad en unidad base)
        for (ElaboracionPlanificada elab : datos.elaboraciones) {
            var receta = elab.getReceta();
            if (receta == null || receta.getIngredientes() == null) continue;
            double factor = calcularFactor(elab, receta);
            String nombreElab = elab.getNombreElaboracion() != null
                    ? elab.getNombreElaboracion() : (receta.getNombre() != null ? receta.getNombre() : "Sin nombre");
            for (var ing : receta.getIngredientes()) {
                if (ing.getNombre() == null || ing.getNombre().isBlank()) continue;
                String nombre = ing.getNombre().trim();
                double cantBase = parseCantidad(ing.getCantidad());
                double cantEscalada = cantBase * factor;
                datos.requerimientos.merge(nombre, cantEscalada, Double::sum);
                datos.elaboracionesPorIngrediente.computeIfAbsent(nombre, k -> new ArrayList<>()).add(nombreElab);
                datos.tipoIngrediente.putIfAbsent(nombre, ing.getTipo() != null ? ing.getTipo().name() : "");
            }
        }

        // Stock actual
        for (InsumoInventario inv : insumoService.listarTodos()) {
            if (inv.getNombre() != null) {
                datos.stockActual.put(inv.getNombre().trim(), inv);
            }
        }

        // Historial de precios
        if (!datos.requerimientos.isEmpty()) {
            List<String> nombres = datos.requerimientos.keySet().stream()
                    .map(String::toLowerCase).collect(Collectors.toList());
            List<FacturaItem> historial = facturaItemRepo.findUltimosPrecios(nombres);
            for (FacturaItem fi : historial) {
                if (fi.getNombre() != null && fi.getValorUnitario() != null
                        && fi.getValorUnitario().compareTo(BigDecimal.ZERO) > 0) {
                    datos.ultimoPrecio.putIfAbsent(fi.getNombre().trim().toLowerCase(), fi);
                }
            }
        }
        return datos;
    }

    private double calcularFactor(ElaboracionPlanificada elab, com.alera.model.Receta receta) {
        if (elab.getVolumenEstimado() == null || receta.getVolumenBase() == null
                || receta.getVolumenBase().compareTo(BigDecimal.ZERO) == 0) return 1.0;
        return elab.getVolumenEstimado().doubleValue() / receta.getVolumenBase().doubleValue();
    }

    private double parseCantidad(String cantStr) {
        if (cantStr == null || cantStr.isBlank()) return 0;
        try {
            return Double.parseDouble(cantStr.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ── Construcción del prompt ────────────────────────────────────────────

    private String construirPrompt(DatosAnalisis datos, int semanas) {
        var sb = new StringBuilder();
        sb.append("Eres un experto en compras y gestión de inventario para cervecerías artesanales. ");
        sb.append("Analiza los datos de producción planificada e inventario actual, y genera una lista de compras optimizada.\n\n");

        sb.append("## Elaboraciones planificadas (próximas ").append(semanas).append(" semanas)\n");
        for (ElaboracionPlanificada e : datos.elaboraciones) {
            String nombre = e.getNombreElaboracion() != null ? e.getNombreElaboracion() :
                    (e.getReceta() != null ? e.getReceta().getNombre() : "Sin nombre");
            sb.append("- **").append(nombre).append("**");
            if (e.getFechaPlaneada() != null) sb.append(" (").append(e.getFechaPlaneada()).append(")");
            if (e.getVolumenEstimado() != null) sb.append(" — ").append(e.getVolumenEstimado()).append(" L");
            sb.append("\n");
        }

        sb.append("\n## Requerimientos consolidados vs inventario actual\n");
        sb.append("| Ingrediente | Tipo | Necesario | En stock | Déficit | Último precio ref. | Proveedor ref. |\n");
        sb.append("|---|---|---|---|---|---|---|\n");
        for (Map.Entry<String, Double> entry : datos.requerimientos.entrySet()) {
            String nombre = entry.getKey();
            double necesario = entry.getValue();
            InsumoInventario inv = datos.stockActual.get(nombre);
            double stock = inv != null && inv.getCantidad() != null ? inv.getCantidad().doubleValue() : 0;
            double deficit = Math.max(0, necesario - stock);
            String unidad = inv != null ? (inv.getUnidad() != null ? inv.getUnidad() : "?") : "?";
            FacturaItem fi = datos.ultimoPrecio.get(nombre.toLowerCase());
            String precio = fi != null && fi.getValorUnitario() != null
                    ? fi.getValorUnitario().toPlainString() : "sin referencia";
            String proveedor = inv != null && inv.getProveedor() != null ? inv.getProveedor() : "?";
            String tipo = datos.tipoIngrediente.getOrDefault(nombre, "");
            sb.append("| ").append(nombre)
              .append(" | ").append(tipo)
              .append(" | ").append(String.format("%.0f %s", necesario, unidad))
              .append(" | ").append(String.format("%.0f %s", stock, unidad))
              .append(" | ").append(String.format("%.0f %s", deficit, unidad))
              .append(" | ").append(precio)
              .append(" | ").append(proveedor)
              .append(" |\n");
        }

        sb.append("\n## Tu tarea\n");
        sb.append("Genera una lista de compras priorizada. Para cada ítem a comprar:\n");
        sb.append("- Prioriza ingredientes con mayor déficit\n");
        sb.append("- Sugiere un 10-15% adicional como margen de seguridad\n");
        sb.append("- Agrupa por proveedor cuando sea posible\n");
        sb.append("- Identifica alertas críticas (déficit >50% de lo necesario, lotes que comparten ingredientes, etc.)\n\n");

        sb.append("Responde ÚNICAMENTE con JSON válido, sin markdown:\n");
        sb.append("{\n");
        sb.append("  \"resumen\": \"<resumen ejecutivo de 1-2 oraciones sobre la situación de compras>\",\n");
        sb.append("  \"alertas\": [\"<alerta crítica 1>\", \"<alerta 2>\"],\n");
        sb.append("  \"itemsSugeridos\": [\n");
        sb.append("    {\n");
        sb.append("      \"nombre\": \"<nombre ingrediente>\",\n");
        sb.append("      \"tipo\": \"<tipo>\",\n");
        sb.append("      \"cantidadAComprar\": <número>,\n");
        sb.append("      \"unidad\": \"<unidad>\",\n");
        sb.append("      \"precioUnitarioReferencia\": \"<precio o 'sin referencia'>\",\n");
        sb.append("      \"costoEstimado\": \"<costo total o 'sin referencia'>\",\n");
        sb.append("      \"proveedorSugerido\": \"<proveedor o null>\",\n");
        sb.append("      \"elaboraciones\": [\"<elaboración1>\", \"<elaboración2>\"]\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}\n\n");
        sb.append("Solo incluye ítems que realmente necesiten comprarse (déficit > 0 o stock crítico). ");
        sb.append("Si todo está bien cubierto, retorna itemsSugeridos vacío con resumen positivo.");

        return sb.toString();
    }

    // ── Parseo de respuesta ────────────────────────────────────────────────

    private OptimizacionOcDto parsearRespuesta(String texto, DatosAnalisis datos) {
        try {
            String limpio = texto.trim();
            int inicio = limpio.indexOf('{');
            int fin = limpio.lastIndexOf('}');
            if (inicio >= 0 && fin > inicio) limpio = limpio.substring(inicio, fin + 1);

            var node = mapper.readTree(limpio);
            var dto = new OptimizacionOcDto();

            if (node.has("resumen")) dto.setResumen(node.get("resumen").asText());

            var alertasList = new ArrayList<String>();
            if (node.has("alertas")) node.get("alertas").forEach(a -> alertasList.add(a.asText()));
            dto.setAlertas(alertasList);

            var items = new ArrayList<ItemCompraDto>();
            if (node.has("itemsSugeridos")) {
                for (var itemNode : node.get("itemsSugeridos")) {
                    var item = new ItemCompraDto();
                    if (itemNode.has("nombre"))                   item.setNombre(itemNode.get("nombre").asText());
                    if (itemNode.has("tipo"))                     item.setTipo(itemNode.get("tipo").asText());
                    if (itemNode.has("cantidadAComprar"))         item.setCantidadAComprar(itemNode.get("cantidadAComprar").asDouble());
                    if (itemNode.has("unidad"))                   item.setUnidad(itemNode.get("unidad").asText());
                    if (itemNode.has("precioUnitarioReferencia")) item.setPrecioUnitarioReferencia(itemNode.get("precioUnitarioReferencia").asText());
                    if (itemNode.has("costoEstimado"))            item.setCostoEstimado(itemNode.get("costoEstimado").asText());
                    if (itemNode.has("proveedorSugerido") && !itemNode.get("proveedorSugerido").isNull())
                        item.setProveedorSugerido(itemNode.get("proveedorSugerido").asText());

                    // Enriquecer con datos locales (stock actual y elaboraciones)
                    if (item.getNombre() != null) {
                        InsumoInventario inv = datos.stockActual.get(item.getNombre());
                        item.setCantidadEnStock(inv != null && inv.getCantidad() != null
                                ? inv.getCantidad().doubleValue() : 0);
                        item.setCantidadNecesaria(datos.requerimientos.getOrDefault(item.getNombre(), 0.0));
                    }

                    var elabs = new ArrayList<String>();
                    if (itemNode.has("elaboraciones")) itemNode.get("elaboraciones").forEach(e -> elabs.add(e.asText()));
                    if (elabs.isEmpty() && item.getNombre() != null)
                        elabs.addAll(datos.elaboracionesPorIngrediente.getOrDefault(item.getNombre(), List.of()));
                    item.setElaboraciones(elabs);
                    items.add(item);
                }
            }
            dto.setItemsSugeridos(items);
            return dto;
        } catch (Exception ex) {
            log.warn("No se pudo parsear respuesta de optimización OC", ex);
            return mockResponse(datos);
        }
    }

    // ── Mock ───────────────────────────────────────────────────────────────

    private OptimizacionOcDto mockResponse(DatosAnalisis datos) {
        var dto = new OptimizacionOcDto();
        dto.setModoMock(true);
        dto.setElaboracionesAnalizadas(datos.elaboraciones.size());

        var items = new ArrayList<ItemCompraDto>();
        var alertas = new ArrayList<String>();

        for (Map.Entry<String, Double> entry : datos.requerimientos.entrySet()) {
            String nombre = entry.getKey();
            double necesario = entry.getValue();
            InsumoInventario inv = datos.stockActual.get(nombre);
            double stock = inv != null && inv.getCantidad() != null ? inv.getCantidad().doubleValue() : 0;
            double deficit = Math.max(0, necesario - stock);
            if (deficit <= 0) continue;

            var item = new ItemCompraDto();
            item.setNombre(nombre);
            item.setTipo(datos.tipoIngrediente.getOrDefault(nombre, ""));
            item.setCantidadNecesaria(necesario);
            item.setCantidadEnStock(stock);
            double aComprar = deficit * 1.12; // 12% margen seguridad
            item.setCantidadAComprar(Math.round(aComprar * 10.0) / 10.0);
            item.setUnidad(inv != null && inv.getUnidad() != null ? inv.getUnidad() : "?");
            item.setElaboraciones(datos.elaboracionesPorIngrediente.getOrDefault(nombre, List.of()));

            FacturaItem fi = datos.ultimoPrecio.get(nombre.toLowerCase());
            if (fi != null && fi.getValorUnitario() != null) {
                item.setPrecioUnitarioReferencia(fi.getValorUnitario().toPlainString());
                item.setCostoEstimado(fi.getValorUnitario().multiply(
                        BigDecimal.valueOf(item.getCantidadAComprar())).toPlainString());
            } else {
                item.setPrecioUnitarioReferencia("sin referencia");
                item.setCostoEstimado("sin referencia");
            }
            if (inv != null && inv.getProveedor() != null) item.setProveedorSugerido(inv.getProveedor());

            List<String> elaboracionesPorIng = datos.elaboracionesPorIngrediente.getOrDefault(nombre, List.of());
            if (elaboracionesPorIng.size() > 1) {
                alertas.add(nombre + " es requerido por " + elaboracionesPorIng.size() + " elaboraciones");
            }
            items.add(item);
        }

        // Ordenar por déficit desc
        items.sort((a, b) -> Double.compare(b.getCantidadAComprar() - b.getCantidadEnStock(),
                a.getCantidadAComprar() - a.getCantidadEnStock()));

        dto.setItemsSugeridos(items);
        dto.setAlertas(alertas);
        dto.setResumen(items.isEmpty()
                ? "El inventario cubre todas las elaboraciones planificadas. No se requieren compras urgentes."
                : "Se necesitan comprar " + items.size() + " ingredientes para cubrir " +
                  datos.elaboraciones.size() + " elaboraciones planificadas.");
        return dto;
    }

    // ── Clase auxiliar ─────────────────────────────────────────────────────

    private static class DatosAnalisis {
        List<ElaboracionPlanificada> elaboraciones = new ArrayList<>();
        Map<String, Double> requerimientos = new LinkedHashMap<>();
        Map<String, InsumoInventario> stockActual = new HashMap<>();
        Map<String, FacturaItem> ultimoPrecio = new HashMap<>();
        Map<String, List<String>> elaboracionesPorIngrediente = new HashMap<>();
        Map<String, String> tipoIngrediente = new HashMap<>();
    }
}
