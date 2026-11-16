package com.alera.model;

import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "webhook_endpoints")
@EntityListeners(AuditingEntityListener.class)
public class WebhookEndpoint extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(nullable = false, length = 100)
    private String secret;

    @Column(nullable = false, length = 500)
    private String eventos = "lote.fase_cambiada";

    @Column(nullable = false)
    private boolean activo = true;

    public List<String> getEventosList() {
        return Arrays.stream(eventos.split(","))
                .map(String::trim)
                .filter(e -> !e.isBlank())
                .toList();
    }

    public boolean suscritoA(String evento) {
        return getEventosList().stream().anyMatch(e -> e.equals(evento));
    }

    // ── getters/setters ──────────────────────────────────────────────

    public Long getId()                   { return id; }
    public void setId(Long id)            { this.id = id; }
    public String getNombre()             { return nombre; }
    public void setNombre(String nombre)  { this.nombre = nombre; }
    public String getUrl()                { return url; }
    public void setUrl(String url)        { this.url = url; }
    public String getSecret()             { return secret; }
    public void setSecret(String secret)  { this.secret = secret; }
    public String getEventos()            { return eventos; }
    public void setEventos(String eventos){ this.eventos = eventos; }
    public boolean isActivo()             { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
