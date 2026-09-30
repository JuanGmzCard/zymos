package com.alera.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.web.session.InvalidSessionStrategy;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.List;

@Component
public class ZymosInvalidSessionStrategy implements InvalidSessionStrategy {

    private static final Logger log = LoggerFactory.getLogger(ZymosInvalidSessionStrategy.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void onInvalidSessionDetected(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String staleId = request.getRequestedSessionId();
        log.warn("Sesión inválida detectada: staleId='{}' uri={} method={}",
                staleId, request.getRequestURI(), request.getMethod());

        // Diagnóstico: verificar si el ID existe en BD y qué IDs hay en spring_session
        if (staleId != null) {
            try {
                Integer cnt = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM spring_session WHERE session_id = ?",
                        Integer.class, staleId);
                List<String> sample = jdbcTemplate.queryForList(
                        "SELECT session_id FROM spring_session LIMIT 5", String.class);
                log.warn("  → DB spring_session: staleId encontrado={} total_sample={} ids={}",
                        cnt, sample.size(), sample);
            } catch (Exception e) {
                log.warn("  → DB check falló: {}", e.getMessage());
            }
        }

        // Crear sesión nueva antes del redirect: Spring Session sobrescribe la cookie obsoleta
        // con el nuevo ID válido → el siguiente request no dispara la estrategia → rompe el loop.
        request.getSession();

        String cp = request.getContextPath();
        response.sendRedirect((cp != null ? cp : "") + "/login?expired=true");
    }
}
