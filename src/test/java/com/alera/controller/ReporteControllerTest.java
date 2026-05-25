package com.alera.controller;

import com.alera.config.*;
import com.alera.repository.LoteCervezaRepository;
import com.alera.repository.TenantRepository;
import com.alera.service.ExcelExportService;
import com.alera.service.JwtService;
import com.alera.service.LogAccesoService;
import com.alera.service.UsuarioService;
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
    @MockBean AleraAuthSuccessHandler    successHandler;
    @MockBean AleraAuthFailureHandler    failureHandler;
    @MockBean AleraAccessDeniedHandler   accessDeniedHandler;
    @MockBean UsuarioService             usuarioService;
    @MockBean LogAccesoService           logAccesoService;
    @MockBean LoginAttemptService        loginAttemptService;
    @MockBean JwtService                 jwtService;
    @MockBean LoteCervezaRepository      loteRepo;
    @MockBean ExcelExportService         excelService;

    @BeforeEach
    void setUp() {
        WebMvcTestHelper.configureTenantMock(tenantRepo);
        when(loteRepo.findByPeriodo(any(), any())).thenReturn(List.of());
        when(loteRepo.findResumenPorEstilo(any(), any())).thenReturn(List.of());
        when(excelService.generarExcelReporteProduccion(any(), any(), any(), any(), anyString()))
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
}
