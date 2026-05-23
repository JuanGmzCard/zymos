package com.alera.model;

import com.alera.model.enums.TipoIngrediente;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "recetas")
@SQLRestriction("deleted_at IS NULL")
public class Receta extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String estilo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    private boolean activa = true;

    private BigDecimal aguaMacerado;
    private String unidadAguaMacerado;
    private BigDecimal aguaSparge;
    private String unidadAguaSparge;
    private Integer tiempoHervorMinutos;
    private Integer ogObjetivo;
    private Integer fgObjetivo;
    private BigDecimal volumenBase;

    @Column(columnDefinition = "TEXT")
    private String notas;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;


    @OneToMany(mappedBy = "receta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("tipo ASC, nombre ASC")
    private List<RecetaIngrediente> ingredientes = new ArrayList<>();

    @OneToMany(mappedBy = "receta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<EscalonMacerado> escalones = new ArrayList<>();

    @OneToMany(mappedBy = "receta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("minutosRestantes DESC, orden ASC")
    private List<AdicionHervor> adicionesHervor = new ArrayList<>();

    public List<RecetaIngrediente> getMaltas() {
        return ingredientes.stream().filter(i -> i.getTipo() == TipoIngrediente.MALTA).collect(Collectors.toList());
    }
    public List<RecetaIngrediente> getLupulos() {
        return ingredientes.stream().filter(i -> i.getTipo() == TipoIngrediente.LUPULO).collect(Collectors.toList());
    }
    public List<RecetaIngrediente> getLevaduras() {
        return ingredientes.stream().filter(i -> i.getTipo() == TipoIngrediente.LEVADURA).collect(Collectors.toList());
    }
    public List<RecetaIngrediente> getClarificantes() {
        return ingredientes.stream().filter(i -> i.getTipo() == TipoIngrediente.CLARIFICANTE).collect(Collectors.toList());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEstilo() { return estilo; }
    public void setEstilo(String estilo) { this.estilo = estilo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }
    public BigDecimal getAguaMacerado() { return aguaMacerado; }
    public void setAguaMacerado(BigDecimal aguaMacerado) { this.aguaMacerado = aguaMacerado; }
    public String getUnidadAguaMacerado() { return unidadAguaMacerado; }
    public void setUnidadAguaMacerado(String unidadAguaMacerado) { this.unidadAguaMacerado = unidadAguaMacerado; }
    public BigDecimal getAguaSparge() { return aguaSparge; }
    public void setAguaSparge(BigDecimal aguaSparge) { this.aguaSparge = aguaSparge; }
    public String getUnidadAguaSparge() { return unidadAguaSparge; }
    public void setUnidadAguaSparge(String unidadAguaSparge) { this.unidadAguaSparge = unidadAguaSparge; }
    public Integer getTiempoHervorMinutos() { return tiempoHervorMinutos; }
    public void setTiempoHervorMinutos(Integer tiempoHervorMinutos) { this.tiempoHervorMinutos = tiempoHervorMinutos; }
    public Integer getOgObjetivo() { return ogObjetivo; }
    public void setOgObjetivo(Integer ogObjetivo) { this.ogObjetivo = ogObjetivo; }
    public Integer getFgObjetivo() { return fgObjetivo; }
    public void setFgObjetivo(Integer fgObjetivo) { this.fgObjetivo = fgObjetivo; }
    public BigDecimal getVolumenBase() { return volumenBase; }
    public void setVolumenBase(BigDecimal volumenBase) { this.volumenBase = volumenBase; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
    public List<RecetaIngrediente> getIngredientes() { return ingredientes; }
    public void setIngredientes(List<RecetaIngrediente> ingredientes) { this.ingredientes = ingredientes; }
    public List<EscalonMacerado> getEscalones() { return escalones; }
    public void setEscalones(List<EscalonMacerado> escalones) { this.escalones = escalones; }
    public List<AdicionHervor> getAdicionesHervor() { return adicionesHervor; }
    public void setAdicionesHervor(List<AdicionHervor> adicionesHervor) { this.adicionesHervor = adicionesHervor; }
}
