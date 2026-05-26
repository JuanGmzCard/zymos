package com.alera.config;

import com.alera.model.Tenant;
import java.awt.Color;

/**
 * Colores y nombre de marca del tenant, pre-parseados para PDF y Excel.
 * Separado de Tenant para no acoplar los servicios de exportación al modelo JPA.
 */
public record ExportBranding(
        String name,
        Color primary,       // colorPrimary  → verde medio (títulos de sección)
        Color primaryDark,   // colorNavbar   → verde oscuro (cabeceras de tabla)
        Color accent,        // colorAccent   → dorado (acento)
        Color cream,         // colorCream    → crema (texto sobre fondo oscuro)
        Color background     // colorBodyBg   → fondo (filas alternas)
) {
    private static final Color DEF_PRIMARY      = new Color(54, 67, 24);
    private static final Color DEF_PRIMARY_DARK = new Color(36, 46, 13);
    private static final Color DEF_ACCENT       = new Color(201, 160, 40);
    private static final Color DEF_CREAM        = new Color(245, 237, 208);
    private static final Color DEF_BACKGROUND   = new Color(240, 237, 226);

    public static ExportBranding from(Tenant tenant) {
        if (tenant == null) return defaults("Alera");
        return new ExportBranding(
                tenant.getName() != null ? tenant.getName() : "Alera",
                parseHex(tenant.getColorPrimary(),  DEF_PRIMARY),
                parseHex(tenant.getColorNavbar(),   DEF_PRIMARY_DARK),
                parseHex(tenant.getColorAccent(),   DEF_ACCENT),
                parseHex(tenant.getColorCream(),    DEF_CREAM),
                parseHex(tenant.getColorBodyBg(),   DEF_BACKGROUND)
        );
    }

    public static ExportBranding defaults(String name) {
        return new ExportBranding(name,
                DEF_PRIMARY, DEF_PRIMARY_DARK, DEF_ACCENT, DEF_CREAM, DEF_BACKGROUND);
    }

    /** Aclara un color mezclándolo con blanco según el factor (0=sin cambio, 1=blanco). */
    public static Color lighten(Color c, float factor) {
        int r = Math.min(255, c.getRed()   + (int) ((255 - c.getRed())   * factor));
        int g = Math.min(255, c.getGreen() + (int) ((255 - c.getGreen()) * factor));
        int b = Math.min(255, c.getBlue()  + (int) ((255 - c.getBlue())  * factor));
        return new Color(r, g, b);
    }

    private static Color parseHex(String hex, Color fallback) {
        if (hex == null || hex.isBlank()) return fallback;
        try {
            String h = hex.startsWith("#") ? hex.substring(1) : hex;
            if (h.length() == 6) {
                int rgb = Integer.parseUnsignedInt(h, 16);
                return new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
            }
        } catch (NumberFormatException ignored) {}
        return fallback;
    }
}
