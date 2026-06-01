package com.alera.model;

import com.alera.model.enums.EstadoVenta;
import jakarta.persistence.*;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ventas")
@SQLRestriction("deleted_at IS NULL")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    @Column(nullable = false, length = 200)
    private String cliente;

    @Column(name = "fecha_despacho", nullable = false)
    private LocalDate fechaDespacho;

    @Column(length = 500)
    private String notas;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoVenta estado = EstadoVenta.PENDIENTE;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<VentaItem> items = new ArrayList<>();

    @Formula("(SELECT COALESCE(SUM(vi.cantidad * vi.precio_unitario * (1 - vi.descuento_pct / 100.0)), 0) " +
             "FROM venta_items vi WHERE vi.venta_id = id)")
    private BigDecimal valorTotal;

    @Formula("(SELECT vi.codigo_lote FROM venta_items vi " +
             "WHERE vi.venta_id = id AND vi.codigo_lote IS NOT NULL ORDER BY vi.id LIMIT 1)")
    private String primerCodigoLote;

    @Formula("(SELECT COUNT(*) FROM venta_items vi WHERE vi.venta_id = id)")
    private Integer itemsCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;

    @Column(name = "last_modified_by")
    private String lastModifiedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

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
        return valorTotal != null ? valorTotal : BigDecimal.ZERO;
    }

    public String getPrimerCodigoLote() {
        return primerCodigoLote;
    }

    public int getItemsCount() {
        return itemsCount != null ? itemsCount : 0;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }
    public LocalDate getFechaDespacho() { return fechaDespacho; }
    public void setFechaDespacho(LocalDate fechaDespacho) { this.fechaDespacho = fechaDespacho; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    public EstadoVenta getEstado() { return estado; }
    public void setEstado(EstadoVenta estado) { this.estado = estado; }
    public List<VentaItem> getItems() { return items; }
    public void setItems(List<VentaItem> items) { this.items = items; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getLastModifiedAt() { return lastModifiedAt; }
    public String getLastModifiedBy() { return lastModifiedBy; }
    public void setLastModifiedBy(String lastModifiedBy) { this.lastModifiedBy = lastModifiedBy; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}
