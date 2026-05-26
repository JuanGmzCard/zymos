package com.alera.controller;

import com.alera.config.ZymosAccessDeniedHandler;
import com.alera.config.ZymosAuthFailureHandler;
import com.alera.config.ZymosAuthSuccessHandler;
import com.alera.config.BrandingProperties;
import com.alera.config.LoginAttemptService;
import com.alera.exception.LoteNoEncontradoException;
import com.alera.dto.DashboardStats;
import com.alera.model.LoteCerveza;
import com.alera.repository.LoteCervezaRepository;
import com.alera.repository.TenantRepository;
import com.alera.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApiController.class)
class ApiControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean TrazabilidadService    trazabilidadService;
    @MockBean LoteCervezaRepository  loteRepo;
    @MockBean RecetaService          recetaService;
    @MockBean InsumoInventarioService insumoService;
    @MockBean DashboardService       dashboardService;
    @MockBean LogAccesoService       logAccesoService;
    @MockBean UsuarioService         usuarioService;
    @MockBean TenantRepository       tenantRepo;
    @MockBean BrandingProperties     brandingProperties;
    @MockBean ZymosAuthSuccessHandler  successHandler;
    @MockBean ZymosAuthFailureHandler  failureHandler;
    @MockBean ZymosAccessDeniedHandler accessDeniedHandler;
    @MockBean LoginAttemptService      loginAttemptService;
    @MockBean JwtService               jwtService;

    @BeforeEach
    void setUp() {
        WebMvcTestHelper.configureTenantMock(tenantRepo);
    }

    // ── Seguridad ──────────────────────────────────────────────────────

    @Test
    void sinAutenticarDevuelveUnauthorized() throws Exception {
        // Con httpBasic() configurado en SecurityConfig, peticiones sin Accept:text/html
        // devuelven 401 cuando no hay credenciales (no redirect).
        mockMvc.perform(get("/api/v1/lotes"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/v1/lotes ─────────────────────────────────────────────

    @Test
    @WithMockUser
    void lotesRetornaJsonPaginado() throws Exception {
        when(trazabilidadService.listarPaginado(anyString(), anyString(), anyInt()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/lotes"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.lotes").isArray());
    }

    @Test
    @WithMockUser
    void lotesConFiltroEstiloRetornaJson() throws Exception {
        LoteCerveza lote = new LoteCerveza();
        lote.setEstilo("IPA");
        when(trazabilidadService.listarPaginado(eq("IPA"), anyString(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(lote)));

        mockMvc.perform(get("/api/v1/lotes").param("estilo", "IPA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));
    }

    // ── GET /api/v1/lotes/{id} ────────────────────────────────────────

    @Test
    @WithMockUser
    void lotePorIdRetornaJson() throws Exception {
        LoteCerveza lote = new LoteCerveza();
        lote.setEstilo("Stout");
        lote.setCodigoLote("STO-001");
        when(trazabilidadService.buscarPorId(1L)).thenReturn(lote);

        mockMvc.perform(get("/api/v1/lotes/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.estilo").value("Stout"))
                .andExpect(jsonPath("$.codigoLote").value("STO-001"));
    }

    @Test
    @WithMockUser
    void loteNoEncontradoRetorna404() throws Exception {
        when(trazabilidadService.buscarPorId(999L))
                .thenThrow(new LoteNoEncontradoException(999L));

        mockMvc.perform(get("/api/v1/lotes/999"))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/v1/lotes/{id}/historial ──────────────────────────────

    @Test
    @WithMockUser
    void historialLoteRetornaListaJson() throws Exception {
        when(trazabilidadService.obtenerHistorial(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/lotes/1/historial"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$").isArray());
    }

    // ── GET /api/v1/recetas ───────────────────────────────────────────

    @Test
    @WithMockUser
    void recetasRetornaListaJson() throws Exception {
        when(recetaService.listarActivas()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/recetas"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$").isArray());
    }

    // ── GET /api/v1/inventario/alertas ────────────────────────────────

    @Test
    @WithMockUser
    void alertasInventarioRetornaJson() throws Exception {
        when(insumoService.listarBajoStock()).thenReturn(List.of());
        when(insumoService.listarProximosAVencer(30)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/inventario/alertas"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.bajoStock").isArray())
                .andExpect(jsonPath("$.proximosAVencer").isArray());
    }

    // ── GET /api/v1/dashboard ─────────────────────────────────────────

    @Test
    @WithMockUser
    void dashboardRetornaEstadisticas() throws Exception {
        DashboardStats stats = new DashboardStats()
                .totalLotes(10)
                .enProceso(3)
                .completados(7)
                .estilosDistintos(4)
                .totalInsumos(20)
                .bajoStock(2)
                .proximosAVencer(1)
                .totalEquipos(5)
                .mantenimientoPendiente(0)
                .totalFacturas(15);
        when(dashboardService.obtenerEstadisticas()).thenReturn(stats);

        mockMvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.totalLotes").value(10))
                .andExpect(jsonPath("$.lotesEnProceso").value(3))
                .andExpect(jsonPath("$.lotesCompletados").value(7));
    }
}