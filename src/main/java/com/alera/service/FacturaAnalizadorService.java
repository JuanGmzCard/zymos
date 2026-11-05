package com.alera.service;

import com.alera.dto.FacturaAnalizadaDto;
import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.Base64PdfSource;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.DocumentBlockParam;
import com.anthropic.models.messages.ImageBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.TextBlockParam;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class FacturaAnalizadorService {

    private static final Logger log = LoggerFactory.getLogger(FacturaAnalizadorService.class);

    private static final String PROMPT = """
            Analiza esta factura de proveedor y extrae los datos en formato JSON estricto.
            Responde ÚNICAMENTE con el JSON, sin texto adicional ni markdown.

            Formato requerido:
            {
              "proveedor": "Nombre del proveedor",
              "numeroFactura": "FAC-001",
              "fechaFactura": "YYYY-MM-DD",
              "descripcion": "descripción breve opcional",
              "costoEnvio": 0.0,
              "items": [
                {
                  "nombre": "Nombre del ítem",
                  "tipoItem": "INSUMO",
                  "tipoInsumo": "MALTA",
                  "cantidad": 25.0,
                  "unidad": "kg",
                  "valorUnitario": 8500.0,
                  "porcentajeIvaItem": 19.0
                }
              ]
            }

            Reglas:
            - tipoItem: "INSUMO" para materias primas/insumos, "EQUIPO" para equipos, "SERVICIO" para servicios, "OTRO" si no aplica
            - tipoInsumo (solo si tipoItem=INSUMO): "MALTA", "LUPULO", "LEVADURA", "QUIMICO", "ENVASE", "OTRO"
            - fechaFactura: formato ISO YYYY-MM-DD. Si no hay fecha, usa null
            - costoEnvio: 0.0 si no aparece en la factura
            - porcentajeIvaItem: porcentaje numérico (ej: 19.0, 0.0). Si no aplica, usa 0.0
            - cantidad y valorUnitario: valores numéricos sin símbolo de moneda
            - Si algún dato no está en la factura, usa null para strings y 0.0 para números
            """;

    @Value("${app.anthropic.api-key:}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public FacturaAnalizadaDto analizar(MultipartFile archivo) {
        if (!isConfigured()) {
            log.info("ANTHROPIC_API_KEY no configurada — devolviendo datos de ejemplo");
            return mockResponse();
        }

        try {
            String base64 = Base64.getEncoder().encodeToString(archivo.getBytes());
            String contentType = archivo.getContentType();

            ContentBlockParam documentoBlock;
            if ("application/pdf".equals(contentType)) {
                DocumentBlockParam docParam = DocumentBlockParam.builder()
                        .source(Base64PdfSource.builder().data(base64).build())
                        .build();
                documentoBlock = ContentBlockParam.ofDocument(docParam);
            } else {
                Base64ImageSource.MediaType mediaType = resolverMediaType(contentType);
                ImageBlockParam imageParam = ImageBlockParam.builder()
                        .source(Base64ImageSource.builder()
                                .data(base64)
                                .mediaType(mediaType)
                                .build())
                        .build();
                documentoBlock = ContentBlockParam.ofImage(imageParam);
            }

            TextBlockParam textoParam = TextBlockParam.builder().text(PROMPT).build();
            ContentBlockParam textoBlock = ContentBlockParam.ofText(textoParam);

            AnthropicClient client = AnthropicOkHttpClient.builder()
                    .apiKey(apiKey)
                    .build();

            Message response = client.messages().create(
                    MessageCreateParams.builder()
                            .model("claude-haiku-4-5")
                            .maxTokens(2048L)
                            .addUserMessageOfBlockParams(List.of(documentoBlock, textoBlock))
                            .build());

            String jsonText = response.content().stream()
                    .filter(b -> b.isText())
                    .map(b -> b.asText().text())
                    .findFirst()
                    .orElse("{}");

            return parsearRespuesta(jsonText);

        } catch (Exception e) {
            log.error("Error al analizar factura con Claude", e);
            FacturaAnalizadaDto err = new FacturaAnalizadaDto();
            err.setError("Error al procesar el documento: " + e.getMessage());
            return err;
        }
    }

    private Base64ImageSource.MediaType resolverMediaType(String contentType) {
        if (contentType == null) return Base64ImageSource.MediaType.IMAGE_JPEG;
        return switch (contentType.toLowerCase()) {
            case "image/png"  -> Base64ImageSource.MediaType.IMAGE_PNG;
            case "image/gif"  -> Base64ImageSource.MediaType.IMAGE_GIF;
            case "image/webp" -> Base64ImageSource.MediaType.IMAGE_WEBP;
            default           -> Base64ImageSource.MediaType.IMAGE_JPEG;
        };
    }

    private FacturaAnalizadaDto parsearRespuesta(String json) throws Exception {
        String jsonLimpio = json.trim();
        if (jsonLimpio.startsWith("```")) {
            jsonLimpio = jsonLimpio.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("```$", "").trim();
        }

        JsonNode root = objectMapper.readTree(jsonLimpio);
        FacturaAnalizadaDto dto = new FacturaAnalizadaDto();
        dto.setProveedor(textoOrNull(root, "proveedor"));
        dto.setNumeroFactura(textoOrNull(root, "numeroFactura"));
        dto.setFechaFactura(textoOrNull(root, "fechaFactura"));
        dto.setDescripcion(textoOrNull(root, "descripcion"));
        dto.setCostoEnvio(root.has("costoEnvio") && !root.get("costoEnvio").isNull()
                ? root.get("costoEnvio").asDouble() : null);

        List<FacturaAnalizadaDto.ItemAnalizadoDto> items = new ArrayList<>();
        if (root.has("items") && root.get("items").isArray()) {
            for (JsonNode n : root.get("items")) {
                FacturaAnalizadaDto.ItemAnalizadoDto item = new FacturaAnalizadaDto.ItemAnalizadoDto();
                item.setNombre(textoOrNull(n, "nombre"));
                item.setTipoItem(textoOrNull(n, "tipoItem"));
                item.setTipoInsumo(textoOrNull(n, "tipoInsumo"));
                item.setCantidad(n.has("cantidad") && !n.get("cantidad").isNull() ? n.get("cantidad").asDouble() : null);
                item.setUnidad(textoOrNull(n, "unidad"));
                item.setValorUnitario(n.has("valorUnitario") && !n.get("valorUnitario").isNull() ? n.get("valorUnitario").asDouble() : null);
                item.setPorcentajeIvaItem(n.has("porcentajeIvaItem") && !n.get("porcentajeIvaItem").isNull() ? n.get("porcentajeIvaItem").asDouble() : null);
                items.add(item);
            }
        }
        dto.setItems(items);
        return dto;
    }

    private String textoOrNull(JsonNode n, String campo) {
        return (n.has(campo) && !n.get(campo).isNull()) ? n.get(campo).asText() : null;
    }

    private FacturaAnalizadaDto mockResponse() {
        FacturaAnalizadaDto dto = new FacturaAnalizadaDto();
        dto.setProveedor("Fermentis S.A. (EJEMPLO)");
        dto.setNumeroFactura("FAC-2024-0042");
        dto.setFechaFactura("2024-11-05");
        dto.setDescripcion("Materias primas para producción — Lote IPA");
        dto.setCostoEnvio(15000.0);
        dto.setModoMock(true);

        List<FacturaAnalizadaDto.ItemAnalizadoDto> items = new ArrayList<>();

        FacturaAnalizadaDto.ItemAnalizadoDto malta = new FacturaAnalizadaDto.ItemAnalizadoDto();
        malta.setNombre("Cebada Malteada Pale");
        malta.setTipoItem("INSUMO");
        malta.setTipoInsumo("MALTA");
        malta.setCantidad(25.0);
        malta.setUnidad("kg");
        malta.setValorUnitario(8500.0);
        malta.setPorcentajeIvaItem(19.0);
        items.add(malta);

        FacturaAnalizadaDto.ItemAnalizadoDto lupulo = new FacturaAnalizadaDto.ItemAnalizadoDto();
        lupulo.setNombre("Cascade Hops");
        lupulo.setTipoItem("INSUMO");
        lupulo.setTipoInsumo("LUPULO");
        lupulo.setCantidad(500.0);
        lupulo.setUnidad("gr");
        lupulo.setValorUnitario(85.0);
        lupulo.setPorcentajeIvaItem(19.0);
        items.add(lupulo);

        FacturaAnalizadaDto.ItemAnalizadoDto levadura = new FacturaAnalizadaDto.ItemAnalizadoDto();
        levadura.setNombre("US-05 Dry Yeast");
        levadura.setTipoItem("INSUMO");
        levadura.setTipoInsumo("LEVADURA");
        levadura.setCantidad(2.0);
        levadura.setUnidad("und");
        levadura.setValorUnitario(12000.0);
        levadura.setPorcentajeIvaItem(19.0);
        items.add(levadura);

        dto.setItems(items);
        return dto;
    }
}
