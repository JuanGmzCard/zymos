package com.alera.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "push_subscriptions")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String endpoint;

    @Column(name = "p256dh", columnDefinition = "TEXT", nullable = false)
    private String p256dh;

    @Column(name = "auth_key", columnDefinition = "TEXT", nullable = false)
    private String authKey;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
