package com.alera.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

// Genera un nonce por request para CSP (Content-Security-Policy) y lo expone como
// request attribute "cspNonce" -> GlobalControllerAdvice lo publica a los templates.
// Fase C: header enforced (bloquea). style-src incluye 'unsafe-inline' por los 600+
// atributos style= en templates; script-src sin unsafe-inline (todos los handlers migrados).
public class CspNonceFilter extends OncePerRequestFilter {

    public static final String CSP_NONCE_ATTR = "cspNonce";

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        String nonce = Base64.getEncoder().encodeToString(bytes);
        request.setAttribute(CSP_NONCE_ATTR, nonce);

        response.setHeader("Content-Security-Policy",
                "default-src 'self'; "
                + "script-src 'self' 'nonce-" + nonce + "' https://cdn.jsdelivr.net; "
                + "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://fonts.googleapis.com; "
                + "font-src 'self' https://fonts.gstatic.com https://cdn.jsdelivr.net data:; "
                + "img-src 'self' data: https:; "
                + "connect-src 'self'; "
                + "object-src 'none'; base-uri 'self'; frame-ancestors 'self'");

        filterChain.doFilter(request, response);
    }
}
