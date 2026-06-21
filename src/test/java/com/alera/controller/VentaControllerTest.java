package com.alera.controller;

import com.alera.config.*;
import com.alera.model.Venta;
import com.alera.model.enums.EstadoVenta;
import com.alera.repository.TenantRepository;
import com.alera.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VentaController.class)
@DisplayName("VentaController")
class VentaControllerTest {

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
    @MockBean VentaService              ventaService;
    @MockBean TrazabilidadService       trazabilidadService;
    @MockBean ExcelExportService        excelExportService;
    @MockBean PdfExportService          pdfExportService;
    @MockBean ClienteService            clienteService;

    @BeforeEach
    void setUp() {
        WebMvcTestHelper.configureTenantMock(tenantRepo);
        when(ventaService.listarPaginado(any(), any(), any(), anyInt()))
                .thenReturn(new PageImpl<>(List.of()));
        when(ventaService.countTotal()).thenReturn(0L);
        when(ventaService.countByEstado(any())).thenReturn(0L);
        when(ventaService.countClientesUnicos()).thenReturn(0L);
        when(ventaService.sumIngresosDespachados()).thenReturn(BigDecimal.ZERO);
        when(ventaService.suggest(anyString())).thenReturn(List.of());
        when(ventaService.topClientes()).thenReturn(List.of());
        when(ventaService.listarHistorial(anyLong())).thenReturn(List.of());
    }

    @Test
    @DisplayName("GET /ventas sin autenticar retorna 401")
    void lista_sinAuth_retorna401() throws Exception {
        mockMvc.perform(get("/ventas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /ventas con ADMIN retorna 200 y vista correcta")
    void lista_conAdmin_retorna200() throws Exception {
        mockMvc.perform(get("/ventas"))
                .andExpect(status().isOk())
                .andExpect(view().name("ventas/lista"));
    }

    @Test
    @WithMockUser(roles = "FACTURACION")
    @DisplayName("GET /ventas con FACTURACION retorna 200")
    void lista_conFacturacion_retorna200() throws Exception {
        mockMvc.perform(get("/ventas"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /ventas/suggest retorna JSON con lista")
    void suggest_retornaJson() throws Exception {
        when(ventaService.suggest("norte")).thenReturn(List.of(
                Map.of("titulo", "Distribuidora Norte", "sub", "IPA-001", "url", "/ventas/ver/1")
        ));
        mockMvc.perform(get("/ventas/suggest").param("q", "norte"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titulo").value("Distribuidora Norte"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /ventas/ver/{id} retorna detalle con historial")
    void ver_retornaDetalle() throws Exception {
        Venta v = new Venta();
        v.setCliente("Cliente Test");
        v.setEstado(EstadoVenta.PENDIENTE);
        when(ventaService.buscarPorId(1L)).thenReturn(Optional.of(v));

        mockMvc.perform(get("/ventas/ver/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("ventas/detalle"))
                .andExpect(model().attributeExists("historial"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /ventas/nuevo retorna formulario vacío")
    void nuevo_retornaFormulario() throws Exception {
        mockMvc.perform(get("/ventas/nuevo"))
                .andExpect(status().isOk())
                .andExpect(view().name("ventas/formulario"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /ventas/{id}/pdf retorna PDF con Content-Disposition")
    void pdf_retornaPdf() throws Exception {
        Venta v = new Venta();
        v.setCliente("Cliente Test");
        v.setEstado(EstadoVenta.DESPACHADO);
        when(ventaService.buscarPorId(1L)).thenReturn(Optional.of(v));
        when(pdfExportService.generarPdfVenta(any(), any(), any())).thenReturn(new byte[]{0x25, 0x50, 0x44, 0x46});

        mockMvc.perform(get("/ventas/1/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("remision-venta-1.pdf")));
    }
}
