package com.alera.config;

import com.alera.service.LogAccesoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class ZymosAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final LogAccesoService logService;
    private final LoginAttemptService loginAttemptService;

    public ZymosAuthSuccessHandler(LogAccesoService logService,
                                    LoginAttemptService loginAttemptService) {
        this.logService          = logService;
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse resp,
                                        Authentication auth) throws IOException {
        loginAttemptService.resetear(clientIp(req));
        logService.registrar(auth.getName(), "LOGIN_OK",
                clientIp(req), "/login", req.getHeader("User-Agent"), null);
        resp.sendRedirect("/dashboard");
    }

    private String clientIp(HttpServletRequest req) {
        String fwd = req.getHeader("X-Forwarded-For");
        return (fwd != null && !fwd.isBlank()) ? fwd.split(",")[0].trim() : req.getRemoteAddr();
    }
}
