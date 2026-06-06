package com.alera.model;

import com.alera.model.enums.EstadoOrdenCompra;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ordenes_compra")
public class OrdenCompra extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String numeroOc;

    private String proveedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedorRef;

    @Column(nullable = false)
    private LocalDate fechaEmision;

    private LocalDate fechaRequerida;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoOrdenCompra estado = EstadoOrdenCompra.BORRADOR;

    @Column(length = 500)
    private String notas;

    private Long facturaId;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrdenCompraItem> items = new ArrayList<>();

    public BigDecimal getSubtotalEstimado() {
        return items.stream()
                .filter(i -> i.getPrecioUnitarioEstimado() != null)
                .map(i -> i.getCantidad().multiply(i.getPrecioUnitarioEstimado()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTotalEstimado() {
        return items.stream()
                .map(OrdenCompraItem::getValorLinea)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public boolean isEditable() {
        return estado == EstadoOrdenCompra.BORRADOR;
    }

    public boolean isConvertible() {
        return estado == EstadoOrdenCompra.RECIBIDA && facturaId == null;
    }

    public Long getId()                              { return id; }
    public void setId(Long id)                       { this.id = id; }
    public String getNumeroOc()                      { return numeroOc; }
    public void setNumeroOc(String numeroOc)         { this.numeroOc = numeroOc; }
    public String getProveedor()                     { return proveedor; }
    public void setProveedor(String proveedor)       { this.proveedor = proveedor; }
    public Proveedor getProveedorRef()               { return proveedorRef; }
    public void setProveedorRef(Proveedor p)         { this.proveedorRef = p; }
    public LocalDate getFechaEmision()               { return fechaEmision; }
    public void setFechaEmision(LocalDate d)         { this.fechaEmision = d; }
    public LocalDate getFechaRequerida()             { return fechaRequerida; }
    public void setFechaRequerida(LocalDate d)       { this.fechaRequerida = d; }
    public EstadoOrdenCompra getEstado()             { return estado; }
    public void setEstado(EstadoOrdenCompra estado)  { this.estado = estado; }
    public String getNotas()                         { return notas; }
    public void setNotas(String notas)               { this.notas = notas; }
    public Long getFacturaId()                       { return facturaId; }
    public void setFacturaId(Long facturaId)         { this.facturaId = facturaId; }
    public List<OrdenCompraItem> getItems()          { return items; }
    public void setItems(List<OrdenCompraItem> i)    { this.items = i; }
}
