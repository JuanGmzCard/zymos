package com.alera.model;

import com.alera.model.enums.TipoIngrediente;
import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "receta_ingredientes")
public class RecetaIngrediente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receta_id", nullable = false)
    private Receta receta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoIngrediente tipo;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(length = 50)
    private String cantidad;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Receta getReceta() { return receta; }
    public void setReceta(Receta receta) { this.receta = receta; }
    public TipoIngrediente getTipo() { return tipo; }
    public void setTipo(TipoIngrediente tipo) { this.tipo = tipo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCantidad() { return cantidad; }
    public void setCantidad(String cantidad) { this.cantidad = cantidad; }
}
