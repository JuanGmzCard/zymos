package com.alera.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contactos_whatsapp")
@Getter @Setter @NoArgsConstructor
public class ContactoWhatsApp extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 30)
    private String telefono;

    private boolean activo = true;

    @OneToMany(mappedBy = "contacto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContactoWhatsAppSuscripcion> suscripciones = new ArrayList<>();

    public List<String> getTiposSuscritos() {
        return suscripciones.stream().map(ContactoWhatsAppSuscripcion::getTipo).toList();
    }
}
