package com.alera.config;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {

    @Value("${app.default-subdomain:default}")
    private String defaultSubdomain;

    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenant = TenantContext.getCurrentTenant();
        return tenant != null ? tenant : defaultSubdomain;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }
}
