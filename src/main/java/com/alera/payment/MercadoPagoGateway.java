package com.alera.payment;

import com.alera.model.CheckoutPendiente;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.resources.preference.Preference;
import com.mercadopago.resources.payment.Payment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class MercadoPagoGateway implements PasarelaPago {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoGateway.class);

    @Value("${app.mp.access-token:}")
    private String accessToken;

    @Value("${app.mp.webhook-secret:}")
    private String webhookSecret;

    @Value("${app.mp.sandbox:true}")
    private boolean sandbox;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    void init() {
        if (!accessToken.isBlank()) {
            MercadoPagoConfig.setAccessToken(accessToken);
            log.info("MercadoPago inicializado — modo {}", sandbox ? "SANDBOX" : "PRODUCCIÓN");
        } else {
            log.warn("MercadoPago: app.mp.access-token no configurado — pagos deshabilitados (modo mock)");
        }
    }

    public boolean isHabilitado() {
        return !accessToken.isBlank();
    }

    @Override
    public String crearSesion(CheckoutPendiente checkout, String baseUrl) {
        if (!isHabilitado()) {
            // En modo mock, simular éxito directo
            return baseUrl + "/checkout/exito?external_reference=" + checkout.getId()
                   + "&payment_id=MOCK-" + System.currentTimeMillis()
                   + "&collection_status=approved";
        }
        try {
            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .title(checkout.getPlanNivel().getDisplayName() + " — Zymos")
                    .quantity(1)
                    .unitPrice(BigDecimal.valueOf(checkout.getPlanNivel().getPrecioNumerico()))
                    .currencyId("COP")
                    .build();

            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success(baseUrl + "/checkout/exito")
                    .failure(baseUrl + "/checkout/fallo")
                    .pending(baseUrl + "/checkout/pendiente")
                    .build();

            PreferenceRequest request = PreferenceRequest.builder()
                    .items(List.of(item))
                    .backUrls(backUrls)
                    .autoReturn("approved")
                    .externalReference(checkout.getId())
                    .notificationUrl(baseUrl + "/webhook/mp")
                    .build();

            Preference preference = new PreferenceClient().create(request);
            return sandbox ? preference.getSandboxInitPoint() : preference.getInitPoint();

        } catch (Exception e) {
            log.error("Error creando preferencia MP para checkout {}: {}", checkout.getId(), e.getMessage(), e);
            throw new RuntimeException("No se pudo iniciar el proceso de pago. Intentá de nuevo.", e);
        }
    }

    @Override
    public Optional<String> resolverPaymentIdDesdeWebhook(String payload, Map<String, String> params) {
        // Formato webhook Checkout Pro: {"action":"payment.created","data":{"id":"12345"}}
        // Formato IPN (query params):   ?id=12345&topic=payment
        try {
            if (params.containsKey("id") && "payment".equals(params.get("topic"))) {
                return Optional.of(params.get("id"));
            }
            if (payload != null && !payload.isBlank()) {
                JsonNode node = objectMapper.readTree(payload);
                String type = node.path("type").asText("");
                String action = node.path("action").asText("");
                if ("payment".equals(type) || action.startsWith("payment.")) {
                    String id = node.path("data").path("id").asText("");
                    if (!id.isBlank()) return Optional.of(id);
                }
            }
        } catch (Exception e) {
            log.debug("No se pudo parsear webhook MP: {}", e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public boolean pagoAprobado(String paymentId) {
        if (paymentId.startsWith("MOCK-")) return true;  // modo mock
        try {
            Payment payment = new PaymentClient().get(Long.parseLong(paymentId));
            return "approved".equals(payment.getStatus());
        } catch (Exception e) {
            log.error("Error consultando pago MP {}: {}", paymentId, e.getMessage());
            return false;
        }
    }

    @Override
    public String obtenerExternalReference(String paymentId) {
        if (paymentId.startsWith("MOCK-")) return null;
        try {
            Payment payment = new PaymentClient().get(Long.parseLong(paymentId));
            return payment.getExternalReference();
        } catch (Exception e) {
            log.error("Error obteniendo external_reference para pago {}: {}", paymentId, e.getMessage());
            return null;
        }
    }

    @Override
    public boolean verificarFirmaWebhook(String xSignature, String xRequestId,
                                          String payload, Map<String, String> params) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            return true; // sin secreto configurado, omitir validación
        }
        if (xSignature == null || xSignature.isBlank()) {
            log.warn("Webhook MP: falta header x-signature — rechazado");
            return false;
        }

        // Parsear x-signature: ts=<timestamp>,v1=<hmac_hex>
        String ts = null;
        String v1 = null;
        for (String part : xSignature.split(",")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) {
                String k = kv[0].trim();
                String v = kv[1].trim();
                if ("ts".equals(k)) ts = v;
                else if ("v1".equals(k)) v1 = v;
            }
        }
        if (ts == null || v1 == null) {
            log.warn("Webhook MP: formato x-signature inválido: {}", xSignature);
            return false;
        }

        String dataId = extraerDataId(payload, params);
        String rid    = xRequestId != null ? xRequestId : "";

        // Template: id:<dataId>;request-id:<xRequestId>;ts:<ts>;
        String template = "id:" + dataId + ";request-id:" + rid + ";ts:" + ts + ";";

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String computed = HexFormat.of().formatHex(mac.doFinal(template.getBytes(StandardCharsets.UTF_8)));
            boolean valida = computed.equals(v1);
            if (!valida) log.warn("Webhook MP: firma inválida — esperado={} recibido={}", computed, v1);
            return valida;
        } catch (Exception e) {
            log.error("Error calculando HMAC-SHA256 para webhook MP: {}", e.getMessage());
            return false;
        }
    }

    private String extraerDataId(String payload, Map<String, String> params) {
        if (params != null && params.containsKey("id")) return params.get("id");
        try {
            if (payload != null && !payload.isBlank()) {
                JsonNode node = objectMapper.readTree(payload);
                String id = node.path("data").path("id").asText("");
                if (!id.isBlank()) return id;
            }
        } catch (Exception ignored) { }
        return "";
    }
}
