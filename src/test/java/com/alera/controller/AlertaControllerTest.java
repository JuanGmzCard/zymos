package com.alera.controller;

import com.alera.config.AleraAccessDeniedHandler;
import com.alera.config.AleraAuthFailureHandler;
import com.alera.config.AleraAuthSuccessHandler;
import com.alera.config.BrandingProperties;
import com.alera.model.Equipo;
import com.alera.repository.InsumoInventarioRepository;
import com.alera.repository.TenantRepository;
import com.alera.service.EquipoService;
import com.alera.service.LogAccesoService;
import com.alera.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AlertaController.class)
class AlertaControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean InsumoInventarioRepository insumoRepo;
    @MockBean EquipoService              equipoService;
    @MockBean LogAccesoService           logAccesoService;
    @MockBean UsuarioService             usuarioService;
    @MockBean TenantRepository           tenantRepo;
    @MockBean BrandingProperties         brandingProperties;
    @MockBean AleraAuthSuccessHandler    successHandler;
    @MockBean AleraAuthFailureHandler    failureHandler;
    @MockBean AleraAccessDeniedHandler   accessDeniedHandler;

    @BeforeEach
    void setUp() {
        WebMvcTestHelper.configureTenantMock(tenantRepo);
    }

    // ── Seguridad ─────────────────────────────────────────────────────────

    @Test
    void sinAutenticarDevuelveUnauthorized() throws Exception {
        // Con httpBasic() configurado en SecurityConfig, peticiones sin Accept:text/html
        // devuelven 401 (no redirect) cuando no hay credenciales.
        mockMvc.perform(get("/alertas/contadores"))
                .andExpect(status().isUnauthorized());
    }

    // ── Estructura JSON ───────────────────────────────────────────────────

    @Test
    @WithMockUser
    void respuestaContieneTodasLasClaves() throws Exception {
        when(insumoRepo.countBajoStock()).thenReturn(0L);
        when(insumoRepo.countProximosAVencer(any(LocalDate.class))).thenReturn(0L);
        when(equipoService.listarMantenimientoPendiente()).thenReturn(List.of());

        mockMvc.perform(get("/alertas/contadores"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.bajoStock").exists())
                .andExpect(jsonPath("$.vencimientos").exists())
                .andExpect(jsonPath("$.mantenimiento").exists())
                .andExpect(jsonPath("$.total").exists());
    }

    // ── Totales ───────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void totalEsSumaDeLosTresContadores() throws Exception {
        when(insumoRepo.countBajoStock()).thenReturn(3L);
        when(insumoRepo.countProximosAVencer(any(LocalDate.class))).thenReturn(2L);
        when(equipoService.listarMantenimientoPendiente()).thenReturn(List.of(new Equipo()));

        mockMvc.perform(get("/alertas/contadores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bajoStock").value(3))
                .andExpect(jsonPath("$.vencimientos").value(2))
                .andExpect(jsonPath("$.mantenimiento").value(1))
                .andExpect(jsonPath("$.total").value(6));
    }

    @Test
    @WithMockUser
    void sinAlertasTotalEsCero() throws Exception {
        when(insumoRepo.countBajoStock()).thenReturn(0L);
        when(insumoRepo.countProximosAVencer(any(LocalDate.class))).thenReturn(0L);
        when(equipoService.listarMantenimientoPendiente()).thenReturn(List.of());

        mockMvc.perform(get("/alertas/contadores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    @WithMockUser
    void soloMantenimientoRetornaTotalCorrecto() throws Exception {
        when(insumoRepo.countBajoStock()).thenReturn(0L);
        when(insumoRepo.countProximosAVencer(any(LocalDate.class))).thenReturn(0L);
        when(equipoService.listarMantenimientoPendiente())
                .thenReturn(List.of(new Equipo(), new Equipo()));

        mockMvc.perform(get("/alertas/contadores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bajoStock").value(0))
                .andExpect(jsonPath("$.mantenimiento").value(2))
                .andExpect(jsonPath("$.total").value(2));
    }
}
