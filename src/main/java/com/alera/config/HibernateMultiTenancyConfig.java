package com.alera.config;

import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Configuration;
import java.util.Map;

@Configuration
public class HibernateMultiTenancyConfig implements HibernatePropertiesCustomizer {

    private final TenantIdentifierResolver resolver;

    public HibernateMultiTenancyConfig(TenantIdentifierResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, resolver);
    }
}
