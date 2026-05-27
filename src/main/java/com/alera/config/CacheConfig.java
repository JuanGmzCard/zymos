package com.alera.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        // Estadísticas del dashboard: 5 minutos TTL, 1 entrada
        manager.registerCustomCache("dashboard-stats",
                Caffeine.newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(50)
                        .recordStats()
                        .build());
        // Datos de gráficas: 10 minutos TTL (cambian menos frecuente)
        manager.registerCustomCache("dashboard-litros-mes",
                Caffeine.newBuilder()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(50)
                        .recordStats()
                        .build());
        manager.registerCustomCache("dashboard-estilos",
                Caffeine.newBuilder()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(50)
                        .recordStats()
                        .build());
        return manager;
    }
}