package com.alera.model;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.time.LocalDateTime;

@Entity
@Table(name = "historial_lotes")
public class HistorialLote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    @Column(nullable = false)
    private Long loteId;

    @Column(length = 50)
    private String codigoLote;

    @Column(nullable = false, length = 20)
    private String accion;

    @Column(length = 100)
    private String usuario;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(columnDefinition = "TEXT")
    private String notas;

    public static HistorialLote of(Long loteId, String codigoLote,
                                    String accion, String usuario, String notas) {
        HistorialLote h = new HistorialLote();
        h.loteId     = loteId;
        h.codigoLote = codigoLote;
        h.accion     = accion;
        h.usuario    = usuario;
        h.fecha      = LocalDateTime.now();
        h.notas      = notas;
        return h;
    }

    public Long getId()              { return id; }
    public String getTenantId()      { return tenantId; }
    public Long getLoteId()          { return loteId; }
    public String getCodigoLote()    { return codigoLote; }
    public String getAccion()        { return accion; }
    public String getUsuario()       { return usuario; }
    public LocalDateTime getFecha()  { return fecha; }
    public String getNotas()         { return notas; }
}
