package com.alera.controller;

import com.alera.config.*;
import com.alera.repository.TenantRepository;
import com.alera.service.LogAccesoService;
import com.alera.service.TipoCervezaService;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TipoCervezaController.class)
@DisplayName("TipoCervezaController")
class TipoCervezaControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean TenantRepository           tenantRepo;
    @MockBean BrandingProperties         brandingProperties;
    @MockBean AleraAuthSuccessHandler    successHandler;
    @MockBean AleraAuthFailureHandler    failureHandler;
    @MockBean AleraAccessDeniedHandler   accessDeniedHandler;
    @MockBean UsuarioService             usuarioService;
    @MockBean LogAccesoService           logAccesoService;
    @MockBean LoginAttemptService        loginAttemptService;
    @MockBean TipoCervezaService         tipoCervezaService;

    @BeforeEach
    void setUp() {
        WebMvcTestHelper.configureTenantMock(tenantRepo);
        when(tipoCervezaService.listarTodos()).thenReturn(List.of());
    }

    @Test
    @DisplayName("GET /tipos-cerveza sin autenticar retorna 401")
    void lista_sinAuth_retorna401() throws Exception {
        mockMvc.perform(get("/tipos-cerveza"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /tipos-cerveza con ADMIN retorna 200")
    void lista_conAdmin_retorna200() throws Exception {
        mockMvc.perform(get("/tipos-cerveza"))
                .andExpect(status().isOk())
                .andExpect(view().name("tipos-cerveza/lista"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /tipos-cerveza/guardar-rapido retorna JSON con éxito")
    void guardarRapido_nombreNuevo_retornaSuccess() throws Exception {
        com.alera.model.TipoCerveza tipo = new com.alera.model.TipoCerveza();
        tipo.setId(1L); tipo.setNombre("Porter");
        when(tipoCervezaService.existePorNombre("Porter")).thenReturn(false);
        when(tipoCervezaService.guardar(org.mockito.ArgumentMatchers.any())).thenReturn(tipo);

        mockMvc.perform(post("/tipos-cerveza/guardar-rapido").with(csrf())
                .param("nombre", "Porter"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
