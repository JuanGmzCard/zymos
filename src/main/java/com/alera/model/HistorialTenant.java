package com.alera.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "historial_tenants")
public class HistorialTenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String subdomain;

    @Column(nullable = false, length = 50)
    private String accion;

    @Column(length = 100)
    private String usuario;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(length = 500)
    private String detalles;

    public static HistorialTenant of(String subdomain, String accion, String usuario, String detalles) {
        HistorialTenant h = new HistorialTenant();
        h.subdomain = subdomain;
        h.accion    = accion;
        h.usuario   = usuario;
        h.fecha     = LocalDateTime.now();
        h.detalles  = detalles;
        return h;
    }

    public Long getId()             { return id; }
    public String getSubdomain()    { return subdomain; }
    public String getAccion()       { return accion; }
    public String getUsuario()      { return usuario; }
    public LocalDateTime getFecha() { return fecha; }
    public String getDetalles()     { return detalles; }
}
