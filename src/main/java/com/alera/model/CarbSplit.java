package com.alera.model;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.math.BigDecimal;

@Entity
@Table(name = "carb_splits")
public class CarbSplit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id", nullable = false)
    private LoteCerveza lote;

    @TenantId
    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    private Integer orden = 0;

    @Column(precision = 10, scale = 3)
    private BigDecimal volumen;

    @Column(length = 20)
    private String metodo;

    @Column(name = "azucar_tipo", length = 200)
    private String azucarTipo;

    @Column(name = "azucar_gramos", precision = 10, scale = 2)
    private BigDecimal azucarGramos;

    @Column(name = "presion_psi", precision = 6, scale = 2)
    private BigDecimal presionPsi;

    @Column(name = "tiempo_horas")
    private Integer tiempoHoras;

    @Column(length = 50)
    private String tecnica;

    @Column(name = "co2_objetivo", precision = 4, scale = 2)
    private BigDecimal co2Objetivo;

    @Column(name = "co2_real", precision = 4, scale = 2)
    private BigDecimal co2Real;

    @Column(columnDefinition = "TEXT")
    private String notas;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LoteCerveza getLote() { return lote; }
    public void setLote(LoteCerveza lote) { this.lote = lote; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }
    public BigDecimal getVolumen() { return volumen; }
    public void setVolumen(BigDecimal volumen) { this.volumen = volumen; }
    public String getMetodo() { return metodo; }
    public void setMetodo(String metodo) { this.metodo = metodo; }
    public String getAzucarTipo() { return azucarTipo; }
    public void setAzucarTipo(String azucarTipo) { this.azucarTipo = azucarTipo; }
    public BigDecimal getAzucarGramos() { return azucarGramos; }
    public void setAzucarGramos(BigDecimal azucarGramos) { this.azucarGramos = azucarGramos; }
    public BigDecimal getPresionPsi() { return presionPsi; }
    public void setPresionPsi(BigDecimal presionPsi) { this.presionPsi = presionPsi; }
    public Integer getTiempoHoras() { return tiempoHoras; }
    public void setTiempoHoras(Integer tiempoHoras) { this.tiempoHoras = tiempoHoras; }
    public String getTecnica() { return tecnica; }
    public void setTecnica(String tecnica) { this.tecnica = tecnica; }
    public BigDecimal getCo2Objetivo() { return co2Objetivo; }
    public void setCo2Objetivo(BigDecimal co2Objetivo) { this.co2Objetivo = co2Objetivo; }
    public BigDecimal getCo2Real() { return co2Real; }
    public void setCo2Real(BigDecimal co2Real) { this.co2Real = co2Real; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
}
