package com.alera.model;

import com.alera.model.enums.EstadoEquipo;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "equipos")
public class Equipo extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoEquipo estado = EstadoEquipo.OPERATIVO;

    @Column(precision = 10, scale = 2)
    private BigDecimal capacidad;

    private String unidadCapacidad;

    private LocalDate fechaAdquisicion;
    private LocalDate fechaUltimoMantenimiento;
    private LocalDate proximoMantenimiento;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    public boolean isMantenimientoPendiente() {
        if (proximoMantenimiento == null) return false;
        return !proximoMantenimiento.isAfter(LocalDate.now().plusDays(7));
    }

    public String getColorEstado() {
        return switch (estado) {
            case OPERATIVO -> "success";
            case MANTENIMIENTO -> "warning";
            case INACTIVO -> "secondary";
        };
    }

    public String getNombreTipo() {
        return tipo != null ? tipo : "";
    }

    public boolean isEnMantenimiento() {
        return estado == EstadoEquipo.MANTENIMIENTO;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public EstadoEquipo getEstado() { return estado; }
    public void setEstado(EstadoEquipo estado) { this.estado = estado; }
    public BigDecimal getCapacidad() { return capacidad; }
    public void setCapacidad(BigDecimal capacidad) { this.capacidad = capacidad; }
    public String getUnidadCapacidad() { return unidadCapacidad; }
    public void setUnidadCapacidad(String unidadCapacidad) { this.unidadCapacidad = unidadCapacidad; }
    public LocalDate getFechaAdquisicion() { return fechaAdquisicion; }
    public void setFechaAdquisicion(LocalDate fechaAdquisicion) { this.fechaAdquisicion = fechaAdquisicion; }
    public LocalDate getFechaUltimoMantenimiento() { return fechaUltimoMantenimiento; }
    public void setFechaUltimoMantenimiento(LocalDate fechaUltimoMantenimiento) { this.fechaUltimoMantenimiento = fechaUltimoMantenimiento; }
    public LocalDate getProximoMantenimiento() { return proximoMantenimiento; }
    public void setProximoMantenimiento(LocalDate proximoMantenimiento) { this.proximoMantenimiento = proximoMantenimiento; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}
