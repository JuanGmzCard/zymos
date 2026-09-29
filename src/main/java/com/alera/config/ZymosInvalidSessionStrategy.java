package com.alera.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.web.session.InvalidSessionStrategy;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class ZymosInvalidSessionStrategy implements InvalidSessionStrategy {

    private static final Logger log = LoggerFactory.getLogger(ZymosInvalidSessionStrategy.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void onInvalidSessionDetected(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String staleId = request.getRequestedSessionId();
        log.warn("Sesión inválida: staleId={} uri={} method={}",
                staleId, request.getRequestURI(), request.getMethod());

        // Diagnóstico: consulta directa a la BD para ver el estado de la sesión
        try {
            Integer countById = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM spring_session WHERE session_id = ?", Integer.class, staleId);
            Integer totalSessions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM spring_session", Integer.class);
            log.warn("  → BD: filas con session_id={}: {} (total sesiones en BD: {})",
                staleId, countById, totalSessions);
            if (totalSessions != null && totalSessions > 0) {
                jdbcTemplate.query(
                    "SELECT session_id, principal_name, last_access_time, max_inactive_interval, " +
                    "(last_access_time + max_inactive_interval*1000) as expiry_ms, " +
                    "extract(epoch from now())*1000 as now_ms " +
                    "FROM spring_session ORDER BY last_access_time DESC LIMIT 3",
                    rs -> {
                        log.warn("  → Sesión en BD: id={} principal={} last_access={} max_inactive={}s expiry_ms={} now_ms={}",
                            rs.getString("session_id"),
                            rs.getString("principal_name"),
                            rs.getLong("last_access_time"),
                            rs.getInt("max_inactive_interval"),
                            rs.getLong("expiry_ms"),
                            rs.getLong("now_ms"));
                    });
            }
        } catch (Exception e) {
            log.warn("  → Error consultando spring_session: {}", e.getMessage());
        }

        Cookie expired = new Cookie("SESSION", "");
        expired.setMaxAge(0);
        expired.setPath("/");
        expired.setHttpOnly(true);
        response.addCookie(expired);

        String cp = request.getContextPath();
        response.sendRedirect((cp != null ? cp : "") + "/login?expired=true");
    }
}
