package com.alera.model;

import com.alera.model.enums.EstadoTarea;
import com.alera.model.enums.PrioridadTarea;
import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tareas")
@EntityListeners(AuditingEntityListener.class)
public class Tarea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PrioridadTarea prioridad = PrioridadTarea.MEDIA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoTarea estado = EstadoTarea.PENDIENTE;

    @Column(name = "asignado_a", length = 100)
    private String asignadoA;

    @Column(name = "creado_por", length = 100)
    private String creadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id")
    private LoteCerveza lote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipo_id")
    private Equipo equipo;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "tarea", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("ordenItem ASC, id ASC")
    private List<TareaItem> items = new ArrayList<>();

    public boolean isVencida() {
        return fechaVencimiento != null
                && LocalDate.now().isAfter(fechaVencimiento)
                && estado != EstadoTarea.COMPLETADA;
    }

    public int getPorcentajeCompletado() {
        if (items == null || items.isEmpty()) return 0;
        long done = items.stream().filter(i -> Boolean.TRUE.equals(i.getCompletado())).count();
        return (int) (done * 100 / items.size());
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }
    public PrioridadTarea getPrioridad() { return prioridad; }
    public void setPrioridad(PrioridadTarea prioridad) { this.prioridad = prioridad; }
    public EstadoTarea getEstado() { return estado; }
    public void setEstado(EstadoTarea estado) { this.estado = estado; }
    public String getAsignadoA() { return asignadoA; }
    public void setAsignadoA(String asignadoA) { this.asignadoA = asignadoA; }
    public String getCreadoPor() { return creadoPor; }
    public void setCreadoPor(String creadoPor) { this.creadoPor = creadoPor; }
    public LoteCerveza getLote() { return lote; }
    public void setLote(LoteCerveza lote) { this.lote = lote; }
    public Equipo getEquipo() { return equipo; }
    public void setEquipo(Equipo equipo) { this.equipo = equipo; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public List<TareaItem> getItems() { return items; }
    public void setItems(List<TareaItem> items) { this.items = items; }
}
