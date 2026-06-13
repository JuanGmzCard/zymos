package com.alera.controller;

import com.alera.config.*;
import com.alera.model.CategoriaEquipo;
import com.alera.model.CategoriaInsumo;
import com.alera.repository.TenantRepository;
import com.alera.service.*;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoriaController.class)
@DisplayName("CategoriaController")
class CategoriaControllerTest {

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
    @MockBean CategoriaInsumoService    insumoService;
    @MockBean CategoriaEquipoService    equipoService;

    @BeforeEach
    void setUp() {
        WebMvcTestHelper.configureTenantMock(tenantRepo);
        when(insumoService.listarTodos()).thenReturn(List.of());
        when(equipoService.listarTodos()).thenReturn(List.of());
    }

    // ── Seguridad ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /admin/categorias sin autenticar retorna 401")
    void index_sinAuth_retorna401() throws Exception {
        mockMvc.perform(get("/admin/categorias"))
                .andExpect(status().isUnauthorized());
    }

    // ── index ─────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/categorias con ADMIN retorna 200 y modelo")
    void index_conAdmin_retorna200() throws Exception {
        mockMvc.perform(get("/admin/categorias"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/categorias"))
                .andExpect(model().attributeExists("categoriasInsumo", "categoriasEquipo"));
    }

    // ── guardar insumo ─────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/categorias/insumo/guardar redirige con success")
    void guardarInsumo_exitoso_redirige() throws Exception {
        CategoriaInsumo cat = new CategoriaInsumo();
        cat.setNombre("Malta");
        when(insumoService.guardar(anyString())).thenReturn(cat);

        mockMvc.perform(post("/admin/categorias/insumo/guardar").with(csrf())
                .param("nombre", "Malta"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categorias"))
                .andExpect(flash().attribute("tipoMensaje", "success"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/categorias/insumo/guardar con error redirige con danger")
    void guardarInsumo_duplicado_redirigeDanger() throws Exception {
        when(insumoService.guardar(anyString()))
            .thenThrow(new RuntimeException("Ya existe una categoría con ese nombre"));

        mockMvc.perform(post("/admin/categorias/insumo/guardar").with(csrf())
                .param("nombre", "Malta"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipoMensaje", "danger"));
    }

    // ── guardar-rapido insumo ──────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/categorias/insumo/guardar-rapido retorna JSON success")
    void guardarInsumoRapido_retornaJson() throws Exception {
        CategoriaInsumo cat = new CategoriaInsumo();
        cat.setId(1L);
        cat.setNombre("Levadura");
        when(insumoService.guardar(anyString())).thenReturn(cat);

        mockMvc.perform(post("/admin/categorias/insumo/guardar-rapido").with(csrf())
                .param("nombre", "Levadura"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.nombre").value("Levadura"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/categorias/insumo/guardar-rapido con error retorna JSON failure")
    void guardarInsumoRapido_error_retornaJsonFailure() throws Exception {
        when(insumoService.guardar(anyString()))
            .thenThrow(new RuntimeException("Ya existe"));

        mockMvc.perform(post("/admin/categorias/insumo/guardar-rapido").with(csrf())
                .param("nombre", "Malta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── toggle insumo ─────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/categorias/insumo/1/toggle redirige con success")
    void toggleInsumo_redirige() throws Exception {
        mockMvc.perform(post("/admin/categorias/insumo/1/toggle").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categorias"))
                .andExpect(flash().attribute("tipoMensaje", "success"));
    }

    // ── eliminar insumo ────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/categorias/insumo/1/eliminar redirige con success")
    void eliminarInsumo_redirige() throws Exception {
        mockMvc.perform(post("/admin/categorias/insumo/1/eliminar").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categorias"))
                .andExpect(flash().attribute("tipoMensaje", "success"));
    }

    // ── guardar equipo ─────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/categorias/equipo/guardar redirige con success")
    void guardarEquipo_exitoso_redirige() throws Exception {
        CategoriaEquipo cat = new CategoriaEquipo();
        cat.setNombre("Fermentador");
        when(equipoService.guardar(anyString())).thenReturn(cat);

        mockMvc.perform(post("/admin/categorias/equipo/guardar").with(csrf())
                .param("nombre", "Fermentador"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipoMensaje", "success"));
    }

    // ── guardar-rapido equipo ──────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/categorias/equipo/guardar-rapido retorna JSON success")
    void guardarEquipoRapido_retornaJson() throws Exception {
        CategoriaEquipo cat = new CategoriaEquipo();
        cat.setId(1L);
        cat.setNombre("Bomba");
        when(equipoService.guardar(anyString())).thenReturn(cat);

        mockMvc.perform(post("/admin/categorias/equipo/guardar-rapido").with(csrf())
                .param("nombre", "Bomba"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.nombre").value("Bomba"));
    }
}
