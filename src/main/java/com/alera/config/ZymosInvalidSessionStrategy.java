package com.alera.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.session.InvalidSessionStrategy;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ZymosInvalidSessionStrategy implements InvalidSessionStrategy {

    @Override
    public void onInvalidSessionDetected(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // Spring Security enruta MissingCsrfTokenException → InvalidSessionAccessDeniedHandler → esta
        // estrategia cuando el token CSRF falta (sin cookie XSRF-TOKEN). En ese caso el usuario puede
        // estar autenticado con una sesión válida — no es una sesión expirada sino un fallo de CSRF.
        // Si hay autenticación real, devolver 403; si no hay sesión activa, redirigir al login.
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);

        if (isAuthenticated) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        request.getSession();
        String cp = request.getContextPath();
        response.sendRedirect((cp != null ? cp : "") + "/login?expired=true");
    }
}
