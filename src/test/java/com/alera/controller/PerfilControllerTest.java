package com.alera.controller;

import com.alera.config.*;
import com.alera.model.Usuario;
import com.alera.repository.TenantRepository;
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

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PerfilController.class)
@DisplayName("PerfilController")
class PerfilControllerTest {

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
    @DisplayName("GET /perfil/password sin autenticar retorna 401")
    void password_sinAuth_retorna401() throws Exception {
        mockMvc.perform(get("/perfil/password"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /perfil/password con usuario autenticado retorna 200")
    void password_autenticado_retorna200() throws Exception {
        mockMvc.perform(get("/perfil/password"))
                .andExpect(status().isOk())
                .andExpect(view().name("perfil/password"));
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("POST /perfil/password con contraseña inválida redirige con error")
    void password_contraseniaInvalida_redirige() throws Exception {
        mockMvc.perform(post("/perfil/password").with(csrf())
                .param("nuevaPassword", "corta")
                .param("confirmarPassword", "corta"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/perfil/password"));
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("POST /perfil/password con contraseña válida redirige al dashboard")
    void password_valida_redirigeDashboard() throws Exception {
        Usuario u = new Usuario();
        u.setId(1L); u.setUsername("admin");
        when(usuarioService.buscarPorUsername("admin")).thenReturn(Optional.of(u));

        mockMvc.perform(post("/perfil/password").with(csrf())
                .param("nuevaPassword", "admin1234")
                .param("confirmarPassword", "admin1234"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }
}
