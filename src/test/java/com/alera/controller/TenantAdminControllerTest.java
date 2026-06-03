package com.alera.controller;

import com.alera.config.*;
import com.alera.repository.TenantRepository;
import com.alera.repository.UsuarioRepository;
import com.alera.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TenantAdminController.class)
@DisplayName("TenantAdminController")
class TenantAdminControllerTest {

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
    @MockBean TenantService              tenantService;
    @MockBean UsuarioRepository          usuarioRepository;
    @MockBean EmailService               emailService;
    @MockBean PasswordEncoder            passwordEncoder;
    @MockBean TenantMetricsService       metricsService;

    @BeforeEach
    void setUp() {
        WebMvcTestHelper.configureTenantMock(tenantRepo);
        when(tenantService.listarTodos()).thenReturn(List.of());
        when(tenantService.listarHistorialPaginado(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(org.springframework.data.domain.Page.empty());
    }

    @Test
    @DisplayName("GET /admin/tenants sin autenticar retorna 401")
    void lista_sinAuth_retorna401() throws Exception {
        mockMvc.perform(get("/admin/tenants"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/tenants con ADMIN retorna 200")
    void lista_conAdmin_retorna200() throws Exception {
        mockMvc.perform(get("/admin/tenants"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/tenants"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/tenants/nuevo retorna formulario")
    void nuevo_conAdmin_retornaFormulario() throws Exception {
        mockMvc.perform(get("/admin/tenants/nuevo"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/tenant-formulario"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/tenants/{sub}/config retorna JSON")
    void config_retornaJson() throws Exception {
        com.alera.model.Tenant tenant = new com.alera.model.Tenant();
        tenant.setSubdomain("mosto"); tenant.setName("Mosto");
        tenant.setColorNavbar("#242E0D"); tenant.setColorPrimary("#364318");
        tenant.setColorAccent("#C9A028"); tenant.setColorAccentHover("#E0B840");
        tenant.setColorCream("#F5EDD0"); tenant.setColorBodyBg("#F0EDE2");
        when(tenantService.buscarPorSubdomain("mosto"))
                .thenReturn(Optional.of(tenant));

        mockMvc.perform(get("/admin/tenants/mosto/config"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/tenants/editar/{sub} con subdomain inexistente redirige con mensaje de error")
    void formularioEditar_noExiste_redirige() throws Exception {
        when(tenantService.buscarPorSubdomain("noexiste")).thenReturn(Optional.empty());

        mockMvc.perform(get("/admin/tenants/editar/noexiste"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tenants"))
                .andExpect(flash().attribute("tipoMensaje", "danger"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/tenants/{sub}/historial con subdomain inexistente redirige con mensaje de error")
    void historial_noExiste_redirige() throws Exception {
        when(tenantService.buscarPorSubdomain("noexiste")).thenReturn(Optional.empty());

        mockMvc.perform(get("/admin/tenants/noexiste/historial"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tenants"))
                .andExpect(flash().attribute("tipoMensaje", "danger"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/tenants/{sub}/usuarios con subdomain inexistente redirige con mensaje de error")
    void usuarios_noExiste_redirige() throws Exception {
        when(tenantService.buscarPorSubdomain("noexiste")).thenReturn(Optional.empty());

        mockMvc.perform(get("/admin/tenants/noexiste/usuarios"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tenants"))
                .andExpect(flash().attribute("tipoMensaje", "danger"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/tenants/{sub}/config con subdomain inexistente retorna 404")
    void config_noExiste_retorna404() throws Exception {
        when(tenantService.buscarPorSubdomain("noexiste")).thenReturn(Optional.empty());

        mockMvc.perform(get("/admin/tenants/noexiste/config"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/tenants/{sub}/metricas retorna panel de métricas")
    void metricas_conAdmin_retorna200() throws Exception {
        com.alera.model.Tenant tenant = new com.alera.model.Tenant();
        tenant.setSubdomain("mosto"); tenant.setName("Mosto Cervecería");
        tenant.setActive(true);
        tenant.setColorNavbar("#242E0D"); tenant.setColorAccent("#C9A028");
        when(tenantService.buscarPorSubdomain("mosto")).thenReturn(Optional.of(tenant));
        when(metricsService.obtener("mosto")).thenReturn(
                new com.alera.service.TenantMetricsService.TenantMetrics(
                        5L, 2L, 3L, java.math.BigDecimal.valueOf(300),
                        10L, java.math.BigDecimal.valueOf(5000000), 8L,
                        4L, java.math.BigDecimal.valueOf(1200000),
                        20L, 2L, 5L,
                        3L, null));

        mockMvc.perform(get("/admin/tenants/mosto/metricas"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/tenant-metricas"))
                .andExpect(model().attributeExists("tenant", "metricas"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/tenants/{sub}/metricas con subdomain inexistente redirige con mensaje de error")
    void metricas_noExiste_redirige() throws Exception {
        when(tenantService.buscarPorSubdomain("noexiste")).thenReturn(Optional.empty());

        mockMvc.perform(get("/admin/tenants/noexiste/metricas"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tenants"))
                .andExpect(flash().attribute("tipoMensaje", "danger"));
    }
}
