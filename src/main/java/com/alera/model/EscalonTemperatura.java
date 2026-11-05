package com.alera.model;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "lote_escalones_temperatura")
public class EscalonTemperatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_id")
    private LoteCerveza lote;

    @Column(nullable = false, length = 20)
    private String fase;

    @Column(name = "temperatura_objetivo", nullable = false, precision = 5, scale = 2)
    private BigDecimal temperaturaObjetivo;

    private LocalDate fechaObjetivo;
    private LocalDate fechaReal;

    @Column(nullable = false)
    private Boolean logrado = false;

    @Column(length = 500)
    private String notas;

    @Column(nullable = false)
    private Integer orden = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public LoteCerveza getLote() { return lote; }
    public void setLote(LoteCerveza lote) { this.lote = lote; }
    public String getFase() { return fase; }
    public void setFase(String fase) { this.fase = fase; }
    public BigDecimal getTemperaturaObjetivo() { return temperaturaObjetivo; }
    public void setTemperaturaObjetivo(BigDecimal v) { this.temperaturaObjetivo = v; }
    public LocalDate getFechaObjetivo() { return fechaObjetivo; }
    public void setFechaObjetivo(LocalDate v) { this.fechaObjetivo = v; }
    public LocalDate getFechaReal() { return fechaReal; }
    public void setFechaReal(LocalDate v) { this.fechaReal = v; }
    public boolean isLogrado() { return Boolean.TRUE.equals(logrado); }
    public void setLogrado(Boolean logrado) { this.logrado = logrado; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }
}
