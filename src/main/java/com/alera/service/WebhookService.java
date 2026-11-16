package com.alera.service;

import com.alera.dto.WebhookEndpointFormDto;
import com.alera.model.InsumoInventario;
import com.alera.model.LoteCerveza;
import com.alera.model.Tarea;
import com.alera.model.WebhookEndpoint;
import com.alera.repository.WebhookEndpointRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Transactional
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    public static final String LOTE_FASE_CAMBIADA    = "lote.fase_cambiada";
    public static final String LOTE_COMPLETADO       = "lote.completado";
    public static final String INVENTARIO_BAJO_STOCK = "inventario.bajo_stock";
    public static final String TAREA_ASIGNADA        = "tarea.asignada";
    public static final String LECTURA_AGREGADA      = "lectura.agregada";
    public static final String PING                  = "ping";

    public static final List<String> EVENTOS_DISPONIBLES = List.of(
            LOTE_FASE_CAMBIADA, LOTE_COMPLETADO, INVENTARIO_BAJO_STOCK,
            TAREA_ASIGNADA, LECTURA_AGREGADA
    );

    private final WebhookEndpointRepository repo;
    private final ObjectMapper              objectMapper;
    private final RestTemplate              restTemplate;

    public WebhookService(WebhookEndpointRepository repo, ObjectMapper objectMapper) {
        this.repo         = repo;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(10_000);
        this.restTemplate = new RestTemplate(factory);
    }

    // ── CRUD ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<WebhookEndpoint> listarTodos() {
        return repo.findAllByOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public WebhookEndpoint buscarPorId(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Webhook no encontrado: " + id));
    }

    public void guardar(WebhookEndpointFormDto dto) {
        WebhookEndpoint ep = new WebhookEndpoint();
        mapDto(dto, ep);
        if (ep.getSecret() == null || ep.getSecret().isBlank()) {
            ep.setSecret(generarSecret());
        }
        repo.save(ep);
    }

    public void actualizar(Long id, WebhookEndpointFormDto dto) {
        WebhookEndpoint ep = buscarPorId(id);
        mapDto(dto, ep);
        if (ep.getSecret() == null || ep.getSecret().isBlank()) {
            ep.setSecret(generarSecret());
        }
        repo.save(ep);
    }

    public void eliminar(Long id) {
        repo.deleteById(id);
    }

    public void toggleActivo(Long id) {
        WebhookEndpoint ep = buscarPorId(id);
        ep.setActivo(!ep.isActivo());
        repo.save(ep);
    }

    public static String generarSecret() {
        byte[] bytes = new byte[20];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    // ── Despacho — eventos específicos ───────────────────────────────

    public void notificarFaseCambiada(String tenantId, LoteCerveza lote, String eventoHistorial) {
        boolean esCompletado = "COMPLETADO".equals(eventoHistorial);
        String evento = esCompletado ? LOTE_COMPLETADO : LOTE_FASE_CAMBIADA;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("loteId",      lote.getId());
        data.put("codigoLote",  lote.getCodigoLote());
        data.put("estilo",      lote.getEstilo());
        data.put("fase",        lote.getFaseActual());
        data.put("evento",      eventoHistorial);
        enviarEvento(tenantId, evento, data);
    }

    public void notificarBajoStock(String tenantId, List<InsumoInventario> insumos) {
        if (insumos.isEmpty()) return;
        List<Map<String, Object>> items = insumos.stream().map(i -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("nombre", i.getNombre());
            m.put("tipo",   i.getTipo() != null ? i.getTipo() : "");
            m.put("stock",  i.getCantidad());
            m.put("minimo", i.getStockMinimo());
            m.put("unidad", i.getUnidad() != null ? i.getUnidad() : "");
            return m;
        }).toList();
        enviarEvento(tenantId, INVENTARIO_BAJO_STOCK, Map.of("insumos", items));
    }

    public void notificarTareaAsignada(String tenantId, Tarea tarea) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tareaId",    tarea.getId());
        data.put("titulo",     tarea.getTitulo());
        data.put("asignadoA",  tarea.getAsignadoA());
        data.put("creadoPor",  tarea.getCreadoPor());
        data.put("prioridad",  tarea.getPrioridad() != null ? tarea.getPrioridad().name() : null);
        data.put("vencimiento", tarea.getFechaVencimiento());
        enviarEvento(tenantId, TAREA_ASIGNADA, data);
    }

    public void notificarLecturaAgregada(String tenantId, Long loteId, String codigoLote,
                                          Integer densidad, BigDecimal temperatura) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("loteId",      loteId);
        data.put("codigoLote",  codigoLote);
        data.put("densidad",    densidad);
        data.put("temperatura", temperatura);
        enviarEvento(tenantId, LECTURA_AGREGADA, data);
    }

    // ── Despacho — bajo nivel ─────────────────────────────────────────

    public void enviarEvento(String tenantId, String evento, Object data) {
        List<WebhookEndpoint> endpoints = repo.findActivosByTenantId(tenantId);
        for (WebhookEndpoint ep : endpoints) {
            if (ep.suscritoA(evento)) {
                CompletableFuture.runAsync(() -> enviarRaw(ep, evento, data));
            }
        }
    }

    public boolean probar(Long id) {
        WebhookEndpoint ep = buscarPorId(id);
        try {
            enviarRaw(ep, PING, Map.of("message", "Test webhook from Zymos", "tenant", ep.getTenantId()));
            return true;
        } catch (Exception e) {
            log.warn("[Webhook] Prueba fallida para '{}': {}", ep.getUrl(), e.getMessage());
            return false;
        }
    }

    private void enviarRaw(WebhookEndpoint ep, String evento, Object data) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("event",     evento);
            payload.put("tenant",    ep.getTenantId());
            payload.put("timestamp", Instant.now().toString());
            payload.put("delivery",  UUID.randomUUID().toString());
            payload.put("data",      data);

            String body      = objectMapper.writeValueAsString(payload);
            String signature = computeSignature(ep.getSecret(), body);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Zymos-Event",         evento);
            headers.set("X-Zymos-Signature-256", "sha256=" + signature);
            headers.set("X-Zymos-Delivery",      (String) payload.get("delivery"));

            restTemplate.postForEntity(ep.getUrl(), new HttpEntity<>(body, headers), String.class);
            log.info("[Webhook] {} → {} OK", evento, ep.getUrl());
        } catch (Exception e) {
            log.warn("[Webhook] {} → {} FAILED: {}", evento, ep.getUrl(), e.getMessage());
        }
    }

    private String computeSignature(String secret, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }

    private void mapDto(WebhookEndpointFormDto dto, WebhookEndpoint ep) {
        ep.setNombre(dto.getNombre());
        ep.setUrl(dto.getUrl().trim());
        ep.setActivo(dto.isActivo());
        if (dto.getSecret() != null && !dto.getSecret().isBlank()) {
            ep.setSecret(dto.getSecret().trim());
        }
        List<String> eventos = dto.getEventosList() != null
                ? dto.getEventosList().stream().filter(e -> !e.isBlank()).toList()
                : List.of();
        ep.setEventos(eventos.isEmpty() ? LOTE_FASE_CAMBIADA : String.join(",", eventos));
    }
}
