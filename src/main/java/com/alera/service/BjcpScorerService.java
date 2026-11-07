package com.alera.service;

import com.alera.dto.BjcpScorerDto;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.TextBlockParam;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BjcpScorerService {

    private static final Logger log = LoggerFactory.getLogger(BjcpScorerService.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${app.anthropic.api-key:}")
    private String apiKey;

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public BjcpScorerDto analizar(String notas, String estilo) {
        if (notas == null || notas.isBlank()) {
            var dto = new BjcpScorerDto();
            dto.setError("Las notas de cata no pueden estar vacías.");
            return dto;
        }
        if (!isConfigured()) {
            return mockResponse(notas, estilo);
        }
        try {
            var client = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
            String prompt = construirPrompt(notas, estilo);
            ContentBlockParam bloque = ContentBlockParam.ofText(
                    TextBlockParam.builder().text(prompt).build());
            var response = client.messages().create(
                    MessageCreateParams.builder()
                            .model("claude-haiku-4-5")
                            .maxTokens(1024L)
                            .addUserMessageOfBlockParams(List.of(bloque))
                            .build());
            String texto = response.content().stream()
                    .filter(b -> b.isText())
                    .map(b -> b.asText().text())
                    .findFirst().orElse("{}");
            return parsearRespuesta(texto);
        } catch (Exception ex) {
            log.error("Error al analizar evaluación BJCP", ex);
            var dto = new BjcpScorerDto();
            dto.setError(ex.getMessage());
            return dto;
        }
    }

    private String construirPrompt(String notas, String estilo) {
        var sb = new StringBuilder();
        sb.append("Eres un juez BJCP certificado con experiencia en evaluación de cervezas artesanales. ");
        sb.append("Analiza las siguientes notas de cata en texto libre y proporciona una evaluación estructurada.\n\n");

        if (estilo != null && !estilo.isBlank()) {
            sb.append("**Estilo de la cerveza:** ").append(estilo).append("\n\n");
        }
        sb.append("**Notas de cata del evaluador:**\n").append(notas).append("\n\n");

        sb.append("Basándote en las notas, estima los puntajes BJCP para cada categoría e identifica fortalezas, defectos y recomendaciones. ");
        sb.append("Si las notas mencionan un aspecto positivo asigna puntaje alto en esa categoría; si mencionan defectos, asigna puntaje menor.\n\n");

        sb.append("**Escala BJCP:**\n");
        sb.append("- Aroma: 0–12 (peso mayor: complejidad, limpieza, adecuación al estilo)\n");
        sb.append("- Apariencia: 0–3 (color, claridad, espuma)\n");
        sb.append("- Sabor: 0–20 (el más importante: balance, finish, adecuación al estilo)\n");
        sb.append("- Sensación en boca: 0–5 (cuerpo, carbonatación, astringencia, calor)\n");
        sb.append("- Impresión general: 0–10 (impresión holística, drinkability, complejidad)\n\n");

        sb.append("**Clasificación por puntaje total:**\n");
        sb.append("- 47-50: Excepcional | 38-46: Excelente | 30-37: Muy buena | 21-29: Buena | 14-20: Aceptable | 7-13: Deficiente | 0-6: Inaceptable\n\n");

        sb.append("Responde ÚNICAMENTE con JSON válido, sin markdown:\n");
        sb.append("{\n");
        sb.append("  \"aromaEstimado\": <0-12>,\n");
        sb.append("  \"aparienciaEstimada\": <0-3>,\n");
        sb.append("  \"saborEstimado\": <0-20>,\n");
        sb.append("  \"sensacionBocaEstimada\": <0-5>,\n");
        sb.append("  \"impresionGeneralEstimada\": <0-10>,\n");
        sb.append("  \"clasificacion\": \"<Excepcional|Excelente|Muy buena|Buena|Aceptable|Deficiente|Inaceptable>\",\n");
        sb.append("  \"fortalezas\": [\"<punto fuerte 1>\", \"<punto fuerte 2>\"],\n");
        sb.append("  \"defectos\": [\"<defecto 1 si hay>\"],\n");
        sb.append("  \"recomendaciones\": [\"<mejora sugerida 1>\", \"<mejora sugerida 2>\"]\n");
        sb.append("}\n\n");
        sb.append("Si no hay defectos detectables, usa un array vacío para defectos.");

        return sb.toString();
    }

    private BjcpScorerDto parsearRespuesta(String texto) {
        try {
            String limpio = texto.trim();
            int inicio = limpio.indexOf('{');
            int fin = limpio.lastIndexOf('}');
            if (inicio >= 0 && fin > inicio) {
                limpio = limpio.substring(inicio, fin + 1);
            }
            var node = mapper.readTree(limpio);
            var dto = new BjcpScorerDto();

            if (node.has("aromaEstimado"))            dto.setAromaEstimado(clamp(node.get("aromaEstimado").asInt(), 0, 12));
            if (node.has("aparienciaEstimada"))       dto.setAparienciaEstimada(clamp(node.get("aparienciaEstimada").asInt(), 0, 3));
            if (node.has("saborEstimado"))            dto.setSaborEstimado(clamp(node.get("saborEstimado").asInt(), 0, 20));
            if (node.has("sensacionBocaEstimada"))    dto.setSensacionBocaEstimada(clamp(node.get("sensacionBocaEstimada").asInt(), 0, 5));
            if (node.has("impresionGeneralEstimada")) dto.setImpresionGeneralEstimada(clamp(node.get("impresionGeneralEstimada").asInt(), 0, 10));
            if (node.has("clasificacion"))            dto.setClasificacion(node.get("clasificacion").asText());

            int total = safeInt(dto.getAromaEstimado()) + safeInt(dto.getAparienciaEstimada())
                    + safeInt(dto.getSaborEstimado()) + safeInt(dto.getSensacionBocaEstimada())
                    + safeInt(dto.getImpresionGeneralEstimada());
            dto.setPuntajeTotalEstimado(total);

            if (node.has("fortalezas"))       dto.setFortalezas(toStringList(node.get("fortalezas")));
            if (node.has("defectos"))         dto.setDefectos(toStringList(node.get("defectos")));
            if (node.has("recomendaciones"))  dto.setRecomendaciones(toStringList(node.get("recomendaciones")));
            return dto;
        } catch (Exception ex) {
            log.warn("No se pudo parsear JSON BJCP scorer: {}", texto, ex);
            var dto = new BjcpScorerDto();
            dto.setError("No se pudo interpretar la respuesta de IA.");
            return dto;
        }
    }

    private BjcpScorerDto mockResponse(String notas, String estilo) {
        var dto = new BjcpScorerDto();
        // Detecta palabras clave simples para ajustar scores en el mock
        String lower = notas.toLowerCase();
        int aroma = lower.contains("cítrico") || lower.contains("floral") || lower.contains("frutal") ? 9 : 7;
        int apariencia = lower.contains("turbia") || lower.contains("hazy") ? 2 : 3;
        int sabor = lower.contains("amargo") && lower.contains("balance") ? 15 :
                    lower.contains("dulce") && !lower.contains("balance") ? 11 : 14;
        int boca = lower.contains("cuerpo") && lower.contains("ligero") ? 3 : 4;
        int general = lower.contains("excelente") || lower.contains("excepcional") ? 9 : 7;
        int total = aroma + apariencia + sabor + boca + general;

        dto.setAromaEstimado(aroma);
        dto.setAparienciaEstimada(apariencia);
        dto.setSaborEstimado(sabor);
        dto.setSensacionBocaEstimada(boca);
        dto.setImpresionGeneralEstimada(general);
        dto.setPuntajeTotalEstimado(total);
        dto.setClasificacion(total >= 38 ? "Excelente" : total >= 30 ? "Muy buena" : total >= 21 ? "Buena" : "Aceptable");
        dto.setFortalezas(List.of("Perfil aromático limpio y adecuado al estilo", "Balance satisfactorio entre maltas y lúpulos"));
        dto.setDefectos(lower.contains("astringen") || lower.contains("oxidado") ? List.of("Posible astringencia detectada") : List.of());
        dto.setRecomendaciones(List.of("Revisar temperatura de fermentación para mayor complejidad",
                "Considerar dry-hopping adicional para reforzar el aroma"));
        dto.setModoMock(true);
        return dto;
    }

    private int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

    private int safeInt(Integer val) {
        return val != null ? val : 0;
    }

    private java.util.List<String> toStringList(com.fasterxml.jackson.databind.JsonNode arr) {
        var list = new java.util.ArrayList<String>();
        if (arr != null && arr.isArray()) arr.forEach(n -> list.add(n.asText()));
        return list;
    }
}
