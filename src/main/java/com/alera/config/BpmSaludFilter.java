package com.alera.config;

import com.alera.model.RegistroSintomas;
import com.alera.repository.RegistroSintomasRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

public class BpmSaludFilter extends OncePerRequestFilter {

    private final RegistroSintomasRepository sintomasRepo;

    public BpmSaludFilter(RegistroSintomasRepository sintomasRepo) {
        this.sintomasRepo = sintomasRepo;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {

        if (shouldSkip(req.getServletPath())) {
            chain.doFilter(req, resp);
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            chain.doFilter(req, resp);
            return;
        }

        // ADMIN y SUPERADMIN no requieren chequeo diario
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                            || a.getAuthority().equals("ROLE_SUPERADMIN"));
        if (isAdmin) {
            chain.doFilter(req, resp);
            return;
        }

        String username = auth.getName();
        try {
            Optional<RegistroSintomas> registro =
                    sintomasRepo.findByNombreManipuladorAndFecha(username, LocalDate.now());

            if (registro.isEmpty()) {
                resp.sendRedirect(req.getContextPath() + "/bpm/salud/diario");
                return;
            }

            RegistroSintomas r = registro.get();
            if (r.tieneSintomas() && !r.isAutorizadoPorAdmin()) {
                resp.sendRedirect(req.getContextPath() + "/bpm/salud/bloqueado");
                return;
            }
        } catch (Exception e) {
            // En caso de error de BD, no bloqueamos el acceso
            logger.warn("BpmSaludFilter: error al verificar registro diario para " + username, e);
        }

        chain.doFilter(req, resp);
    }

    private boolean shouldSkip(String path) {
        return path.startsWith("/bpm/salud/")
            || path.startsWith("/css/")
            || path.startsWith("/js/")
            || path.startsWith("/img/")
            || path.startsWith("/webjars/")
            || path.startsWith("/actuator/")
            || path.startsWith("/api/")
            || path.equals("/login")
            || path.startsWith("/login?")
            || path.equals("/logout")
            || path.equals("/error")
            || path.startsWith("/error/")
            || path.equals("/plan-vencido")
            || path.equals("/favicon.ico");
    }
}
