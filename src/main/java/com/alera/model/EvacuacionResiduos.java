package com.alera.model;

import com.alera.model.enums.TipoResiduo;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.TenantId;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "bpm_evacuacion_residuos")
public class EvacuacionResiduos {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecha;

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    @Column(name = "hora_salida")
    private LocalTime horaSalida;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_residuo", length = 50)
    private TipoResiduo tipoResiduo;

    @Column(name = "recipientes_limpios")
    private boolean recipientesLimpios;

    @Column(length = 200)
    private String responsable;

    @Column(columnDefinition = "TEXT")
    private String firma;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate v) { this.fecha = v; }
    public LocalTime getHoraSalida() { return horaSalida; }
    public void setHoraSalida(LocalTime v) { this.horaSalida = v; }
    public TipoResiduo getTipoResiduo() { return tipoResiduo; }
    public void setTipoResiduo(TipoResiduo v) { this.tipoResiduo = v; }
    public boolean isRecipientesLimpios() { return recipientesLimpios; }
    public void setRecipientesLimpios(boolean v) { this.recipientesLimpios = v; }
    public String getResponsable() { return responsable; }
    public void setResponsable(String v) { this.responsable = v; }
    public String getFirma() { return firma; }
    public void setFirma(String v) { this.firma = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
