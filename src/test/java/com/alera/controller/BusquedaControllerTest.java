package com.alera.controller;

import com.alera.config.*;
import com.alera.repository.InsumoInventarioRepository;
import com.alera.repository.LoteCervezaRepository;
import com.alera.repository.RecetaRepository;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BusquedaController.class)
@DisplayName("BusquedaController")
class BusquedaControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean TenantRepository              tenantRepo;
    @MockBean BrandingProperties            brandingProperties;
    @MockBean ZymosAuthSuccessHandler       successHandler;
    @MockBean ZymosAuthFailureHandler       failureHandler;
    @MockBean ZymosAccessDeniedHandler      accessDeniedHandler;
    @MockBean UsuarioService                usuarioService;
    @MockBean LogAccesoService              logAccesoService;
    @MockBean LoginAttemptService           loginAttemptService;
    @MockBean JwtService                    jwtService;
    @MockBean LoteCervezaRepository         loteRepo;
    @MockBean RecetaRepository              recetaRepo;
    @MockBean InsumoInventarioRepository    insumoRepo;

    @BeforeEach
    void setUp() {
        WebMvcTestHelper.configureTenantMock(tenantRepo);
        when(loteRepo.search(any(), any())).thenReturn(java.util.List.of());
        when(recetaRepo.search(any(), any())).thenReturn(java.util.List.of());
        doReturn(new PageImpl<>(java.util.Collections.emptyList())).when(insumoRepo).findByFiltros(any(), any(), any());
    }

    @Test
    @DisplayName("GET /buscar sin autenticar retorna 401")
    void buscar_sinAuth_retorna401() throws Exception {
        mockMvc.perform(get("/buscar").param("q", "IPA"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /buscar con query retorna 200")
    void buscar_autenticado_retorna200() throws Exception {
        mockMvc.perform(get("/buscar").param("q", "IPA"))
                .andExpect(status().isOk())
                .andExpect(view().name("busqueda"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /buscar/suggest retorna JSON con estructura correcta")
    void suggest_retornaJson() throws Exception {
        mockMvc.perform(get("/buscar/suggest").param("q", "IP"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
