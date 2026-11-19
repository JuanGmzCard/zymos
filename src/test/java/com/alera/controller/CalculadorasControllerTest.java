package com.alera.controller;

import com.alera.config.*;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CalculadorasController.class)
@DisplayName("CalculadorasController")
class CalculadorasControllerTest {

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
    @WithMockUser
    @DisplayName("GET /calculadoras retorna 200 con vista calculadoras")
    void calculadoras_retorna200() throws Exception {
        mockMvc.perform(get("/calculadoras"))
                .andExpect(status().isOk())
                .andExpect(view().name("calculadoras"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /calculadoras no tiene errores en el modelo")
    void calculadoras_modeloSinErrores() throws Exception {
        mockMvc.perform(get("/calculadoras"))
                .andExpect(status().isOk())
                .andExpect(model().hasNoErrors());
    }
}
