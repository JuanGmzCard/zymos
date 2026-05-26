package com.alera.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RecetaFormDto {

    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "Máximo 150 caracteres")
    private String nombre;

    @NotBlank(message = "El estilo es obligatorio")
    @Size(max = 100, message = "Máximo 100 caracteres")
    private String estilo;

    private String descripcion;
    private boolean activa = true;

    private BigDecimal aguaMacerado;
    private String unidadAguaMacerado;
    private BigDecimal aguaSparge;
    private String unidadAguaSparge;
    private Integer tiempoHervorMinutos;
    private Integer ogObjetivo;
    private Integer fgObjetivo;
    private BigDecimal volumenBase;
    private BigDecimal phAgua;
    private String notas;

    private List<InsumoDto> maltas           = new ArrayList<>();
    private List<InsumoDto> lupulos          = new ArrayList<>();
    private List<InsumoDto> levaduras        = new ArrayList<>();
    private List<InsumoDto> clarificantes    = new ArrayList<>();
    private List<EscalonDto> escalones       = new ArrayList<>();
    private List<AdicionHervorDto> adicionesHervor = new ArrayList<>();

    public static class AdicionHervorDto {
        private String nombre;
        private Integer minutosRestantes;
        private BigDecimal cantidad;
        private String unidad;

        public AdicionHervorDto() {}
        public boolean isEmpty() {
            return (nombre == null || nombre.isBlank()) && minutosRestantes == null && cantidad == null;
        }
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public Integer getMinutosRestantes() { return minutosRestantes; }
        public void setMinutosRestantes(Integer minutosRestantes) { this.minutosRestantes = minutosRestantes; }
        public BigDecimal getCantidad() { return cantidad; }
        public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }
        public String getUnidad() { return unidad; }
        public void setUnidad(String unidad) { this.unidad = unidad; }
    }

    public static class EscalonDto {
        private String nombre;
        private Integer duracionMinutos;
        private BigDecimal temperaturaC;

        public EscalonDto() {}
        public EscalonDto(String nombre, Integer duracionMinutos, BigDecimal temperaturaC) {
            this.nombre = nombre; this.duracionMinutos = duracionMinutos; this.temperaturaC = temperaturaC;
        }
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public Integer getDuracionMinutos() { return duracionMinutos; }
        public void setDuracionMinutos(Integer duracionMinutos) { this.duracionMinutos = duracionMinutos; }
        public BigDecimal getTemperaturaC() { return temperaturaC; }
        public void setTemperaturaC(BigDecimal temperaturaC) { this.temperaturaC = temperaturaC; }
    }

    public static RecetaFormDto empty() {
        RecetaFormDto dto = new RecetaFormDto();
        dto.maltas.add(new InsumoDto());
        dto.lupulos.add(new InsumoDto());
        dto.levaduras.add(new InsumoDto());
        dto.clarificantes.add(new InsumoDto());
        return dto;
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
    public BigDecimal getPhAgua() { return phAgua; }
    public void setPhAgua(BigDecimal phAgua) { this.phAgua = phAgua; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    public List<InsumoDto> getMaltas() { return maltas; }
    public void setMaltas(List<InsumoDto> maltas) { this.maltas = maltas; }
    public List<InsumoDto> getLupulos() { return lupulos; }
    public void setLupulos(List<InsumoDto> lupulos) { this.lupulos = lupulos; }
    public List<InsumoDto> getLevaduras() { return levaduras; }
    public void setLevaduras(List<InsumoDto> levaduras) { this.levaduras = levaduras; }
    public List<InsumoDto> getClarificantes() { return clarificantes; }
    public void setClarificantes(List<InsumoDto> clarificantes) { this.clarificantes = clarificantes; }
    public List<EscalonDto> getEscalones() { return escalones; }
    public void setEscalones(List<EscalonDto> escalones) { this.escalones = escalones; }
    public List<AdicionHervorDto> getAdicionesHervor() { return adicionesHervor; }
    public void setAdicionesHervor(List<AdicionHervorDto> adicionesHervor) { this.adicionesHervor = adicionesHervor; }
}
