package com.alera.config;

import com.alera.model.Tenant;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
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
    @ModelAttribute("currentLang")
    public String currentLang() {
        return LocaleContextHolder.getLocale().getLanguage();
    }

    // Nombre del rol custom para el badge del navbar (solo usuarios RBAC).
    // Viene de la authority "NOMBRE_ROL_{nombre}" emitida en loadUserByUsername.
    @ModelAttribute("rolNombreCustom")
    public String rolNombreCustom(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return null;
        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("NOMBRE_ROL_"))
                .findFirst()
                .map(a -> a.substring("NOMBRE_ROL_".length()))
                .orElse(null);
    }
}
