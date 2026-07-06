package com.alera.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.TenantId;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "bpm_soluciones_desinfectantes")
public class SolucionDesinfectante {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecha;

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime hora;

    @NotBlank
    @Column(length = 200)
    private String producto;

    @Column(name = "cantidad_agua", precision = 10, scale = 3)
    private BigDecimal cantidadAgua;

    @Column(name = "unidad_agua", length = 10)
    private String unidadAgua = "L";

    @Column(name = "cantidad_producto", precision = 10, scale = 3)
    private BigDecimal cantidadProducto;

    @Column(name = "unidad_producto", length = 10)
    private String unidadProducto = "mL";

    @Column(name = "concentracion_final", precision = 10, scale = 2)
    private BigDecimal concentracionFinal;

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
    public LocalTime getHora() { return hora; }
    public void setHora(LocalTime v) { this.hora = v; }
    public String getProducto() { return producto; }
    public void setProducto(String v) { this.producto = v; }
    public BigDecimal getCantidadAgua() { return cantidadAgua; }
    public void setCantidadAgua(BigDecimal v) { this.cantidadAgua = v; }
    public String getUnidadAgua() { return unidadAgua; }
    public void setUnidadAgua(String v) { this.unidadAgua = v; }
    public BigDecimal getCantidadProducto() { return cantidadProducto; }
    public void setCantidadProducto(BigDecimal v) { this.cantidadProducto = v; }
    public String getUnidadProducto() { return unidadProducto; }
    public void setUnidadProducto(String v) { this.unidadProducto = v; }
    public BigDecimal getConcentracionFinal() { return concentracionFinal; }
    public void setConcentracionFinal(BigDecimal v) { this.concentracionFinal = v; }
    public String getResponsable() { return responsable; }
    public void setResponsable(String v) { this.responsable = v; }
    public String getFirma() { return firma; }
    public void setFirma(String v) { this.firma = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
