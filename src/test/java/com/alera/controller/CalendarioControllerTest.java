package com.alera.controller;

import com.alera.config.*;
import com.alera.repository.LoteCervezaRepository;
import com.alera.repository.TenantRepository;
import com.alera.service.JwtService;
import com.alera.service.LogAccesoService;
import com.alera.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CalendarioController.class)
@DisplayName("CalendarioController")
class CalendarioControllerTest {

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
    @MockBean LoteCervezaRepository      loteRepo;

    @BeforeEach
    void setUp() {
        WebMvcTestHelper.configureTenantMock(tenantRepo);
        when(loteRepo.findByPeriodo(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());
    }

    @Test
    @DisplayName("GET /calendario sin autenticar retorna 401")
    void calendario_sinAuth_retorna401() throws Exception {
        mockMvc.perform(get("/calendario"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /calendario con usuario autenticado retorna 200")
    void calendario_autenticado_retorna200() throws Exception {
        mockMvc.perform(get("/calendario"))
                .andExpect(status().isOk())
                .andExpect(view().name("calendario"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /calendario/eventos retorna JSON")
    void eventos_retornaJson() throws Exception {
        mockMvc.perform(get("/calendario/eventos"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
