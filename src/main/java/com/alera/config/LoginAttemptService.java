package com.alera.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Rastrea intentos fallidos de login por IP.
 * Bloquea temporalmente una IP tras superar el umbral configurable.
 * El contador expira automáticamente transcurrido el período de bloqueo.
 */
@Service
public class LoginAttemptService {

    private final Cache<String, Integer> cache;
    private final int maxIntentos;
    private final long bloqueoMinutos;

    public LoginAttemptService(
            @Value("${app.login.max-intentos:5}") int maxIntentos,
            @Value("${app.login.bloqueo-minutos:15}") long bloqueoMinutos) {
        this.maxIntentos   = maxIntentos;
        this.bloqueoMinutos = bloqueoMinutos;
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(bloqueoMinutos, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .build();
    }

    public void registrarFallo(String ip) {
        cache.put(ip, getIntentos(ip) + 1);
    }

    public void resetear(String ip) {
        cache.invalidate(ip);
    }

    public boolean estaBloqueado(String ip) {
        return getIntentos(ip) >= maxIntentos;
    }

    public int getIntentos(String ip) {
        Integer n = cache.getIfPresent(ip);
        return n == null ? 0 : n;
    }

    public int getMaxIntentos()      { return maxIntentos; }
    public long getBloqueoMinutos()  { return bloqueoMinutos; }
}
