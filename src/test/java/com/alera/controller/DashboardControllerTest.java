package com.alera.controller;

import com.alera.config.*;
import com.alera.dto.DashboardStats;
import com.alera.repository.TenantRepository;
import com.alera.service.DashboardService;
import com.alera.service.JwtService;
import com.alera.service.InsumoInventarioService;
import com.alera.service.LogAccesoService;
import com.alera.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@DisplayName("DashboardController")
class DashboardControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean TenantRepository           tenantRepo;
    @MockBean BrandingProperties         brandingProperties;
    @MockBean ZymosAuthSuccessHandler    successHandler;
    @MockBean ZymosAuthFailureHandler    failureHandler;
    @MockBean ZymosAccessDeniedHandler   accessDeniedHandler;
    @MockBean UsuarioService             usuarioService;
    @MockBean LogAccesoService           logAccesoService;
    @MockBean LoginAttemptService        loginAttemptService;
    @MockBean JwtService                 jwtService;
    @MockBean DashboardService           dashboardService;
    @MockBean InsumoInventarioService    insumoService;

    @BeforeEach
    void setUp() {
        WebMvcTestHelper.configureTenantMock(tenantRepo);
        when(dashboardService.obtenerEstadisticas()).thenReturn(new DashboardStats());
        when(dashboardService.getLitrosPorMes()).thenReturn(java.util.Collections.emptyMap());
        when(dashboardService.getLotesPorEstilo()).thenReturn(java.util.Collections.emptyMap());
        when(insumoService.listarBajoStock()).thenReturn(List.of());
        when(insumoService.listarProximosAVencer(30)).thenReturn(List.of());
    }

    @Test
    @DisplayName("GET /dashboard sin autenticar retorna 401")
    void dashboard_sinAuth_retorna401() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /dashboard con usuario autenticado retorna 200")
    void dashboard_autenticado_retorna200() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));
    }
}
