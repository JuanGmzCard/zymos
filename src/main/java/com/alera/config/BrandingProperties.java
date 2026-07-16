package com.alera.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.brand")
public class BrandingProperties {

    private String name          = "Alera";
    private String tagline       = "Sistema de Trazabilidad Cervecera";
    private String logoUrl       = "";
    private String colorNavbar   = "#242E0D";
    private String colorPrimary  = "#364318";
    private String colorAccent   = "#C9A028";
    private String colorAccentHover = "#E0B840";
    private String colorCream    = "#F5EDD0";
    private String colorBodyBg   = "#F0EDE2";
    private String fontHeadings    = "Cinzel";
    private String fontBody        = "Raleway";
    private String simboloMoneda   = "$";

    public String getName()             { return name; }
    public void   setName(String v)     { this.name = v; }
    public String getTagline()          { return tagline; }
    public void   setTagline(String v)  { this.tagline = v; }
    public String getLogoUrl()          { return logoUrl; }
    public void   setLogoUrl(String v)  { this.logoUrl = v; }
    public String getColorNavbar()      { return colorNavbar; }
    public void   setColorNavbar(String v)      { this.colorNavbar = v; }
    public String getColorPrimary()     { return colorPrimary; }
    public void   setColorPrimary(String v)     { this.colorPrimary = v; }
    public String getColorAccent()      { return colorAccent; }
    public void   setColorAccent(String v)      { this.colorAccent = v; }
    public String getColorAccentHover() { return colorAccentHover; }
    public void   setColorAccentHover(String v) { this.colorAccentHover = v; }
    public String getColorCream()       { return colorCream; }
    public void   setColorCream(String v)       { this.colorCream = v; }
    public String getColorBodyBg()      { return colorBodyBg; }
    public void   setColorBodyBg(String v)      { this.colorBodyBg = v; }
    public String getFontHeadings()      { return fontHeadings; }
    public void   setFontHeadings(String v)      { this.fontHeadings = v; }
    public String getFontBody()          { return fontBody; }
    public void   setFontBody(String v)          { this.fontBody = v; }
    public String getSimboloMoneda()     { return simboloMoneda; }
    public void   setSimboloMoneda(String v)     { this.simboloMoneda = v; }
}
