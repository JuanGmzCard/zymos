package com.alera.service;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class IaProviderService {

    private static final Logger log = LoggerFactory.getLogger(IaProviderService.class);
    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash:generateContent";
    private static final String ANTHROPIC_MODEL = "claude-haiku-4-5";

    @Value("${app.ia.provider:auto}")
    private String providerConfig;

    @Value("${app.anthropic.api-key:}")
    private String anthropicKey;

    @Value("${app.gemini.api-key:}")
    private String geminiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public enum Proveedor { ANTHROPIC, GEMINI, MOCK }

    public Proveedor resolverProveedor() {
        return switch (providerConfig.toLowerCase()) {
            case "anthropic" -> anthropicKey.isBlank() ? Proveedor.MOCK : Proveedor.ANTHROPIC;
            case "gemini"    -> geminiKey.isBlank()    ? Proveedor.MOCK : Proveedor.GEMINI;
            case "mock"      -> Proveedor.MOCK;
            default -> {
                if (!anthropicKey.isBlank()) yield Proveedor.ANTHROPIC;
                if (!geminiKey.isBlank())    yield Proveedor.GEMINI;
                yield Proveedor.MOCK;
            }
        };
    }

    public boolean isConfigured() {
        return resolverProveedor() != Proveedor.MOCK;
    }

    public String getNombreProveedor() {
        return resolverProveedor().name().toLowerCase();
    }

    /**
     * Envía un prompt de texto al proveedor configurado.
     * Retorna null si el proveedor está en MOCK o si ocurre error/límite (con log de warning).
     */
    public String completar(String prompt, long maxTokens) {
        Proveedor proveedor = resolverProveedor();
        if (proveedor == Proveedor.MOCK) return null;
        try {
            return switch (proveedor) {
                case ANTHROPIC -> completarAnthropic(prompt, maxTokens);
                case GEMINI    -> completarGemini(prompt, maxTokens);
                default        -> null;
            };
        } catch (Exception e) {
            registrarError(proveedor, e);
            return null;
        }
    }

    /**
     * Envía un prompt con un archivo (imagen o PDF) al proveedor configurado.
     * Retorna null si el proveedor está en MOCK o si ocurre error/límite.
     */
    public String completarConArchivo(String prompt, String base64Data, String mimeType, long maxTokens) {
        Proveedor proveedor = resolverProveedor();
        if (proveedor == Proveedor.MOCK) return null;
        try {
            return switch (proveedor) {
                case ANTHROPIC -> completarConArchivoAnthropic(prompt, base64Data, mimeType, maxTokens);
                case GEMINI    -> completarConArchivoGemini(prompt, base64Data, mimeType, maxTokens);
                default        -> null;
            };
        } catch (Exception e) {
            registrarError(proveedor, e);
            return null;
        }
    }

    // ── Anthropic ────────────────────────────────────────────────────────────────

    private String completarAnthropic(String prompt, long maxTokens) {
        var client = AnthropicOkHttpClient.builder().apiKey(anthropicKey).build();
        var response = client.messages().create(
                MessageCreateParams.builder()
                        .model(ANTHROPIC_MODEL)
                        .maxTokens(maxTokens)
                        .addUserMessageOfBlockParams(List.of(
                                ContentBlockParam.ofText(TextBlockParam.builder().text(prompt).build())))
                        .build());
        return response.content().stream()
                .filter(ContentBlock::isText)
                .map(b -> b.asText().text())
                .findFirst().orElse("{}");
    }

    private String completarConArchivoAnthropic(String prompt, String base64Data,
                                                 String mimeType, long maxTokens) {
        ContentBlockParam archivoBlock;
        if ("application/pdf".equals(mimeType)) {
            archivoBlock = ContentBlockParam.ofDocument(
                    DocumentBlockParam.builder()
                            .source(Base64PdfSource.builder().data(base64Data).build())
                            .build());
        } else {
            Base64ImageSource.MediaType tipo = resolverMediaTypeAnthropic(mimeType);
            archivoBlock = ContentBlockParam.ofImage(
                    ImageBlockParam.builder()
                            .source(Base64ImageSource.builder()
                                    .data(base64Data).mediaType(tipo).build())
                            .build());
        }
        ContentBlockParam textoBlock = ContentBlockParam.ofText(
                TextBlockParam.builder().text(prompt).build());
        var client = AnthropicOkHttpClient.builder().apiKey(anthropicKey).build();
        var response = client.messages().create(
                MessageCreateParams.builder()
                        .model(ANTHROPIC_MODEL)
                        .maxTokens(maxTokens)
                        .addUserMessageOfBlockParams(List.of(archivoBlock, textoBlock))
                        .build());
        return response.content().stream()
                .filter(ContentBlock::isText)
                .map(b -> b.asText().text())
                .findFirst().orElse("{}");
    }

    private Base64ImageSource.MediaType resolverMediaTypeAnthropic(String mimeType) {
        if (mimeType == null) return Base64ImageSource.MediaType.IMAGE_JPEG;
        return switch (mimeType.toLowerCase()) {
            case "image/png"  -> Base64ImageSource.MediaType.IMAGE_PNG;
            case "image/gif"  -> Base64ImageSource.MediaType.IMAGE_GIF;
            case "image/webp" -> Base64ImageSource.MediaType.IMAGE_WEBP;
            default           -> Base64ImageSource.MediaType.IMAGE_JPEG;
        };
    }

    // ── Gemini (REST directo) ─────────────────────────────────────────────────────

    private String completarGemini(String prompt, long maxTokens) throws Exception {
        Map<String, Object> part = new LinkedHashMap<>();
        part.put("text", prompt);
        return llamarGemini(List.of(part), maxTokens);
    }

    private String completarConArchivoGemini(String prompt, String base64Data,
                                              String mimeType, long maxTokens) throws Exception {
        Map<String, Object> partTexto = new LinkedHashMap<>();
        partTexto.put("text", prompt);

        Map<String, Object> inlineData = new LinkedHashMap<>();
        inlineData.put("mime_type", mimeType);
        inlineData.put("data", base64Data);

        Map<String, Object> partArchivo = new LinkedHashMap<>();
        partArchivo.put("inline_data", inlineData);

        return llamarGemini(List.of(partTexto, partArchivo), maxTokens);
    }

    private String llamarGemini(List<Map<String, Object>> parts, long maxTokens) throws Exception {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("role", "user");
        content.put("parts", parts);

        Map<String, Object> genConfig = new LinkedHashMap<>();
        genConfig.put("maxOutputTokens", maxTokens);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents", List.of(content));
        body.put("generationConfig", genConfig);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        String url = GEMINI_URL + "?key=" + geminiKey;
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        JsonNode root = mapper.readTree(response.getBody());
        return root.at("/candidates/0/content/parts/0/text").asText("{}");
    }

    // ── Utilitarios ───────────────────────────────────────────────────────────────

    private void registrarError(Proveedor proveedor, Exception e) {
        if (esLimiteAlcanzado(e)) {
            log.warn("[IA:{}] Límite de tasa o cuota alcanzada — se usará respuesta mock. Detalle: {}",
                    proveedor.name(), e.getMessage());
        } else {
            log.error("[IA:{}] Error inesperado al llamar la API", proveedor.name(), e);
        }
    }

    private boolean esLimiteAlcanzado(Exception e) {
        if (e instanceof HttpClientErrorException httpEx) {
            return httpEx.getStatusCode().value() == 429;
        }
        if (e instanceof HttpServerErrorException httpEx) {
            return httpEx.getStatusCode().value() == 503;
        }
        String msg = e.getMessage();
        if (msg == null) return false;
        return msg.contains("429") || msg.contains("503")
                || msg.contains("RESOURCE_EXHAUSTED") || msg.contains("UNAVAILABLE")
                || msg.contains("rate_limit") || msg.contains("quota");
    }
}
