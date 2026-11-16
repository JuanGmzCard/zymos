package com.alera.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class WebhookEndpointFormDto {

    private Long id;

    @NotBlank
    @Size(max = 100)
    private String nombre;

    @NotBlank
    @Size(max = 500)
    @Pattern(regexp = "https?://.+", message = "La URL debe empezar con http:// o https://")
    private String url;

    @Size(max = 100)
    private String secret;

    private List<String> eventosList = new ArrayList<>();

    private boolean activo = true;

    // ── getters/setters ──────────────────────────────────────────────

    public Long getId()                             { return id; }
    public void setId(Long id)                      { this.id = id; }
    public String getNombre()                       { return nombre; }
    public void setNombre(String nombre)            { this.nombre = nombre; }
    public String getUrl()                          { return url; }
    public void setUrl(String url)                  { this.url = url; }
    public String getSecret()                       { return secret; }
    public void setSecret(String secret)            { this.secret = secret; }
    public List<String> getEventosList()            { return eventosList; }
    public void setEventosList(List<String> e)      { this.eventosList = e; }
    public boolean isActivo()                       { return activo; }
    public void setActivo(boolean activo)           { this.activo = activo; }
}
