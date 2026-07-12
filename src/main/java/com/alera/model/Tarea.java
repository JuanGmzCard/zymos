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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insumo_id")
    private InsumoInventario insumo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "elaboracion_id")
    private ElaboracionPlanificada elaboracion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_compra_id")
    private OrdenCompra ordenCompra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id")
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id")
    private FacturaProveedor factura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

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
    public InsumoInventario getInsumo() { return insumo; }
    public void setInsumo(InsumoInventario insumo) { this.insumo = insumo; }
    public ElaboracionPlanificada getElaboracion() { return elaboracion; }
    public void setElaboracion(ElaboracionPlanificada elaboracion) { this.elaboracion = elaboracion; }
    public OrdenCompra getOrdenCompra() { return ordenCompra; }
    public void setOrdenCompra(OrdenCompra ordenCompra) { this.ordenCompra = ordenCompra; }
    public Venta getVenta() { return venta; }
    public void setVenta(Venta venta) { this.venta = venta; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public FacturaProveedor getFactura() { return factura; }
    public void setFactura(FacturaProveedor factura) { this.factura = factura; }
    public Proveedor getProveedor() { return proveedor; }
    public void setProveedor(Proveedor proveedor) { this.proveedor = proveedor; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public List<TareaItem> getItems() { return items; }
    public void setItems(List<TareaItem> items) { this.items = items; }

    /** Tipo de referencia activa (solo una puede ser no-null a la vez). */
    public String getRefTipo() {
        if (lote != null)        return "LOTE";
        if (equipo != null)      return "EQUIPO";
        if (insumo != null)      return "INSUMO";
        if (elaboracion != null) return "ELABORACION";
        if (ordenCompra != null) return "ORDEN_COMPRA";
        if (venta != null)       return "VENTA";
        if (cliente != null)     return "CLIENTE";
        if (factura != null)     return "FACTURA";
        if (proveedor != null)   return "PROVEEDOR";
        return null;
    }

    public Long getRefId() {
        if (lote != null)        return lote.getId();
        if (equipo != null)      return equipo.getId();
        if (insumo != null)      return insumo.getId();
        if (elaboracion != null) return elaboracion.getId();
        if (ordenCompra != null) return ordenCompra.getId();
        if (venta != null)       return venta.getId();
        if (cliente != null)     return cliente.getId();
        if (factura != null)     return factura.getId();
        if (proveedor != null)   return proveedor.getId();
        return null;
    }

    public String getRefLabel() {
        if (lote != null)        return lote.getCodigoLote() + (lote.getEstilo() != null ? " — " + lote.getEstilo() : "");
        if (equipo != null)      return equipo.getNombre();
        if (insumo != null)      return insumo.getNombre() + (insumo.getTipo() != null ? " (" + insumo.getTipo() + ")" : "");
        if (elaboracion != null) return elaboracion.getNombreElaboracion();
        if (ordenCompra != null) return (ordenCompra.getNumeroOc() != null ? ordenCompra.getNumeroOc() : "OC sin número")
                                        + (ordenCompra.getProveedor() != null ? " — " + ordenCompra.getProveedor() : "");
        if (venta != null)       return venta.getCliente()
                                        + (venta.getRemisionNumero() != null ? " #" + venta.getRemisionNumero() : "");
        if (cliente != null)     return cliente.getNombre()
                                        + (cliente.getNit() != null ? " — " + cliente.getNit() : "");
        if (factura != null)     return (factura.getNumeroFactura() != null && !factura.getNumeroFactura().isBlank()
                                        ? factura.getNumeroFactura() : "#" + factura.getId())
                                        + (factura.getProveedor() != null ? " — " + factura.getProveedor() : "");
        if (proveedor != null)   return proveedor.getNombre()
                                        + (proveedor.getNit() != null ? " — " + proveedor.getNit() : "");
        return null;
    }

    public String getRefUrl() {
        if (lote != null)        return "/ver/" + lote.getId();
        if (equipo != null)      return "/equipos/ver/" + equipo.getId();
        if (insumo != null)      return "/inventario/" + insumo.getId() + "/historial";
        if (elaboracion != null) return "/planificacion";
        if (ordenCompra != null) return "/ordenes-compra/ver/" + ordenCompra.getId();
        if (venta != null)       return "/ventas/ver/" + venta.getId();
        if (cliente != null)     return "/clientes/ver/" + cliente.getId();
        if (factura != null)     return "/facturas/ver/" + factura.getId();
        if (proveedor != null)   return "/proveedores/editar/" + proveedor.getId();
        return null;
    }
}
