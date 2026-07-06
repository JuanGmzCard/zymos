package com.alera.controller;

import com.alera.config.*;
import com.alera.repository.TenantRepository;
import com.alera.service.JwtService;
import com.alera.service.LogAccesoService;
import com.alera.service.StockService;
import com.alera.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StockController.class)
@DisplayName("StockController")
class StockControllerTest {

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
    @MockBean StockService              stockService;

    @BeforeEach
    void setUp() {
        WebMvcTestHelper.configureTenantMock(tenantRepo);
        when(stockService.listarStock()).thenReturn(List.of());
        when(stockService.countLotesConStock()).thenReturn(0L);
        when(stockService.countLotesAgotados()).thenReturn(0L);
        when(stockService.getTotalDisponibleLitros()).thenReturn(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("GET /stock sin autenticar retorna 401")
    void index_sinAuth_retorna401() throws Exception {
        mockMvc.perform(get("/stock"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /stock con usuario autenticado retorna 200")
    void index_autenticado_retorna200() throws Exception {
        mockMvc.perform(get("/stock"))
                .andExpect(status().isOk())
                .andExpect(view().name("stock/index"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /stock/ajustes/{loteId} retorna JSON con los ajustes del lote")
    void listarAjustes_retornaJson() throws Exception {
        when(stockService.listarAjustesPorLote(1L)).thenReturn(List.of());

        mockMvc.perform(get("/stock/ajustes/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /stock/ajustar/{loteId} con datos válidos redirige a /stock con éxito")
    void ajustar_datosValidos_redirige() throws Exception {
        mockMvc.perform(post("/stock/ajustar/1").with(csrf())
                        .param("cantidad", "10")
                        .param("unidad", "L")
                        .param("motivo", "Degustación")
                        .param("fecha", "2025-03-01"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/stock"))
                .andExpect(flash().attribute("tipoMensaje", "success"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /stock/ajuste/eliminar/{ajusteId} redirige a /stock con éxito")
    void eliminarAjuste_redirige() throws Exception {
        mockMvc.perform(post("/stock/ajuste/eliminar/99").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/stock"))
                .andExpect(flash().attribute("tipoMensaje", "success"));
    }
}
