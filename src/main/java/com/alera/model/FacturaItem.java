package com.alera.model;

import com.alera.model.enums.TipoItemFactura;
import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "factura_items")
public class FacturaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    private TipoItemFactura tipoItem;

    private String nombre;

    private String tipoInsumo;

    private String tipoEquipo;

    @Column(precision = 10, scale = 3)
    private BigDecimal cantidad = BigDecimal.ONE;

    private String unidad;

    @Column(precision = 15, scale = 2)
    private BigDecimal valorUnitario = BigDecimal.ZERO;

    @Column(precision = 5, scale = 2)
    private BigDecimal porcentajeDescuento = BigDecimal.ZERO;

    @Column(precision = 5, scale = 2)
    private BigDecimal porcentajeIvaItem = BigDecimal.ZERO;

    @Column(name = "impuesto_consumo", precision = 15, scale = 2)
    private BigDecimal impuestoConsumo = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal valorLinea = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id")
    private FacturaProveedor factura;

    public BigDecimal getValorUnitarioSinIva() {
        if (valorUnitario == null) return BigDecimal.ZERO;
        if (factura != null && factura.isIvaIncluido()) {
            BigDecimal iva = porcentajeIvaItem != null ? porcentajeIvaItem : BigDecimal.ZERO;
            if (iva.compareTo(BigDecimal.ZERO) == 0) return valorUnitario;
            return valorUnitario.divide(
                BigDecimal.ONE.add(iva.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)),
                2, RoundingMode.HALF_UP);
        }
        return valorUnitario;
    }

    public BigDecimal getValorBase() {
        if (cantidad == null || valorUnitario == null) return BigDecimal.ZERO;
        BigDecimal desc = porcentajeDescuento != null ? porcentajeDescuento : BigDecimal.ZERO;
        return cantidad
                .multiply(getValorUnitarioSinIva())
                .multiply(BigDecimal.ONE.subtract(desc.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getValorIvaItem() {
        BigDecimal iva = porcentajeIvaItem != null ? porcentajeIvaItem : BigDecimal.ZERO;
        return getValorBase()
                .multiply(iva.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calcularValorLinea() {
        BigDecimal ic = impuestoConsumo != null ? impuestoConsumo : BigDecimal.ZERO;
        return getValorBase().add(getValorIvaItem()).add(ic);
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TipoItemFactura getTipoItem() { return tipoItem; }
    public void setTipoItem(TipoItemFactura tipoItem) { this.tipoItem = tipoItem; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getTipoInsumo() { return tipoInsumo; }
    public void setTipoInsumo(String tipoInsumo) { this.tipoInsumo = tipoInsumo; }
    public String getTipoEquipo() { return tipoEquipo; }
    public void setTipoEquipo(String tipoEquipo) { this.tipoEquipo = tipoEquipo; }
    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }
    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
    public BigDecimal getValorUnitario() { return valorUnitario; }
    public void setValorUnitario(BigDecimal valorUnitario) { this.valorUnitario = valorUnitario; }
    public BigDecimal getPorcentajeDescuento() { return porcentajeDescuento; }
    public void setPorcentajeDescuento(BigDecimal porcentajeDescuento) { this.porcentajeDescuento = porcentajeDescuento; }
    public BigDecimal getPorcentajeIvaItem() { return porcentajeIvaItem; }
    public void setPorcentajeIvaItem(BigDecimal porcentajeIvaItem) { this.porcentajeIvaItem = porcentajeIvaItem; }
    public BigDecimal getImpuestoConsumo() { return impuestoConsumo; }
    public void setImpuestoConsumo(BigDecimal impuestoConsumo) { this.impuestoConsumo = impuestoConsumo; }
    public BigDecimal getValorLinea() { return valorLinea; }
    public void setValorLinea(BigDecimal valorLinea) { this.valorLinea = valorLinea; }
    public FacturaProveedor getFactura() { return factura; }
    public void setFactura(FacturaProveedor factura) { this.factura = factura; }
}
