package com.alera.model;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "lecturas_fermentacion")
public class LecturaFermentacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id", nullable = false)
    private LoteCerveza lote;

    @Column(nullable = false)
    private LocalDate fecha;

    // Densidad en formato XXXX (ej: 1042). Puede ser null si solo se registra temperatura.
    private Integer densidad;

    @Column(precision = 5, scale = 2)
    private BigDecimal temperatura;

    @Column(length = 500)
    private String notas;

    // ABV parcial: progreso respecto al OG del lote
    public BigDecimal getAbvParcial(Integer ogLote) {
        if (densidad == null || ogLote == null || densidad >= ogLote) return null;
        return java.math.BigDecimal.valueOf((ogLote - densidad) * 131.25 / 1000.0)
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public Long getId()                        { return id; }
    public void setId(Long id)                 { this.id = id; }
    public String getTenantId()                { return tenantId; }
    public void setTenantId(String tenantId)   { this.tenantId = tenantId; }
    public LoteCerveza getLote()               { return lote; }
    public void setLote(LoteCerveza lote)      { this.lote = lote; }
    public LocalDate getFecha()                { return fecha; }
    public void setFecha(LocalDate fecha)      { this.fecha = fecha; }
    public Integer getDensidad()               { return densidad; }
    public void setDensidad(Integer densidad)  { this.densidad = densidad; }
    public BigDecimal getTemperatura()         { return temperatura; }
    public void setTemperatura(BigDecimal t)   { this.temperatura = t; }
    public String getNotas()                   { return notas; }
    public void setNotas(String notas)         { this.notas = notas; }
}