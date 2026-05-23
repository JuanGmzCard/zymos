package com.alera.controller;

import com.alera.config.AleraAccessDeniedHandler;
import com.alera.config.AleraAuthFailureHandler;
import com.alera.config.AleraAuthSuccessHandler;
import com.alera.config.BrandingProperties;
import com.alera.config.LoginAttemptService;
import com.alera.model.ElaboracionPlanificada;
import com.alera.repository.TenantRepository;
import com.alera.service.LogAccesoService;
import com.alera.service.PlanificacionService;
import com.alera.service.RecetaService;
import com.alera.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlanificacionController.class)
class PlanificacionControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean PlanificacionService planService;
    @MockBean RecetaService        recetaService;
    @MockBean LogAccesoService     logAccesoService;
    @MockBean UsuarioService       usuarioService;
    @MockBean TenantRepository     tenantRepo;
    @MockBean BrandingProperties   brandingProperties;
    @MockBean AleraAuthSuccessHandler  successHandler;
    @MockBean AleraAuthFailureHandler  failureHandler;
    @MockBean AleraAccessDeniedHandler accessDeniedHandler;
    @MockBean LoginAttemptService      loginAttemptService;

    @BeforeEach
    void setUp() {
        WebMvcTestHelper.configureTenantMock(tenantRepo);
    }

    // ── Seguridad ─────────────────────────────────────────────────────────

    @Test
    void sinAutenticarDevuelveUnauthorized() throws Exception {
        // Con httpBasic() configurado en SecurityConfig, retorna 401 sin credenciales.
        mockMvc.perform(get("/planificacion"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sinAutenticarPostDevuelveUnauthorized() throws Exception {
        mockMvc.perform(post("/planificacion/guardar").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /planificacion ────────────────────────────────────────────────

    @Test
    @WithMockUser
    void paginaPrincipalDevuelveVistaCorrecta() throws Exception {
        when(planService.listarProximas()).thenReturn(List.of());
        when(planService.listarTodas()).thenReturn(List.of());
        when(recetaService.listarActivas()).thenReturn(List.of());

        mockMvc.perform(get("/planificacion"))
                .andExpect(status().isOk())
                .andExpect(view().name("planificacion/index"))
                .andExpect(model().attributeExists("proximas", "todas", "recetas", "estados"));
    }

    // ── GET /planificacion/eventos ────────────────────────────────────────

    @Test
    @WithMockUser
    void eventosRetornaJsonVacio() throws Exception {
        when(planService.listarPorRango(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/planificacion/eventos"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser
    void eventosConPlanRetornaJsonConCamposEsperados() throws Exception {
        var plan = new ElaboracionPlanificada();
        plan.setNombreElaboracion("IPA Test");
        java.lang.reflect.Field idField;
        try {
            idField = ElaboracionPlanificada.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(plan, 1L);
        } catch (Exception ignored) {}
        plan.setFechaPlaneada(java.time.LocalDate.of(2025, 7, 15));

        when(planService.listarPorRango(any(), any())).thenReturn(List.of(plan));

        mockMvc.perform(get("/planificacion/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").exists())
                .andExpect(jsonPath("$[0].start").value("2025-07-15"))
                .andExpect(jsonPath("$[0].backgroundColor").exists())
                .andExpect(jsonPath("$[0].extendedProps").exists());
    }

    // ── POST /planificacion/guardar ───────────────────────────────────────

    @Test
    @WithMockUser(roles = "INVENTARIO")
    void guardarSinRolAdminEsDenegado() throws Exception {
        // AleraAccessDeniedHandler redirige a /error?status=403 (302), no responde 403 directo.
        mockMvc.perform(post("/planificacion/guardar")
                        .param("fechaPlaneada", "2025-07-01")
                        .param("nombreElaboracion", "Test")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void guardarConRolAdminRedirigePlanificacion() throws Exception {
        var plan = new ElaboracionPlanificada();
        when(planService.guardar(any(), isNull())).thenReturn(plan);

        mockMvc.perform(post("/planificacion/guardar")
                        .param("fechaPlaneada", "2025-07-01")
                        .param("nombreElaboracion", "IPA Test")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/planificacion"));
    }

    // ── POST /planificacion/{id}/estado ───────────────────────────────────

    @Test
    @WithMockUser(roles = "FACTURACION")
    void cambiarEstadoSinRolAdminEsDenegado() throws Exception {
        mockMvc.perform(post("/planificacion/1/estado")
                        .param("estado", "EN_PROCESO")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void cambiarEstadoConRolAdminRedirige() throws Exception {
        mockMvc.perform(post("/planificacion/1/estado")
                        .param("estado", "EN_PROCESO")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/planificacion"));
    }

    // ── POST /planificacion/{id}/eliminar ─────────────────────────────────

    @Test
    @WithMockUser(roles = "EQUIPOS")
    void eliminarSinRolAdminEsDenegado() throws Exception {
        mockMvc.perform(post("/planificacion/1/eliminar").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void eliminarConRolAdminRedirige() throws Exception {
        mockMvc.perform(post("/planificacion/1/eliminar").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/planificacion"));
    }
}
