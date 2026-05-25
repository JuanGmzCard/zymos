package com.alera.config;

import com.alera.service.JwtService;
import com.alera.service.UsuarioService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Valida tokens Bearer JWT para los endpoints /api/**.
 * Si el token es válido y el tenant del token coincide con el tenant activo,
 * establece la autenticación en el SecurityContext y deja pasar la request.
 * Si no hay token o el token es inválido, el request continúa sin autenticación
 * (Basic Auth puede tomar el relevo).
 *
 * No es @Component — se registra como @Bean en SecurityConfig para evitar
 * doble registro como servlet filter.
 */
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService     jwtService;
    private final UsuarioService usuarioService;

    public JwtFilter(JwtService jwtService, UsuarioService usuarioService) {
        this.jwtService     = jwtService;
        this.usuarioService = usuarioService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        if (!jwtService.validarToken(token)) {
            chain.doFilter(request, response);
            return;
        }

        // Verificar que el tenant del token coincide con el tenant activo del request
        String tokenTenant   = jwtService.extraerTenant(token);
        String currentTenant = TenantContext.getCurrentTenant();
        if (tokenTenant != null && !tokenTenant.equals(currentTenant)) {
            chain.doFilter(request, response);
            return;
        }

        String username = jwtService.extraerUsername(token);
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails user = usuarioService.loadUserByUsername(username);
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Solo actúa en /api/** — el resto usa Basic Auth o sesión
        return !request.getRequestURI().startsWith("/api/");
    }
}
