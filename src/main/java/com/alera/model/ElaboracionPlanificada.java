package com.alera.model;

import com.alera.model.enums.EstadoPlanificacion;
import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "elaboraciones_planificadas")
public class ElaboracionPlanificada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    private String tenantId;

    @Column(name = "fecha_planeada", nullable = false)
    private LocalDate fechaPlaneada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receta_id")
    private Receta receta;

    @Column(name = "nombre_elaboracion", nullable = false, length = 150)
    private String nombreElaboracion;

    @Column(name = "volumen_estimado", precision = 10, scale = 2)
    private BigDecimal volumenEstimado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPlanificacion estado = EstadoPlanificacion.PLANIFICADA;

    @Column(length = 500)
    private String notas;

    @Column(name = "creado_at", nullable = false, updatable = false)
    private LocalDateTime creadoAt;

    @PrePersist
    void prePersist() {
        if (creadoAt == null) creadoAt = LocalDateTime.now();
    }

    public Long getId()                               { return id; }
    public void setId(Long id)                        { this.id = id; }
    public String getTenantId()                       { return tenantId; }
    public void setTenantId(String tenantId)          { this.tenantId = tenantId; }
    public LocalDate getFechaPlaneada()               { return fechaPlaneada; }
    public void setFechaPlaneada(LocalDate v)         { this.fechaPlaneada = v; }
    public Receta getReceta()                         { return receta; }
    public void setReceta(Receta receta)              { this.receta = receta; }
    public String getNombreElaboracion()              { return nombreElaboracion; }
    public void setNombreElaboracion(String v)        { this.nombreElaboracion = v; }
    public BigDecimal getVolumenEstimado()            { return volumenEstimado; }
    public void setVolumenEstimado(BigDecimal v)      { this.volumenEstimado = v; }
    public EstadoPlanificacion getEstado()            { return estado; }
    public void setEstado(EstadoPlanificacion estado) { this.estado = estado; }
    public String getNotas()                          { return notas; }
    public void setNotas(String notas)                { this.notas = notas; }
    public LocalDateTime getCreadoAt()                { return creadoAt; }
}
