package com.alera.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.TenantId;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bpm_registros_sintomas")
public class RegistroSintomas {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    @NotNull
    private LocalDate fecha;

    @NotBlank
    @Column(name = "nombre_manipulador", length = 200)
    private String nombreManipulador;

    private boolean diarrea;
    private boolean vomito;
    private boolean fiebre;

    @Column(name = "infeccion_respiratoria")
    private boolean infeccionRespiratoria;

    @Column(name = "lesion_piel")
    private boolean lesionPiel;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "firma_manipulador", length = 200)
    private String firmaManipulador;

    @Column(name = "firma_responsable", length = 200)
    private String firmaResponsable;

    @Column(name = "autorizado_por_admin")
    private boolean autorizadoPorAdmin = false;

    @Column(name = "autorizado_por", length = 100)
    private String autorizadoPor;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public String getNombreManipulador() { return nombreManipulador; }
    public void setNombreManipulador(String v) { this.nombreManipulador = v; }
    public boolean isDiarrea() { return diarrea; }
    public void setDiarrea(boolean v) { this.diarrea = v; }
    public boolean isVomito() { return vomito; }
    public void setVomito(boolean v) { this.vomito = v; }
    public boolean isFiebre() { return fiebre; }
    public void setFiebre(boolean v) { this.fiebre = v; }
    public boolean isInfeccionRespiratoria() { return infeccionRespiratoria; }
    public void setInfeccionRespiratoria(boolean v) { this.infeccionRespiratoria = v; }
    public boolean isLesionPiel() { return lesionPiel; }
    public void setLesionPiel(boolean v) { this.lesionPiel = v; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String v) { this.observaciones = v; }
    public String getFirmaManipulador() { return firmaManipulador; }
    public void setFirmaManipulador(String v) { this.firmaManipulador = v; }
    public String getFirmaResponsable() { return firmaResponsable; }
    public void setFirmaResponsable(String v) { this.firmaResponsable = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isAutorizadoPorAdmin() { return autorizadoPorAdmin; }
    public void setAutorizadoPorAdmin(boolean v) { this.autorizadoPorAdmin = v; }
    public String getAutorizadoPor() { return autorizadoPor; }
    public void setAutorizadoPor(String v) { this.autorizadoPor = v; }

    public boolean tieneSintomas() {
        return diarrea || vomito || fiebre || infeccionRespiratoria || lesionPiel;
    }
}
