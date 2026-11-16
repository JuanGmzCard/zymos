package com.alera.controller;

import com.alera.config.*;
import com.alera.repository.TenantRepository;
import com.alera.repository.UsuarioRepository;
import com.alera.service.*;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WebhookAdminController.class)
@DisplayName("WebhookAdminController")
class WebhookAdminControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean WebhookService             webhookService;
    @MockBean LogAccesoService           logAccesoService;
    @MockBean UsuarioService             usuarioService;
    @MockBean TenantRepository           tenantRepo;
    @MockBean BrandingProperties         brandingProperties;
    @MockBean ZymosAuthSuccessHandler    successHandler;
    @MockBean ZymosAuthFailureHandler    failureHandler;
    @MockBean ZymosAccessDeniedHandler   accessDeniedHandler;
    @MockBean LoginAttemptService        loginAttemptService;
    @MockBean JwtService                 jwtService;
    @MockBean PasswordEncoder            passwordEncoder;
    @MockBean UsuarioRepository          usuarioRepository;

    @BeforeEach
    void setUp() {
        WebMvcTestHelper.configureTenantMock(tenantRepo);
        when(webhookService.listarTodos()).thenReturn(List.of());
    }

    @Test
    @DisplayName("sin autenticar → 401")
    void sinAutenticarDevuelveUnauthorized() throws Exception {
        mockMvc.perform(get("/admin/webhooks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/webhooks → 200 con lista vacía")
    void listaConAdminRetorna200() throws Exception {
        mockMvc.perform(get("/admin/webhooks"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/webhooks/lista"))
                .andExpect(model().attributeExists("webhooks", "eventosDisponibles"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/webhooks/nuevo → 200 con formulario vacío")
    void formularioNuevoRetorna200() throws Exception {
        mockMvc.perform(get("/admin/webhooks/nuevo"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/webhooks/formulario"))
                .andExpect(model().attributeExists("webhookFormDto", "eventosDisponibles", "secretGenerado"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/webhooks/guardar válido → redirect a lista")
    void guardarValidoRedirectALista() throws Exception {
        mockMvc.perform(post("/admin/webhooks/guardar").with(csrf())
                        .param("nombre", "Mi ERP")
                        .param("url", "https://erp.example.com/hooks")
                        .param("eventosList", "lote.fase_cambiada")
                        .param("activo", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/webhooks"));

        verify(webhookService).guardar(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/webhooks/guardar sin eventos → re-renderiza formulario")
    void guardarSinEventosReRenderizaFormulario() throws Exception {
        mockMvc.perform(post("/admin/webhooks/guardar").with(csrf())
                        .param("nombre", "Mi ERP")
                        .param("url", "https://erp.example.com/hooks"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/webhooks/formulario"));

        verify(webhookService, never()).guardar(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/webhooks/eliminar/{id} → redirect a lista")
    void eliminarRedirectALista() throws Exception {
        mockMvc.perform(post("/admin/webhooks/eliminar/1").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/webhooks"));

        verify(webhookService).eliminar(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/webhooks/toggle/{id} → redirect a lista")
    void toggleRedirectALista() throws Exception {
        mockMvc.perform(post("/admin/webhooks/toggle/1").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/webhooks"));

        verify(webhookService).toggleActivo(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/webhooks/probar/{id} OK → flash success")
    void probarExitosoFlashSuccess() throws Exception {
        when(webhookService.probar(1L)).thenReturn(true);

        mockMvc.perform(post("/admin/webhooks/probar/1").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/webhooks"))
                .andExpect(flash().attribute("tipoMensaje", "success"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/webhooks/probar/{id} fallido → flash danger")
    void probarFallidoFlashDanger() throws Exception {
        when(webhookService.probar(1L)).thenReturn(false);

        mockMvc.perform(post("/admin/webhooks/probar/1").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/webhooks"))
                .andExpect(flash().attribute("tipoMensaje", "danger"));
    }
}
