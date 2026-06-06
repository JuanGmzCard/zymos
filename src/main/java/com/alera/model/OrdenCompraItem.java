package com.alera.model;

import com.alera.model.enums.TipoItemFactura;
import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "orden_compra_items")
public class OrdenCompraItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_id", nullable = false)
    private OrdenCompra orden;

    @Enumerated(EnumType.STRING)
    private TipoItemFactura tipoItem;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(length = 300)
    private String descripcion;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal cantidad = BigDecimal.ONE;

    private String unidad;

    @Column(precision = 12, scale = 2)
    private BigDecimal precioUnitarioEstimado;

    @Column(precision = 5, scale = 2)
    private BigDecimal porcentajeIvaItem = BigDecimal.ZERO;

    @Column(length = 100)
    private String tipoInsumo;

    @Column(length = 100)
    private String tipoEquipo;

    public BigDecimal getValorLinea() {
        if (cantidad == null || precioUnitarioEstimado == null) return BigDecimal.ZERO;
        BigDecimal iva = porcentajeIvaItem != null ? porcentajeIvaItem : BigDecimal.ZERO;
        return cantidad
                .multiply(precioUnitarioEstimado)
                .multiply(BigDecimal.ONE.add(iva.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public Long getId()                                      { return id; }
    public void setId(Long id)                               { this.id = id; }
    public String getTenantId()                              { return tenantId; }
    public OrdenCompra getOrden()                            { return orden; }
    public void setOrden(OrdenCompra orden)                  { this.orden = orden; }
    public TipoItemFactura getTipoItem()                     { return tipoItem; }
    public void setTipoItem(TipoItemFactura tipoItem)        { this.tipoItem = tipoItem; }
    public String getNombre()                                { return nombre; }
    public void setNombre(String nombre)                     { this.nombre = nombre; }
    public String getDescripcion()                           { return descripcion; }
    public void setDescripcion(String descripcion)           { this.descripcion = descripcion; }
    public BigDecimal getCantidad()                          { return cantidad; }
    public void setCantidad(BigDecimal cantidad)             { this.cantidad = cantidad; }
    public String getUnidad()                                { return unidad; }
    public void setUnidad(String unidad)                     { this.unidad = unidad; }
    public BigDecimal getPrecioUnitarioEstimado()            { return precioUnitarioEstimado; }
    public void setPrecioUnitarioEstimado(BigDecimal p)      { this.precioUnitarioEstimado = p; }
    public BigDecimal getPorcentajeIvaItem()                 { return porcentajeIvaItem; }
    public void setPorcentajeIvaItem(BigDecimal p)           { this.porcentajeIvaItem = p; }
    public String getTipoInsumo()                            { return tipoInsumo; }
    public void setTipoInsumo(String tipoInsumo)             { this.tipoInsumo = tipoInsumo; }
    public String getTipoEquipo()                            { return tipoEquipo; }
    public void setTipoEquipo(String tipoEquipo)             { this.tipoEquipo = tipoEquipo; }
}
