package com.alera.model;

import com.alera.model.enums.TipoIngrediente;
import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "ingredientes")
public class Ingrediente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoIngrediente tipo;

    @Column(nullable = false)
    private String nombre;

    private String cantidad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id")
    private LoteCerveza lote;

    public Ingrediente() {}

    public Ingrediente(TipoIngrediente tipo, String nombre, String cantidad, LoteCerveza lote) {
        this.tipo = tipo;
        this.nombre = nombre;
        this.cantidad = cantidad;
        this.lote = lote;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public TipoIngrediente getTipo() { return tipo; }
    public void setTipo(TipoIngrediente tipo) { this.tipo = tipo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCantidad() { return cantidad; }
    public void setCantidad(String cantidad) { this.cantidad = cantidad; }
    public LoteCerveza getLote() { return lote; }
    public void setLote(LoteCerveza lote) { this.lote = lote; }
}
