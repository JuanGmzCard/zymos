package com.alera.dto;

import java.util.List;

public class FermentacionDiagnosticoDto {

    /** "normal" | "advertencia" | "critico" */
    private String estado;
    private String resumen;
    private List<String> anomalias;
    private List<String> recomendaciones;
    private String pronosticoFg;
    private boolean modoMock;
    private String error;

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getResumen() { return resumen; }
    public void setResumen(String resumen) { this.resumen = resumen; }
    public List<String> getAnomalias() { return anomalias; }
    public void setAnomalias(List<String> anomalias) { this.anomalias = anomalias; }
    public List<String> getRecomendaciones() { return recomendaciones; }
    public void setRecomendaciones(List<String> recomendaciones) { this.recomendaciones = recomendaciones; }
    public String getPronosticoFg() { return pronosticoFg; }
    public void setPronosticoFg(String pronosticoFg) { this.pronosticoFg = pronosticoFg; }
    public boolean isModoMock() { return modoMock; }
    public void setModoMock(boolean modoMock) { this.modoMock = modoMock; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
