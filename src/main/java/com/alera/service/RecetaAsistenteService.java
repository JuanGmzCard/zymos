package com.alera.service;

import com.alera.dto.RecetaAsistidaDto;
import com.alera.model.InsumoInventario;
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecetaAsistenteService {

    private static final Logger log = LoggerFactory.getLogger(RecetaAsistenteService.class);

    @Value("${app.anthropic.api-key:}")
    private String apiKey;

    private final InsumoInventarioService insumoService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RecetaAsistenteService(InsumoInventarioService insumoService) {
        this.insumoService = insumoService;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public RecetaAsistidaDto generar(String descripcion, Double volumen) {
        if (!isConfigured()) {
            log.info("ANTHROPIC_API_KEY no configurada — devolviendo receta de ejemplo");
            return mockResponse(descripcion, volumen);
        }

        try {
            String prompt = construirPrompt(descripcion, volumen);

            TextBlockParam textoParam = TextBlockParam.builder().text(prompt).build();
            ContentBlockParam textoBlock = ContentBlockParam.ofText(textoParam);

            AnthropicClient client = AnthropicOkHttpClient.builder()
                    .apiKey(apiKey)
                    .build();

            Message response = client.messages().create(
                    MessageCreateParams.builder()
                            .model("claude-haiku-4-5")
                            .maxTokens(3000L)
                            .addUserMessageOfBlockParams(List.of(textoBlock))
                            .build());

            String jsonText = response.content().stream()
                    .filter(b -> b.isText())
                    .map(b -> b.asText().text())
                    .findFirst()
                    .orElse("{}");

            return parsearRespuesta(jsonText);

        } catch (Exception e) {
            log.error("Error al generar receta con Claude", e);
            RecetaAsistidaDto err = new RecetaAsistidaDto();
            err.setError("Error al generar la receta: " + e.getMessage());
            return err;
        }
    }

    private String construirPrompt(String descripcion, Double volumen) {
        List<String> maltas      = nombresInventario("Malta");
        List<String> lupulos     = nombresInventario("Lúpulo");
        List<String> levaduras   = nombresInventario("Levadura");
        List<String> clarificantes = nombresInventario("Clarificante");

        double vol = (volumen != null && volumen > 0) ? volumen : 20.0;

        return """
                Eres un maestro cervecero experto. Genera una receta de cerveza artesanal completa basada en la siguiente descripción del usuario.

                DESCRIPCIÓN: %s
                VOLUMEN OBJETIVO: %.0f litros

                INGREDIENTES DISPONIBLES EN INVENTARIO:
                - Maltas: %s
                - Lúpulos: %s
                - Levaduras: %s
                - Clarificantes: %s

                INSTRUCCIONES:
                - Prioriza ingredientes del inventario. Si alguno no está disponible, sugiérelo igualmente.
                - Las densidades OG/FG usan formato XXXX (ej: 1050, 1012). No decimal.
                - Cantidades en gramos (gr) para sólidos, litros (L) para líquidos.
                - Incluye escalones de maceración típicos (ej: Maceración 65°C 60 min, Mash Out 75°C 10 min).
                - Incluye adiciones de lúpulo en hervor con minutosRestantes (60 = inicio hervor, 0 = flameout).
                - Responde ÚNICAMENTE con el JSON, sin texto adicional ni markdown.

                Formato JSON requerido:
                {
                  "nombre": "Nombre de la receta",
                  "estilo": "Estilo BJCP (ej: American IPA)",
                  "descripcion": "Descripción breve de 1-2 oraciones",
                  "volumenBase": %.0f,
                  "tiempoHervorMinutos": 60,
                  "ogObjetivo": 1055,
                  "fgObjetivo": 1012,
                  "aguaMacerado": 25.0,
                  "unidadAgua": "L",
                  "aguaSparge": 15.0,
                  "unidadSparge": "L",
                  "notas": "Notas técnicas opcionales",
                  "maltas": [{"nombre": "Malta Pale Ale", "cantidad": 5000, "unidad": "gr"}],
                  "lupulos": [{"nombre": "Cascade", "cantidad": 30, "unidad": "gr"}],
                  "levaduras": [{"nombre": "US-05", "cantidad": 11, "unidad": "gr"}],
                  "clarificantes": [],
                  "adicionesHervor": [
                    {"nombre": "Cascade", "minutosRestantes": 60, "cantidad": 30, "unidad": "gr"},
                    {"nombre": "Cascade", "minutosRestantes": 10, "cantidad": 20, "unidad": "gr"}
                  ],
                  "escalones": [
                    {"nombre": "Maceración", "temperatura": 65.0, "duracionMinutos": 60},
                    {"nombre": "Mash Out", "temperatura": 75.0, "duracionMinutos": 10}
                  ]
                }
                """.formatted(
                        descripcion,
                        vol,
                        listaOrDefault(maltas, "Malta Pale Ale, Malta Caramel 60L"),
                        listaOrDefault(lupulos, "Cascade, Centennial, Hallertau"),
                        listaOrDefault(levaduras, "US-05, WLP001"),
                        listaOrDefault(clarificantes, "Irish Moss, Whirlfloc"),
                        vol);
    }

    private List<String> nombresInventario(String tipo) {
        return insumoService.listarPorTipo(tipo).stream()
                .map(InsumoInventario::getNombre)
                .collect(Collectors.toList());
    }

    private String listaOrDefault(List<String> nombres, String fallback) {
        return nombres.isEmpty() ? fallback : String.join(", ", nombres);
    }

    private RecetaAsistidaDto parsearRespuesta(String json) throws Exception {
        String jsonLimpio = json.trim();
        if (jsonLimpio.startsWith("```")) {
            jsonLimpio = jsonLimpio.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("```$", "").trim();
        }

        JsonNode root = objectMapper.readTree(jsonLimpio);
        RecetaAsistidaDto dto = new RecetaAsistidaDto();

        dto.setNombre(textoOrNull(root, "nombre"));
        dto.setEstilo(textoOrNull(root, "estilo"));
        dto.setDescripcion(textoOrNull(root, "descripcion"));
        dto.setNotas(textoOrNull(root, "notas"));
        dto.setVolumenBase(numOrNull(root, "volumenBase"));
        dto.setTiempoHervorMinutos(root.has("tiempoHervorMinutos") && !root.get("tiempoHervorMinutos").isNull()
                ? root.get("tiempoHervorMinutos").asInt() : null);
        dto.setOgObjetivo(root.has("ogObjetivo") && !root.get("ogObjetivo").isNull()
                ? root.get("ogObjetivo").asInt() : null);
        dto.setFgObjetivo(root.has("fgObjetivo") && !root.get("fgObjetivo").isNull()
                ? root.get("fgObjetivo").asInt() : null);
        dto.setAguaMacerado(numOrNull(root, "aguaMacerado"));
        dto.setUnidadAgua(textoOrDefault(root, "unidadAgua", "L"));
        dto.setAguaSparge(numOrNull(root, "aguaSparge"));
        dto.setUnidadSparge(textoOrDefault(root, "unidadSparge", "L"));

        dto.setMaltas(parsearIngredientes(root, "maltas"));
        dto.setLupulos(parsearIngredientes(root, "lupulos"));
        dto.setLevaduras(parsearIngredientes(root, "levaduras"));
        dto.setClarificantes(parsearIngredientes(root, "clarificantes"));
        dto.setAdicionesHervor(parsearAdiciones(root));
        dto.setEscalones(parsearEscalones(root));

        return dto;
    }

    private List<RecetaAsistidaDto.IngredienteDto> parsearIngredientes(JsonNode root, String campo) {
        List<RecetaAsistidaDto.IngredienteDto> lista = new ArrayList<>();
        if (!root.has(campo) || !root.get(campo).isArray()) return lista;
        for (JsonNode n : root.get(campo)) {
            RecetaAsistidaDto.IngredienteDto ing = new RecetaAsistidaDto.IngredienteDto();
            ing.setNombre(textoOrNull(n, "nombre"));
            ing.setCantidad(numOrNull(n, "cantidad"));
            ing.setUnidad(textoOrDefault(n, "unidad", "gr"));
            if (ing.getNombre() != null) lista.add(ing);
        }
        return lista;
    }

    private List<RecetaAsistidaDto.AdicionDto> parsearAdiciones(JsonNode root) {
        List<RecetaAsistidaDto.AdicionDto> lista = new ArrayList<>();
        if (!root.has("adicionesHervor") || !root.get("adicionesHervor").isArray()) return lista;
        for (JsonNode n : root.get("adicionesHervor")) {
            RecetaAsistidaDto.AdicionDto ad = new RecetaAsistidaDto.AdicionDto();
            ad.setNombre(textoOrNull(n, "nombre"));
            ad.setMinutosRestantes(n.has("minutosRestantes") && !n.get("minutosRestantes").isNull()
                    ? n.get("minutosRestantes").asInt() : null);
            ad.setCantidad(numOrNull(n, "cantidad"));
            ad.setUnidad(textoOrDefault(n, "unidad", "gr"));
            if (ad.getNombre() != null) lista.add(ad);
        }
        return lista;
    }

    private List<RecetaAsistidaDto.EscalonDto> parsearEscalones(JsonNode root) {
        List<RecetaAsistidaDto.EscalonDto> lista = new ArrayList<>();
        if (!root.has("escalones") || !root.get("escalones").isArray()) return lista;
        for (JsonNode n : root.get("escalones")) {
            RecetaAsistidaDto.EscalonDto esc = new RecetaAsistidaDto.EscalonDto();
            esc.setNombre(textoOrNull(n, "nombre"));
            esc.setTemperatura(numOrNull(n, "temperatura"));
            esc.setDuracionMinutos(n.has("duracionMinutos") && !n.get("duracionMinutos").isNull()
                    ? n.get("duracionMinutos").asInt() : null);
            if (esc.getNombre() != null) lista.add(esc);
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

    private Double numOrNull(JsonNode n, String campo) {
        return (n.has(campo) && !n.get(campo).isNull()) ? n.get(campo).asDouble() : null;
    }

    private RecetaAsistidaDto mockResponse(String descripcionUsuario, Double volumen) {
        double vol = (volumen != null && volumen > 0) ? volumen : 20.0;

        RecetaAsistidaDto dto = new RecetaAsistidaDto();
        dto.setModoMock(true);
        dto.setNombre("American IPA de Ejemplo");
        dto.setEstilo("American IPA");
        dto.setDescripcion("IPA de cuerpo medio con carácter cítrico y resinoso. Generada como ejemplo — activa la API de IA para recetas personalizadas.");
        dto.setVolumenBase(vol);
        dto.setTiempoHervorMinutos(60);
        dto.setOgObjetivo(1058);
        dto.setFgObjetivo(1012);
        dto.setAguaMacerado(vol * 1.3);
        dto.setUnidadAgua("L");
        dto.setAguaSparge(vol * 0.7);
        dto.setUnidadSparge("L");
        dto.setNotas("Fermentar a 18-20°C. Dryhopping opcional con 50 gr de Cascade al día 3.");

        RecetaAsistidaDto.IngredienteDto malta = new RecetaAsistidaDto.IngredienteDto();
        malta.setNombre("Malta Pale Ale"); malta.setCantidad(vol * 200.0); malta.setUnidad("gr");

        RecetaAsistidaDto.IngredienteDto caramel = new RecetaAsistidaDto.IngredienteDto();
        caramel.setNombre("Malta Caramel 40L"); caramel.setCantidad(vol * 30.0); caramel.setUnidad("gr");

        dto.setMaltas(List.of(malta, caramel));

        RecetaAsistidaDto.IngredienteDto lupulo = new RecetaAsistidaDto.IngredienteDto();
        lupulo.setNombre("Cascade"); lupulo.setCantidad(vol * 2.5); lupulo.setUnidad("gr");
        dto.setLupulos(List.of(lupulo));

        RecetaAsistidaDto.IngredienteDto lev = new RecetaAsistidaDto.IngredienteDto();
        lev.setNombre("US-05"); lev.setCantidad(11.0); lev.setUnidad("gr");
        dto.setLevaduras(List.of(lev));

        dto.setClarificantes(List.of());

        RecetaAsistidaDto.AdicionDto ad1 = new RecetaAsistidaDto.AdicionDto();
        ad1.setNombre("Cascade"); ad1.setMinutosRestantes(60); ad1.setCantidad(vol * 1.5); ad1.setUnidad("gr");

        RecetaAsistidaDto.AdicionDto ad2 = new RecetaAsistidaDto.AdicionDto();
        ad2.setNombre("Cascade"); ad2.setMinutosRestantes(10); ad2.setCantidad(vol * 1.5); ad2.setUnidad("gr");

        RecetaAsistidaDto.AdicionDto ad3 = new RecetaAsistidaDto.AdicionDto();
        ad3.setNombre("Cascade"); ad3.setMinutosRestantes(0); ad3.setCantidad(vol); ad3.setUnidad("gr");

        dto.setAdicionesHervor(List.of(ad1, ad2, ad3));

        RecetaAsistidaDto.EscalonDto esc1 = new RecetaAsistidaDto.EscalonDto();
        esc1.setNombre("Maceración"); esc1.setTemperatura(65.0); esc1.setDuracionMinutos(60);

        RecetaAsistidaDto.EscalonDto esc2 = new RecetaAsistidaDto.EscalonDto();
        esc2.setNombre("Mash Out"); esc2.setTemperatura(75.0); esc2.setDuracionMinutos(10);

        dto.setEscalones(List.of(esc1, esc2));
        return dto;
    }
}
