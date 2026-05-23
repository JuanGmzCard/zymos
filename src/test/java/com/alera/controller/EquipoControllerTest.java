package com.alera.controller;

import com.alera.config.*;
import com.alera.repository.TenantRepository;
import com.alera.service.EquipoService;
import com.alera.service.LogAccesoService;
import com.alera.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EquipoController.class)
@DisplayName("EquipoController")
class EquipoControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean TenantRepository           tenantRepo;
    @MockBean BrandingProperties         brandingProperties;
    @MockBean AleraAuthSuccessHandler    successHandler;
    @MockBean AleraAuthFailureHandler    failureHandler;
    @MockBean AleraAccessDeniedHandler   accessDeniedHandler;
    @MockBean UsuarioService             usuarioService;
    @MockBean LogAccesoService           logAccesoService;
    @MockBean LoginAttemptService        loginAttemptService;
    @MockBean EquipoService              equipoService;

    @BeforeEach
    void setUp() {
        WebMvcTestHelper.configureTenantMock(tenantRepo);
        doReturn(new PageImpl<>(Collections.emptyList()))
                .when(equipoService).listarPaginado(any(), anyInt());
        when(equipoService.suggest(anyString(), any())).thenReturn(List.of());
        when(equipoService.listarFermentadoresDisponibles()).thenReturn(List.of());
    }

    @Test
    @DisplayName("GET /equipos sin autenticar retorna 401")
    void lista_sinAuth_retorna401() throws Exception {
        mockMvc.perform(get("/equipos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /equipos con ADMIN retorna 200")
    void lista_conAdmin_retorna200() throws Exception {
        mockMvc.perform(get("/equipos"))
                .andExpect(status().isOk())
                .andExpect(view().name("equipos/lista"));
    }

    @Test
    @WithMockUser(roles = "EQUIPOS")
    @DisplayName("GET /equipos/suggest retorna JSON")
    void suggest_retornaJson() throws Exception {
        mockMvc.perform(get("/equipos/suggest").param("q", "fer"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
