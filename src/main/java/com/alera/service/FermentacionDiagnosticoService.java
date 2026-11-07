package com.alera.service;

import com.alera.dto.FermentacionDiagnosticoDto;
import com.alera.model.LecturaFermentacion;
import com.alera.model.LoteCerveza;
import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.TextBlockParam;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class FermentacionDiagnosticoService {

    private static final Logger log = LoggerFactory.getLogger(FermentacionDiagnosticoService.class);

    @Value("${app.anthropic.api-key:}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public FermentacionDiagnosticoDto diagnosticar(LoteCerveza lote, List<LecturaFermentacion> lecturas) {
        if (!isConfigured()) {
            log.info("ANTHROPIC_API_KEY no configurada — devolviendo diagnóstico de ejemplo");
            return mockResponse(lote, lecturas);
        }

        try {
            String prompt = construirPrompt(lote, lecturas);

            TextBlockParam textoParam = TextBlockParam.builder().text(prompt).build();
            ContentBlockParam textoBlock = ContentBlockParam.ofText(textoParam);

            AnthropicClient client = AnthropicOkHttpClient.builder()
                    .apiKey(apiKey)
                    .build();

            Message response = client.messages().create(
                    MessageCreateParams.builder()
                            .model("claude-haiku-4-5")
                            .maxTokens(2048L)
                            .addUserMessageOfBlockParams(List.of(textoBlock))
                            .build());

            String jsonText = response.content().stream()
                    .filter(b -> b.isText())
                    .map(b -> b.asText().text())
                    .findFirst()
                    .orElse("{}");

            return parsearRespuesta(jsonText);

        } catch (Exception e) {
            log.error("Error al diagnosticar fermentación con Claude", e);
            FermentacionDiagnosticoDto err = new FermentacionDiagnosticoDto();
            err.setError("Error al generar el diagnóstico: " + e.getMessage());
            return err;
        }
    }

    private String construirPrompt(LoteCerveza lote, List<LecturaFermentacion> lecturas) {
        var sb = new StringBuilder();

        sb.append("Eres un maestro cervecero experto analizando la fermentación de un lote de cerveza artesanal.\n\n");
        sb.append("DATOS DEL LOTE:\n");
        sb.append("- Código: ").append(lote.getCodigoLote()).append("\n");
        sb.append("- Estilo: ").append(lote.getEstilo()).append("\n");
        if (lote.getFechaElaboracion() != null)
            sb.append("- Fecha elaboración: ").append(lote.getFechaElaboracion()).append("\n");
        if (lote.getDensidadInicial() != null)
            sb.append("- OG real: ").append(lote.getDensidadInicial()).append(" (formato XXXX)\n");
        if (lote.getDensidadFinal() != null)
            sb.append("- FG actual: ").append(lote.getDensidadFinal()).append(" (formato XXXX)\n");

        if (lote.getReceta() != null) {
            var r = lote.getReceta();
            if (r.getOgObjetivo() != null) sb.append("- OG objetivo receta: ").append(r.getOgObjetivo()).append("\n");
            if (r.getFgObjetivo() != null) sb.append("- FG objetivo receta: ").append(r.getFgObjetivo()).append("\n");
        }

        if (lote.getFermFechaInicial() != null) {
            sb.append("- Inicio fermentación: ").append(lote.getFermFechaInicial()).append("\n");
            long diasFerm = ChronoUnit.DAYS.between(lote.getFermFechaInicial(), LocalDate.now());
            sb.append("- Días en fermentación: ").append(diasFerm).append("\n");
        }
        if (lote.getFermFechaFinalIdeal() != null)
            sb.append("- Fecha ideal de fin: ").append(lote.getFermFechaFinalIdeal()).append("\n");
        if (lote.getFermTemperatura() != null)
            sb.append("- Temperatura objetivo fermentación: ").append(lote.getFermTemperatura()).append(" °C\n");

        // Levaduras del lote
        var levaduras = lote.getIngredientes().stream()
                .filter(i -> "LEVADURA".equals(i.getTipo() != null ? i.getTipo().name() : ""))
                .map(i -> i.getNombre() + (i.getCantidad() != null ? " " + i.getCantidad() : ""))
                .toList();
        if (!levaduras.isEmpty())
            sb.append("- Levaduras: ").append(String.join(", ", levaduras)).append("\n");

        if (lote.getNotasFermentacion() != null && !lote.getNotasFermentacion().isBlank())
            sb.append("- Notas fermentación: ").append(lote.getNotasFermentacion()).append("\n");

        sb.append("\nLECTURAS DE DENSIDAD Y TEMPERATURA (cronológicas):\n");
        if (lecturas.isEmpty()) {
            sb.append("Sin lecturas registradas aún.\n");
        } else {
            sb.append("Fecha | Densidad | Temperatura | Notas\n");
            for (var l : lecturas) {
                sb.append(l.getFecha())
                  .append(" | ")
                  .append(l.getDensidad() != null ? l.getDensidad() : "—")
                  .append(" | ")
                  .append(l.getTemperatura() != null ? l.getTemperatura() + "°C" : "—")
                  .append(" | ")
                  .append(l.getNotas() != null ? l.getNotas() : "")
                  .append("\n");
            }
        }

        sb.append("""

                TAREA: Analiza si la fermentación es normal o tiene anomalías. Considera:
                - Velocidad de atenuación (caída de densidad)
                - Estancamiento (densidad sin cambio en más de 3 días)
                - Variaciones de temperatura fuera del rango esperado
                - Tiempo transcurrido vs tiempo ideal
                - Distancia entre FG actual y FG objetivo

                Responde ÚNICAMENTE con el siguiente JSON, sin texto adicional ni markdown:
                {
                  "estado": "normal",
                  "resumen": "Párrafo de 2-3 oraciones resumiendo el estado de la fermentación.",
                  "anomalias": ["Anomalía 1 detectada", "Anomalía 2 detectada"],
                  "recomendaciones": ["Acción recomendada 1", "Acción recomendada 2"],
                  "pronosticoFg": "Estimado de FG final basado en la tendencia actual (ej: '1012-1014 en 3-5 días')"
                }

                Si no hay anomalías, deja el array "anomalias" vacío [].
                El campo "estado" debe ser: "normal" (todo bien), "advertencia" (hay algo a monitorear) o "critico" (requiere acción inmediata).
                """);

        return sb.toString();
    }

    private FermentacionDiagnosticoDto parsearRespuesta(String json) throws Exception {
        String jsonLimpio = json.trim();
        if (jsonLimpio.startsWith("```")) {
            jsonLimpio = jsonLimpio.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("```$", "").trim();
        }

        JsonNode root = objectMapper.readTree(jsonLimpio);
        FermentacionDiagnosticoDto dto = new FermentacionDiagnosticoDto();

        dto.setEstado(textoOrDefault(root, "estado", "normal"));
        dto.setResumen(textoOrNull(root, "resumen"));
        dto.setPronosticoFg(textoOrNull(root, "pronosticoFg"));
        dto.setAnomalias(parsearLista(root, "anomalias"));
        dto.setRecomendaciones(parsearLista(root, "recomendaciones"));
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

    private FermentacionDiagnosticoDto mockResponse(LoteCerveza lote, List<LecturaFermentacion> lecturas) {
        FermentacionDiagnosticoDto dto = new FermentacionDiagnosticoDto();
        dto.setModoMock(true);

        if (lecturas.isEmpty()) {
            dto.setEstado("advertencia");
            dto.setResumen("El lote " + lote.getCodigoLote() + " aún no tiene lecturas de fermentación registradas. Registra la primera lectura para comenzar el monitoreo de la curva.");
            dto.setAnomalias(List.of("Sin lecturas de densidad/temperatura registradas"));
            dto.setRecomendaciones(List.of(
                "Registrar la primera lectura de densidad a las 24-48 h del inicio",
                "Verificar que la fermentación haya arrancado (actividad visible en airlock)"
            ));
            dto.setPronosticoFg("No es posible estimar sin lecturas");
            return dto;
        }

        // Analizar la última lectura vs OG
        var ultima = lecturas.get(lecturas.size() - 1);
        boolean hayAtenuacion = ultima.getDensidad() != null
                && lote.getDensidadInicial() != null
                && ultima.getDensidad() < lote.getDensidadInicial();

        if (hayAtenuacion) {
            int atenuacion = lote.getDensidadInicial() - ultima.getDensidad();
            dto.setEstado("normal");
            dto.setResumen("La fermentación del lote " + lote.getCodigoLote() + " muestra una progresión normal. Se han atenuado " + atenuacion + " puntos de gravedad desde el OG inicial, lo que indica actividad levadura adecuada. Continuar monitoreo cada 48 h.");
            dto.setAnomalias(List.of());
            dto.setRecomendaciones(List.of(
                "Continuar tomando lecturas cada 48 h",
                "Verificar que la temperatura se mantenga estable en el rango objetivo"
            ));
            int fgEst = lote.getDensidadInicial() != null ? (int)(lote.getDensidadInicial() * 0.78) : 1012;
            dto.setPronosticoFg("~" + fgEst + " en 4-7 días según la tendencia actual");
        } else {
            dto.setEstado("advertencia");
            dto.setResumen("La fermentación del lote " + lote.getCodigoLote() + " no muestra atenuación significativa todavía. Esto puede ser normal en las primeras 24-48 h, pero si persiste pasado ese período puede indicar un arranque lento de la levadura.");
            dto.setAnomalias(List.of("Sin atenuación de densidad detectada en las lecturas disponibles"));
            dto.setRecomendaciones(List.of(
                "Verificar temperatura del fermentador (debe estar en el rango óptimo de la levadura)",
                "Confirmar que la levadura fue rehidratada o starter preparado correctamente",
                "Si han pasado más de 72 h sin actividad, considerar repitching"
            ));
            dto.setPronosticoFg("Indeterminado — requiere más lecturas");
        }
        return dto;
    }
}
