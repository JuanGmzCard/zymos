package com.alera.controller;

import com.alera.config.*;
import com.alera.dto.MensajeChatDto;
import com.alera.repository.TenantRepository;
import com.alera.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
@DisplayName("ChatController")
class ChatControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean TenantRepository         tenantRepo;
    @MockBean BrandingProperties       brandingProperties;
    @MockBean ZymosAuthSuccessHandler  successHandler;
    @MockBean ZymosAuthFailureHandler  failureHandler;
    @MockBean ZymosAccessDeniedHandler accessDeniedHandler;
    @MockBean UsuarioService           usuarioService;
    @MockBean LogAccesoService         logAccesoService;
    @MockBean LoginAttemptService      loginAttemptService;
    @MockBean JwtService               jwtService;
    @MockBean TenantChatService        chatService;

    @BeforeEach
    void setUp() {
        WebMvcTestHelper.configureTenantMock(tenantRepo);
        when(chatService.isConfigured()).thenReturn(false);
        when(chatService.getSugerencias(any())).thenReturn(
                List.of("¿Cuántos lotes activos tengo?", "¿Qué insumos hay bajo stock?"));
    }

    @Test
    @DisplayName("GET /chat sin autenticar retorna 401")
    void get_sinAuth_retorna401() throws Exception {
        mockMvc.perform(get("/chat"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /chat con ADMIN retorna 200 con sugerencias en modelo")
    void get_conAdmin_retorna200() throws Exception {
        mockMvc.perform(get("/chat"))
                .andExpect(status().isOk())
                .andExpect(view().name("chat/index"))
                .andExpect(model().attributeExists("iaConfigurada"))
                .andExpect(model().attributeExists("sugerencias"));
    }

    @Test
    @WithMockUser(roles = "INVENTARIO")
    @DisplayName("GET /chat con usuario básico retorna 200")
    void get_conUsuarioBasico_retorna200() throws Exception {
        mockMvc.perform(get("/chat"))
                .andExpect(status().isOk())
                .andExpect(view().name("chat/index"));
    }

    @Test
    @DisplayName("POST /chat/mensaje sin autenticar retorna 401")
    void post_sinAuth_retorna401() throws Exception {
        mockMvc.perform(post("/chat/mensaje")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pregunta\":\"Hola\",\"historial\":[]}")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /chat/mensaje retorna respuesta JSON")
    void post_mensaje_retornaJson() throws Exception {
        MensajeChatDto respuesta = new MensajeChatDto("assistant", "Tienes 3 lotes activos.", false);
        when(chatService.chat(anyString(), anyList(), anyString())).thenReturn(respuesta);

        mockMvc.perform(post("/chat/mensaje")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pregunta\":\"¿Cuántos lotes tengo?\",\"historial\":[]}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("assistant"))
                .andExpect(jsonPath("$.contenido").value("Tienes 3 lotes activos."))
                .andExpect(jsonPath("$.modoMock").value(false));
    }

    @Test
    @WithMockUser(roles = "PRODUCCION")
    @DisplayName("POST /chat/mensaje mock mode incluye modoMock=true")
    void post_mensaje_modoMock() throws Exception {
        MensajeChatDto respuesta = new MensajeChatDto("assistant", "Respuesta de prueba.", true);
        when(chatService.chat(anyString(), anyList(), anyString())).thenReturn(respuesta);

        mockMvc.perform(post("/chat/mensaje")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pregunta\":\"Hola\",\"historial\":[]}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modoMock").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /chat expone iaConfigurada=false cuando servicio no está configurado")
    void get_exponeIaConfigurada_false() throws Exception {
        when(chatService.isConfigured()).thenReturn(false);

        mockMvc.perform(get("/chat"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("iaConfigurada", false));
    }
}
