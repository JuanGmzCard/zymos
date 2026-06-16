package com.alera.config;

import io.sentry.Sentry;
import io.sentry.protocol.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Enriquece cada evento de Sentry con el tenant activo y el usuario autenticado.
 * Es un no-op cuando Sentry está deshabilitado (SENTRY_DSN vacío).
 */
@Component
public class SentryTenantInterceptor implements HandlerInterceptor, WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this);
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        if (!Sentry.isEnabled()) return true;

        Sentry.configureScope(scope -> {
            String tenantId = TenantContext.getCurrentTenant();
            if (tenantId != null && !tenantId.isBlank()) {
                scope.setTag("tenant_id", tenantId);
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()
                    && !(auth instanceof AnonymousAuthenticationToken)) {
                User sentryUser = new User();
                sentryUser.setUsername(auth.getName());
                scope.setUser(sentryUser);
            }
        });

        return true;
    }
}
