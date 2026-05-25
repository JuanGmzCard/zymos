package com.alera.model;

import com.alera.model.enums.EstadoFactura;
import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.time.LocalDateTime;

@Entity
@Table(name = "factura_historial_estado")
public class FacturaHistorialEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    private String tenantId;

    @Column(nullable = false)
    private Long facturaId;

    @Enumerated(EnumType.STRING)
    private EstadoFactura estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoFactura estadoNuevo;

    private String usuario;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @PrePersist
    void prePersist() { this.fecha = LocalDateTime.now(); }

    public static FacturaHistorialEstado of(Long facturaId,
                                            EstadoFactura anterior,
                                            EstadoFactura nuevo,
                                            String usuario) {
        var h = new FacturaHistorialEstado();
        h.facturaId = facturaId;
        h.estadoAnterior = anterior;
        h.estadoNuevo = nuevo;
        h.usuario = usuario;
        return h;
    }

    public Long getId()                      { return id; }
    public String getTenantId()              { return tenantId; }
    public Long getFacturaId()               { return facturaId; }
    public EstadoFactura getEstadoAnterior() { return estadoAnterior; }
    public EstadoFactura getEstadoNuevo()    { return estadoNuevo; }
    public String getUsuario()               { return usuario; }
    public LocalDateTime getFecha()          { return fecha; }
}
