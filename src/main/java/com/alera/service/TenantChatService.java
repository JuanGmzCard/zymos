package com.alera.service;

import com.alera.dto.MensajeChatDto;
import com.alera.model.InsumoInventario;
import com.alera.model.LoteCerveza;
import com.alera.model.Venta;
import com.alera.repository.InsumoInventarioRepository;
import com.alera.repository.LoteCervezaRepository;
import com.alera.repository.VentaRepository;
import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.*;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class TenantChatService {

    private static final Logger log = LoggerFactory.getLogger(TenantChatService.class);
    private static final int MAX_HISTORY = 10;

    private final Cache<String, String> contextoCache =
            Caffeine.newBuilder().expireAfterWrite(2, TimeUnit.MINUTES).maximumSize(50).build();

    @Value("${app.anthropic.api-key:}")
    private String apiKey;

    private final LoteCervezaRepository      loteRepo;
    private final InsumoInventarioRepository insumoRepo;
    private final VentaRepository            ventaRepo;

    public TenantChatService(LoteCervezaRepository loteRepo,
                              InsumoInventarioRepository insumoRepo,
                              VentaRepository ventaRepo) {
        this.loteRepo   = loteRepo;
        this.insumoRepo = insumoRepo;
        this.ventaRepo  = ventaRepo;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Genera 3-4 sugerencias de preguntas contextuales basadas en el estado actual del tenant.
     * Nunca lanza excepción — retorna preguntas genéricas si falla la consulta.
     */
    public List<String> getSugerencias(String tenantName) {
        List<String> sugs = new ArrayList<>();
        try {
            List<LoteCerveza> activos = loteRepo.findByPeriodo(null, null).stream()
                    .filter(l -> !l.isCompletado()).collect(Collectors.toList());

            Map<String, Long> porFase = activos.stream()
                    .collect(Collectors.groupingBy(LoteCerveza::getFaseActual, Collectors.counting()));

            if (porFase.getOrDefault("Fermentación", 0L) > 0)
                sugs.add("¿Cómo están mis lotes en fermentación?");

            if (porFase.getOrDefault("Carbonatación", 0L) > 0)
                sugs.add("¿Qué lote está listo para empacar pronto?");

            long bajoStock = insumoRepo.findAllByOrderByNombreAsc().stream()
                    .filter(InsumoInventario::isBajoStock).count();
            if (bajoStock > 0)
                sugs.add("¿Qué insumos necesito reponer urgentemente?");

            try {
                long ventasMes = ventaRepo.findByPeriodo(LocalDate.now().withDayOfMonth(1), null).size();
                if (ventasMes > 0)
                    sugs.add("¿Cómo están mis ventas este mes?");
            } catch (Exception ignored) { }

        } catch (Exception e) {
            log.warn("Error generando sugerencias para '{}': {}", tenantName, e.getMessage());
        }

        // Fallbacks hasta alcanzar 4 sugerencias
        for (String f : List.of(
                "¿Cuántos lotes activos tengo?",
                "¿Cuál es mi lote más reciente?",
                "¿Qué estilos produzco más?",
                "¿Qué lote debo priorizar esta semana?")) {
            if (sugs.size() >= 4) break;
            if (!sugs.contains(f)) sugs.add(f);
        }
        return Collections.unmodifiableList(sugs.subList(0, Math.min(4, sugs.size())));
    }

    public MensajeChatDto chat(String pregunta, List<MensajeChatDto> historial, String tenantName) {
        if (pregunta == null || pregunta.isBlank()) {
            return new MensajeChatDto("assistant", "Por favor escribe tu pregunta.", false);
        }

        String contexto = contextoCache.get(tenantName, this::buildContexto);

        if (!isConfigured()) {
            return mockChat(pregunta, contexto, tenantName);
        }

        try {
            String promptCompleto = buildPrompt(contexto, historial, pregunta);

            AnthropicClient client = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
            ContentBlockParam bloque = ContentBlockParam.ofText(
                    TextBlockParam.builder().text(promptCompleto).build());

            Message response = client.messages().create(
                    MessageCreateParams.builder()
                            .model("claude-haiku-4-5")
                            .maxTokens(800L)
                            .addUserMessageOfBlockParams(List.of(bloque))
                            .build());

            String respuesta = response.content().stream()
                    .filter(ContentBlock::isText)
                    .map(b -> b.asText().text())
                    .findFirst()
                    .orElse("No pude generar una respuesta.");

            return new MensajeChatDto("assistant", respuesta.trim(), false);

        } catch (Exception e) {
            log.error("Error en TenantChatService", e);
            return new MensajeChatDto("assistant",
                    "Error al conectar con la IA: " + e.getMessage(), false);
        }
    }

    private String buildContexto(String tenantName) {
        StringBuilder sb = new StringBuilder();
        sb.append("CERVECERÍA: ").append(tenantName).append("\n\n");

        // ── Lotes ────────────────────────────────────────────────────────────
        try {
            List<LoteCerveza> lotes = loteRepo.findByPeriodo(null, null);
            long total       = lotes.size();
            long completados = lotes.stream().filter(LoteCerveza::isCompletado).count();
            long activos     = lotes.stream().filter(l -> !l.isCompletado()).count();

            Map<String, Long> porFase = lotes.stream()
                    .filter(l -> !l.isCompletado())
                    .collect(Collectors.groupingBy(LoteCerveza::getFaseActual, Collectors.counting()));

            Map<String, Long> porEstilo = lotes.stream()
                    .filter(l -> l.getEstilo() != null && !l.getEstilo().isBlank())
                    .collect(Collectors.groupingBy(LoteCerveza::getEstilo, Collectors.counting()));

            sb.append("🍺 PRODUCCIÓN:\n");
            sb.append("- Total lotes: ").append(total).append(" (activos: ").append(activos)
              .append(", completados: ").append(completados).append(")\n");

            if (!porFase.isEmpty()) {
                sb.append("- Lotes activos por fase: ");
                porFase.forEach((fase, cnt) -> sb.append(fase).append(": ").append(cnt).append(", "));
                sb.setLength(sb.length() - 2);
                sb.append("\n");
            }

            if (!porEstilo.isEmpty()) {
                sb.append("- Estilos (todos los lotes): ");
                porEstilo.entrySet().stream()
                        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                        .limit(8)
                        .forEach(e -> sb.append(e.getKey()).append(" (").append(e.getValue()).append("), "));
                if (sb.charAt(sb.length() - 2) == ',') sb.setLength(sb.length() - 2);
                sb.append("\n");
            }

            // 5 lotes más recientes (activos primero)
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            List<LoteCerveza> recientes = lotes.stream()
                    .filter(l -> l.getFechaElaboracion() != null)
                    .sorted(Comparator.comparing(LoteCerveza::getFechaElaboracion).reversed())
                    .limit(5)
                    .collect(Collectors.toList());
            if (!recientes.isEmpty()) {
                sb.append("- Lotes más recientes:\n");
                recientes.forEach(l -> sb.append("  • ")
                        .append(l.getCodigoLote() != null ? l.getCodigoLote() : "Sin código")
                        .append(" (").append(l.getEstilo() != null ? l.getEstilo() : "?")
                        .append(") — ").append(l.getFaseActual())
                        .append(" — ").append(l.getFechaElaboracion().format(fmt))
                        .append(l.isCompletado() ? " ✓" : "").append("\n"));
            }

        } catch (Exception e) {
            sb.append("- [Error al cargar datos de lotes]\n");
        }

        // ── Inventario ───────────────────────────────────────────────────────
        try {
            List<InsumoInventario> insumos = insumoRepo.findAllByOrderByNombreAsc();
            long totalInsumos = insumos.size();
            List<InsumoInventario> bajoStock = insumos.stream()
                    .filter(InsumoInventario::isBajoStock).collect(Collectors.toList());

            Map<String, Long> porTipo = insumos.stream()
                    .filter(i -> i.getTipo() != null)
                    .collect(Collectors.groupingBy(InsumoInventario::getTipo, Collectors.counting()));

            sb.append("\n📦 INVENTARIO:\n");
            sb.append("- Total insumos: ").append(totalInsumos).append("\n");

            if (!porTipo.isEmpty()) {
                sb.append("- Por tipo: ");
                porTipo.forEach((tipo, cnt) -> sb.append(tipo).append(" (").append(cnt).append("), "));
                if (sb.charAt(sb.length() - 2) == ',') sb.setLength(sb.length() - 2);
                sb.append("\n");
            }

            if (!bajoStock.isEmpty()) {
                sb.append("- ⚠️ Con bajo stock (").append(bajoStock.size()).append(" ítems): ");
                bajoStock.stream().limit(5).forEach(i ->
                        sb.append(i.getNombre()).append(", "));
                if (sb.charAt(sb.length() - 2) == ',') sb.setLength(sb.length() - 2);
                if (bajoStock.size() > 5) sb.append("... y ").append(bajoStock.size() - 5).append(" más");
                sb.append("\n");
            } else {
                sb.append("- Sin alertas de bajo stock\n");
            }

        } catch (Exception e) {
            sb.append("- [Error al cargar datos de inventario]\n");
        }

        // ── Ventas (mes actual) ──────────────────────────────────────────────
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
            List<Venta> ventasMes = ventaRepo.findByPeriodo(inicioMes, null);
            if (!ventasMes.isEmpty()) {
                sb.append("\n📊 VENTAS (mes actual):\n");
                sb.append("- Total ventas del mes: ").append(ventasMes.size()).append("\n");
                sb.append("- Últimas ventas:\n");
                ventasMes.stream().limit(5).forEach(v -> {
                    sb.append("  • ").append(v.getCliente() != null ? v.getCliente() : "Sin cliente");
                    if (v.getFechaDespacho() != null)
                        sb.append(" — ").append(v.getFechaDespacho().format(fmt));
                    if (v.getValorTotal() != null)
                        sb.append(" — $").append(v.getValorTotal().toPlainString());
                    sb.append("\n");
                });
            }
        } catch (Exception e) {
            sb.append("- [Error al cargar datos de ventas]\n");
        }

        return sb.toString();
    }

    private String buildPrompt(String contexto, List<MensajeChatDto> historial, String pregunta) {
        StringBuilder sb = new StringBuilder();
        sb.append("Eres un asistente inteligente de gestión para la cervecería. ");
        sb.append("Responde preguntas sobre producción, inventario, recetas y operaciones. ");
        sb.append("Sé conciso, directo y útil. Si no tienes suficiente información en el contexto, dilo claramente.\n\n");
        sb.append("DATOS ACTUALES DE LA CERVECERÍA:\n");
        sb.append(contexto).append("\n");
        sb.append("---\n\n");

        // Historial (últimas N mensajes)
        List<MensajeChatDto> hist = historial != null ? historial : List.of();
        if (hist.size() > MAX_HISTORY) {
            hist = hist.subList(hist.size() - MAX_HISTORY, hist.size());
        }
        if (!hist.isEmpty()) {
            sb.append("HISTORIAL DE LA CONVERSACIÓN:\n");
            hist.forEach(m -> {
                sb.append(m.getRole().equals("user") ? "Usuario: " : "Asistente: ");
                sb.append(m.getContenido()).append("\n\n");
            });
        }

        sb.append("NUEVA PREGUNTA DEL USUARIO:\n").append(pregunta);
        return sb.toString();
    }

    private MensajeChatDto mockChat(String pregunta, String contexto, String tenantName) {
        String lower = pregunta.toLowerCase();
        String respuesta;

        if (lower.contains("lote") || lower.contains("batch") || lower.contains("cuántos") || lower.contains("cuantos")) {
            long activos = 0;
            try {
                activos = loteRepo.findByPeriodo(null, null).stream()
                        .filter(l -> !l.isCompletado()).count();
            } catch (Exception ignored) {}
            respuesta = "Basándome en los datos actuales, tienes " + activos + " lote(s) activos en producción. " +
                    "Puedo darte más detalles sobre estilos, fases o fechas si me lo indicas.";

        } else if (lower.contains("inventario") || lower.contains("stock") || lower.contains("insumo")) {
            long bajo = 0;
            try {
                bajo = insumoRepo.findAllByOrderByNombreAsc().stream()
                        .filter(InsumoInventario::isBajoStock).count();
            } catch (Exception ignored) {}
            respuesta = bajo > 0
                    ? "Hay " + bajo + " insumo(s) con bajo stock que requieren atención. Te recomiendo revisar el módulo de Inventario para ver el detalle y crear una orden de compra."
                    : "El inventario está en buen estado. No hay insumos reportados bajo el stock mínimo.";

        } else if (lower.contains("fase") || lower.contains("ferment") || lower.contains("carbonat")) {
            respuesta = "Para consultar el estado por fases, puedes revisar el Kanban de producción. " +
                    "Desde ahí tienes una visión completa de qué lotes están en Fermentación, Acondicionamiento, Maduración y Carbonatación.";

        } else if (lower.contains("estilo") || lower.contains("ipa") || lower.contains("stout") || lower.contains("receta")) {
            respuesta = "Los estilos están detallados en el reporte de producción. " +
                    "También puedes ver las recetas activas en el módulo de Recetas.";

        } else if (lower.contains("hola") || lower.contains("buenas") || lower.contains("hey")) {
            respuesta = "¡Hola! Soy el asistente IA de " + tenantName + ". Puedo ayudarte con preguntas sobre tus lotes de producción, inventario, recetas y más. ¿Qué necesitas saber?";

        } else {
            respuesta = "Entiendo tu pregunta sobre \"" + pregunta + "\". Para obtener una respuesta más precisa y personalizada con tus datos reales, configura la API key de Claude en la variable de entorno ANTHROPIC_API_KEY. Con ella, puedo analizar en detalle los datos de " + tenantName + " y darte recomendaciones específicas.";
        }

        return new MensajeChatDto("assistant", respuesta, true);
    }
}
