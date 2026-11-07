package com.alera.controller;

import com.alera.config.*;
import com.alera.model.Receta;
import com.alera.repository.LoteCervezaRepository;
import com.alera.repository.TenantRepository;
import com.alera.service.JwtService;
import com.alera.service.LogAccesoService;
import com.alera.service.RecetaService;
import com.alera.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BienvenidaController.class)
@DisplayName("BienvenidaController")
class BienvenidaControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean TenantRepository         tenantRepo;
    @MockBean BrandingProperties       brandingProperties;
    @MockBean ZymosAuthSuccessHandler  successHandler;
    @MockBean ZymosAuthFailureHandler  failureHandler;
    @MockBean ZymosAccessDeniedHandler accessDeniedHandler;
    @MockBean UsuarioService           usuarioService;
    @MockBean LogAccesoService         logAccesoService;
    @MockBean LoginAttemptService      loginAttemptService;
    @MockBean JwtService               jwtService;
    @MockBean RecetaService            recetaService;
    @MockBean LoteCervezaRepository    loteRepo;

    @BeforeEach
    void setUp() {
        WebMvcTestHelper.configureTenantMock(tenantRepo);
    }

    // ── GET /bienvenida ──────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /bienvenida sin autenticar retorna 401")
    void get_sinAuth_retorna401() throws Exception {
        mockMvc.perform(get("/bienvenida"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /bienvenida sin recetas ni lotes retorna vista bienvenida")
    void get_sinDatos_muestraWizard() throws Exception {
        when(recetaService.listarActivas()).thenReturn(List.of());
        when(loteRepo.count()).thenReturn(0L);

        mockMvc.perform(get("/bienvenida"))
                .andExpect(status().isOk())
                .andExpect(view().name("bienvenida"))
                .andExpect(model().attribute("tieneRecetas", false))
                .andExpect(model().attribute("tieneLotes",   false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /bienvenida con recetas pero sin lotes muestra paso 2 activo")
    void get_conRecetasSinLotes_muestraWizard() throws Exception {
        when(recetaService.listarActivas()).thenReturn(List.of(new Receta()));
        when(loteRepo.count()).thenReturn(0L);

        mockMvc.perform(get("/bienvenida"))
                .andExpect(status().isOk())
                .andExpect(view().name("bienvenida"))
                .andExpect(model().attribute("tieneRecetas", true))
                .andExpect(model().attribute("tieneLotes",   false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /bienvenida con recetas y lotes redirige a /")
    void get_conRecetasYLotes_redirige() throws Exception {
        when(recetaService.listarActivas()).thenReturn(List.of(new Receta()));
        when(loteRepo.count()).thenReturn(1L);

        mockMvc.perform(get("/bienvenida"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    // ── POST /bienvenida/saltar ──────────────────────────────────────────────

    @Test
    @DisplayName("POST /bienvenida/saltar sin autenticar retorna 401")
    void post_saltar_sinAuth_retorna401() throws Exception {
        mockMvc.perform(post("/bienvenida/saltar").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /bienvenida/saltar redirige a /")
    void post_saltar_autenticado_redirige() throws Exception {
        mockMvc.perform(post("/bienvenida/saltar").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }
}
