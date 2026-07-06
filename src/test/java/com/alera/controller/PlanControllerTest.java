package com.alera.controller;

import com.alera.config.*;
import com.alera.model.Tenant;
import com.alera.repository.LoteCervezaRepository;
import com.alera.repository.TenantRepository;
import com.alera.repository.UsuarioRepository;
import com.alera.service.JwtService;
import com.alera.service.LogAccesoService;
import com.alera.service.StockService;
import com.alera.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlanController.class)
@DisplayName("PlanController")
class PlanControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean TenantRepository          tenantRepo;
    @MockBean BrandingProperties        brandingProperties;
    @MockBean ZymosAuthSuccessHandler   successHandler;
    @MockBean ZymosAuthFailureHandler   failureHandler;
    @MockBean ZymosAccessDeniedHandler  accessDeniedHandler;
    @MockBean UsuarioService            usuarioService;
    @MockBean LogAccesoService          logAccesoService;
    @MockBean LoginAttemptService       loginAttemptService;
    @MockBean JwtService                jwtService;
    @MockBean LoteCervezaRepository     loteRepo;
    @MockBean UsuarioRepository         usuarioRepo;
    @MockBean StockService              stockService;

    @BeforeEach
    void setUp() {
        WebMvcTestHelper.configureTenantMock(tenantRepo);
        when(stockService.getTotalDisponibleLitros()).thenReturn(BigDecimal.ZERO);
        when(loteRepo.count()).thenReturn(0L);
        when(usuarioRepo.countByTenantId(anyString())).thenReturn(0L);
    }

    @Test
    @DisplayName("GET /plan-vencido sin autenticar retorna 401")
    void planVencido_sinAuth_retorna401() throws Exception {
        mockMvc.perform(get("/plan-vencido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /plan-vencido autenticado retorna 200 con vista plan/vencido")
    void planVencido_autenticado_retorna200() throws Exception {
        mockMvc.perform(get("/plan-vencido"))
                .andExpect(status().isOk())
                .andExpect(view().name("plan/vencido"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /mi-plan con tenant existente retorna 200 con vista plan/mi-plan")
    void miPlan_tenantExiste_retorna200() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setSubdomain("default");
        tenant.setName("Alera");
        tenant.setMaxLotes(50);
        tenant.setMaxUsuarios(10);
        when(tenantRepo.findById(any())).thenReturn(Optional.of(tenant));

        mockMvc.perform(get("/mi-plan"))
                .andExpect(status().isOk())
                .andExpect(view().name("plan/mi-plan"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /mi-plan sin tenant en BD redirige a /")
    void miPlan_tenantNoExiste_redirige() throws Exception {
        when(tenantRepo.findById(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/mi-plan"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }
}
