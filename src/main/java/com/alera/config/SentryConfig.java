package com.alera.config;

import io.sentry.EventProcessor;
import io.sentry.Hint;
import io.sentry.SentryEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SentryConfig {

    /**
     * Añade el tag "tenant" a cada evento de Sentry con el subdomain activo en el request.
     * El Spring Boot Starter recoge automáticamente beans de tipo EventProcessor.
     */
    @Bean
    public EventProcessor tenantEventProcessor() {
        return new EventProcessor() {
            @Override
            public SentryEvent process(SentryEvent event, Hint hint) {
                String tenant = TenantContext.getCurrentTenant();
                if (tenant != null && !tenant.isBlank()) {
                    event.setTag("tenant", tenant);
                }
                return event;
            }
        };
    }
}
