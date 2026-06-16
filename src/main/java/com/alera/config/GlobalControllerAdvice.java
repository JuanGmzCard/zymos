package com.alera.config;

import com.alera.model.Tenant;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.i18n.LocaleContextHolder;
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

    // Nonce CSP generado por CspNonceFilter — usar th:attr="nonce=${cspNonce}" en <script>/<style> inline.
    @ModelAttribute("cspNonce")
    public String cspNonce(HttpServletRequest request) {
        Object nonce = request.getAttribute(CspNonceFilter.CSP_NONCE_ATTR);
        return nonce != null ? nonce.toString() : "";
    }

    // Idioma activo — usado en el toggle de idioma del navbar.
    // Patrón en templates: #{enum.EstadoVenta.__${venta.estado}__}
    @ModelAttribute("currentLang")
    public String currentLang() {
        return LocaleContextHolder.getLocale().getLanguage();
    }
}
