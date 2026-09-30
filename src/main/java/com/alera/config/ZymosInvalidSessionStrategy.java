package com.alera.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.session.InvalidSessionStrategy;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class ZymosInvalidSessionStrategy implements InvalidSessionStrategy {

    private static final Logger log = LoggerFactory.getLogger(ZymosInvalidSessionStrategy.class);

    @Override
    public void onInvalidSessionDetected(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        log.warn("Sesión inválida detectada: staleId={} uri={} method={}",
                request.getRequestedSessionId(), request.getRequestURI(), request.getMethod());

        // Crear sesión nueva antes del redirect: Spring Session sobrescribe la cookie obsoleta
        // con el nuevo ID válido → el siguiente request no dispara la estrategia → rompe el loop.
        // Es el mismo patrón de SimpleRedirectInvalidSessionStrategy (createNewSession=true).
        request.getSession();

        String cp = request.getContextPath();
        response.sendRedirect((cp != null ? cp : "") + "/login?expired=true");
    }
}
