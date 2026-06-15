package com.alera.config;

import com.alera.model.Tenant;
import com.alera.repository.TenantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantFilterTest {

    @Mock TenantRepository tenantRepo;
    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @Mock FilterChain chain;

    private static final int DIAS_GRACIA = 7;

    private TenantFilter filter;

    @BeforeEach
    void setup() {
        filter = new TenantFilter(tenantRepo, "default", 5, DIAS_GRACIA);
        lenient().when(request.getServerName()).thenReturn("localhost");
        lenient().when(request.getRequestURI()).thenReturn("/dashboard");
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    private Tenant tenant(LocalDate planFin) {
        Tenant t = new Tenant();
        t.setSubdomain("default");
        t.setActive(true);
        t.setPlanFin(planFin);
        return t;
    }

    @Test
    void tenantNoEncontrado_responde503() throws Exception {
        when(tenantRepo.findBySubdomainAndActiveTrue("default")).thenReturn(Optional.empty());

        filter.doFilter(request, response, chain);

        verify(response).sendError(eq(HttpServletResponse.SC_SERVICE_UNAVAILABLE), any());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void planSinFecha_continua() throws Exception {
        when(tenantRepo.findBySubdomainAndActiveTrue("default")).thenReturn(Optional.of(tenant(null)));
        when(request.getRequestURI()).thenReturn("/dashboard");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).sendRedirect(any());
    }

    @Test
    void planVencidoDentroDeGracia_continua() throws Exception {
        when(tenantRepo.findBySubdomainAndActiveTrue("default"))
                .thenReturn(Optional.of(tenant(LocalDate.now().minusDays(3))));
        when(request.getRequestURI()).thenReturn("/dashboard");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).sendRedirect(any());
    }

    @Test
    void planVencidoFueraDeGracia_redirigeAPlanVencido() throws Exception {
        when(tenantRepo.findBySubdomainAndActiveTrue("default"))
                .thenReturn(Optional.of(tenant(LocalDate.now().minusDays(DIAS_GRACIA + 1))));
        when(request.getRequestURI()).thenReturn("/dashboard");

        filter.doFilter(request, response, chain);

        verify(response).sendRedirect("/plan-vencido");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void planVencidoFueraDeGracia_permitePlanVencido() throws Exception {
        when(tenantRepo.findBySubdomainAndActiveTrue("default"))
                .thenReturn(Optional.of(tenant(LocalDate.now().minusDays(DIAS_GRACIA + 1))));
        when(request.getRequestURI()).thenReturn("/plan-vencido");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).sendRedirect(any());
    }

    @Test
    void planVencidoFueraDeGracia_permiteLogout() throws Exception {
        when(tenantRepo.findBySubdomainAndActiveTrue("default"))
                .thenReturn(Optional.of(tenant(LocalDate.now().minusDays(DIAS_GRACIA + 1))));
        when(request.getRequestURI()).thenReturn("/logout");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).sendRedirect(any());
    }

    @Test
    void limpiaTenantContextDespuesDeFiltrar() throws Exception {
        when(tenantRepo.findBySubdomainAndActiveTrue("default")).thenReturn(Optional.of(tenant(null)));
        when(request.getRequestURI()).thenReturn("/dashboard");

        filter.doFilter(request, response, chain);

        TenantContext.setCurrentTenant("check");
        org.assertj.core.api.Assertions.assertThat(TenantContext.getCurrentTenant()).isEqualTo("check");
        TenantContext.clear();
    }
}
