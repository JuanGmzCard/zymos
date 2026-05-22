package com.alera.config;

import com.alera.service.LogAccesoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class AleraAuthFailureHandler implements AuthenticationFailureHandler {

    private final LogAccesoService logService;

    public AleraAuthFailureHandler(LogAccesoService logService) {
        this.logService = logService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest req, HttpServletResponse resp,
                                        AuthenticationException ex) throws IOException {
        String usuario = req.getParameter("username");
        logService.registrar(usuario, "LOGIN_FALLIDO",
                clientIp(req), "/login", req.getHeader("User-Agent"),
                ex.getMessage());
        resp.sendRedirect("/login?error");
    }

    private String clientIp(HttpServletRequest req) {
        String fwd = req.getHeader("X-Forwarded-For");
        return (fwd != null && !fwd.isBlank()) ? fwd.split(",")[0].trim() : req.getRemoteAddr();
    }
}
