package com.alera.controller;

import com.alera.config.*;
import com.alera.repository.EquipoRepository;
import com.alera.repository.InsumoInventarioRepository;
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

@WebMvcTest(FacturaProveedorController.class)
@DisplayName("FacturaProveedorController")
class FacturaProveedorControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean TenantRepository              tenantRepo;
    @MockBean BrandingProperties            brandingProperties;
    @MockBean AleraAuthSuccessHandler       successHandler;
    @MockBean AleraAuthFailureHandler       failureHandler;
    @MockBean AleraAccessDeniedHandler      accessDeniedHandler;
    @MockBean UsuarioService                usuarioService;
    @MockBean LogAccesoService              logAccesoService;
    @MockBean LoginAttemptService           loginAttemptService;
    @MockBean JwtService                    jwtService;
    @MockBean FacturaProveedorService       facturaService;
    @MockBean ProveedorService              proveedorService;
    @MockBean InsumoInventarioRepository    insumoRepo;
    @MockBean EquipoRepository              equipoRepo;
    @MockBean InsumoInventarioService       insumoService;
    @MockBean EquipoService                 equipoService;
    @MockBean ExcelExportService            excelService;

    @BeforeEach
    void setUp() {
        WebMvcTestHelper.configureTenantMock(tenantRepo);
        when(facturaService.listarPaginado(any(), anyInt())).thenReturn(new PageImpl<>(List.of()));
        when(facturaService.suggest(anyString())).thenReturn(List.of());
        when(proveedorService.listarActivos()).thenReturn(List.of());
        when(insumoRepo.findAll()).thenReturn(List.of());
        when(equipoRepo.findAll()).thenReturn(List.of());
    }

    @Test
    @DisplayName("GET /facturas sin autenticar retorna 401")
    void lista_sinAuth_retorna401() throws Exception {
        mockMvc.perform(get("/facturas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /facturas con ADMIN retorna 200")
    void lista_conAdmin_retorna200() throws Exception {
        mockMvc.perform(get("/facturas"))
                .andExpect(status().isOk())
                .andExpect(view().name("facturas/lista"));
    }

    @Test
    @WithMockUser(roles = "FACTURACION")
    @DisplayName("GET /facturas/suggest retorna JSON")
    void suggest_retornaJson() throws Exception {
        mockMvc.perform(get("/facturas/suggest").param("q", "001"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
