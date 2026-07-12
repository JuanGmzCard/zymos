package com.alera.model;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "tarea_items")
public class TareaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tarea_id", nullable = false)
    private Tarea tarea;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(nullable = false)
    private Boolean completado = Boolean.FALSE;

    @Column(name = "orden_item", nullable = false)
    private Integer ordenItem = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id")
    private LoteCerveza lote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipo_id")
    private Equipo equipo;

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public Tarea getTarea() { return tarea; }
    public void setTarea(Tarea tarea) { this.tarea = tarea; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Boolean getCompletado() { return completado; }
    public void setCompletado(Boolean completado) { this.completado = completado != null ? completado : Boolean.FALSE; }
    public Integer getOrdenItem() { return ordenItem; }
    public void setOrdenItem(Integer ordenItem) { this.ordenItem = ordenItem != null ? ordenItem : 0; }
    public LoteCerveza getLote() { return lote; }
    public void setLote(LoteCerveza lote) { this.lote = lote; }
    public Equipo getEquipo() { return equipo; }
    public void setEquipo(Equipo equipo) { this.equipo = equipo; }
}
