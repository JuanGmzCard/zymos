package com.alera.model;

import com.alera.model.enums.PlanNivel;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "checkout_pendiente")
public class CheckoutPendiente {

    @Id
    private String id;

    @Column(nullable = false)
    private String subdominio;

    @Column(nullable = false)
    private String email;

    @Column(name = "nombre_cerveceria", nullable = false)
    private String nombreCerveceria;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_nivel", nullable = false)
    private PlanNivel planNivel;

    @Column(name = "mp_preference_id")
    private String mpPreferenceId;

    @Column(name = "mp_payment_id")
    private String mpPaymentId;

    /** PENDIENTE | PROCESADO | FALLIDO */
    @Column(nullable = false)
    private String estado = "PENDIENTE";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "procesado_at")
    private LocalDateTime procesadoAt;

    @PrePersist
    void prePersist() { createdAt = LocalDateTime.now(); }

    // Getters / setters
    public String          getId()                 { return id; }
    public void            setId(String v)         { this.id = v; }
    public String          getSubdominio()          { return subdominio; }
    public void            setSubdominio(String v)  { this.subdominio = v; }
    public String          getEmail()               { return email; }
    public void            setEmail(String v)       { this.email = v; }
    public String          getNombreCerveceria()    { return nombreCerveceria; }
    public void            setNombreCerveceria(String v) { this.nombreCerveceria = v; }
    public PlanNivel       getPlanNivel()            { return planNivel; }
    public void            setPlanNivel(PlanNivel v) { this.planNivel = v; }
    public String          getMpPreferenceId()       { return mpPreferenceId; }
    public void            setMpPreferenceId(String v){ this.mpPreferenceId = v; }
    public String          getMpPaymentId()          { return mpPaymentId; }
    public void            setMpPaymentId(String v)  { this.mpPaymentId = v; }
    public String          getEstado()               { return estado; }
    public void            setEstado(String v)       { this.estado = v; }
    public LocalDateTime   getCreatedAt()            { return createdAt; }
    public void            setCreatedAt(LocalDateTime v){ this.createdAt = v; }
    public LocalDateTime   getProcesadoAt()          { return procesadoAt; }
    public void            setProcesadoAt(LocalDateTime v){ this.procesadoAt = v; }
}
