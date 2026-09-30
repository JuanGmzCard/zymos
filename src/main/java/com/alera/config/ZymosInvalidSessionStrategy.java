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
        // estrategia cuando el token CSRF falta (sin cookie XSRF-TOKEN). El distinctor clave es:
        //   - CSRF path: la sesión SÍ existe en BD (sessionValid=true) pero falta el token CSRF.
        //   - SessionManagement path: la sesión NO existe en BD (sessionValid=false) → expiración real.
        // Solo devolver 403 cuando la sesión es válida Y el usuario está autenticado (CSRF path puro).
        boolean sessionIsValid = request.isRequestedSessionIdValid();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);

        String cp = request.getContextPath() != null ? request.getContextPath() : "";

        if (isAuthenticated && sessionIsValid) {
            // Sesión válida en BD + usuario autenticado: el token CSRF falta (cookie XSRF-TOKEN ausente
            // o caducada). No es una sesión expirada. Redirigir al inicio para que el navegador reciba
            // una nueva cookie CSRF y el usuario pueda continuar sin ver un error confuso.
            response.sendRedirect(cp + "/");
            return;
        }

        request.getSession();
        response.sendRedirect(cp + "/login?expired=true");
    }
}
