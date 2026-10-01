package com.alera.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

/**
 * Wraps HttpSessionCsrfTokenRepository to silently ignore IllegalStateException
 * when saveToken() is called during Tomcat error dispatches where the response
 * is already committed (e.g., after a partial write in an export endpoint).
 */
public class SafeHttpSessionCsrfTokenRepository implements CsrfTokenRepository {

    private static final Logger log = LoggerFactory.getLogger(SafeHttpSessionCsrfTokenRepository.class);
    private final HttpSessionCsrfTokenRepository delegate = new HttpSessionCsrfTokenRepository();

    @Override
    public CsrfToken generateToken(HttpServletRequest request) {
        return delegate.generateToken(request);
    }

    @Override
    public void saveToken(CsrfToken token, HttpServletRequest request, HttpServletResponse response) {
        try {
            delegate.saveToken(token, request, response);
        } catch (IllegalStateException e) {
            log.debug("CSRF token not saved — response already committed (error dispatch): {}", e.getMessage());
        }
    }

    @Override
    public CsrfToken loadToken(HttpServletRequest request) {
        return delegate.loadToken(request);
    }
}
