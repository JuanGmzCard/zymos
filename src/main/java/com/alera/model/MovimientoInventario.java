package com.alera.model;

import com.alera.model.enums.TipoMovimiento;
import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos_inventario")
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    private String tenantId;

    @Column(name = "insumo_id", nullable = false)
    private Long insumoId;

    @Column(name = "insumo_nombre", nullable = false, length = 200)
    private String insumoNombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoMovimiento tipo;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidad;

    @Column(name = "cantidad_anterior", precision = 14, scale = 3)
    private BigDecimal cantidadAnterior;

    @Column(name = "cantidad_posterior", precision = 14, scale = 3)
    private BigDecimal cantidadPosterior;

    @Column(length = 300)
    private String motivo;

    @Column(length = 100)
    private String referencia;

    @Column(length = 100)
    private String usuario;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @PrePersist
    void prePersist() {
        if (fecha == null) fecha = LocalDateTime.now();
    }

    public static MovimientoInventario of(Long insumoId, String insumoNombre, TipoMovimiento tipo,
                                          BigDecimal cantidad, BigDecimal cantidadAnterior,
                                          BigDecimal cantidadPosterior, String motivo,
                                          String referencia, String usuario) {
        MovimientoInventario m = new MovimientoInventario();
        m.insumoId          = insumoId;
        m.insumoNombre      = insumoNombre;
        m.tipo              = tipo;
        m.cantidad          = cantidad;
        m.cantidadAnterior  = cantidadAnterior;
        m.cantidadPosterior = cantidadPosterior;
        m.motivo            = motivo;
        m.referencia        = referencia;
        m.usuario           = usuario;
        return m;
    }

    public Long getId()                    { return id; }
    public String getTenantId()            { return tenantId; }
    public Long getInsumoId()              { return insumoId; }
    public String getInsumoNombre()        { return insumoNombre; }
    public TipoMovimiento getTipo()        { return tipo; }
    public BigDecimal getCantidad()        { return cantidad; }
    public BigDecimal getCantidadAnterior()  { return cantidadAnterior; }
    public BigDecimal getCantidadPosterior() { return cantidadPosterior; }
    public String getMotivo()              { return motivo; }
    public String getReferencia()          { return referencia; }
    public String getUsuario()             { return usuario; }
    public LocalDateTime getFecha()        { return fecha; }
}
