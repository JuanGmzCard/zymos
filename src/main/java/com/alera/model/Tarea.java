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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.annotations.BatchSize;

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
    @JoinColumn(name = "receta_id")
    private Receta receta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barril_id")
    private Barril barril;

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

    @OneToMany(mappedBy = "tarea", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orden ASC, id ASC")
    @BatchSize(size = 30)
    private List<TareaReferencia> referencias = new ArrayList<>();

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
    public Receta getReceta() { return receta; }
    public void setReceta(Receta receta) { this.receta = receta; }
    public Barril getBarril() { return barril; }
    public void setBarril(Barril barril) { this.barril = barril; }
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
    public List<TareaReferencia> getReferencias() { return referencias; }
    public void setReferencias(List<TareaReferencia> referencias) { this.referencias = referencias; }

    /** Todas las referencias activas como lista [{tipo, id, label, url}]. */
    public List<Map<String, Object>> getRefEntries() {
        // Usar tabla tarea_referencias si ya fue poblada (V79+)
        if (referencias != null && !referencias.isEmpty()) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (TareaReferencia ref : referencias) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("tipo",  ref.getTipo());
                m.put("id",    ref.getEntidadId());
                m.put("label", ref.getLabel() != null ? ref.getLabel() : "");
                m.put("url",   ref.getUrl()   != null ? ref.getUrl()   : "#");
                result.add(m);
            }
            return result;
        }
        // Fallback: columnas FK individuales (datos anteriores a V79)
        List<Map<String, Object>> result = new ArrayList<>();
        if (lote != null) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("tipo", "LOTE"); m.put("id", lote.getId());
            m.put("label", lote.getCodigoLote() + (lote.getEstilo() != null ? " — " + lote.getEstilo() : ""));
            m.put("url", "/ver/" + lote.getId()); result.add(m);
        }
        if (equipo != null) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("tipo", "EQUIPO"); m.put("id", equipo.getId());
            m.put("label", equipo.getNombre()); m.put("url", "/equipos/ver/" + equipo.getId()); result.add(m);
        }
        if (insumo != null) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("tipo", "INSUMO"); m.put("id", insumo.getId());
            m.put("label", insumo.getNombre() + (insumo.getTipo() != null ? " (" + insumo.getTipo() + ")" : ""));
            m.put("url", "/inventario/" + insumo.getId() + "/historial"); result.add(m);
        }
        if (elaboracion != null) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("tipo", "ELABORACION"); m.put("id", elaboracion.getId());
            m.put("label", elaboracion.getNombreElaboracion()); m.put("url", "/planificacion"); result.add(m);
        }
        if (ordenCompra != null) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("tipo", "ORDEN_COMPRA"); m.put("id", ordenCompra.getId());
            m.put("label", (ordenCompra.getNumeroOc() != null ? ordenCompra.getNumeroOc() : "OC sin número")
                    + (ordenCompra.getProveedor() != null ? " — " + ordenCompra.getProveedor() : ""));
            m.put("url", "/ordenes-compra/ver/" + ordenCompra.getId()); result.add(m);
        }
        if (venta != null) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("tipo", "VENTA"); m.put("id", venta.getId());
            m.put("label", venta.getCliente() + (venta.getRemisionNumero() != null ? " #" + venta.getRemisionNumero() : ""));
            m.put("url", "/ventas/ver/" + venta.getId()); result.add(m);
        }
        if (cliente != null) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("tipo", "CLIENTE"); m.put("id", cliente.getId());
            m.put("label", cliente.getNombre() + (cliente.getNit() != null ? " — " + cliente.getNit() : ""));
            m.put("url", "/clientes/ver/" + cliente.getId()); result.add(m);
        }
        if (factura != null) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("tipo", "FACTURA"); m.put("id", factura.getId());
            m.put("label", (factura.getNumeroFactura() != null && !factura.getNumeroFactura().isBlank()
                    ? factura.getNumeroFactura() : "#" + factura.getId())
                    + (factura.getProveedor() != null ? " — " + factura.getProveedor() : ""));
            m.put("url", "/facturas/ver/" + factura.getId()); result.add(m);
        }
        if (proveedor != null) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("tipo", "PROVEEDOR"); m.put("id", proveedor.getId());
            m.put("label", proveedor.getNombre() + (proveedor.getNit() != null ? " — " + proveedor.getNit() : ""));
            m.put("url", "/proveedores/editar/" + proveedor.getId()); result.add(m);
        }
        if (receta != null) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("tipo", "RECETA"); m.put("id", receta.getId());
            m.put("label", receta.getNombre() + (receta.getEstilo() != null ? " — " + receta.getEstilo() : ""));
            m.put("url", "/recetas/ver/" + receta.getId()); result.add(m);
        }
        if (barril != null) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("tipo", "BARRIL"); m.put("id", barril.getId());
            m.put("label", barril.getCodigo() + (barril.getTipo() != null ? " — " + barril.getTipo() : ""));
            m.put("url", "/barriles/ver/" + barril.getId()); result.add(m);
        }
        return result;
    }

    /** Tipo de la primera referencia activa (retro-compatibilidad). Delega a getRefEntries(). */
    public String getRefTipo() {
        List<Map<String, Object>> entries = getRefEntries();
        return entries.isEmpty() ? null : (String) entries.get(0).get("tipo");
    }

    public Long getRefId() {
        List<Map<String, Object>> entries = getRefEntries();
        if (entries.isEmpty()) return null;
        Object id = entries.get(0).get("id");
        if (id instanceof Long l) return l;
        if (id instanceof Number n) return n.longValue();
        return null;
    }

    public String getRefLabel() {
        List<Map<String, Object>> entries = getRefEntries();
        return entries.isEmpty() ? null : (String) entries.get(0).get("label");
    }

    public String getRefUrl() {
        List<Map<String, Object>> entries = getRefEntries();
        return entries.isEmpty() ? null : (String) entries.get(0).get("url");
    }
}
