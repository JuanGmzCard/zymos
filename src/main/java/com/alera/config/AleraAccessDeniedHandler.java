package com.alera.config;

import com.alera.service.LogAccesoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class AleraAccessDeniedHandler implements AccessDeniedHandler {

    private final LogAccesoService logService;

    public AleraAccessDeniedHandler(LogAccesoService logService) {
        this.logService = logService;
    }

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse resp,
                       AccessDeniedException ex) throws IOException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String usuario = (auth != null) ? auth.getName() : "anónimo";
        logService.registrar(usuario, "ACCESO_DENEGADO",
                clientIp(req), req.getRequestURI(),
                req.getHeader("User-Agent"), null);
        resp.sendRedirect("/error?status=403");
    }

    private String clientIp(HttpServletRequest req) {
        String fwd = req.getHeader("X-Forwarded-For");
        return (fwd != null && !fwd.isBlank()) ? fwd.split(",")[0].trim() : req.getRemoteAddr();
    }
}
