package com.alera.config;

import jakarta.servlet.http.Cookie;
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
        String staleId = request.getRequestedSessionId();
        log.warn("Sesión inválida detectada — staleId={} uri={} method={}",
                staleId, request.getRequestURI(), request.getMethod());

        // Expirar la cookie stale para que el navegador no la reenvíe
        Cookie expired = new Cookie("SESSION", "");
        expired.setMaxAge(0);
        expired.setPath(contextPath(request));
        expired.setHttpOnly(true);
        response.addCookie(expired);

        response.sendRedirect(contextPath(request) + "/login?expired=true");
    }

    private static String contextPath(HttpServletRequest request) {
        String cp = request.getContextPath();
        return (cp != null && !cp.isEmpty()) ? cp : "/";
    }
}
