package com.alera.controller;

import com.alera.config.*;
import com.alera.repository.FacturaItemRepository;
import com.alera.repository.LoteCervezaRepository;
import com.alera.repository.TenantRepository;
import com.alera.service.*;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RecetaController.class)
@DisplayName("RecetaController")
class RecetaControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean TenantRepository           tenantRepo;
    @MockBean BrandingProperties         brandingProperties;
    @MockBean AleraAuthSuccessHandler    successHandler;
    @MockBean AleraAuthFailureHandler    failureHandler;
    @MockBean AleraAccessDeniedHandler   accessDeniedHandler;
    @MockBean UsuarioService             usuarioService;
    @MockBean LogAccesoService           logAccesoService;
    @MockBean LoginAttemptService        loginAttemptService;
    @MockBean JwtService                 jwtService;
    @MockBean RecetaService              recetaService;
    @MockBean LoteCervezaRepository      loteRepo;
    @MockBean InsumoInventarioService    insumoService;
    @MockBean TipoCervezaService         tipoCervezaService;
    @MockBean FacturaItemRepository      facturaItemRepo;
    @MockBean PdfExportService           pdfExportService;

    @BeforeEach
    void setUp() {
        WebMvcTestHelper.configureTenantMock(tenantRepo);
        when(recetaService.listarPaginado(any(), anyInt())).thenReturn(new PageImpl<>(List.of()));
        when(recetaService.suggest(anyString(), any())).thenReturn(List.of());
        when(insumoService.listarTodos()).thenReturn(List.of());
        when(tipoCervezaService.listarActivos()).thenReturn(List.of());
    }

    @Test
    @DisplayName("GET /recetas sin autenticar retorna 401")
    void lista_sinAuth_retorna401() throws Exception {
        mockMvc.perform(get("/recetas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /recetas con ADMIN retorna 200")
    void lista_conAdmin_retorna200() throws Exception {
        mockMvc.perform(get("/recetas"))
                .andExpect(status().isOk())
                .andExpect(view().name("recetas/lista"));
    }

    @Test
    @WithMockUser(roles = "INVENTARIO")
    @DisplayName("GET /recetas con INVENTARIO retorna 200")
    void lista_conInventario_retorna200() throws Exception {
        mockMvc.perform(get("/recetas"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /recetas/suggest retorna JSON")
    void suggest_retornaJson() throws Exception {
        mockMvc.perform(get("/recetas/suggest").param("q", "IP"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
