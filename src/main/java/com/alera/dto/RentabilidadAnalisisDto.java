package com.alera.dto;

import java.util.List;

public class RentabilidadAnalisisDto {

    private List<String> insights;
    private String estiloMasRentable;
    private String estiloMenosRentable;
    private String tendencia;
    private List<String> recomendaciones;
    private List<String> alertas;
    private boolean modoMock;
    private String error;

    public List<String> getInsights()           { return insights; }
    public void setInsights(List<String> v)      { this.insights = v; }

    public String getEstiloMasRentable()         { return estiloMasRentable; }
    public void setEstiloMasRentable(String v)   { this.estiloMasRentable = v; }

    public String getEstiloMenosRentable()       { return estiloMenosRentable; }
    public void setEstiloMenosRentable(String v) { this.estiloMenosRentable = v; }

    public String getTendencia()                 { return tendencia; }
    public void setTendencia(String v)           { this.tendencia = v; }

    public List<String> getRecomendaciones()     { return recomendaciones; }
    public void setRecomendaciones(List<String> v){ this.recomendaciones = v; }

    public List<String> getAlertas()             { return alertas; }
    public void setAlertas(List<String> v)       { this.alertas = v; }

    public boolean isModoMock()                  { return modoMock; }
    public void setModoMock(boolean v)           { this.modoMock = v; }

    public String getError()                     { return error; }
    public void setError(String v)               { this.error = v; }
}
