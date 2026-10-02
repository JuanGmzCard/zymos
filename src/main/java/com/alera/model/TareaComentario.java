package com.alera.model;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.time.LocalDateTime;

@Entity
@Table(name = "tarea_comentarios")
public class TareaComentario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tarea_id", nullable = false)
    private Tarea tarea;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String texto;

    @Column(nullable = false, length = 100)
    private String autor;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private LocalDateTime fechaHora;

    public TareaComentario() {}

    public TareaComentario(Tarea tarea, String texto, String autor, LocalDateTime fechaHora) {
        this.tarea     = tarea;
        this.texto     = texto;
        this.autor     = autor;
        this.fechaHora = fechaHora;
    }

    public Long getId()                          { return id; }
    public void setId(Long id)                   { this.id = id; }
    public String getTenantId()                  { return tenantId; }
    public Tarea getTarea()                      { return tarea; }
    public void setTarea(Tarea tarea)            { this.tarea = tarea; }
    public String getTexto()                     { return texto; }
    public void setTexto(String texto)           { this.texto = texto; }
    public String getAutor()                     { return autor; }
    public void setAutor(String autor)           { this.autor = autor; }
    public LocalDateTime getFechaHora()          { return fechaHora; }
    public void setFechaHora(LocalDateTime fh)   { this.fechaHora = fh; }
}