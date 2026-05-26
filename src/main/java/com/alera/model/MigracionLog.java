package com.alera.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "migracion_log")
public class MigracionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(nullable = false, length = 50)
    private String modulo;

    @Column(length = 255)
    private String archivo;

    @Column(nullable = false)
    private int procesadas;

    @Column(nullable = false)
    private int exitosas;

    @Column(name = "con_errores", nullable = false)
    private int conErrores;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(columnDefinition = "TEXT")
    private String detalles;

    @Column(length = 100)
    private String usuario;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @PrePersist
    void prePersist() {
        if (fecha == null) fecha = LocalDateTime.now();
    }

    public static MigracionLog of(String tenantId, String modulo, String archivo,
                                   int procesadas, int exitosas, int conErrores,
                                   String estado, String detalles, String usuario) {
        MigracionLog log = new MigracionLog();
        log.tenantId    = tenantId;
        log.modulo      = modulo;
        log.archivo     = archivo;
        log.procesadas  = procesadas;
        log.exitosas    = exitosas;
        log.conErrores  = conErrores;
        log.estado      = estado;
        log.detalles    = detalles;
        log.usuario     = usuario;
        return log;
    }

    public Long getId()          { return id; }
    public String getTenantId()  { return tenantId; }
    public String getModulo()    { return modulo; }
    public String getArchivo()   { return archivo; }
    public int getProcesadas()   { return procesadas; }
    public int getExitosas()     { return exitosas; }
    public int getConErrores()   { return conErrores; }
    public String getEstado()    { return estado; }
    public String getDetalles()  { return detalles; }
    public String getUsuario()   { return usuario; }
    public LocalDateTime getFecha() { return fecha; }
}
