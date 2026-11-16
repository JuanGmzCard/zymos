package com.alera.controller;

import com.alera.config.*;
import com.alera.repository.TenantRepository;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PushSubscriptionController.class)
@DisplayName("PushSubscriptionController")
class PushSubscriptionControllerTest {

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
    @MockBean PasswordEncoder            passwordEncoder;
    @MockBean EmailService               emailService;
    @MockBean PushNotificationService    pushNotificationService;

    @BeforeEach
    void setUp() {
        WebMvcTestHelper.configureTenantMock(tenantRepo);
        when(pushNotificationService.getVapidPublicKey()).thenReturn("mock-vapid-public-key");
        when(pushNotificationService.isConfigured()).thenReturn(false);
        when(pushNotificationService.isSubscribed(any(), any())).thenReturn(false);
        doNothing().when(pushNotificationService).upsertSubscription(any(), any(), any(), any(), any());
        doNothing().when(pushNotificationService).removeSubscription(any());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /push/vapid-public-key → 200 con publicKey")
    void vapidPublicKey_retorna200() throws Exception {
        mockMvc.perform(get("/push/vapid-public-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicKey").value("mock-vapid-public-key"))
                .andExpect(jsonPath("$.isConfigured").value("false"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /push/subscribe con payload válido → 200")
    void subscribe_ok() throws Exception {
        String body = """
                {"endpoint":"https://push.example.com/sub","p256dh":"dGVzdA==","auth":"dGVzdA=="}
                """;
        mockMvc.perform(post("/push/subscribe")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /push/unsubscribe → 200")
    void unsubscribe_ok() throws Exception {
        String body = "{\"endpoint\":\"https://push.example.com/sub\"}";
        mockMvc.perform(post("/push/unsubscribe")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /push/status → 200 con subscribed:false")
    void status_ok() throws Exception {
        mockMvc.perform(get("/push/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscribed").value(false));
    }

    @Test
    @DisplayName("POST /push/subscribe sin autenticar → 401")
    void subscribe_sinAuth_retorna401() throws Exception {
        mockMvc.perform(post("/push/subscribe")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"endpoint\":\"x\",\"p256dh\":\"y\",\"auth\":\"z\"}"))
                .andExpect(status().isUnauthorized());
    }
}
