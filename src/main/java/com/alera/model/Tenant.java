package com.alera.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tenants")
public class Tenant {

    @Id
    @Column(length = 100)
    private String subdomain;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 300)
    private String tagline;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "color_navbar", length = 20)
    private String colorNavbar   = "#242E0D";

    @Column(name = "color_primary", length = 20)
    private String colorPrimary  = "#364318";

    @Column(name = "color_accent", length = 20)
    private String colorAccent   = "#C9A028";

    @Column(name = "color_accent_hover", length = 20)
    private String colorAccentHover = "#E0B840";

    @Column(name = "color_cream", length = 20)
    private String colorCream    = "#F5EDD0";

    @Column(name = "color_body_bg", length = 20)
    private String colorBodyBg   = "#F0EDE2";

    @Column(name = "font_headings", length = 100)
    private String fontHeadings = "Cinzel";

    @Column(name = "font_body", length = 100)
    private String fontBody     = "Raleway";

    @Column(name = "email_admin", length = 200)
    private String emailAdmin;

    private boolean active = true;

    @Column(name = "alertas_intentos_fallidos", nullable = false)
    private int alertasIntentosFallidos = 0;

    @Column(name = "alertas_ultimo_intento")
    private LocalDateTime alertasUltimoIntento;

    @Column(name = "alertas_ultimo_exito")
    private LocalDateTime alertasUltimoExito;

    @Column(name = "max_lotes")
    private Integer maxLotes;

    @Column(name = "max_usuarios")
    private Integer maxUsuarios;

    /** "MENSUAL","TRIMESTRAL","SEMESTRAL","ANUAL","BIANUAL" — null = sin vencimiento */
    @Column(name = "plan_tipo", length = 20)
    private String planTipo;

    @Column(name = "plan_inicio")
    private LocalDate planInicio;

    @Column(name = "plan_fin")
    private LocalDate planFin;

    public boolean isPlanVencido() {
        return planFin != null && planFin.isBefore(LocalDate.now());
    }

    public boolean isPlanPorVencer() {
        return planFin != null && !isPlanVencido() && planFin.isBefore(LocalDate.now().plusDays(7));
    }

    public String getPlanFinTexto() {
        if (planFin == null) return null;
        if (isPlanVencido()) return "Vencido";
        if (isPlanPorVencer()) return "Por vencer";
        return null;
    }

    public String getSubdomain()            { return subdomain; }
    public void   setSubdomain(String v)    { this.subdomain = v; }
    public String getName()                 { return name; }
    public void   setName(String v)         { this.name = v; }
    public String getTagline()              { return tagline; }
    public void   setTagline(String v)      { this.tagline = v; }
    public String getLogoUrl()              { return logoUrl; }
    public void   setLogoUrl(String v)      { this.logoUrl = v; }
    public String getColorNavbar()          { return colorNavbar; }
    public void   setColorNavbar(String v)  { this.colorNavbar = v; }
    public String getColorPrimary()         { return colorPrimary; }
    public void   setColorPrimary(String v) { this.colorPrimary = v; }
    public String getColorAccent()          { return colorAccent; }
    public void   setColorAccent(String v)  { this.colorAccent = v; }
    public String getColorAccentHover()     { return colorAccentHover; }
    public void   setColorAccentHover(String v) { this.colorAccentHover = v; }
    public String getColorCream()           { return colorCream; }
    public void   setColorCream(String v)   { this.colorCream = v; }
    public String getColorBodyBg()          { return colorBodyBg; }
    public void   setColorBodyBg(String v)  { this.colorBodyBg = v; }
    public String getFontHeadings()           { return fontHeadings; }
    public void   setFontHeadings(String v)  { this.fontHeadings = v; }
    public String getFontBody()              { return fontBody; }
    public void   setFontBody(String v)      { this.fontBody = v; }
    public String getEmailAdmin()             { return emailAdmin; }
    public void   setEmailAdmin(String v)   { this.emailAdmin = v; }
    public boolean isActive()               { return active; }
    public void    setActive(boolean v)     { this.active = v; }
    public int getAlertasIntentosFallidos()             { return alertasIntentosFallidos; }
    public void setAlertasIntentosFallidos(int v)       { this.alertasIntentosFallidos = v; }
    public LocalDateTime getAlertasUltimoIntento()      { return alertasUltimoIntento; }
    public void setAlertasUltimoIntento(LocalDateTime v){ this.alertasUltimoIntento = v; }
    public LocalDateTime getAlertasUltimoExito()        { return alertasUltimoExito; }
    public void setAlertasUltimoExito(LocalDateTime v)  { this.alertasUltimoExito = v; }
    public Integer getMaxLotes()                        { return maxLotes; }
    public void setMaxLotes(Integer v)                  { this.maxLotes = v; }
    public Integer getMaxUsuarios()                     { return maxUsuarios; }
    public void setMaxUsuarios(Integer v)               { this.maxUsuarios = v; }
    public String getPlanTipo()                         { return planTipo; }
    public void setPlanTipo(String v)                   { this.planTipo = v; }
    public LocalDate getPlanInicio()                    { return planInicio; }
    public void setPlanInicio(LocalDate v)              { this.planInicio = v; }
    public LocalDate getPlanFin()                       { return planFin; }
    public void setPlanFin(LocalDate v)                 { this.planFin = v; }
}
