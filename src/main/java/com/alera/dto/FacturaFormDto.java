package com.alera.dto;

import com.alera.model.enums.EstadoFactura;
import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class FacturaFormDto {

    private String numeroFactura;
    private EstadoFactura estado = EstadoFactura.RECIBIDA;
    private Long proveedorId;

    @NotBlank(message = "El proveedor es obligatorio")
    private String proveedor;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaFactura;
    private String descripcion;
    private BigDecimal porcentajeIva = BigDecimal.valueOf(19);
    private BigDecimal costoEnvio = BigDecimal.ZERO;
    private boolean ivaIncluido = false;

    private List<FacturaItemDto> items = new ArrayList<>();

    public static FacturaFormDto empty() {
        FacturaFormDto dto = new FacturaFormDto();
        dto.items.add(new FacturaItemDto());
        return dto;
    }

    public String getNumeroFactura() { return numeroFactura; }
    public void setNumeroFactura(String numeroFactura) { this.numeroFactura = numeroFactura; }
    public EstadoFactura getEstado() { return estado; }
    public void setEstado(EstadoFactura estado) { this.estado = estado; }
    public Long getProveedorId() { return proveedorId; }
    public void setProveedorId(Long proveedorId) { this.proveedorId = proveedorId; }
    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }
    public LocalDate getFechaFactura() { return fechaFactura; }
    public void setFechaFactura(LocalDate fechaFactura) { this.fechaFactura = fechaFactura; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public BigDecimal getPorcentajeIva() { return porcentajeIva; }
    public void setPorcentajeIva(BigDecimal porcentajeIva) { this.porcentajeIva = porcentajeIva; }
    public BigDecimal getCostoEnvio() { return costoEnvio; }
    public void setCostoEnvio(BigDecimal costoEnvio) { this.costoEnvio = costoEnvio; }
    public boolean isIvaIncluido() { return ivaIncluido; }
    public void setIvaIncluido(boolean ivaIncluido) { this.ivaIncluido = ivaIncluido; }
    public List<FacturaItemDto> getItems() { return items; }
    public void setItems(List<FacturaItemDto> items) { this.items = items; }
}
