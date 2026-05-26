package com.alera.controller;

import com.alera.config.*;
import com.alera.repository.FacturaItemRepository;
import com.alera.repository.TenantRepository;
import com.alera.service.ExcelExportService;
import com.alera.service.InsumoInventarioService;
import com.alera.service.JwtService;
import com.alera.service.LogAccesoService;
import com.alera.service.ProveedorService;
import com.alera.service.UsuarioService;
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

@WebMvcTest(InsumoInventarioController.class)
@DisplayName("InsumoInventarioController")
class InsumoInventarioControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean TenantRepository           tenantRepo;
    @MockBean BrandingProperties         brandingProperties;
    @MockBean AleraAuthSuccessHandler    successHandler;
    @MockBean AleraAuthFailureHandler    failureHandler;
    @MockBean AleraAccessDeniedHandler   accessDeniedHandler;
    @MockBean UsuarioService             usuarioService;
    @MockBean LogAccesoService           logAccesoService;
    @MockBean LoginAttemptService        loginAttemptService;
    @MockBean JwtService                 jwtService;
    @MockBean InsumoInventarioService    insumoService;
    @MockBean FacturaItemRepository      facturaItemRepo;
    @MockBean ExcelExportService         excelService;
    @MockBean ProveedorService           proveedorService;

    @BeforeEach
    void setUp() {
        WebMvcTestHelper.configureTenantMock(tenantRepo);
        when(insumoService.listarPaginado(any(), any(), anyInt())).thenReturn(new PageImpl<>(List.of()));
        when(insumoService.listarBajoStock()).thenReturn(List.of());
        when(insumoService.listarProximosAVencer(anyInt())).thenReturn(List.of());
        when(facturaItemRepo.findNombresDistintos()).thenReturn(List.of());
        when(proveedorService.listarActivos()).thenReturn(List.of());
    }

    @Test
    @DisplayName("GET /inventario sin autenticar retorna 401")
    void lista_sinAuth_retorna401() throws Exception {
        mockMvc.perform(get("/inventario"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /inventario con ADMIN retorna 200")
    void lista_conAdmin_retorna200() throws Exception {
        mockMvc.perform(get("/inventario"))
                .andExpect(status().isOk())
                .andExpect(view().name("inventario/lista"));
    }

    @Test
    @WithMockUser(roles = "INVENTARIO")
    @DisplayName("GET /inventario/suggest retorna JSON")
    void suggest_retornaJson() throws Exception {
        when(insumoService.listarPaginado(any(), any(), anyInt())).thenReturn(new PageImpl<>(List.of()));
        mockMvc.perform(get("/inventario/suggest").param("nombre", "ma"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
