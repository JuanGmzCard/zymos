package com.alera.payment;

import com.alera.model.CheckoutPendiente;

import java.util.Map;
import java.util.Optional;

/**
 * Abstracción de pasarela de pago — permite intercambiar MercadoPago por Stripe (u otra)
 * sin modificar el controller ni el servicio de provisionamiento.
 */
public interface PasarelaPago {

    /**
     * Crea una sesión de pago en la pasarela y retorna la URL de redirección al checkout externo.
     *
     * @param checkout objeto con los datos de la compra (plan, subdominio, email, nombre)
     * @param baseUrl  URL base de la app (ej: https://app.zymos.app) para construir back_urls
     */
    String crearSesion(CheckoutPendiente checkout, String baseUrl);

    /**
     * Extrae el ID de pago de la pasarela a partir del payload/params del webhook.
     * Retorna Optional.empty() si el evento no es un pago completado.
     */
    Optional<String> resolverPaymentIdDesdeWebhook(String payload, java.util.Map<String, String> params);

    /**
     * Verifica si el pago identificado por {@code paymentId} fue aprobado.
     */
    boolean pagoAprobado(String paymentId);

    /**
     * Retorna el external_reference (ID del CheckoutPendiente) asociado al pago.
     */
    String obtenerExternalReference(String paymentId);

    /**
     * Verifica la firma HMAC-SHA256 del webhook usando el header {@code x-signature}.
     * Retorna {@code true} si la firma es válida o si el secreto no está configurado.
     * <p>
     * Formato {@code x-signature}: {@code ts=<timestamp>,v1=<hmac_hex>}
     * String firmado: {@code id:<dataId>;request-id:<xRequestId>;ts:<ts>;}
     *
     * @param xSignature  valor del header {@code x-signature}
     * @param xRequestId  valor del header {@code x-request-id}
     * @param payload     body raw del webhook (puede ser null)
     * @param params      query params del webhook
     */
    default boolean verificarFirmaWebhook(String xSignature, String xRequestId,
                                           String payload, Map<String, String> params) {
        return true;
    }
}
