package com.alera.dto;

import com.alera.model.enums.TipoItemFactura;
import java.math.BigDecimal;

public class OrdenCompraItemDto {

    private TipoItemFactura tipoItem;
    private String nombre;
    private String descripcion;
    private BigDecimal cantidad;
    private String unidad;
    private BigDecimal precioUnitarioEstimado;
    private BigDecimal porcentajeIvaItem = BigDecimal.ZERO;
    private String tipoInsumo;
    private String tipoEquipo;

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
