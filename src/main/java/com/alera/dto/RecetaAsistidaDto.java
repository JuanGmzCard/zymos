package com.alera.dto;

import java.util.List;

public class RecetaAsistidaDto {

    private String nombre;
    private String estilo;
    private String descripcion;
    private Double volumenBase;
    private Integer tiempoHervorMinutos;
    private Integer ogObjetivo;
    private Integer fgObjetivo;
    private Double aguaMacerado;
    private String unidadAgua;
    private Double aguaSparge;
    private String unidadSparge;
    private String notas;
    private List<IngredienteDto> maltas;
    private List<IngredienteDto> lupulos;
    private List<IngredienteDto> levaduras;
    private List<IngredienteDto> clarificantes;
    private List<AdicionDto> adicionesHervor;
    private List<EscalonDto> escalones;
    private boolean modoMock;
    private String error;

    public static class IngredienteDto {
        private String nombre;
        private Double cantidad;
        private String unidad;
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public Double getCantidad() { return cantidad; }
        public void setCantidad(Double cantidad) { this.cantidad = cantidad; }
        public String getUnidad() { return unidad; }
        public void setUnidad(String unidad) { this.unidad = unidad; }
    }

    public static class AdicionDto {
        private String nombre;
        private Integer minutosRestantes;
        private Double cantidad;
        private String unidad;
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public Integer getMinutosRestantes() { return minutosRestantes; }
        public void setMinutosRestantes(Integer minutosRestantes) { this.minutosRestantes = minutosRestantes; }
        public Double getCantidad() { return cantidad; }
        public void setCantidad(Double cantidad) { this.cantidad = cantidad; }
        public String getUnidad() { return unidad; }
        public void setUnidad(String unidad) { this.unidad = unidad; }
    }

    public static class EscalonDto {
        private String nombre;
        private Double temperatura;
        private Integer duracionMinutos;
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public Double getTemperatura() { return temperatura; }
        public void setTemperatura(Double temperatura) { this.temperatura = temperatura; }
        public Integer getDuracionMinutos() { return duracionMinutos; }
        public void setDuracionMinutos(Integer duracionMinutos) { this.duracionMinutos = duracionMinutos; }
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEstilo() { return estilo; }
    public void setEstilo(String estilo) { this.estilo = estilo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Double getVolumenBase() { return volumenBase; }
    public void setVolumenBase(Double volumenBase) { this.volumenBase = volumenBase; }
    public Integer getTiempoHervorMinutos() { return tiempoHervorMinutos; }
    public void setTiempoHervorMinutos(Integer tiempoHervorMinutos) { this.tiempoHervorMinutos = tiempoHervorMinutos; }
    public Integer getOgObjetivo() { return ogObjetivo; }
    public void setOgObjetivo(Integer ogObjetivo) { this.ogObjetivo = ogObjetivo; }
    public Integer getFgObjetivo() { return fgObjetivo; }
    public void setFgObjetivo(Integer fgObjetivo) { this.fgObjetivo = fgObjetivo; }
    public Double getAguaMacerado() { return aguaMacerado; }
    public void setAguaMacerado(Double aguaMacerado) { this.aguaMacerado = aguaMacerado; }
    public String getUnidadAgua() { return unidadAgua; }
    public void setUnidadAgua(String unidadAgua) { this.unidadAgua = unidadAgua; }
    public Double getAguaSparge() { return aguaSparge; }
    public void setAguaSparge(Double aguaSparge) { this.aguaSparge = aguaSparge; }
    public String getUnidadSparge() { return unidadSparge; }
    public void setUnidadSparge(String unidadSparge) { this.unidadSparge = unidadSparge; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    public List<IngredienteDto> getMaltas() { return maltas; }
    public void setMaltas(List<IngredienteDto> maltas) { this.maltas = maltas; }
    public List<IngredienteDto> getLupulos() { return lupulos; }
    public void setLupulos(List<IngredienteDto> lupulos) { this.lupulos = lupulos; }
    public List<IngredienteDto> getLevaduras() { return levaduras; }
    public void setLevaduras(List<IngredienteDto> levaduras) { this.levaduras = levaduras; }
    public List<IngredienteDto> getClarificantes() { return clarificantes; }
    public void setClarificantes(List<IngredienteDto> clarificantes) { this.clarificantes = clarificantes; }
    public List<AdicionDto> getAdicionesHervor() { return adicionesHervor; }
    public void setAdicionesHervor(List<AdicionDto> adicionesHervor) { this.adicionesHervor = adicionesHervor; }
    public List<EscalonDto> getEscalones() { return escalones; }
    public void setEscalones(List<EscalonDto> escalones) { this.escalones = escalones; }
    public boolean isModoMock() { return modoMock; }
    public void setModoMock(boolean modoMock) { this.modoMock = modoMock; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
