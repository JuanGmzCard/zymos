package com.alera.dto;

import java.util.List;

public class BjcpScorerDto {

    private Integer aromaEstimado;
    private Integer aparienciaEstimada;
    private Integer saborEstimado;
    private Integer sensacionBocaEstimada;
    private Integer impresionGeneralEstimada;
    private Integer puntajeTotalEstimado;
    private String clasificacion;
    private List<String> fortalezas;
    private List<String> defectos;
    private List<String> recomendaciones;
    private boolean modoMock;
    private String error;

    public Integer getAromaEstimado() { return aromaEstimado; }
    public void setAromaEstimado(Integer aromaEstimado) { this.aromaEstimado = aromaEstimado; }
    public Integer getAparienciaEstimada() { return aparienciaEstimada; }
    public void setAparienciaEstimada(Integer aparienciaEstimada) { this.aparienciaEstimada = aparienciaEstimada; }
    public Integer getSaborEstimado() { return saborEstimado; }
    public void setSaborEstimado(Integer saborEstimado) { this.saborEstimado = saborEstimado; }
    public Integer getSensacionBocaEstimada() { return sensacionBocaEstimada; }
    public void setSensacionBocaEstimada(Integer sensacionBocaEstimada) { this.sensacionBocaEstimada = sensacionBocaEstimada; }
    public Integer getImpresionGeneralEstimada() { return impresionGeneralEstimada; }
    public void setImpresionGeneralEstimada(Integer impresionGeneralEstimada) { this.impresionGeneralEstimada = impresionGeneralEstimada; }
    public Integer getPuntajeTotalEstimado() { return puntajeTotalEstimado; }
    public void setPuntajeTotalEstimado(Integer puntajeTotalEstimado) { this.puntajeTotalEstimado = puntajeTotalEstimado; }
    public String getClasificacion() { return clasificacion; }
    public void setClasificacion(String clasificacion) { this.clasificacion = clasificacion; }
    public List<String> getFortalezas() { return fortalezas; }
    public void setFortalezas(List<String> fortalezas) { this.fortalezas = fortalezas; }
    public List<String> getDefectos() { return defectos; }
    public void setDefectos(List<String> defectos) { this.defectos = defectos; }
    public List<String> getRecomendaciones() { return recomendaciones; }
    public void setRecomendaciones(List<String> recomendaciones) { this.recomendaciones = recomendaciones; }
    public boolean isModoMock() { return modoMock; }
    public void setModoMock(boolean modoMock) { this.modoMock = modoMock; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
