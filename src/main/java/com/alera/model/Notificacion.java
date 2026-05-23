package com.alera.model;

import com.alera.model.enums.TipoNotificacion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones")
@Getter @Setter @NoArgsConstructor
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TipoNotificacion tipo;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(length = 500)
    private String mensaje;

    @Column(name = "url_accion", length = 300)
    private String urlAccion;

    @Column(nullable = false)
    private boolean leida = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public static Notificacion of(TipoNotificacion tipo, String titulo, String mensaje, String urlAccion) {
        Notificacion n = new Notificacion();
        n.tipo      = tipo;
        n.titulo    = titulo;
        n.mensaje   = mensaje;
        n.urlAccion = urlAccion;
        return n;
    }
}
