package com.alera.config;

import com.alera.model.Tenant;
import com.alera.repository.TenantRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class TenantFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantFilter.class);
    private static final String TENANT_ATTR = "currentTenant";

    private final TenantRepository tenantRepo;
    private final String defaultSubdomain;

    // Cache con TTL — expira automáticamente; evictCache/evictAll para invalidación manual
    private final Cache<String, Tenant> cache;

    public TenantFilter(TenantRepository tenantRepo, String defaultSubdomain, long ttlMinutes) {
        this.tenantRepo = tenantRepo;
        this.defaultSubdomain = defaultSubdomain;
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES)
                .maximumSize(200)
                .build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/css/") || path.startsWith("/js/")
            || path.startsWith("/img/") || path.startsWith("/webjars/")
            || path.startsWith("/favicon");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String subdomain = extractSubdomain(request.getServerName());
        Tenant tenant = resolveTenant(subdomain);

        if (tenant == null) {
            log.warn("Tenant '{}' no encontrado — usando respuesta 503", subdomain);
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Tenant no encontrado");
            return;
        }

        request.setAttribute(TENANT_ATTR, tenant);
        TenantContext.setCurrentTenant(tenant.getSubdomain());
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private Tenant resolveTenant(String subdomain) {
        Tenant t = cache.get(subdomain,
                s -> tenantRepo.findBySubdomainAndActiveTrue(s).orElse(null));
        if (t != null) return t;
        if (!subdomain.equals(defaultSubdomain)) {
            t = cache.get(defaultSubdomain,
                    s -> tenantRepo.findBySubdomainAndActiveTrue(s).orElse(null));
        }
        return t;
    }

    public void evictCache(String subdomain) {
        cache.invalidate(subdomain);
    }

    public void evictAll() {
        cache.invalidateAll();
    }

    private String extractSubdomain(String host) {
        if (host == null) return defaultSubdomain;
        // Quitar puerto si existe: "cerveceria1.app.com:8080" → "cerveceria1.app.com"
        host = host.split(":")[0];
        if (host.equals("localhost") || host.equals("127.0.0.1") || !host.contains(".")) {
            return defaultSubdomain;
        }
        return host.split("\\.")[0];
    }
}
