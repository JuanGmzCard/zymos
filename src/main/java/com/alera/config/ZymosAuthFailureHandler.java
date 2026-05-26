package com.alera.config;

import com.alera.service.LogAccesoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class ZymosAuthFailureHandler implements AuthenticationFailureHandler {

    private final LogAccesoService logService;
    private final LoginAttemptService loginAttemptService;

    public ZymosAuthFailureHandler(LogAccesoService logService,
                                    LoginAttemptService loginAttemptService) {
        this.logService          = logService;
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest req, HttpServletResponse resp,
                                        AuthenticationException ex) throws IOException {
        String ip = clientIp(req);
        loginAttemptService.registrarFallo(ip);

        String usuario = req.getParameter("username");
        logService.registrar(usuario, "LOGIN_FALLIDO",
                ip, "/login", req.getHeader("User-Agent"),
                ex.getMessage());

        if (loginAttemptService.estaBloqueado(ip)) {
            resp.sendRedirect("/login?bloqueado=true");
        } else {
            resp.sendRedirect("/login?error");
        }
    }

    private String clientIp(HttpServletRequest req) {
        String fwd = req.getHeader("X-Forwarded-For");
        return (fwd != null && !fwd.isBlank()) ? fwd.split(",")[0].trim() : req.getRemoteAddr();
    }
}
