package com.alera.service;

import com.alera.dto.PrediccionFermentacionDto;
import com.alera.model.LecturaFermentacion;
import com.alera.model.LoteCerveza;
import com.alera.repository.LecturaFermentacionRepository;
import com.alera.repository.LoteCervezaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FermentacionPredictivaService {

    private static final Logger log = LoggerFactory.getLogger(FermentacionPredictivaService.class);
    private static final int MAX_HISTORICOS = 5;

    private final IaProviderService iaProvider;
    private final LoteCervezaRepository loteRepo;
    private final LecturaFermentacionRepository lecturaRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FermentacionPredictivaService(IaProviderService iaProvider,
                                         LoteCervezaRepository loteRepo,
                                         LecturaFermentacionRepository lecturaRepo) {
        this.iaProvider  = iaProvider;
        this.loteRepo    = loteRepo;
        this.lecturaRepo = lecturaRepo;
    }

    public boolean isConfigured() {
        return iaProvider.isConfigured();
    }

    public PrediccionFermentacionDto predecir(LoteCerveza lote, List<LecturaFermentacion> lecturas) {
        List<LoteCerveza> historicos = loteRepo.findCompletadosPorEstilo(
                lote.getEstilo(), lote.getId() != null ? lote.getId() : -1L,
                PageRequest.of(0, MAX_HISTORICOS));

        Map<Long, List<LecturaFermentacion>> lecturasHistoricas = new HashMap<>();
        if (!historicos.isEmpty()) {
            var ids = historicos.stream().map(LoteCerveza::getId).collect(Collectors.toList());
            lecturaRepo.findByLoteIdIn(ids).forEach(lec ->
                lecturasHistoricas.computeIfAbsent(lec.getLote().getId(), k -> new ArrayList<>()).add(lec));
        }

        String prompt = construirPrompt(lote, lecturas, historicos, lecturasHistoricas);
        String jsonText = iaProvider.completar(prompt, 2048L);
        if (jsonText == null) {
            log.info("IA no configurada o no disponible — devolviendo predicción de ejemplo");
            return mockResponse(lote, lecturas, historicos);
        }

        try {
            var dto = parsearRespuesta(jsonText);
            dto.setLotesHistoricosUsados(historicos.size());
            dto.setSinHistorial(historicos.isEmpty());
            return dto;
        } catch (Exception e) {
            log.error("Error al parsear predicción de fermentación", e);
            var err = new PrediccionFermentacionDto();
            err.setError("Error al generar la predicción: " + e.getMessage());
            return err;
        }
    }

    private String construirPrompt(LoteCerveza lote,
                                   List<LecturaFermentacion> lecturas,
                                   List<LoteCerveza> historicos,
                                   Map<Long, List<LecturaFermentacion>> lecturasHistoricas) {
        var sb = new StringBuilder();
        sb.append("Eres un maestro cervecero experto con acceso a datos históricos de fermentación.\n");
        sb.append("Tu tarea es PREDECIR el comportamiento futuro de una fermentación activa basándote en datos históricos de lotes similares.\n\n");

        sb.append("═══ LOTE ACTUAL ═══\n");
        sb.append("Código: ").append(lote.getCodigoLote()).append("\n");
        sb.append("Estilo: ").append(lote.getEstilo()).append("\n");
        if (lote.getFechaElaboracion() != null)
            sb.append("Fecha elaboración: ").append(lote.getFechaElaboracion()).append("\n");
        if (lote.getDensidadInicial() != null)
            sb.append("OG (densidad inicial): ").append(lote.getDensidadInicial()).append(" (formato XXXX, ej: 1056)\n");
        if (lote.getDensidadFinal() != null)
            sb.append("FG actual: ").append(lote.getDensidadFinal()).append("\n");
        if (lote.getReceta() != null) {
            var r = lote.getReceta();
            if (r.getFgObjetivo() != null)
                sb.append("FG objetivo receta: ").append(r.getFgObjetivo()).append("\n");
            if (r.getOgObjetivo() != null)
                sb.append("OG objetivo receta: ").append(r.getOgObjetivo()).append("\n");
        }
        if (lote.getFermFechaInicial() != null) {
            long dias = ChronoUnit.DAYS.between(lote.getFermFechaInicial(), LocalDate.now());
            sb.append("Inicio fermentación: ").append(lote.getFermFechaInicial()).append(" (").append(dias).append(" días)\n");
        }
        if (lote.getFermFechaFinalIdeal() != null)
            sb.append("Fecha ideal de fin: ").append(lote.getFermFechaFinalIdeal()).append("\n");
        if (lote.getFermTemperatura() != null)
            sb.append("Temperatura objetivo: ").append(lote.getFermTemperatura()).append(" °C\n");

        var levaduras = lote.getIngredientes().stream()
                .filter(i -> "LEVADURA".equals(i.getTipo() != null ? i.getTipo().name() : ""))
                .map(i -> i.getNombre() + (i.getCantidad() != null ? " " + i.getCantidad() : ""))
                .toList();
        if (!levaduras.isEmpty())
            sb.append("Levaduras: ").append(String.join(", ", levaduras)).append("\n");

        sb.append("\nLECTURAS ACTUALES (cronológicas):\n");
        if (lecturas.isEmpty()) {
            sb.append("Sin lecturas aún.\n");
        } else {
            sb.append("Fecha | Densidad | Temperatura | Notas\n");
            for (var l : lecturas) {
                sb.append(l.getFecha()).append(" | ")
                  .append(l.getDensidad() != null ? l.getDensidad() : "—").append(" | ")
                  .append(l.getTemperatura() != null ? l.getTemperatura() + "°C" : "—").append(" | ")
                  .append(l.getNotas() != null ? l.getNotas() : "").append("\n");
            }
        }

        if (!historicos.isEmpty()) {
            sb.append("\n═══ LOTES HISTÓRICOS DEL MISMO ESTILO (").append(historicos.size()).append(" lotes completados) ═══\n");
            for (var h : historicos) {
                sb.append("\n── Lote: ").append(h.getCodigoLote()).append(" (").append(h.getFechaElaboracion()).append(") ──\n");
                sb.append("OG: ").append(h.getDensidadInicial() != null ? h.getDensidadInicial() : "—").append(" | ");
                sb.append("FG final: ").append(h.getDensidadFinal() != null ? h.getDensidadFinal() : "—").append(" | ");
                if (h.getDensidadInicial() != null && h.getDensidadFinal() != null)
                    sb.append("ABV: ").append(String.format("%.1f", (h.getDensidadInicial() - h.getDensidadFinal()) * 0.13125)).append("%");
                sb.append("\n");
                if (h.getFermFechaInicial() != null && h.getFermFechaFinal() != null) {
                    long duracion = ChronoUnit.DAYS.between(h.getFermFechaInicial(), h.getFermFechaFinal());
                    sb.append("Duración fermentación: ").append(duracion).append(" días\n");
                }
                var lecs = lecturasHistoricas.getOrDefault(h.getId(), List.of());
                if (!lecs.isEmpty()) {
                    sb.append("Curva (").append(lecs.size()).append(" lecturas): ");
                    sb.append(lecs.stream()
                            .filter(l -> l.getDensidad() != null)
                            .map(l -> "día" + ChronoUnit.DAYS.between(
                                    h.getFermFechaInicial() != null ? h.getFermFechaInicial() : l.getFecha(),
                                    l.getFecha()) + "→" + l.getDensidad())
                            .collect(Collectors.joining(", ")));
                    sb.append("\n");
                }
            }
        } else {
            sb.append("\n(Sin historial de lotes del mismo estilo disponible — predecir con conocimiento general del estilo)\n");
        }

        sb.append("""

                ═══ TAREA ═══
                Analiza los datos y genera una predicción cuantitativa del desenlace de esta fermentación.

                Responde ÚNICAMENTE con el siguiente JSON, sin texto adicional ni markdown:
                {
                  "tendencia": "NORMAL",
                  "resumen": "Párrafo de 2-3 oraciones resumiendo el pronóstico.",
                  "fgPredichoMin": 1010,
                  "fgPredichoMax": 1014,
                  "diasRestantes": 4,
                  "fechaEstimadaFin": "2026-11-20",
                  "confianza": 75,
                  "insights": ["Observación 1 basada en historial", "Observación 2"],
                  "recomendaciones": ["Acción recomendada 1", "Acción recomendada 2"],
                  "comparacionHistorica": "Resumen en 1-2 oraciones de qué indican los lotes históricos sobre el comportamiento esperado."
                }

                Reglas:
                - "tendencia": NORMAL (atenuación esperada), LENTA (por debajo del ritmo histórico), RAPIDA (por encima), ATASCADA (densidad sin cambio > 3 días) o INSUFICIENTE (sin lecturas para determinar).
                - "fgPredichoMin/Max": Valores enteros en formato XXXX (ej: 1010-1014). Null si no es posible estimar.
                - "diasRestantes": Días estimados hasta alcanzar FG objetivo. 0 si ya llegó. Null si indeterminado.
                - "fechaEstimadaFin": Fecha en formato YYYY-MM-DD. Null si indeterminado.
                - "confianza": Porcentaje de confianza de la predicción (0-100). Usar 20-40% si sin historial, 60-85% con buen historial.
                - "insights": Máximo 3 observaciones clave extraídas de la comparación histórica. Array vacío si sin historial.
                - Si no hay lecturas aún, tender = "INSUFICIENTE" y confianza <= 30.
                """);

        return sb.toString();
    }

    private PrediccionFermentacionDto parsearRespuesta(String json) throws Exception {
        String limpio = json.trim();
        if (limpio.startsWith("```"))
            limpio = limpio.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("```$", "").trim();

        int i = limpio.indexOf('{'), j = limpio.lastIndexOf('}');
        if (i >= 0 && j > i) limpio = limpio.substring(i, j + 1);

        JsonNode root = objectMapper.readTree(limpio);
        var dto = new PrediccionFermentacionDto();
        dto.setTendencia(textoOrDefault(root, "tendencia", "INSUFICIENTE"));
        dto.setResumen(textoOrNull(root, "resumen"));
        dto.setFgPredichoMin(intOrNull(root, "fgPredichoMin"));
        dto.setFgPredichoMax(intOrNull(root, "fgPredichoMax"));
        dto.setDiasRestantes(intOrNull(root, "diasRestantes"));
        dto.setFechaEstimadaFin(textoOrNull(root, "fechaEstimadaFin"));
        dto.setConfianza(intOrNull(root, "confianza"));
        dto.setInsights(parsearLista(root, "insights"));
        dto.setRecomendaciones(parsearLista(root, "recomendaciones"));
        dto.setComparacionHistorica(textoOrNull(root, "comparacionHistorica"));
        return dto;
    }

    private PrediccionFermentacionDto mockResponse(LoteCerveza lote,
                                                    List<LecturaFermentacion> lecturas,
                                                    List<LoteCerveza> historicos) {
        var dto = new PrediccionFermentacionDto();
        dto.setModoMock(true);
        dto.setLotesHistoricosUsados(historicos.size());
        dto.setSinHistorial(historicos.isEmpty());

        if (lecturas.isEmpty()) {
            dto.setTendencia("INSUFICIENTE");
            dto.setConfianza(20);
            dto.setResumen("Sin lecturas de densidad registradas aún. Es imposible predecir el comportamiento de la fermentación sin datos de atenuación. Registra la primera lectura a las 24–48 h del inicio.");
            dto.setFgPredichoMin(lote.getReceta() != null && lote.getReceta().getFgObjetivo() != null
                    ? lote.getReceta().getFgObjetivo() - 2 : 1010);
            dto.setFgPredichoMax(lote.getReceta() != null && lote.getReceta().getFgObjetivo() != null
                    ? lote.getReceta().getFgObjetivo() + 3 : 1016);
            dto.setInsights(List.of());
            dto.setRecomendaciones(List.of(
                    "Registra la primera lectura de densidad a las 24-48 h del inicio",
                    "Anota también la temperatura del fermentador en cada lectura"
            ));
            return dto;
        }

        var ultima = lecturas.get(lecturas.size() - 1);
        boolean hayAtenuacion = ultima.getDensidad() != null
                && lote.getDensidadInicial() != null
                && ultima.getDensidad() < lote.getDensidadInicial();

        int fgEst = lote.getDensidadInicial() != null ? (int)(lote.getDensidadInicial() * 0.74) : 1012;
        int ogActual = lote.getDensidadInicial() != null ? lote.getDensidadInicial() : 1050;

        boolean estancada = lecturas.size() >= 2
                && Objects.equals(lecturas.get(lecturas.size() - 1).getDensidad(),
                                  lecturas.get(lecturas.size() - 2).getDensidad());

        if (estancada && ultima.getDensidad() != null) {
            dto.setTendencia("ATASCADA");
            dto.setConfianza(70);
            dto.setFgPredichoMin(ultima.getDensidad());
            dto.setFgPredichoMax(ultima.getDensidad() + 2);
            dto.setDiasRestantes(null);
            dto.setResumen("Las últimas dos lecturas muestran densidad estable sin atenuación. Esto puede indicar fermentación completa o un arranque lento de la levadura. Se recomienda verificar si se ha alcanzado el FG objetivo antes de concluir.");
            dto.setInsights(historicos.isEmpty() ? List.of() : List.of(
                    "Lotes históricos del mismo estilo finalizaron con FG entre " + (fgEst - 3) + " y " + (fgEst + 3)
            ));
            dto.setRecomendaciones(List.of(
                    "Tomar 2 lecturas consecutivas con 48 h de diferencia para confirmar FG final",
                    "Verificar temperatura del fermentador — si bajó puede haber dormido la levadura",
                    "Si la densidad actual está cerca del FG objetivo, puede estar completa"
            ));
        } else if (hayAtenuacion) {
            int atenuado = ogActual - ultima.getDensidad();
            int diasFerm = lote.getFermFechaInicial() != null
                    ? (int) ChronoUnit.DAYS.between(lote.getFermFechaInicial(), LocalDate.now()) : 5;
            int diasEst = diasFerm > 0 ? Math.max(2, (int)((ogActual - fgEst) / ((double) atenuado / diasFerm)) - diasFerm) : 5;

            dto.setTendencia("NORMAL");
            dto.setConfianza(historicos.isEmpty() ? 50 : 72);
            dto.setFgPredichoMin(fgEst - 2);
            dto.setFgPredichoMax(fgEst + 4);
            dto.setDiasRestantes(diasEst);
            dto.setFechaEstimadaFin(LocalDate.now().plusDays(diasEst).toString());
            dto.setResumen("La fermentación avanza con normalidad. Se han atenuado " + atenuado + " puntos de gravedad en " + diasFerm + " días. A este ritmo se estima alcanzar el FG objetivo en aproximadamente " + diasEst + " días más.");
            dto.setInsights(historicos.isEmpty() ? List.of() : List.of(
                    historicos.size() + " lotes históricos del mismo estilo muestran un FG típico de " + (fgEst - 2) + "–" + (fgEst + 4),
                    "El ritmo de atenuación actual es consistente con el histórico"
            ));
            dto.setRecomendaciones(List.of(
                    "Continuar tomando lecturas cada 48 h",
                    "Mantener temperatura estable en el rango objetivo de la levadura"
            ));
        } else {
            dto.setTendencia("LENTA");
            dto.setConfianza(45);
            dto.setFgPredichoMin(fgEst - 3);
            dto.setFgPredichoMax(fgEst + 5);
            dto.setResumen("La fermentación no muestra atenuación significativa todavía. Puede ser normal en las primeras 24–48 h, pero si persiste pasado ese período puede indicar arranque lento o problemas con la levadura.");
            dto.setInsights(List.of());
            dto.setRecomendaciones(List.of(
                    "Verificar temperatura — debe estar en el rango óptimo de la levadura",
                    "Confirmar que hubo actividad en el airlock en las primeras 24 h",
                    "Si han pasado más de 72 h sin atenuación, considerar repitching"
            ));
        }

        if (!historicos.isEmpty())
            dto.setComparacionHistorica("Basado en " + historicos.size() + " lotes previos de " + lote.getEstilo()
                    + " de este tenant. FG histórico promedio estimado: ~" + fgEst + ".");

        return dto;
    }

    private List<String> parsearLista(JsonNode root, String campo) {
        List<String> lista = new ArrayList<>();
        if (root.has(campo) && root.get(campo).isArray()) {
            for (JsonNode n : root.get(campo)) {
                if (!n.isNull()) lista.add(n.asText());
            }
        }
        return lista;
    }

    private String textoOrNull(JsonNode n, String campo) {
        return (n.has(campo) && !n.get(campo).isNull()) ? n.get(campo).asText() : null;
    }

    private String textoOrDefault(JsonNode n, String campo, String def) {
        String v = textoOrNull(n, campo);
        return v != null ? v : def;
    }

    private Integer intOrNull(JsonNode n, String campo) {
        if (!n.has(campo) || n.get(campo).isNull()) return null;
        try { return n.get(campo).asInt(); } catch (Exception e) { return null; }
    }
}
