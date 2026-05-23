package com.alera.controller;

import com.alera.config.*;
import com.alera.repository.TenantRepository;
import com.alera.service.LogAccesoService;
import com.alera.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LoginController.class)
@DisplayName("LoginController")
class LoginControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean TenantRepository           tenantRepo;
    @MockBean BrandingProperties         brandingProperties;
    @MockBean AleraAuthSuccessHandler    successHandler;
    @MockBean AleraAuthFailureHandler    failureHandler;
    @MockBean AleraAccessDeniedHandler   accessDeniedHandler;
    @MockBean UsuarioService             usuarioService;
    @MockBean LogAccesoService           logAccesoService;
    @MockBean LoginAttemptService        loginAttemptService;

    @BeforeEach void setUp() { WebMvcTestHelper.configureTenantMock(tenantRepo); }

    @Test
    @DisplayName("GET /login es público y retorna 200")
    void login_esPublico() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /login?error muestra el parámetro error")
    void login_conError_retornaOk() throws Exception {
        mockMvc.perform(get("/login").param("error", ""))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /login?bloqueado muestra bloqueo por fuerza bruta")
    void login_bloqueado_retornaOk() throws Exception {
        mockMvc.perform(get("/login").param("bloqueado", "true"))
                .andExpect(status().isOk());
    }
}
