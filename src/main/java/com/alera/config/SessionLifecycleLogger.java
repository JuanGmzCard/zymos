package com.alera.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.session.events.SessionCreatedEvent;
import org.springframework.session.events.SessionDeletedEvent;
import org.springframework.session.events.SessionExpiredEvent;
import org.springframework.stereotype.Component;

@Component
public class SessionLifecycleLogger {

    private static final Logger log = LoggerFactory.getLogger(SessionLifecycleLogger.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @EventListener
    public void onSessionCreated(SessionCreatedEvent event) {
        log.info("Session CREATED: {}", event.getSessionId());
    }

    @EventListener
    public void onSessionDeleted(SessionDeletedEvent event) {
        log.warn("Session DELETED: {}", event.getSessionId());
    }

    @EventListener
    public void onSessionExpired(SessionExpiredEvent event) {
        log.warn("Session EXPIRED (cleanup): {}", event.getSessionId());
    }

    /** Cada 2 minutos loguea cuántas sesiones hay en spring_session */
    @Scheduled(fixedDelay = 120_000)
    public void logSessionCount() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM spring_session", Integer.class);
            log.info("spring_session total rows: {}", count);
        } catch (Exception e) {
            log.warn("No se pudo consultar spring_session: {}", e.getMessage());
        }
    }
}
