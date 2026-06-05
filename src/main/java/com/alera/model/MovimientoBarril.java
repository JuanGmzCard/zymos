package com.alera.model;

import com.alera.model.enums.EstadoBarril;
import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos_barriles")
public class MovimientoBarril {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    private String tenantId;

    @Column(nullable = false)
    private Long barrilId;

    @Enumerated(EnumType.STRING)
    private EstadoBarril estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoBarril estadoNuevo;

    private String usuario;

    @Column(length = 500)
    private String notas;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @PrePersist
    void prePersist() { this.fecha = LocalDateTime.now(); }

    public static MovimientoBarril of(Long barrilId,
                                      EstadoBarril anterior,
                                      EstadoBarril nuevo,
                                      String usuario,
                                      String notas) {
        var m = new MovimientoBarril();
        m.barrilId       = barrilId;
        m.estadoAnterior = anterior;
        m.estadoNuevo    = nuevo;
        m.usuario        = usuario;
        m.notas          = (notas != null && !notas.isBlank()) ? notas.trim() : null;
        return m;
    }

    public Long getId()                      { return id; }
    public String getTenantId()              { return tenantId; }
    public Long getBarrilId()                { return barrilId; }
    public EstadoBarril getEstadoAnterior()  { return estadoAnterior; }
    public EstadoBarril getEstadoNuevo()     { return estadoNuevo; }
    public String getUsuario()               { return usuario; }
    public String getNotas()                 { return notas; }
    public LocalDateTime getFecha()          { return fecha; }
}
