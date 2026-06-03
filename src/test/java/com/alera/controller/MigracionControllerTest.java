package com.alera.controller;

import com.alera.config.*;
import com.alera.model.Tenant;
import com.alera.repository.TenantRepository;
import com.alera.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MigracionController.class)
@DisplayName("MigracionController")
class MigracionControllerTest {

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
    @MockBean MigracionTemplateService   templateService;
    @MockBean MigracionService           migracionService;

    @BeforeEach
    void setUp() {
        WebMvcTestHelper.configureTenantMock(tenantRepo);
        when(migracionService.historial(anyString())).thenReturn(List.of());
    }

    @Test
    @DisplayName("GET /admin/migracion/{sub} sin autenticar retorna 401")
    void detalle_sinAuth_retorna401() throws Exception {
        mockMvc.perform(get("/admin/migracion/mosto"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/migracion/{sub} con tenant existente retorna 200")
    void detalle_tenantExiste_retorna200() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setSubdomain("mosto");
        tenant.setName("Cervecería Mosto");
        when(tenantRepo.findById("mosto")).thenReturn(Optional.of(tenant));

        mockMvc.perform(get("/admin/migracion/mosto"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/migracion/detalle"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/migracion/{sub} con tenant inexistente redirige con mensaje de error")
    void detalle_noExiste_redirige() throws Exception {
        when(tenantRepo.findById("noexiste")).thenReturn(Optional.empty());

        mockMvc.perform(get("/admin/migracion/noexiste"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tenants"))
                .andExpect(flash().attribute("tipoMensaje", "danger"));
    }
}
