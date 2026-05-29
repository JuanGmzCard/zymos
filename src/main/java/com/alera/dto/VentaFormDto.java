package com.alera.dto;

import com.alera.model.enums.EstadoVenta;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public class VentaFormDto {

    private Long id;

    private Long loteId;
    private String codigoLoteBuscador;

    @NotBlank(message = "El cliente es obligatorio")
    private String cliente;

    @NotNull(message = "La fecha de despacho es obligatoria")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaDespacho;

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.001", message = "La cantidad debe ser mayor a 0")
    private BigDecimal cantidad;

    private String unidad;

    @NotNull(message = "El precio unitario es obligatorio")
    @DecimalMin(value = "0", message = "El precio no puede ser negativo")
    private BigDecimal precioUnitario;

    private BigDecimal descuentoPct = BigDecimal.ZERO;

    private String notas;

    private EstadoVenta estado = EstadoVenta.PENDIENTE;

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getLoteId() { return loteId; }
    public void setLoteId(Long loteId) { this.loteId = loteId; }
    public String getCodigoLoteBuscador() { return codigoLoteBuscador; }
    public void setCodigoLoteBuscador(String codigoLoteBuscador) { this.codigoLoteBuscador = codigoLoteBuscador; }
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
}
