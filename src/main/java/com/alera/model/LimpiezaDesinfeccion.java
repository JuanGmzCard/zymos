package com.alera.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.TenantId;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bpm_limpieza_desinfeccion")
public class LimpiezaDesinfeccion {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    @NotNull
    private LocalDate fecha;

    @Column(length = 20)
    private String dia;

    @NotBlank
    @Column(name = "area_utensilio", length = 200)
    private String areaUtensilio;

    @Column(name = "detergente_usado", length = 200)
    private String detergenteUsado;

    @Column(name = "sanitizador_usado", length = 200)
    private String sanitizadorUsado;

    @Column(length = 100)
    private String concentracion;

    @Column(length = 200)
    private String responsable;

    @Column(name = "visto_bueno")
    private boolean vistoBueno;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate v) { this.fecha = v; }
    public String getDia() { return dia; }
    public void setDia(String v) { this.dia = v; }
    public String getAreaUtensilio() { return areaUtensilio; }
    public void setAreaUtensilio(String v) { this.areaUtensilio = v; }
    public String getDetergenteUsado() { return detergenteUsado; }
    public void setDetergenteUsado(String v) { this.detergenteUsado = v; }
    public String getSanitizadorUsado() { return sanitizadorUsado; }
    public void setSanitizadorUsado(String v) { this.sanitizadorUsado = v; }
    public String getConcentracion() { return concentracion; }
    public void setConcentracion(String v) { this.concentracion = v; }
    public String getResponsable() { return responsable; }
    public void setResponsable(String v) { this.responsable = v; }
    public boolean isVistoBueno() { return vistoBueno; }
    public void setVistoBueno(boolean v) { this.vistoBueno = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
