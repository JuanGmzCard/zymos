package com.alera.model;

import com.alera.model.enums.EstadoVenta;
import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ventas")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id")
    private LoteCerveza lote;

    @Column(name = "codigo_lote", length = 50)
    private String codigoLote;

    @Column(nullable = false, length = 200)
    private String cliente;

    @Column(name = "fecha_despacho", nullable = false)
    private LocalDate fechaDespacho;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal cantidad;

    @Column(length = 50)
    private String unidad;

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "descuento_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal descuentoPct = BigDecimal.ZERO;

    @Column(length = 500)
    private String notas;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoVenta estado = EstadoVenta.PENDIENTE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;

    @Column(name = "last_modified_by")
    private String lastModifiedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastModifiedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        lastModifiedAt = LocalDateTime.now();
    }

    public BigDecimal getValorTotal() {
        if (cantidad == null || precioUnitario == null) return BigDecimal.ZERO;
        BigDecimal base = cantidad.multiply(precioUnitario);
        if (descuentoPct != null && descuentoPct.compareTo(BigDecimal.ZERO) > 0) {
            base = base.multiply(BigDecimal.ONE.subtract(
                    descuentoPct.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)));
        }
        return base.setScale(2, RoundingMode.HALF_UP);
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public LoteCerveza getLote() { return lote; }
    public void setLote(LoteCerveza lote) { this.lote = lote; }
    public String getCodigoLote() { return codigoLote; }
    public void setCodigoLote(String codigoLote) { this.codigoLote = codigoLote; }
    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }
    public LocalDate getFechaDespacho() { return fechaDespacho; }
    public void setFechaDespacho(LocalDate fechaDespacho) { this.fechaDespacho = fechaDespacho; }
    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }
    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }
    public BigDecimal getDescuentoPct() { return descuentoPct; }
    public void setDescuentoPct(BigDecimal descuentoPct) { this.descuentoPct = descuentoPct != null ? descuentoPct : BigDecimal.ZERO; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    public EstadoVenta getEstado() { return estado; }
    public void setEstado(EstadoVenta estado) { this.estado = estado; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getLastModifiedAt() { return lastModifiedAt; }
    public String getLastModifiedBy() { return lastModifiedBy; }
    public void setLastModifiedBy(String lastModifiedBy) { this.lastModifiedBy = lastModifiedBy; }
}
