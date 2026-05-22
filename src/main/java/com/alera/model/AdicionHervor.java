package com.alera.model;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.math.BigDecimal;

@Entity
@Table(name = "adiciones_hervor")
public class AdicionHervor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receta_id", nullable = false)
    private Receta receta;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(name = "minutos_restantes")
    private Integer minutosRestantes;

    @Column(precision = 10, scale = 3)
    private BigDecimal cantidad;

    private String unidad;

    @Column(nullable = false)
    private Integer orden = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Receta getReceta() { return receta; }
    public void setReceta(Receta receta) { this.receta = receta; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Integer getMinutosRestantes() { return minutosRestantes; }
    public void setMinutosRestantes(Integer minutosRestantes) { this.minutosRestantes = minutosRestantes; }
    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }
    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }
}