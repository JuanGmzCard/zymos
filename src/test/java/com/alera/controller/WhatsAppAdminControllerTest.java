package com.alera.controller;

import com.alera.config.*;
import com.alera.repository.TenantRepository;
import com.alera.repository.UsuarioRepository;
import com.alera.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WhatsAppAdminController.class)
@DisplayName("WhatsAppAdminController")
class WhatsAppAdminControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean ContactoWhatsAppService     service;
    @MockBean WhatsAppService             whatsAppService;
    @MockBean TenantRepository            tenantRepo;
    @MockBean BrandingProperties          brandingProperties;
    @MockBean ZymosAuthSuccessHandler     successHandler;
    @MockBean ZymosAuthFailureHandler     failureHandler;
    @MockBean ZymosAccessDeniedHandler    accessDeniedHandler;
    @MockBean UsuarioService              usuarioService;
    @MockBean LogAccesoService            logAccesoService;
    @MockBean LoginAttemptService         loginAttemptService;
    @MockBean JwtService                  jwtService;
    @MockBean PasswordEncoder             passwordEncoder;
    @MockBean UsuarioRepository           usuarioRepository;

    @BeforeEach
    void setUp() {
        WebMvcTestHelper.configureTenantMock(tenantRepo);
        when(service.listarTodos()).thenReturn(List.of());
        when(whatsAppService.isConfigured()).thenReturn(false);
    }

    @Test
    @DisplayName("GET /admin/whatsapp sin autenticar → 401")
    void lista_sinAuth_retorna401() throws Exception {
        mockMvc.perform(get("/admin/whatsapp"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/whatsapp con ADMIN → 200 y vista lista")
    void lista_conAdmin_retorna200() throws Exception {
        mockMvc.perform(get("/admin/whatsapp"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/whatsapp/lista"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/whatsapp/nuevo con ADMIN → 200")
    void nuevo_conAdmin_retorna200() throws Exception {
        mockMvc.perform(get("/admin/whatsapp/nuevo"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/whatsapp/formulario"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/whatsapp/guardar datos válidos → redirect")
    void guardar_datosValidos_redirige() throws Exception {
        doNothing().when(service).guardar(any());

        mockMvc.perform(post("/admin/whatsapp/guardar").with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("nombre", "Encargado de Compras")
                        .param("telefono", "+573001234567")
                        .param("activo", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/whatsapp"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/whatsapp/guardar teléfono inválido → re-render formulario")
    void guardar_telefonoInvalido_rerenderFormulario() throws Exception {
        mockMvc.perform(post("/admin/whatsapp/guardar").with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("nombre", "Contacto")
                        .param("telefono", "telefonoSinPlus"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/whatsapp/formulario"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/whatsapp/eliminar/{id} → redirect")
    void eliminar_redirige() throws Exception {
        doNothing().when(service).eliminar(anyLong());

        mockMvc.perform(post("/admin/whatsapp/eliminar/1").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/whatsapp"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/whatsapp/toggle/{id} → redirect")
    void toggle_redirige() throws Exception {
        doNothing().when(service).toggleActivo(anyLong());

        mockMvc.perform(post("/admin/whatsapp/toggle/1").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/whatsapp"));
    }
}
