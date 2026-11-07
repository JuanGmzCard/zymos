package com.alera.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "contactos_whatsapp_suscripciones")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ContactoWhatsAppSuscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contacto_id", nullable = false)
    private ContactoWhatsApp contacto;

    @Column(nullable = false, length = 50)
    private String tipo;

    @TenantId
    private String tenantId;
}
