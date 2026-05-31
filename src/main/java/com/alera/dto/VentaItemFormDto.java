package com.alera.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class VentaItemFormDto {

    private Long loteId;
    private String codigoLoteBuscador;
    private String descripcion;

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.001", message = "La cantidad debe ser mayor a 0")
    private BigDecimal cantidad;

    private String unidad;

    @NotNull(message = "El precio unitario es obligatorio")
    @DecimalMin(value = "0", inclusive = true, message = "El precio no puede ser negativo")
    private BigDecimal precioUnitario;

    private BigDecimal descuentoPct = BigDecimal.ZERO;

    // Getters & Setters
    public Long getLoteId() { return loteId; }
    public void setLoteId(Long loteId) { this.loteId = loteId; }
    public String getCodigoLoteBuscador() { return codigoLoteBuscador; }
    public void setCodigoLoteBuscador(String c) { this.codigoLoteBuscador = c; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }
    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal p) { this.precioUnitario = p; }
    public BigDecimal getDescuentoPct() { return descuentoPct; }
    public void setDescuentoPct(BigDecimal d) { this.descuentoPct = d != null ? d : BigDecimal.ZERO; }
}
