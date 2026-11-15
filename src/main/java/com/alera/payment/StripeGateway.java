package com.alera.payment;

import com.alera.model.CheckoutPendiente;
import com.stripe.Stripe;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class StripeGateway implements PasarelaPago {

    private static final Logger log = LoggerFactory.getLogger(StripeGateway.class);

    @Value("${app.stripe.secret-key:}")
    private String secretKey;

    @Value("${app.stripe.webhook-secret:}")
    private String webhookSecret;

    @PostConstruct
    void init() {
        if (!secretKey.isBlank()) {
            Stripe.apiKey = secretKey;
            log.info("Stripe inicializado");
        } else {
            log.warn("Stripe: app.stripe.secret-key no configurado — modo mock");
        }
    }

    public boolean isHabilitado() {
        return !secretKey.isBlank();
    }

    @Override
    public String crearSesion(CheckoutPendiente checkout, String baseUrl) {
        if (!isHabilitado()) {
            return baseUrl + "/checkout/exito?external_reference=" + checkout.getId()
                   + "&payment_id=STRIPE-MOCK-" + System.currentTimeMillis()
                   + "&collection_status=approved";
        }
        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency("usd")
                                    .setUnitAmount((long) checkout.getPlanNivel().getPrecioUsd())
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName("Zymos " + checkout.getPlanNivel().getDisplayName())
                                            .setDescription("Acceso mensual — " + checkout.getNombreCerveceria())
                                            .build())
                                    .build())
                            .build())
                    .setSuccessUrl(baseUrl + "/checkout/exito?external_reference=" + checkout.getId()
                            + "&payment_id={CHECKOUT_SESSION_ID}&collection_status=approved")
                    .setCancelUrl(baseUrl + "/checkout/fallo")
                    .setClientReferenceId(checkout.getId())
                    .putMetadata("checkout_id", checkout.getId())
                    .setCustomerEmail(checkout.getEmail())
                    .build();

            Session session = Session.create(params);
            return session.getUrl();
        } catch (Exception e) {
            log.error("Error creando sesión Stripe para checkout {}: {}", checkout.getId(), e.getMessage(), e);
            throw new RuntimeException("No se pudo conectar con Stripe. Intentá de nuevo.", e);
        }
    }

    @Override
    public Optional<String> resolverPaymentIdDesdeWebhook(String payload, Map<String, String> params) {
        return Optional.empty(); // Stripe usa construirEvento()
    }

    @Override
    public boolean pagoAprobado(String paymentId) {
        if (paymentId.startsWith("STRIPE-MOCK-")) return true;
        try {
            Session session = Session.retrieve(paymentId);
            return "paid".equals(session.getPaymentStatus());
        } catch (Exception e) {
            log.error("Error consultando sesión Stripe {}: {}", paymentId, e.getMessage());
            return false;
        }
    }

    @Override
    public String obtenerExternalReference(String paymentId) {
        if (paymentId.startsWith("STRIPE-MOCK-")) return null;
        try {
            Session session = Session.retrieve(paymentId);
            return session.getClientReferenceId();
        } catch (Exception e) {
            log.error("Error obteniendo external_reference para sesión Stripe {}: {}", paymentId, e.getMessage());
            return null;
        }
    }

    @Override
    public boolean verificarFirmaWebhook(String xSignature, String xRequestId,
                                          String payload, Map<String, String> params) {
        if (webhookSecret.isBlank()) return true;
        if (xSignature == null || xSignature.isBlank()) {
            log.warn("Webhook Stripe: falta header Stripe-Signature — rechazado");
            return false;
        }
        try {
            Webhook.constructEvent(payload, xSignature, webhookSecret);
            return true;
        } catch (Exception e) {
            log.warn("Webhook Stripe: firma inválida — {}", e.getMessage());
            return false;
        }
    }

    /** Construye y verifica el Event de Stripe a partir del payload y la firma. */
    public Optional<Event> construirEvento(String payload, String stripeSignature) {
        if (webhookSecret.isBlank() || payload == null) return Optional.empty();
        try {
            return Optional.of(Webhook.constructEvent(payload, stripeSignature, webhookSecret));
        } catch (Exception e) {
            log.warn("Webhook Stripe: no se pudo construir el evento — {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** Extrae la Session de un evento checkout.session.completed. */
    public Optional<Session> extraerSesion(Event event) {
        try {
            Optional<StripeObject> obj = event.getDataObjectDeserializer().getObject();
            if (obj.isPresent() && obj.get() instanceof Session session) {
                return Optional.of(session);
            }
        } catch (Exception e) {
            log.warn("Webhook Stripe: no se pudo deserializar la sesión — {}", e.getMessage());
        }
        return Optional.empty();
    }
}
