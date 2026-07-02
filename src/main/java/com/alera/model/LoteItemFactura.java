package com.alera.model;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "lote_items_factura")
public class LoteItemFactura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id", nullable = false)
    private LoteCerveza lote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_item_id", nullable = false)
    private FacturaItem item;

    @Column(name = "cantidad_asignada", nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidadAsignada;

    // Costo proporcional: (cantidadAsignada / item.cantidad) × item.valorLinea
    // Excepción: cantidad = 0 → costo completo del ítem (costo sin ingrediente, ej: envase, flete)
    public BigDecimal getValorAsignado() {
        if (item == null) return BigDecimal.ZERO;
        BigDecimal valorLinea = item.getValorLinea() != null ? item.getValorLinea() : BigDecimal.ZERO;
        if (cantidadAsignada == null || cantidadAsignada.compareTo(BigDecimal.ZERO) == 0) {
            return valorLinea;
        }
        if (item.getCantidad() == null || item.getCantidad().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return cantidadAsignada
                .multiply(valorLinea)
                .divide(item.getCantidad(), 2, RoundingMode.HALF_UP);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LoteCerveza getLote() { return lote; }
    public void setLote(LoteCerveza lote) { this.lote = lote; }
    public FacturaItem getItem() { return item; }
    public void setItem(FacturaItem item) { this.item = item; }
    public BigDecimal getCantidadAsignada() { return cantidadAsignada; }
    public void setCantidadAsignada(BigDecimal cantidadAsignada) { this.cantidadAsignada = cantidadAsignada; }
    public BigDecimal getCantidadAsignadaDisplay() {
        String u = item != null ? item.getUnidad() : null;
        return com.alera.config.UnidadUtils.displayValor(cantidadAsignada, u);
    }
    public String getUnidadAsignadaDisplay() {
        String u = item != null ? item.getUnidad() : null;
        return com.alera.config.UnidadUtils.displayUnidad(cantidadAsignada, u);
    }
}