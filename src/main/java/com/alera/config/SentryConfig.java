package com.alera.config;

import io.sentry.EventProcessor;
import io.sentry.Hint;
import io.sentry.SentryEvent;
import io.sentry.protocol.User;
import io.sentry.spring.jakarta.SentryUserProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class SentryConfig {

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

    @Bean
    public SentryUserProvider sentryUserProvider() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()
                    || "anonymousUser".equals(auth.getPrincipal())) {
                return null;
            }
            User user = new User();
            user.setUsername(auth.getName());
            return user;
        };
    }
}
