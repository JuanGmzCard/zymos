package com.alera.dto;

import com.alera.model.enums.EstadoVenta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class VentaFormDto {

    private Long id;

    @NotNull(message = "El cliente es obligatorio")
    private Long clienteId;

    // Desnormalizado — se rellena desde Cliente.nombre al guardar
    private String cliente;

    @NotNull(message = "La fecha de despacho es obligatoria")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaDespacho;

    private String notas;

    private EstadoVenta estado = EstadoVenta.PENDIENTE;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate cotizacionExpiraEn;

    @Valid
    @Size(min = 1, message = "Debe agregar al menos un ítem")
    private List<VentaItemFormDto> items = new ArrayList<>();

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }
    public LocalDate getFechaDespacho() { return fechaDespacho; }
    public void setFechaDespacho(LocalDate fechaDespacho) { this.fechaDespacho = fechaDespacho; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    public EstadoVenta getEstado() { return estado; }
    public void setEstado(EstadoVenta estado) { this.estado = estado; }
    public LocalDate getCotizacionExpiraEn() { return cotizacionExpiraEn; }
    public void setCotizacionExpiraEn(LocalDate cotizacionExpiraEn) { this.cotizacionExpiraEn = cotizacionExpiraEn; }
    public List<VentaItemFormDto> getItems() { return items; }
    public void setItems(List<VentaItemFormDto> items) { this.items = items; }
}
