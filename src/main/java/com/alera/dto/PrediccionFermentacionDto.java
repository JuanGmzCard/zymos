package com.alera.dto;

import java.util.List;

public class PrediccionFermentacionDto {

    /** NORMAL | LENTA | RAPIDA | ATASCADA | INSUFICIENTE */
    private String tendencia;
    private String resumen;
    private Integer fgPredichoMin;
    private Integer fgPredichoMax;
    private Integer diasRestantes;
    private String  fechaEstimadaFin;
    private Integer confianza;
    private List<String> insights;
    private List<String> recomendaciones;
    private String comparacionHistorica;
    private int    lotesHistoricosUsados;
    private boolean modoMock;
    private boolean sinHistorial;
    private String error;

    public String getTendencia()                   { return tendencia; }
    public void setTendencia(String v)             { this.tendencia = v; }
    public String getResumen()                     { return resumen; }
    public void setResumen(String v)               { this.resumen = v; }
    public Integer getFgPredichoMin()              { return fgPredichoMin; }
    public void setFgPredichoMin(Integer v)        { this.fgPredichoMin = v; }
    public Integer getFgPredichoMax()              { return fgPredichoMax; }
    public void setFgPredichoMax(Integer v)        { this.fgPredichoMax = v; }
    public Integer getDiasRestantes()              { return diasRestantes; }
    public void setDiasRestantes(Integer v)        { this.diasRestantes = v; }
    public String getFechaEstimadaFin()            { return fechaEstimadaFin; }
    public void setFechaEstimadaFin(String v)      { this.fechaEstimadaFin = v; }
    public Integer getConfianza()                  { return confianza; }
    public void setConfianza(Integer v)            { this.confianza = v; }
    public List<String> getInsights()              { return insights; }
    public void setInsights(List<String> v)        { this.insights = v; }
    public List<String> getRecomendaciones()       { return recomendaciones; }
    public void setRecomendaciones(List<String> v) { this.recomendaciones = v; }
    public String getComparacionHistorica()        { return comparacionHistorica; }
    public void setComparacionHistorica(String v)  { this.comparacionHistorica = v; }
    public int getLotesHistoricosUsados()          { return lotesHistoricosUsados; }
    public void setLotesHistoricosUsados(int v)    { this.lotesHistoricosUsados = v; }
    public boolean isModoMock()                    { return modoMock; }
    public void setModoMock(boolean v)             { this.modoMock = v; }
    public boolean isSinHistorial()                { return sinHistorial; }
    public void setSinHistorial(boolean v)         { this.sinHistorial = v; }
    public String getError()                       { return error; }
    public void setError(String v)                 { this.error = v; }
}
