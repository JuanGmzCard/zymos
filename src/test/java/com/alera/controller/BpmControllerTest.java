package com.alera.controller;

import com.alera.config.*;
import com.alera.model.*;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BpmController.class)
@DisplayName("BpmController")
class BpmControllerTest {

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
    @MockBean BpmService                bpmService;
    @MockBean BpmPdfService             bpmPdfService;

    @BeforeEach
    void setUp() {
        WebMvcTestHelper.configureTenantMock(tenantRepo);
        when(usuarioService.listarTodos()).thenReturn(List.of());
        when(bpmService.contarSintomasMes(any(), any())).thenReturn(0L);
        when(bpmService.contarSolucionesMes(any(), any())).thenReturn(0L);
        when(bpmService.contarPlagasMes(any(), any())).thenReturn(0L);
        when(bpmService.contarResiduosMes(any(), any())).thenReturn(0L);
        when(bpmService.contarLimpiezaMes(any(), any())).thenReturn(0L);
        when(bpmService.listarSintomas()).thenReturn(List.of());
        when(bpmService.listarSoluciones()).thenReturn(List.of());
        when(bpmService.listarPlagas()).thenReturn(List.of());
        when(bpmService.listarResiduos()).thenReturn(List.of());
        when(bpmService.listarLimpieza()).thenReturn(List.of());
    }

    // ── Seguridad general ────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /bpm sin autenticar retorna 401")
    void index_sinAuth_retorna401() throws Exception {
        mockMvc.perform(get("/bpm"))
                .andExpect(status().isUnauthorized());
    }

    // ── Dashboard BPM ────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /bpm retorna 200 con vista bpm/index")
    void index_autenticado_retorna200() throws Exception {
        mockMvc.perform(get("/bpm"))
                .andExpect(status().isOk())
                .andExpect(view().name("bpm/index"));
    }

    // ── Síntomas ─────────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /bpm/sintomas retorna 200 con vista bpm/sintomas/lista")
    void listaSintomas_retorna200() throws Exception {
        mockMvc.perform(get("/bpm/sintomas"))
                .andExpect(status().isOk())
                .andExpect(view().name("bpm/sintomas/lista"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /bpm/sintomas/nuevo retorna 200 con vista de formulario")
    void nuevoSintoma_retorna200() throws Exception {
        mockMvc.perform(get("/bpm/sintomas/nuevo"))
                .andExpect(status().isOk())
                .andExpect(view().name("bpm/sintomas/formulario"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /bpm/sintomas/editar/{id} retorna 200 cuando el registro existe")
    void editarSintoma_existe_retorna200() throws Exception {
        when(bpmService.buscarSintoma(1L)).thenReturn(new RegistroSintomas());

        mockMvc.perform(get("/bpm/sintomas/editar/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("bpm/sintomas/formulario"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /bpm/sintomas/guardar con datos válidos redirige a /bpm/sintomas con éxito")
    void guardarSintoma_datosValidos_redirige() throws Exception {
        mockMvc.perform(post("/bpm/sintomas/guardar").with(csrf())
                        .param("fecha", "2025-01-15")
                        .param("nombreManipulador", "Juan Pérez"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bpm/sintomas"))
                .andExpect(flash().attribute("tipoMensaje", "success"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /bpm/sintomas/eliminar/{id} redirige a /bpm/sintomas con éxito")
    void eliminarSintoma_redirige() throws Exception {
        mockMvc.perform(post("/bpm/sintomas/eliminar/1").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bpm/sintomas"))
                .andExpect(flash().attribute("tipoMensaje", "success"));
    }

    // ── Soluciones Desinfectantes ─────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /bpm/soluciones retorna 200 con vista bpm/soluciones/lista")
    void listaSoluciones_retorna200() throws Exception {
        mockMvc.perform(get("/bpm/soluciones"))
                .andExpect(status().isOk())
                .andExpect(view().name("bpm/soluciones/lista"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /bpm/soluciones/nuevo retorna 200 con vista de formulario")
    void nuevaSolucion_retorna200() throws Exception {
        mockMvc.perform(get("/bpm/soluciones/nuevo"))
                .andExpect(status().isOk())
                .andExpect(view().name("bpm/soluciones/formulario"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /bpm/soluciones/guardar con datos válidos redirige a /bpm/soluciones con éxito")
    void guardarSolucion_datosValidos_redirige() throws Exception {
        mockMvc.perform(post("/bpm/soluciones/guardar").with(csrf())
                        .param("fecha", "2025-01-15")
                        .param("producto", "Hipoclorito de sodio"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bpm/soluciones"))
                .andExpect(flash().attribute("tipoMensaje", "success"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /bpm/soluciones/eliminar/{id} redirige a /bpm/soluciones con éxito")
    void eliminarSolucion_redirige() throws Exception {
        mockMvc.perform(post("/bpm/soluciones/eliminar/2").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bpm/soluciones"))
                .andExpect(flash().attribute("tipoMensaje", "success"));
    }

    // ── Plagas ────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /bpm/plagas retorna 200 con vista bpm/plagas/lista")
    void listaPlagas_retorna200() throws Exception {
        mockMvc.perform(get("/bpm/plagas"))
                .andExpect(status().isOk())
                .andExpect(view().name("bpm/plagas/lista"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /bpm/plagas/eliminar/{id} redirige a /bpm/plagas con éxito")
    void eliminarPlaga_redirige() throws Exception {
        mockMvc.perform(post("/bpm/plagas/eliminar/3").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bpm/plagas"))
                .andExpect(flash().attribute("tipoMensaje", "success"));
    }

    // ── Residuos ──────────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /bpm/residuos retorna 200 con vista bpm/residuos/lista")
    void listaResiduos_retorna200() throws Exception {
        mockMvc.perform(get("/bpm/residuos"))
                .andExpect(status().isOk())
                .andExpect(view().name("bpm/residuos/lista"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /bpm/residuos/eliminar/{id} redirige a /bpm/residuos con éxito")
    void eliminarResiduo_redirige() throws Exception {
        mockMvc.perform(post("/bpm/residuos/eliminar/4").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bpm/residuos"))
                .andExpect(flash().attribute("tipoMensaje", "success"));
    }

    // ── Limpieza y Desinfección ────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /bpm/limpieza retorna 200 con vista bpm/limpieza/lista")
    void listaLimpieza_retorna200() throws Exception {
        mockMvc.perform(get("/bpm/limpieza"))
                .andExpect(status().isOk())
                .andExpect(view().name("bpm/limpieza/lista"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /bpm/limpieza/eliminar/{id} redirige a /bpm/limpieza con éxito")
    void eliminarLimpieza_redirige() throws Exception {
        mockMvc.perform(post("/bpm/limpieza/eliminar/5").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bpm/limpieza"))
                .andExpect(flash().attribute("tipoMensaje", "success"));
    }
}
