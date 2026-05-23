package com.alera.controller;

import com.alera.config.*;
import com.alera.repository.LoteCervezaRepository;
import com.alera.repository.TenantRepository;
import com.alera.service.LogAccesoService;
import com.alera.service.PdfExportService;
import com.alera.service.UsuarioService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ComparativaController.class)
@DisplayName("ComparativaController")
class ComparativaControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean TenantRepository           tenantRepo;
    @MockBean BrandingProperties         brandingProperties;
    @MockBean AleraAuthSuccessHandler    successHandler;
    @MockBean AleraAuthFailureHandler    failureHandler;
    @MockBean AleraAccessDeniedHandler   accessDeniedHandler;
    @MockBean UsuarioService             usuarioService;
    @MockBean LogAccesoService           logAccesoService;
    @MockBean LoginAttemptService        loginAttemptService;
    @MockBean LoteCervezaRepository      loteRepo;
    @MockBean PdfExportService           pdfService;

    @BeforeEach
    void setUp() {
        WebMvcTestHelper.configureTenantMock(tenantRepo);
        when(loteRepo.search(anyString(), any())).thenReturn(List.of());
        when(loteRepo.findByIds(any())).thenReturn(List.of());
    }

    @Test
    @DisplayName("GET /comparativa sin autenticar retorna 401")
    void seleccion_sinAuth_retorna401() throws Exception {
        mockMvc.perform(get("/comparativa"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /comparativa con usuario autenticado retorna 200")
    void seleccion_autenticado_retorna200() throws Exception {
        mockMvc.perform(get("/comparativa"))
                .andExpect(status().isOk())
                .andExpect(view().name("comparativa/seleccion"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /comparativa/resultado con menos de 2 ids redirige")
    void resultado_menosDeDoIds_redirige() throws Exception {
        mockMvc.perform(get("/comparativa/resultado").param("ids", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/comparativa"));
    }
}
