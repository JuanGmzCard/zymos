package com.alera.controller;

import com.alera.config.*;
import com.alera.model.Tarea;
import com.alera.model.enums.EstadoTarea;
import com.alera.model.enums.PrioridadTarea;
import com.alera.repository.TenantRepository;
import com.alera.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TareaController.class)
@DisplayName("TareaController")
class TareaControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean TenantRepository        tenantRepo;
    @MockBean BrandingProperties      brandingProperties;
    @MockBean ZymosAuthSuccessHandler successHandler;
    @MockBean ZymosAuthFailureHandler failureHandler;
    @MockBean ZymosAccessDeniedHandler accessDeniedHandler;
    @MockBean UsuarioService          usuarioService;
    @MockBean LogAccesoService        logAccesoService;
    @MockBean LoginAttemptService     loginAttemptService;
    @MockBean JwtService              jwtService;
    @MockBean TareaService              tareaService;
    @MockBean TrazabilidadService       trazabilidadService;
    @MockBean EquipoService             equipoService;
    @MockBean InsumoInventarioService   insumoInventarioService;
    @MockBean PlanificacionService      planificacionService;
    @MockBean OrdenCompraService        ordenCompraService;
    @MockBean VentaService              ventaService;
    @MockBean ClienteService            clienteService;
    @MockBean FacturaProveedorService   facturaProveedorService;
    @MockBean ProveedorService          proveedorService;
    @MockBean RecetaService             recetaService;
    @MockBean BarrilService             barrilService;

    private Tarea tareaEjemplo() {
        Tarea t = new Tarea();
        t.setId(1L);
        t.setTitulo("Limpiar fermentador");
        t.setEstado(EstadoTarea.PENDIENTE);
        t.setPrioridad(PrioridadTarea.MEDIA);
        t.setItems(new ArrayList<>());
        return t;
    }

    @BeforeEach
    void setUp() {
        WebMvcTestHelper.configureTenantMock(tenantRepo);
        when(usuarioService.listarTodos()).thenReturn(List.of());
        when(tareaService.listar(any(), any())).thenReturn(List.of());
        when(tareaService.contarPorEstado()).thenReturn(
                Map.of("total", 0L, "pendiente", 0L, "en_progreso", 0L, "completada", 0L));
    }

    // ── Seguridad ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /tareas sin autenticar retorna 401")
    void index_sinAuth_retorna401() throws Exception {
        mockMvc.perform(get("/tareas"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /tareas ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /tareas autenticado retorna 200 con vista tareas/index")
    void index_autenticado_retorna200() throws Exception {
        mockMvc.perform(get("/tareas"))
                .andExpect(status().isOk())
                .andExpect(view().name("tareas/index"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /tareas pasa conteos al modelo")
    void index_pasaConteosAlModelo() throws Exception {
        when(tareaService.contarPorEstado()).thenReturn(
                Map.of("total", 5L, "pendiente", 2L, "en_progreso", 1L, "completada", 2L));

        mockMvc.perform(get("/tareas"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("conteos"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /tareas con filtro estado lo pasa al modelo")
    void index_conFiltroEstado_pasa() throws Exception {
        mockMvc.perform(get("/tareas").param("estado", "PENDIENTE"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("estadoFiltro", EstadoTarea.PENDIENTE));
    }

    // ── GET /tareas/nueva ──────────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /tareas/nueva retorna 200 con vista tareas/formulario")
    void nueva_autenticado_retorna200() throws Exception {
        mockMvc.perform(get("/tareas/nueva"))
                .andExpect(status().isOk())
                .andExpect(view().name("tareas/formulario"))
                .andExpect(model().attribute("modoEdicion", false));
    }

    // ── GET /tareas/{id} ───────────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /tareas/{id} con id válido retorna 200 con vista tareas/detalle")
    void ver_idValido_retorna200() throws Exception {
        when(tareaService.buscarPorId(1L)).thenReturn(tareaEjemplo());

        mockMvc.perform(get("/tareas/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("tareas/detalle"))
                .andExpect(model().attributeExists("tarea"));
    }

    // ── GET /tareas/editar/{id} ────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /tareas/editar/{id} retorna 200 con modoEdicion=true")
    void editar_retorna200ConModoEdicion() throws Exception {
        when(tareaService.buscarPorId(1L)).thenReturn(tareaEjemplo());

        mockMvc.perform(get("/tareas/editar/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("tareas/formulario"))
                .andExpect(model().attribute("modoEdicion", true));
    }

    // ── POST /tareas/guardar ───────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("POST /tareas/guardar redirige a /tareas tras crear")
    void guardar_redirige() throws Exception {
        Tarea saved = tareaEjemplo();
        when(tareaService.guardar(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(saved);

        mockMvc.perform(post("/tareas/guardar").with(csrf())
                .param("titulo", "Limpiar fermentador"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tareas"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /tareas/guardar sin CSRF retorna 403")
    void guardar_sinCsrf_retorna403() throws Exception {
        mockMvc.perform(post("/tareas/guardar")
                .param("titulo", "Limpiar fermentador"))
                .andExpect(status().isForbidden());
    }

    // ── POST /tareas/actualizar/{id} ───────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("POST /tareas/actualizar/{id} redirige a /tareas/{id}")
    void actualizar_redirige() throws Exception {
        when(tareaService.actualizar(eq(1L), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(tareaEjemplo());

        mockMvc.perform(post("/tareas/actualizar/1").with(csrf())
                .param("titulo", "Limpiar fermentador actualizado"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tareas/1"));
    }

    // ── POST /tareas/{id}/eliminar ─────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("POST /tareas/{id}/eliminar redirige a /tareas")
    void eliminar_redirige() throws Exception {
        mockMvc.perform(post("/tareas/1/eliminar").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tareas"));
    }

    // ── POST /tareas/{tareaId}/items/{itemId}/toggle ───────────────────────────

    @Test
    @WithMockUser
    @DisplayName("POST toggle retorna JSON con completado/estado/pct")
    void toggleItem_retornaJson() throws Exception {
        when(tareaService.toggleItem(1L, 5L)).thenReturn(
                Map.of("completado", true, "estado", "COMPLETADA", "pct", 100));

        mockMvc.perform(post("/tareas/1/items/5/toggle").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.completado").value(true))
                .andExpect(jsonPath("$.estado").value("COMPLETADA"))
                .andExpect(jsonPath("$.pct").value(100));
    }

    @Test
    @WithMockUser
    @DisplayName("POST toggle con EntityNotFoundException retorna 404")
    void toggleItem_conEntityNotFound_retorna404() throws Exception {
        when(tareaService.toggleItem(1L, 99L))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("no encontrado"));

        mockMvc.perform(post("/tareas/1/items/99/toggle").with(csrf()))
                .andExpect(status().isNotFound());
    }
}
