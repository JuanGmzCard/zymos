package com.alera.model;

import com.alera.model.enums.EstadoVenta;
import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.time.LocalDateTime;

@Entity
@Table(name = "venta_historial_estado")
public class VentaHistorialEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    private String tenantId;

    @Column(nullable = false)
    private Long ventaId;

    @Enumerated(EnumType.STRING)
    private EstadoVenta estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoVenta estadoNuevo;

    private String usuario;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @PrePersist
    void prePersist() { this.fecha = LocalDateTime.now(); }

    public static VentaHistorialEstado of(Long ventaId,
                                          EstadoVenta anterior,
                                          EstadoVenta nuevo,
                                          String usuario) {
        var h = new VentaHistorialEstado();
        h.ventaId = ventaId;
        h.estadoAnterior = anterior;
        h.estadoNuevo = nuevo;
        h.usuario = usuario;
        return h;
    }

    public Long getId()                    { return id; }
    public String getTenantId()            { return tenantId; }
    public Long getVentaId()               { return ventaId; }
    public EstadoVenta getEstadoAnterior() { return estadoAnterior; }
    public EstadoVenta getEstadoNuevo()    { return estadoNuevo; }
    public String getUsuario()             { return usuario; }
    public LocalDateTime getFecha()        { return fecha; }
}
