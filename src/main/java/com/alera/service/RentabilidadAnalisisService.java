package com.alera.service;

import com.alera.dto.RentabilidadAnalisisDto;
import com.alera.dto.RentabilidadLoteDto;
import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RentabilidadAnalisisService {

    private static final Logger log = LoggerFactory.getLogger(RentabilidadAnalisisService.class);

    @Value("${app.anthropic.api-key:}")
    private String apiKey;

    private final ObjectMapper mapper = new ObjectMapper();

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public RentabilidadAnalisisDto analizar(List<RentabilidadLoteDto> filas,
                                             BigDecimal totalIngresos,
                                             BigDecimal totalCosto,
                                             BigDecimal totalMargenPct) {
        if (filas == null || filas.isEmpty()) {
            RentabilidadAnalisisDto err = new RentabilidadAnalisisDto();
            err.setError("No hay datos de rentabilidad para analizar.");
            return err;
        }

        if (!isConfigured()) {
            return mockAnalisis(filas, totalIngresos, totalCosto, totalMargenPct);
        }

        try {
            String prompt = buildPrompt(filas, totalIngresos, totalCosto, totalMargenPct);
            AnthropicClient client = AnthropicOkHttpClient.builder().apiKey(apiKey).build();

            ContentBlockParam bloque = ContentBlockParam.ofText(
                    TextBlockParam.builder().text(prompt).build());

            Message response = client.messages().create(
                    MessageCreateParams.builder()
                            .model("claude-haiku-4-5")
                            .maxTokens(1500L)
                            .addUserMessageOfBlockParams(List.of(bloque))
                            .build());

            String texto = response.content().stream()
                    .filter(ContentBlock::isText)
                    .map(b -> b.asText().text())
                    .findFirst().orElse("{}");

            return parseRespuesta(texto);

        } catch (Exception e) {
            log.error("Error en RentabilidadAnalisisService", e);
            RentabilidadAnalisisDto err = new RentabilidadAnalisisDto();
            err.setError("Error al conectar con la IA: " + e.getMessage());
            return err;
        }
    }

    private String buildPrompt(List<RentabilidadLoteDto> filas,
                                BigDecimal totalIngresos,
                                BigDecimal totalCosto,
                                BigDecimal totalMargenPct) {

        // Agrupación por estilo
        Map<String, List<RentabilidadLoteDto>> porEstilo = filas.stream()
                .filter(r -> r.estilo() != null)
                .collect(Collectors.groupingBy(RentabilidadLoteDto::estilo));

        StringBuilder resumenEstilos = new StringBuilder();
        porEstilo.forEach((estilo, lotes) -> {
            BigDecimal ingSum = lotes.stream().filter(r -> r.ingresos() != null)
                    .map(RentabilidadLoteDto::ingresos).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal costoSum = lotes.stream().filter(r -> r.costo() != null)
                    .map(RentabilidadLoteDto::costo).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal margenSum = ingSum.subtract(costoSum);
            long rentables = lotes.stream().filter(RentabilidadLoteDto::rentable).count();
            resumenEstilos.append(String.format("  - %s: %d lotes, ingresos=%s, costos=%s, margen=%s, %d rentables%n",
                    estilo, lotes.size(),
                    ingSum.setScale(0, RoundingMode.HALF_UP),
                    costoSum.setScale(0, RoundingMode.HALF_UP),
                    margenSum.setScale(0, RoundingMode.HALF_UP),
                    rentables));
        });

        // Top 3 y bottom 3 por margen
        List<RentabilidadLoteDto> conMargen = filas.stream()
                .filter(r -> r.margen() != null && !r.sinCosto() && !r.sinVentas())
                .sorted(Comparator.comparing(RentabilidadLoteDto::margen, Comparator.reverseOrder()))
                .collect(Collectors.toList());

        StringBuilder top3 = new StringBuilder();
        conMargen.stream().limit(3).forEach(r ->
                top3.append(String.format("  - %s (%s): margen=%s, pct=%s%%%n",
                        r.codigoLote(), r.estilo(),
                        r.margen().setScale(0, RoundingMode.HALF_UP),
                        r.margenPct() != null ? r.margenPct() : "—")));

        StringBuilder bottom3 = new StringBuilder();
        List<RentabilidadLoteDto> invertido = new ArrayList<>(conMargen);
        Collections.reverse(invertido);
        invertido.stream().limit(3).forEach(r ->
                bottom3.append(String.format("  - %s (%s): margen=%s, pct=%s%%%n",
                        r.codigoLote(), r.estilo(),
                        r.margen().setScale(0, RoundingMode.HALF_UP),
                        r.margenPct() != null ? r.margenPct() : "—")));

        long sinCosto  = filas.stream().filter(RentabilidadLoteDto::sinCosto).count();
        long sinVentas = filas.stream().filter(RentabilidadLoteDto::sinVentas).count();
        long enRojo    = filas.stream().filter(RentabilidadLoteDto::enRojo).count();
        long rentables = filas.stream().filter(RentabilidadLoteDto::rentable).count();

        return String.format("""
Eres un analista financiero especializado en cervecerías artesanales. Analiza los siguientes datos de rentabilidad y proporciona insights accionables.

DATOS GLOBALES:
- Total lotes: %d
- Ingresos totales: %s
- Costos totales: %s
- Margen total: %s%%
- Lotes rentables: %d | En rojo: %d | Sin costos: %d | Sin ventas: %d

RENTABILIDAD POR ESTILO:
%s
TOP 3 LOTES MÁS RENTABLES:
%s
BOTTOM 3 LOTES MENOS RENTABLES:
%s
Responde SOLO con un objeto JSON con esta estructura exacta (sin markdown):
{
  "insights": ["insight1", "insight2", "insight3"],
  "estiloMasRentable": "nombre del estilo más rentable",
  "estiloMenosRentable": "nombre del estilo menos rentable o con más lotes en rojo",
  "tendencia": "descripción de la tendencia general en 1-2 oraciones",
  "recomendaciones": ["acción1", "acción2", "acción3"],
  "alertas": ["alerta1 si aplica"]
}
Los insights deben ser concretos y específicos para estos datos. Las alertas solo si hay problemas críticos (muchos lotes en rojo, costos muy altos, etc.). Si no hay alertas, dejar el array vacío.
""",
                filas.size(),
                totalIngresos != null ? totalIngresos.setScale(0, RoundingMode.HALF_UP) : "N/D",
                totalCosto != null ? totalCosto.setScale(0, RoundingMode.HALF_UP) : "N/D",
                totalMargenPct != null ? totalMargenPct : "N/D",
                rentables, enRojo, sinCosto, sinVentas,
                resumenEstilos.toString(),
                top3.toString(),
                bottom3.toString());
    }

    private RentabilidadAnalisisDto parseRespuesta(String texto) {
        try {
            int start = texto.indexOf('{');
            int end   = texto.lastIndexOf('}');
            if (start < 0 || end < 0) throw new IllegalArgumentException("No JSON");
            String json = texto.substring(start, end + 1);

            JsonNode root = mapper.readTree(json);
            RentabilidadAnalisisDto dto = new RentabilidadAnalisisDto();

            dto.setInsights(parseStringList(root.get("insights")));
            dto.setEstiloMasRentable(textOrNull(root.get("estiloMasRentable")));
            dto.setEstiloMenosRentable(textOrNull(root.get("estiloMenosRentable")));
            dto.setTendencia(textOrNull(root.get("tendencia")));
            dto.setRecomendaciones(parseStringList(root.get("recomendaciones")));
            dto.setAlertas(parseStringList(root.get("alertas")));
            return dto;

        } catch (Exception e) {
            log.warn("No se pudo parsear respuesta IA de rentabilidad: {}", e.getMessage());
            RentabilidadAnalisisDto err = new RentabilidadAnalisisDto();
            err.setError("No se pudo interpretar la respuesta de la IA.");
            return err;
        }
    }

    private RentabilidadAnalisisDto mockAnalisis(List<RentabilidadLoteDto> filas,
                                                  BigDecimal totalIngresos,
                                                  BigDecimal totalCosto,
                                                  BigDecimal totalMargenPct) {
        RentabilidadAnalisisDto dto = new RentabilidadAnalisisDto();
        dto.setModoMock(true);

        Map<String, List<RentabilidadLoteDto>> porEstilo = filas.stream()
                .filter(r -> r.estilo() != null)
                .collect(Collectors.groupingBy(RentabilidadLoteDto::estilo));

        String mejorEstilo = porEstilo.entrySet().stream()
                .max(Comparator.comparingLong(e -> e.getValue().stream().filter(RentabilidadLoteDto::rentable).count()))
                .map(Map.Entry::getKey).orElse("IPA");
        String peorEstilo = porEstilo.entrySet().stream()
                .max(Comparator.comparingLong(e -> e.getValue().stream().filter(RentabilidadLoteDto::enRojo).count()))
                .map(Map.Entry::getKey).orElse(mejorEstilo);

        long rentables = filas.stream().filter(RentabilidadLoteDto::rentable).count();
        long enRojo    = filas.stream().filter(RentabilidadLoteDto::enRojo).count();
        int pctRentable = filas.isEmpty() ? 0 : (int)(rentables * 100 / filas.size());

        dto.setEstiloMasRentable(mejorEstilo);
        dto.setEstiloMenosRentable(peorEstilo);
        dto.setTendencia("El " + pctRentable + "% de los lotes analizados son rentables. " +
                (enRojo > 0 ? enRojo + " lote(s) en rojo requieren atención." : "No hay lotes en rojo en el período."));

        dto.setInsights(List.of(
                rentables + " de " + filas.size() + " lotes son rentables (" + pctRentable + "%).",
                "El estilo " + mejorEstilo + " presenta el mejor desempeño en rentabilidad.",
                totalMargenPct != null ? "El margen global es del " + totalMargenPct + "% sobre los ingresos totales."
                        : "Algunos lotes aún no tienen costos o ventas asignadas."
        ));

        dto.setRecomendaciones(List.of(
                "Revisar los lotes en rojo para identificar si los costos de producción son demasiado altos.",
                "Considerar aumentar el volumen de producción del estilo " + mejorEstilo + ".",
                "Asignar costos de factura a los lotes que aún los tienen pendientes para una visión completa."
        ));

        List<String> alertas = new ArrayList<>();
        if (enRojo > 0 && enRojo >= filas.size() / 2) {
            alertas.add("Más del 50% de los lotes están en rojo. Revisar estructura de costos urgentemente.");
        }
        long sinCosto = filas.stream().filter(RentabilidadLoteDto::sinCosto).count();
        if (sinCosto > filas.size() / 3) {
            alertas.add(sinCosto + " lotes sin costos asignados — los datos de rentabilidad son incompletos.");
        }
        dto.setAlertas(alertas);

        return dto;
    }

    private List<String> parseStringList(JsonNode node) {
        if (node == null || !node.isArray()) return List.of();
        List<String> result = new ArrayList<>();
        node.forEach(n -> { if (!n.asText().isBlank()) result.add(n.asText()); });
        return result;
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) return null;
        String t = node.asText().trim();
        return t.isBlank() ? null : t;
    }
}
