package com.alera.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.session.InvalidSessionStrategy;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ZymosInvalidSessionStrategy implements InvalidSessionStrategy {

    private static final Logger log = LoggerFactory.getLogger(ZymosInvalidSessionStrategy.class);

    @Override
    public void onInvalidSessionDetected(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // Spring Security enruta MissingCsrfTokenException → InvalidSessionAccessDeniedHandler → esta
        // estrategia cuando el token CSRF falta (sin cookie XSRF-TOKEN). El distinctor clave es:
        //   - CSRF path: la sesión SÍ existe en BD (sessionValid=true) pero falta el token CSRF.
        //   - SessionManagement path: la sesión NO existe en BD (sessionValid=false) → expiración real.
        boolean sessionIsValid = request.isRequestedSessionIdValid();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);

        String cp = request.getContextPath() != null ? request.getContextPath() : "";

        if (isAuthenticated && sessionIsValid) {
            // Cookie CSRF ausente con sesión válida: redirigir a la página de origen (Referer)
            // para que el navegador reciba una nueva cookie CSRF. Si no hay Referer del mismo
            // origen, redirigir al inicio.
            String target = safeReferer(request, cp);
            log.warn("ZymosInvalidSessionStrategy: cookie CSRF ausente para usuario='{}' method={} uri={} → redirect {}",
                    auth.getName(), request.getMethod(), request.getRequestURI(), target);
            response.sendRedirect(target);
            return;
        }

        request.getSession();
        response.sendRedirect(cp + "/login?expired=true");
    }

    /**
     * Devuelve el Referer si es del mismo origen (previene open redirect).
     * Ejemplo: Referer=http://localhost:8080/usuarios → /usuarios
     */
    private String safeReferer(HttpServletRequest request, String cp) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) return cp + "/";

        String scheme = request.getScheme();
        String host   = request.getServerName();
        int    port   = request.getServerPort();
        String origin = scheme + "://" + host;
        if ((scheme.equals("http")  && port != 80) ||
            (scheme.equals("https") && port != 443)) {
            origin += ":" + port;
        }

        if (referer.startsWith(origin + "/") || referer.equals(origin)) {
            return referer;
        }
        return cp + "/";
    }
}
