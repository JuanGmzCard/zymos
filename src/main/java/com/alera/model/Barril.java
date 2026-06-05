package com.alera.model;

import com.alera.model.enums.EstadoBarril;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "barriles")
public class Barril extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String codigo;

    private String tipo;

    @Column(precision = 8, scale = 2)
    private BigDecimal capacidadLitros;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoBarril estado = EstadoBarril.DISPONIBLE;

    @Column(name = "lote_id")
    private Long loteId;

    private String codigoLote;

    private String clienteNombre;

    private LocalDate fechaDespacho;

    @Column(length = 500)
    private String observaciones;

    public boolean isDisponible()  { return estado == EstadoBarril.DISPONIBLE; }
    public boolean isLleno()       { return estado == EstadoBarril.LLENO; }
    public boolean isDespachado()  { return estado == EstadoBarril.DESPACHADO; }
    public boolean isEnBaja()      { return estado == EstadoBarril.BAJA; }

    public Long getId()                       { return id; }
    public void setId(Long id)                { this.id = id; }
    public String getCodigo()                 { return codigo; }
    public void setCodigo(String codigo)      { this.codigo = codigo; }
    public String getTipo()                   { return tipo; }
    public void setTipo(String tipo)          { this.tipo = tipo; }
    public BigDecimal getCapacidadLitros()    { return capacidadLitros; }
    public void setCapacidadLitros(BigDecimal capacidadLitros) { this.capacidadLitros = capacidadLitros; }
    public EstadoBarril getEstado()           { return estado; }
    public void setEstado(EstadoBarril estado){ this.estado = estado; }
    public Long getLoteId()                   { return loteId; }
    public void setLoteId(Long loteId)        { this.loteId = loteId; }
    public String getCodigoLote()             { return codigoLote; }
    public void setCodigoLote(String codigoLote)    { this.codigoLote = codigoLote; }
    public String getClienteNombre()                { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }
    public LocalDate getFechaDespacho()             { return fechaDespacho; }
    public void setFechaDespacho(LocalDate fechaDespacho) { this.fechaDespacho = fechaDespacho; }
    public String getObservaciones()                { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}
