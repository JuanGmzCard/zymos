package com.alera.controller;

import com.alera.config.*;
import com.alera.repository.LoteCervezaRepository;
import com.alera.repository.LoteItemFacturaRepository;
import com.alera.repository.TenantRepository;
import com.alera.repository.VentaItemRepository;
import com.alera.service.ExcelExportService;
import com.alera.service.JwtService;
import com.alera.service.PdfExportService;
import com.alera.service.LogAccesoService;
import com.alera.service.UsuarioService;
import com.alera.service.VentaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReporteController.class)
@DisplayName("ReporteController")
class ReporteControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean TenantRepository           tenantRepo;
    @MockBean BrandingProperties         brandingProperties;
    @MockBean ZymosAuthSuccessHandler    successHandler;
    @MockBean ZymosAuthFailureHandler    failureHandler;
    @MockBean ZymosAccessDeniedHandler   accessDeniedHandler;
    @MockBean UsuarioService             usuarioService;
    @MockBean LogAccesoService           logAccesoService;
    @MockBean LoginAttemptService        loginAttemptService;
    @MockBean JwtService                 jwtService;
    @MockBean LoteCervezaRepository      loteRepo;
    @MockBean LoteItemFacturaRepository  loteItemFacturaRepo;
    @MockBean VentaItemRepository        ventaItemRepo;
    @MockBean ExcelExportService         excelService;
    @MockBean PdfExportService           pdfService;
    @MockBean VentaService               ventaService;

    @BeforeEach
    void setUp() {
        WebMvcTestHelper.configureTenantMock(tenantRepo);
        when(loteRepo.findByPeriodo(any(), any())).thenReturn(List.of());
        when(loteRepo.findAllCompletados(any())).thenReturn(List.of());
        when(loteRepo.findResumenPorEstilo(any(), any(), any())).thenReturn(List.of());
        when(loteItemFacturaRepo.sumCostosPorLote()).thenReturn(List.of());
        when(ventaItemRepo.sumIngresosDespachadosPorLote()).thenReturn(List.of());
        when(excelService.generarExcelReporteProduccion(any(), any(), any(), any(), any(), any()))
                .thenReturn(new byte[]{0x50, 0x4B});
        when(pdfService.generarPdfReporteProduccion(any(), any(), any(), any(), any(), any()))
                .thenReturn(new byte[]{0x25, 0x50, 0x44, 0x46});
        when(ventaService.listarParaExport(any(), any(), any())).thenReturn(List.of());
        when(excelService.generarExcelVentas(any(), any(), any(), any(), any(), any()))
                .thenReturn(new byte[]{0x50, 0x4B});
    }

    @Test
    @DisplayName("GET /reportes/produccion sin autenticar retorna 401")
    void reporte_sinAuth_retorna401() throws Exception {
        mockMvc.perform(get("/reportes/produccion"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /reportes/produccion con ADMIN retorna 200")
    void reporte_conAdmin_retorna200() throws Exception {
        mockMvc.perform(get("/reportes/produccion")
                .param("desde", LocalDate.now().minusMonths(1).toString())
                .param("hasta",  LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("reportes/produccion"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /reportes/produccion/excel descarga archivo")
    void excel_retornaDescarga() throws Exception {
        mockMvc.perform(get("/reportes/produccion/excel")
                .param("desde", LocalDate.now().minusMonths(1).toString())
                .param("hasta",  LocalDate.now().toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /reportes/produccion/pdf descarga archivo")
    void pdf_retornaDescarga() throws Exception {
        mockMvc.perform(get("/reportes/produccion/pdf")
                .param("desde", LocalDate.now().minusMonths(1).toString())
                .param("hasta",  LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("reporte-produccion")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /reportes/ventas con ADMIN retorna 200")
    void ventasReporte_conAdmin_retorna200() throws Exception {
        mockMvc.perform(get("/reportes/ventas")
                .param("desde", LocalDate.now().minusMonths(1).toString())
                .param("hasta",  LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("reportes/ventas"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /reportes/ventas/excel descarga archivo")
    void ventasExcel_retornaDescarga() throws Exception {
        mockMvc.perform(get("/reportes/ventas/excel")
                .param("desde", LocalDate.now().minusMonths(1).toString())
                .param("hasta",  LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("reporte-ventas")));
    }
}
