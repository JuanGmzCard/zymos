package com.alera.model;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "tarea_referencias")
public class TareaReferencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tarea_id", nullable = false)
    private Tarea tarea;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(name = "entidad_id", nullable = false)
    private Long entidadId;

    @Column(columnDefinition = "TEXT")
    private String label = "";

    @Column(length = 500)
    private String url = "";

    @Column
    private Integer orden = 0;

    @TenantId
    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    public TareaReferencia() {}

    public TareaReferencia(Tarea tarea, String tipo, Long entidadId, String label, String url, int orden) {
        this.tarea     = tarea;
        this.tipo      = tipo;
        this.entidadId = entidadId;
        this.label     = label != null ? label : "";
        this.url       = url != null ? url : "";
        this.orden     = orden;
    }

    public Long getId()                  { return id; }
    public void setId(Long id)           { this.id = id; }
    public Tarea getTarea()              { return tarea; }
    public void setTarea(Tarea tarea)    { this.tarea = tarea; }
    public String getTipo()              { return tipo; }
    public void setTipo(String tipo)     { this.tipo = tipo; }
    public Long getEntidadId()           { return entidadId; }
    public void setEntidadId(Long v)     { this.entidadId = v; }
    public String getLabel()             { return label; }
    public void setLabel(String label)   { this.label = label; }
    public String getUrl()               { return url; }
    public void setUrl(String url)       { this.url = url; }
    public Integer getOrden()            { return orden; }
    public void setOrden(Integer orden)  { this.orden = orden; }
    public String getTenantId()          { return tenantId; }
}
