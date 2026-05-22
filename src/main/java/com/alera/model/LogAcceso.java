package com.alera.model;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.time.LocalDateTime;

@Entity
@Table(name = "log_accesos")
public class LogAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    @Column(length = 100)
    private String usuario;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(length = 80)
    private String ip;

    @Column(length = 500)
    private String url;

    @Column(name = "user_agent", length = 300)
    private String userAgent;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(columnDefinition = "TEXT")
    private String detalles;

    public static LogAcceso of(String usuario, String tipo, String ip,
                                String url, String userAgent, String detalles) {
        LogAcceso l = new LogAcceso();
        l.usuario   = usuario;
        l.tipo      = tipo;
        l.ip        = ip;
        l.url       = url;
        l.userAgent = userAgent != null && userAgent.length() > 300
                      ? userAgent.substring(0, 300) : userAgent;
        l.fecha     = LocalDateTime.now();
        l.detalles  = detalles;
        return l;
    }

    public Long getId()              { return id; }
    public String getTenantId()      { return tenantId; }
    public String getUsuario()       { return usuario; }
    public String getTipo()          { return tipo; }
    public String getIp()            { return ip; }
    public String getUrl()           { return url; }
    public String getUserAgent()     { return userAgent; }
    public LocalDateTime getFecha()  { return fecha; }
    public String getDetalles()      { return detalles; }
}
