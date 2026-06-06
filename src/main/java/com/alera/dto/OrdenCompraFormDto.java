package com.alera.dto;

import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OrdenCompraFormDto {

    private Long proveedorId;
    private String proveedor;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaEmision = LocalDate.now();

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaRequerida;

    private String notas;

    private List<OrdenCompraItemDto> items = new ArrayList<>();

    public Long getProveedorId()                       { return proveedorId; }
    public void setProveedorId(Long proveedorId)       { this.proveedorId = proveedorId; }
    public String getProveedor()                       { return proveedor; }
    public void setProveedor(String proveedor)         { this.proveedor = proveedor; }
    public LocalDate getFechaEmision()                 { return fechaEmision; }
    public void setFechaEmision(LocalDate d)           { this.fechaEmision = d; }
    public LocalDate getFechaRequerida()               { return fechaRequerida; }
    public void setFechaRequerida(LocalDate d)         { this.fechaRequerida = d; }
    public String getNotas()                           { return notas; }
    public void setNotas(String notas)                 { this.notas = notas; }
    public List<OrdenCompraItemDto> getItems()         { return items; }
    public void setItems(List<OrdenCompraItemDto> i)   { this.items = i; }
}
