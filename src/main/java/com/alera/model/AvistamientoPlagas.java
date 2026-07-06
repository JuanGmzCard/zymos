package com.alera.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.TenantId;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bpm_avistamiento_plagas")
public class AvistamientoPlagas {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    @NotNull
    private LocalDate fecha;

    @Column(name = "presencia_plagas")
    private boolean presenciaPlagas;

    @Column(name = "tipo_plagas", length = 200)
    private String tipoPlagas;

    @Column(name = "estado_ventanas_puertas", length = 10)
    private String estadoVentanasPuertas = "OK";

    @Column(name = "accion_tomada", columnDefinition = "TEXT")
    private String accionTomada;

    @Column(length = 200)
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
    public boolean isPresenciaPlagas() { return presenciaPlagas; }
    public void setPresenciaPlagas(boolean v) { this.presenciaPlagas = v; }
    public String getTipoPlagas() { return tipoPlagas; }
    public void setTipoPlagas(String v) { this.tipoPlagas = v; }
    public String getEstadoVentanasPuertas() { return estadoVentanasPuertas; }
    public void setEstadoVentanasPuertas(String v) { this.estadoVentanasPuertas = v; }
    public String getAccionTomada() { return accionTomada; }
    public void setAccionTomada(String v) { this.accionTomada = v; }
    public String getFirma() { return firma; }
    public void setFirma(String v) { this.firma = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
