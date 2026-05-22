package com.alera.config;

import com.alera.model.Tenant;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final BrandingProperties defaultBranding;

    public GlobalControllerAdvice(BrandingProperties defaultBranding) {
        this.defaultBranding = defaultBranding;
    }

    // Expone el tenant actual como "branding" — los templates usan ${branding.name}, ${branding.colorAccent}, etc.
    // Si el TenantFilter no pudo resolver el tenant, cae al branding por defecto (properties).
    // Try-catch defensivo: durante el manejo de errores, el request puede estar en estado inconsistente.
    @ModelAttribute("branding")
    public Object branding(HttpServletRequest request) {
        try {
            Object tenant = request.getAttribute("currentTenant");
            if (tenant instanceof Tenant t) return t;
        } catch (Exception ignored) {}
        return defaultBranding;
    }

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
