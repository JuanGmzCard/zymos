package com.alera.dto;

import java.util.List;

public class DescripcionComercialDto {

    private String descripcionCorta;
    private String descripcionLarga;
    private String textoEtiqueta;
    private List<String> hashtags;
    private boolean modoMock;
    private String error;

    public String getDescripcionCorta() { return descripcionCorta; }
    public void setDescripcionCorta(String descripcionCorta) { this.descripcionCorta = descripcionCorta; }
    public String getDescripcionLarga() { return descripcionLarga; }
    public void setDescripcionLarga(String descripcionLarga) { this.descripcionLarga = descripcionLarga; }
    public String getTextoEtiqueta() { return textoEtiqueta; }
    public void setTextoEtiqueta(String textoEtiqueta) { this.textoEtiqueta = textoEtiqueta; }
    public List<String> getHashtags() { return hashtags; }
    public void setHashtags(List<String> hashtags) { this.hashtags = hashtags; }
    public boolean isModoMock() { return modoMock; }
    public void setModoMock(boolean modoMock) { this.modoMock = modoMock; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
