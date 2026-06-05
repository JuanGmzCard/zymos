package com.alera.model;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "evaluaciones_sensoriales")
public class EvaluacionSensorial {

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

    @Column(length = 100)
    private String catador;

    private Integer aroma;            // 0–12
    private Integer apariencia;       // 0–3
    private Integer sabor;            // 0–20
    private Integer sensacionBoca;    // 0–5  → columna sensacion_boca (naming strategy)
    private Integer impresionGeneral; // 0–10 → columna impresion_general

    @Column(length = 1000)
    private String notas;

    @Column(nullable = false)
    private LocalDateTime creadoAt;

    @PrePersist
    void prePersist() { this.creadoAt = LocalDateTime.now(); }

    // ── Puntaje y clasificación BJCP ──────────────────────────────────────

    public Integer getPuntajeTotal() {
        if (aroma == null && apariencia == null && sabor == null
                && sensacionBoca == null && impresionGeneral == null) return null;
        return (aroma != null ? aroma : 0)
             + (apariencia != null ? apariencia : 0)
             + (sabor != null ? sabor : 0)
             + (sensacionBoca != null ? sensacionBoca : 0)
             + (impresionGeneral != null ? impresionGeneral : 0);
    }

    public String getClasificacion() {
        Integer t = getPuntajeTotal();
        if (t == null) return null;
        if (t >= 47) return "Excepcional";
        if (t >= 38) return "Excelente";
        if (t >= 30) return "Muy buena";
        if (t >= 21) return "Buena";
        if (t >= 14) return "Aceptable";
        if (t >= 7)  return "Deficiente";
        return "Inaceptable";
    }

    public String getBadgeClass() {
        Integer t = getPuntajeTotal();
        if (t == null) return "bg-secondary";
        if (t >= 47) return "bg-warning text-dark";
        if (t >= 38) return "bg-success";
        if (t >= 30) return "bg-info text-dark";
        if (t >= 21) return "bg-primary";
        if (t >= 14) return "bg-secondary";
        return "bg-danger";
    }

    public Long getId()                          { return id; }
    public void setId(Long id)                   { this.id = id; }
    public String getTenantId()                  { return tenantId; }
    public void setTenantId(String t)            { this.tenantId = t; }
    public LoteCerveza getLote()                 { return lote; }
    public void setLote(LoteCerveza lote)        { this.lote = lote; }
    public LocalDate getFecha()                  { return fecha; }
    public void setFecha(LocalDate fecha)        { this.fecha = fecha; }
    public String getCatador()                   { return catador; }
    public void setCatador(String catador)       { this.catador = catador; }
    public Integer getAroma()                    { return aroma; }
    public void setAroma(Integer aroma)          { this.aroma = aroma; }
    public Integer getApariencia()               { return apariencia; }
    public void setApariencia(Integer a)         { this.apariencia = a; }
    public Integer getSabor()                    { return sabor; }
    public void setSabor(Integer sabor)          { this.sabor = sabor; }
    public Integer getSensacionBoca()            { return sensacionBoca; }
    public void setSensacionBoca(Integer s)      { this.sensacionBoca = s; }
    public Integer getImpresionGeneral()         { return impresionGeneral; }
    public void setImpresionGeneral(Integer i)   { this.impresionGeneral = i; }
    public String getNotas()                     { return notas; }
    public void setNotas(String notas)           { this.notas = notas; }
    public LocalDateTime getCreadoAt()           { return creadoAt; }
}
