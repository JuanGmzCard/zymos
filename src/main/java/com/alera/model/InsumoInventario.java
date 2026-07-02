package com.alera.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "insumos_inventario")
public class InsumoInventario extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String tipo;

    @Column(precision = 14, scale = 3)
    private BigDecimal cantidad = BigDecimal.ZERO;

    private String unidad;

    @Column(precision = 14, scale = 3)
    private BigDecimal stockMinimo = BigDecimal.ZERO;

    private String proveedor;

    private LocalDate fechaVencimiento;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    public boolean isBajoStock() {
        if (stockMinimo == null || cantidad == null) return false;
        return cantidad.compareTo(stockMinimo) <= 0;
    }

    public int getPorcentajeStock() {
        if (stockMinimo == null || stockMinimo.compareTo(BigDecimal.ZERO) == 0) return 100;
        if (cantidad == null) return 0;
        BigDecimal pct = cantidad.multiply(BigDecimal.valueOf(100)).divide(stockMinimo, 0, java.math.RoundingMode.HALF_UP);
        return Math.min(pct.intValue(), 100);
    }

    public String getClaseVencimiento() {
        if (fechaVencimiento == null) return "";
        LocalDate hoy = LocalDate.now();
        if (fechaVencimiento.isBefore(hoy.plusDays(8))) return "text-danger fw-bold";
        if (fechaVencimiento.isBefore(hoy.plusDays(31))) return "text-warning fw-bold";
        return "";
    }

    public String getTextoVencimiento() {
        if (fechaVencimiento == null) return "Sin vencimiento";
        LocalDate hoy = LocalDate.now();
        long dias = hoy.until(fechaVencimiento, java.time.temporal.ChronoUnit.DAYS);
        if (dias < 0) return "Vencido";
        if (dias == 0) return "Vence hoy";
        if (dias == 1) return "Vence mañana";
        if (dias <= 7) return "Vence en " + dias + " días";
        return fechaVencimiento.toString();
    }

    public String getColorBadgeVencimiento() {
        if (fechaVencimiento == null) return "secondary";
        LocalDate hoy = LocalDate.now();
        long dias = hoy.until(fechaVencimiento, java.time.temporal.ChronoUnit.DAYS);
        if (dias <= 7) return "danger";
        if (dias <= 30) return "warning";
        return "success";
    }

    public String getColorTipo() {
        if (tipo == null) return "secondary";
        return switch (tipo) {
            case "Malta"                   -> "warning";
            case "Lúpulo"                  -> "success";
            case "Levadura"                -> "info";
            case "Clarificante"            -> "primary";
            case "Agente de Carbonatación" -> "warning";
            case "Agua"                    -> "info";
            case "Químico"                 -> "danger";
            case "Envase"                  -> "secondary";
            default                        -> "dark";
        };
    }

    // ── Display inteligente (delega a UnidadUtils) ───────────────────────────

    public BigDecimal getCantidadDisplay()          { return com.alera.config.UnidadUtils.displayValor(cantidad,    unidad); }
    public String     getUnidadDisplay()            { return com.alera.config.UnidadUtils.displayUnidad(cantidad,   unidad); }
    public BigDecimal getStockMinimoDisplay()       { return com.alera.config.UnidadUtils.displayValor(stockMinimo, unidad); }
    public String     getUnidadStockMinimoDisplay() { return com.alera.config.UnidadUtils.displayUnidad(stockMinimo, unidad); }

    public String formatearCantidad(BigDecimal valor) {
        return com.alera.config.UnidadUtils.displayTexto(valor, unidad);
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }
    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
    public BigDecimal getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(BigDecimal stockMinimo) { this.stockMinimo = stockMinimo; }
    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}
