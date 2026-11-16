package com.alera.controller;

import com.alera.config.TenantContext;
import com.alera.service.PushNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/push")
public class PushSubscriptionController {

    private final PushNotificationService pushService;

    public PushSubscriptionController(PushNotificationService pushService) {
        this.pushService = pushService;
    }

    /** Retorna la VAPID public key para que el frontend pueda suscribirse. */
    @GetMapping("/vapid-public-key")
    public ResponseEntity<Map<String, String>> vapidPublicKey() {
        return ResponseEntity.ok(Map.of(
            "publicKey",    pushService.getVapidPublicKey(),
            "isConfigured", String.valueOf(pushService.isConfigured())
        ));
    }

    @PostMapping("/subscribe")
    public ResponseEntity<Void> subscribe(@RequestBody SubscriptionDto dto, Authentication auth) {
        pushService.upsertSubscription(
            auth.getName(),
            TenantContext.getCurrentTenant(),
            dto.endpoint(), dto.p256dh(), dto.auth()
        );
        return ResponseEntity.ok().build();
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<Void> unsubscribe(@RequestBody Map<String, String> body) {
        String endpoint = body.get("endpoint");
        if (endpoint != null) pushService.removeSubscription(endpoint);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> status(Authentication auth) {
        boolean subscribed = pushService.isSubscribed(
            auth.getName(), TenantContext.getCurrentTenant());
        return ResponseEntity.ok(Map.of("subscribed", subscribed));
    }

    public record SubscriptionDto(String endpoint, String p256dh, String auth) {}
}
