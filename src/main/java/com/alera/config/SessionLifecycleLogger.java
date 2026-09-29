package com.alera.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.session.events.SessionCreatedEvent;
import org.springframework.session.events.SessionDeletedEvent;
import org.springframework.session.events.SessionExpiredEvent;
import org.springframework.stereotype.Component;

@Component
public class SessionLifecycleLogger {

    private static final Logger log = LoggerFactory.getLogger(SessionLifecycleLogger.class);

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
}
