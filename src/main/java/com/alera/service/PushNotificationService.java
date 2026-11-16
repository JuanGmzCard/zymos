package com.alera.service;

import com.alera.model.PushSubscription;
import com.alera.repository.PushSubscriptionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    @Value("${app.vapid.public-key:}")  private String vapidPublicKey;
    @Value("${app.vapid.private-key:}") private String vapidPrivateKey;
    @Value("${app.vapid.subject:mailto:noreply@zymos.app}") private String vapidSubject;

    private final PushSubscriptionRepository repo;
    private final ObjectMapper objectMapper;
    private PushService pushService;

    public PushNotificationService(PushSubscriptionRepository repo, ObjectMapper objectMapper) {
        this.repo = repo;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        if (vapidPublicKey.isBlank() || vapidPrivateKey.isBlank()) {
            log.info("[Push] VAPID_PUBLIC_KEY / VAPID_PRIVATE_KEY no configurados — modo mock. " +
                     "Ir a /admin/push/generate-keys para generar claves.");
            return;
        }
        try {
            if (Security.getProvider("BC") == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
            pushService = new PushService()
                .setPublicKey(vapidPublicKey)
                .setPrivateKey(vapidPrivateKey)
                .setSubject(vapidSubject);
            log.info("[Push] Notificaciones push habilitadas (VAPID configurado)");
        } catch (Exception e) {
            log.error("[Push] Error inicializando VAPID: {} — modo mock activo", e.getMessage());
        }
    }

    public boolean isConfigured() { return pushService != null; }

    public String getVapidPublicKey() { return vapidPublicKey; }

    /** Genera y retorna un nuevo par de claves VAPID (para configuración inicial). */
    public Map<String, String> generateVapidKeys() throws Exception {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC", "BC");
        keyGen.initialize(new ECGenParameterSpec("prime256v1"));
        KeyPair keyPair = keyGen.generateKeyPair();

        // Public key: uncompressed EC point (04 || X || Y) → 65 bytes → base64url sin padding
        BCECPublicKey pubKey = (BCECPublicKey) keyPair.getPublic();
        byte[] pubBytes = pubKey.getQ().getEncoded(false);
        String publicKey = Base64.getUrlEncoder().withoutPadding().encodeToString(pubBytes);

        // Private key: valor D → 32 bytes → base64url sin padding
        BCECPrivateKey privKey = (BCECPrivateKey) keyPair.getPrivate();
        byte[] privBytes = privKey.getD().toByteArray();
        // BigInteger.toByteArray() puede agregar byte 0x00 al inicio; eliminarlo si length==33
        if (privBytes.length == 33 && privBytes[0] == 0) {
            privBytes = Arrays.copyOfRange(privBytes, 1, 33);
        }
        String privateKey = Base64.getUrlEncoder().withoutPadding().encodeToString(privBytes);

        Map<String, String> result = new LinkedHashMap<>();
        result.put("publicKey", publicKey);
        result.put("privateKey", privateKey);
        result.put("instrucciones",
            "Agrega VAPID_PUBLIC_KEY y VAPID_PRIVATE_KEY a tus variables de entorno y reinicia la app.");
        return result;
    }

    @Transactional
    public void upsertSubscription(String username, String tenantId,
                                   String endpoint, String p256dh, String auth) {
        repo.findByEndpoint(endpoint).ifPresentOrElse(
            existing -> {
                existing.setUsername(username);
                existing.setTenantId(tenantId);
            },
            () -> repo.save(new PushSubscription(null, tenantId, username, endpoint, p256dh, auth, null))
        );
    }

    @Transactional
    public void removeSubscription(String endpoint) {
        repo.deleteByEndpoint(endpoint);
    }

    public boolean isSubscribed(String username, String tenantId) {
        return repo.existsByUsernameAndTenantId(username, tenantId);
    }

    /** Envía push al usuario específico (p.ej. para TAREA_ASIGNADA). */
    public void enviarAUsuario(String username, String tenantId,
                               String title, String body, String url) {
        if (!isConfigured()) {
            log.info("[Push MOCK] →usuario:{} title:{}", username, title);
            return;
        }
        CompletableFuture.runAsync(() ->
            repo.findByUsernameAndTenantId(username, tenantId)
                .forEach(sub -> enviarRaw(sub, title, body, url)));
    }

    /** Envía push a todos los suscriptores del tenant (p.ej. para alertas de inventario). */
    public void enviarATenant(String tenantId, String title, String body, String url) {
        if (!isConfigured()) {
            log.info("[Push MOCK] →tenant:{} title:{}", tenantId, title);
            return;
        }
        CompletableFuture.runAsync(() ->
            repo.findByTenantId(tenantId)
                .forEach(sub -> enviarRaw(sub, title, body, url)));
    }

    private void enviarRaw(PushSubscription sub, String title, String body, String url) {
        try {
            Subscription subscription = new Subscription(
                sub.getEndpoint(),
                new Subscription.Keys(sub.getP256dh(), sub.getAuthKey())
            );
            String payload = objectMapper.writeValueAsString(Map.of(
                "title", title,
                "body",  body,
                "icon",  "/img/icon.svg",
                "badge", "/img/icon.svg",
                "url",   url != null ? url : "/notificaciones"
            ));
            var response = pushService.send(new Notification(subscription, payload));
            int status = response.getStatusLine().getStatusCode();
            if (status == 410 || status == 404) {
                // Suscripción expirada o inválida — limpiar
                repo.deleteByEndpoint(sub.getEndpoint());
                log.debug("[Push] Suscripción expirada eliminada: {}", sub.getEndpoint());
            } else if (status >= 400) {
                log.warn("[Push] HTTP {} al enviar a endpoint {}", status, sub.getEndpoint());
            }
        } catch (Exception e) {
            log.warn("[Push] Error enviando a {}: {}", sub.getUsername(), e.getMessage());
        }
    }
}
