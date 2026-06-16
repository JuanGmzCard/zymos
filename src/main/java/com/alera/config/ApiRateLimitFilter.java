package com.alera.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiting por IP en ventana fija de 1 minuto.
 * - /api/**            → app.api.rate-limit (default 100 req/min)
 * - /admin/migracion/** → app.admin.import-rate-limit (default 10 req/min)
 */
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private final int apiLimit;
    private final int importLimit;
    private final Cache<String, AtomicInteger> apiCounts;
    private final Cache<String, AtomicInteger> importCounts;

    public ApiRateLimitFilter(int apiLimit, int importLimit) {
        this.apiLimit    = apiLimit;
        this.importLimit = importLimit;
        this.apiCounts    = buildCache();
        this.importCounts = buildCache();
    }

    private static Cache<String, AtomicInteger> buildCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !uri.startsWith("/api/") && !uri.startsWith("/admin/migracion/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String ip = resolveIp(request);
        boolean isApi = request.getRequestURI().startsWith("/api/");

        Cache<String, AtomicInteger> cache = isApi ? apiCounts : importCounts;
        int limit = isApi ? apiLimit : importLimit;

        AtomicInteger count = cache.get(ip, k -> new AtomicInteger(0));
        if (count != null && count.incrementAndGet() > limit) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Rate limit exceeded\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private String resolveIp(HttpServletRequest request) {
        String fwd = request.getHeader("X-Forwarded-For");
        return (fwd != null && !fwd.isBlank()) ? fwd.split(",")[0].trim() : request.getRemoteAddr();
    }
}
