package com.alera.service;

import com.alera.dto.DescripcionComercialDto;
import com.alera.model.EvaluacionSensorial;
import com.alera.model.Ingrediente;
import com.alera.model.LoteCerveza;
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
import java.util.stream.Collectors;

@Service
public class DescripcionComercialService {

    private static final Logger log = LoggerFactory.getLogger(DescripcionComercialService.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${app.anthropic.api-key:}")
    private String apiKey;

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public DescripcionComercialDto generar(LoteCerveza lote, List<EvaluacionSensorial> evaluaciones) {
        if (!isConfigured()) {
            return mockResponse(lote);
        }
        try {
            var client = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
            String prompt = construirPrompt(lote, evaluaciones);
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
            log.error("Error al generar descripción comercial", ex);
            var dto = new DescripcionComercialDto();
            dto.setError(ex.getMessage());
            return dto;
        }
    }

    private String construirPrompt(LoteCerveza lote, List<EvaluacionSensorial> evaluaciones) {
        var sb = new StringBuilder();
        sb.append("Eres un experto en marketing de cerveza artesanal y sommelier certificado. ");
        sb.append("Genera descripciones comerciales atractivas y auténticas para la siguiente cerveza.\n\n");

        sb.append("## Datos del lote\n");
        sb.append("- Código: ").append(lote.getCodigoLote()).append("\n");
        sb.append("- Estilo: ").append(lote.getEstilo()).append("\n");
        if (lote.getAbv() != null) {
            sb.append("- ABV: ").append(String.format("%.1f", lote.getAbv())).append("%\n");
        }
        if (lote.getDensidadInicial() != null) {
            sb.append("- Densidad original (OG): ").append(lote.getDensidadInicial()).append("\n");
        }
        if (lote.getDensidadFinal() != null) {
            sb.append("- Densidad final (FG): ").append(lote.getDensidadFinal()).append("\n");
        }
        if (lote.getLitrosFinales() != null) {
            sb.append("- Volumen producido: ").append(lote.getLitrosFinales()).append(" L\n");
        }

        List<Ingrediente> maltas = lote.getMaltas();
        if (!maltas.isEmpty()) {
            sb.append("- Maltas: ").append(
                    maltas.stream().map(Ingrediente::getNombre).collect(Collectors.joining(", "))
            ).append("\n");
        }
        List<Ingrediente> lupulos = lote.getLupulos();
        if (!lupulos.isEmpty()) {
            sb.append("- Lúpulos: ").append(
                    lupulos.stream().map(Ingrediente::getNombre).collect(Collectors.joining(", "))
            ).append("\n");
        }
        List<Ingrediente> levaduras = lote.getLevaduras();
        if (!levaduras.isEmpty()) {
            sb.append("- Levaduras: ").append(
                    levaduras.stream().map(Ingrediente::getNombre).collect(Collectors.joining(", "))
            ).append("\n");
        }

        if (lote.getNotasAcondicionamiento() != null && !lote.getNotasAcondicionamiento().isBlank()) {
            sb.append("- Notas del maestro cervecero: ").append(lote.getNotasAcondicionamiento()).append("\n");
        } else if (lote.getNotasFermentacion() != null && !lote.getNotasFermentacion().isBlank()) {
            sb.append("- Notas del maestro cervecero: ").append(lote.getNotasFermentacion()).append("\n");
        }

        if (!evaluaciones.isEmpty()) {
            var ev = evaluaciones.get(0);
            if (ev.getNotas() != null && !ev.getNotas().isBlank()) {
                sb.append("- Notas de cata: ").append(ev.getNotas()).append("\n");
            }
            if (ev.getPuntajeTotal() != null) {
                sb.append("- Puntaje BJCP: ").append(ev.getPuntajeTotal()).append("/50\n");
            }
        }

        sb.append("\n## Formato de respuesta requerido (JSON estricto)\n");
        sb.append("Responde ÚNICAMENTE con un objeto JSON válido, sin markdown ni explicaciones adicionales:\n");
        sb.append("{\n");
        sb.append("  \"descripcionCorta\": \"<1-2 oraciones para menú de bar o ficha de venta>\",\n");
        sb.append("  \"descripcionLarga\": \"<3-4 oraciones para página web o catálogo — estilo, proceso, sabores, maridajes sugeridos>\",\n");
        sb.append("  \"textoEtiqueta\": \"<máximo 60 caracteres para etiqueta de botella>\",\n");
        sb.append("  \"hashtags\": [\"#hashtag1\", \"#hashtag2\", \"#hashtag3\", \"#hashtag4\", \"#hashtag5\"]\n");
        sb.append("}\n\n");
        sb.append("El idioma debe ser español. Usa adjetivos sensoriales evocadores. Menciona el estilo y ABV.");

        return sb.toString();
    }

    private DescripcionComercialDto parsearRespuesta(String texto) {
        try {
            String limpio = texto.trim();
            int inicio = limpio.indexOf('{');
            int fin = limpio.lastIndexOf('}');
            if (inicio >= 0 && fin > inicio) {
                limpio = limpio.substring(inicio, fin + 1);
            }
            var node = mapper.readTree(limpio);
            var dto = new DescripcionComercialDto();
            if (node.has("descripcionCorta"))  dto.setDescripcionCorta(node.get("descripcionCorta").asText());
            if (node.has("descripcionLarga"))  dto.setDescripcionLarga(node.get("descripcionLarga").asText());
            if (node.has("textoEtiqueta"))     dto.setTextoEtiqueta(node.get("textoEtiqueta").asText());
            if (node.has("hashtags")) {
                var arr = node.get("hashtags");
                var tags = new java.util.ArrayList<String>();
                arr.forEach(t -> tags.add(t.asText()));
                dto.setHashtags(tags);
            }
            return dto;
        } catch (Exception ex) {
            log.warn("No se pudo parsear JSON de descripción comercial: {}", texto, ex);
            var dto = new DescripcionComercialDto();
            dto.setDescripcionCorta(texto);
            return dto;
        }
    }

    private DescripcionComercialDto mockResponse(LoteCerveza lote) {
        var dto = new DescripcionComercialDto();
        String estilo = lote.getEstilo() != null ? lote.getEstilo() : "Artesanal";
        String abv = lote.getAbv() != null ? String.format("%.1f", lote.getAbv()) + "% ABV" : "";
        dto.setDescripcionCorta("Una " + estilo + " de carácter definido" +
                (abv.isBlank() ? "" : " con " + abv) +
                ", elaborada con lúpulos seleccionados que aportan notas cítricas y resinosas sobre una base maltera equilibrada.");
        dto.setDescripcionLarga("Esta " + estilo +
                " representa lo mejor de la tradición artesanal con un toque contemporáneo. " +
                "Elaborada con una selección cuidadosa de maltas que aportan cuerpo y color, " +
                "y lúpulos que entregan un amargor limpio con aromas florales y frutales. " +
                "Perfecta para acompañar carnes a la parrilla, quesos maduros o simplemente disfrutar en buena compañía.");
        dto.setTextoEtiqueta(estilo + (abv.isBlank() ? "" : " · " + abv));
        dto.setHashtags(List.of("#CervezaArtesanal", "#" + estilo.replace(" ", ""),
                "#Cervecería", "#CraftBeer", "#Zymós"));
        dto.setModoMock(true);
        return dto;
    }
}
