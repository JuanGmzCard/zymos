package com.alera.controller;

import com.alera.config.ZymosAccessDeniedHandler;
import com.alera.config.ZymosAuthFailureHandler;
import com.alera.config.ZymosAuthSuccessHandler;
import com.alera.config.BrandingProperties;
import com.alera.config.LoginAttemptService;
import com.alera.model.Barril;
import com.alera.model.enums.EstadoBarril;
import com.alera.repository.TenantRepository;
import com.alera.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BarrilController.class)
class BarrilControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean BarrilService            barrilService;
    @MockBean TenantRepository         tenantRepo;
    @MockBean BrandingProperties       brandingProperties;
    @MockBean ZymosAuthSuccessHandler  successHandler;
    @MockBean ZymosAuthFailureHandler  failureHandler;
    @MockBean ZymosAccessDeniedHandler accessDeniedHandler;
    @MockBean LoginAttemptService      loginAttemptService;
    @MockBean JwtService               jwtService;
    @MockBean UsuarioService           usuarioService;
    @MockBean LogAccesoService         logAccesoService;

    @BeforeEach
    void setUp() {
        WebMvcTestHelper.configureTenantMock(tenantRepo);
        Page<Barril> vacia = new PageImpl<>(List.of());
        when(barrilService.listarPaginado(any(), any(), anyInt())).thenReturn(vacia);
        when(barrilService.countTotal()).thenReturn(0L);
        when(barrilService.countByEstado(any())).thenReturn(0L);
    }

    // ── Seguridad ──────────────────────────────────────────────────────────

    @Test
    void lista_sinAuth_retorna401() throws Exception {
        mockMvc.perform(get("/barriles"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /barriles ──────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void lista_conAdmin_retorna200() throws Exception {
        mockMvc.perform(get("/barriles"))
                .andExpect(status().isOk())
                .andExpect(view().name("barriles/lista"))
                .andExpect(model().attributeExists("barriles", "estados", "statsTotal"));
    }

    @Test
    @WithMockUser(roles = "INVENTARIO")
    void lista_conInventario_retorna200() throws Exception {
        mockMvc.perform(get("/barriles"))
                .andExpect(status().isOk());
    }

    // ── GET /barriles/nuevo ────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void nuevo_retornaFormulario() throws Exception {
        mockMvc.perform(get("/barriles/nuevo"))
                .andExpect(status().isOk())
                .andExpect(view().name("barriles/formulario"))
                .andExpect(model().attributeExists("barril", "tiposBarril", "estados"));
    }

    // ── GET /barriles/ver/{id} ─────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void ver_retornaDetalle() throws Exception {
        Barril barril = new Barril();
        barril.setId(1L);
        barril.setCodigo("KEG-001");
        barril.setEstado(EstadoBarril.DISPONIBLE);
        when(barrilService.buscarPorId(1L)).thenReturn(barril);
        when(barrilService.listarMovimientos(1L)).thenReturn(List.of());

        mockMvc.perform(get("/barriles/ver/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("barriles/detalle"))
                .andExpect(model().attributeExists("barril", "movimientos", "estados"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void ver_noExiste_redirige() throws Exception {
        when(barrilService.buscarPorId(99L))
                .thenThrow(new RuntimeException("Barril no encontrado: 99"));

        mockMvc.perform(get("/barriles/ver/99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/barriles"))
                .andExpect(flash().attribute("tipoMensaje", "danger"));
    }

    // ── POST /barriles/guardar ─────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void guardar_codigoVacio_retornaFormulario() throws Exception {
        mockMvc.perform(post("/barriles/guardar").with(csrf())
                        .param("codigo", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("barriles/formulario"));
    }

    // ── POST /barriles/{id}/estado ─────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void cambiarEstado_redirigaAlDetalle() throws Exception {
        mockMvc.perform(post("/barriles/1/estado").with(csrf())
                        .param("estado", "VACIO"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/barriles/ver/1"));
    }
}
