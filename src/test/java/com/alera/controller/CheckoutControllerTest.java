package com.alera.controller;

import com.alera.config.*;
import com.alera.model.CheckoutPendiente;
import com.alera.model.enums.PlanNivel;
import com.alera.payment.MercadoPagoGateway;
import com.alera.repository.CheckoutPendienteRepository;
import com.alera.repository.TenantRepository;
import com.alera.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CheckoutController.class)
@DisplayName("CheckoutController")
class CheckoutControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean TenantRepository               tenantRepo;
    @MockBean CheckoutPendienteRepository    checkoutRepo;
    @MockBean MercadoPagoGateway             pasarela;
    @MockBean TenantProvisioningService      provisioningService;
    @MockBean BrandingProperties             brandingProperties;
    @MockBean ZymosAuthSuccessHandler        successHandler;
    @MockBean ZymosAuthFailureHandler        failureHandler;
    @MockBean ZymosAccessDeniedHandler       accessDeniedHandler;
    @MockBean UsuarioService                 usuarioService;
    @MockBean LogAccesoService               logAccesoService;
    @MockBean LoginAttemptService            loginAttemptService;
    @MockBean JwtService                     jwtService;
    @MockBean PasswordEncoder                passwordEncoder;
    @MockBean EmailService                   emailService;

    @BeforeEach
    void setUp() {
        WebMvcTestHelper.configureTenantMock(tenantRepo);
        when(pasarela.isHabilitado()).thenReturn(false);
        when(pasarela.verificarFirmaWebhook(any(), any(), any(), anyMap())).thenReturn(true);
    }

    @Nested
    @DisplayName("GET /planes")
    class Planes {
        @Test
        @WithMockUser
        @DisplayName("retorna 200 y el modelo tiene planes")
        void planes_retorna200() throws Exception {
            mockMvc.perform(get("/planes"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("plan/planes"))
                    .andExpect(model().attributeExists("planes", "mpHabilitado"));
        }
    }

    @Nested
    @DisplayName("GET /checkout/verificar-subdominio")
    class VerificarSubdominio {
        @Test
        @WithMockUser
        @DisplayName("subdominio válido y libre retorna disponible:true")
        void disponible() throws Exception {
            when(tenantRepo.existsById("micerveceria")).thenReturn(false);
            when(checkoutRepo.findBySubdominioAndEstado("micerveceria", "PENDIENTE"))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/checkout/verificar-subdominio").param("s", "micerveceria"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.disponible").value(true));
        }

        @Test
        @WithMockUser
        @DisplayName("subdominio ya existente retorna disponible:false")
        void ocupado() throws Exception {
            when(tenantRepo.existsById("ocupado")).thenReturn(true);

            mockMvc.perform(get("/checkout/verificar-subdominio").param("s", "ocupado"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.disponible").value(false));
        }

        @Test
        @WithMockUser
        @DisplayName("formato inválido retorna disponible:false")
        void formatoInvalido() throws Exception {
            mockMvc.perform(get("/checkout/verificar-subdominio").param("s", "a"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.disponible").value(false));
        }

        @Test
        @WithMockUser
        @DisplayName("palabra reservada retorna disponible:false")
        void reservado() throws Exception {
            mockMvc.perform(get("/checkout/verificar-subdominio").param("s", "admin"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.disponible").value(false));
        }
    }

    @Nested
    @DisplayName("POST /checkout/iniciar")
    class IniciarCheckout {
        @Test
        @WithMockUser
        @DisplayName("subdominio inválido muestra error en planes")
        void subdominioInvalido() throws Exception {
            mockMvc.perform(post("/checkout/iniciar").with(csrf())
                    .param("planNivel", "MACERADO")
                    .param("nombreCerveceria", "Test")
                    .param("subdominio", "a")
                    .param("email", "test@test.com"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("plan/planes"))
                    .andExpect(model().attributeExists("error"));
        }

        @Test
        @WithMockUser
        @DisplayName("subdominio ya existente muestra error")
        void subdominioOcupado() throws Exception {
            when(tenantRepo.existsById("ocupado")).thenReturn(true);

            mockMvc.perform(post("/checkout/iniciar").with(csrf())
                    .param("planNivel", "MACERADO")
                    .param("nombreCerveceria", "Mi Cervecería")
                    .param("subdominio", "ocupado")
                    .param("email", "test@test.com"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("plan/planes"))
                    .andExpect(model().attributeExists("error"));
        }

        @Test
        @WithMockUser
        @DisplayName("datos válidos redirige a URL de checkout")
        void datosValidos_redirige() throws Exception {
            when(tenantRepo.existsById("micerve")).thenReturn(false);
            when(checkoutRepo.findBySubdominioAndEstado("micerve", "PENDIENTE"))
                    .thenReturn(Optional.empty());
            when(checkoutRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(pasarela.crearSesion(any(), anyString()))
                    .thenReturn("http://sandbox.mercadopago.com/checkout?pref=123");

            mockMvc.perform(post("/checkout/iniciar").with(csrf())
                    .param("planNivel", "MACERADO")
                    .param("nombreCerveceria", "Mi Cervecería")
                    .param("subdominio", "micerve")
                    .param("email", "test@test.com"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("http://sandbox.mercadopago.com/checkout?pref=123"));
        }
    }

    @Nested
    @DisplayName("GET /checkout/exito")
    class Exito {
        @Test
        @WithMockUser
        @DisplayName("pago aprobado provisiona y muestra exito")
        void pagoAprobado() throws Exception {
            CheckoutPendiente chk = new CheckoutPendiente();
            chk.setId("uuid-123");
            chk.setSubdominio("micerve");
            chk.setEmail("test@test.com");
            chk.setNombreCerveceria("Mi Cervecería");
            chk.setPlanNivel(PlanNivel.MACERADO);
            chk.setEstado("PENDIENTE");

            when(checkoutRepo.findById("uuid-123")).thenReturn(Optional.of(chk));
            when(pasarela.pagoAprobado("pay-456")).thenReturn(true);
            when(provisioningService.provisionar(any(), any(), any(), any(), any()))
                    .thenReturn(TenantProvisioningService.ResultadoProvision.ok("micerve",
                            "http://micerve.zymos.app"));

            mockMvc.perform(get("/checkout/exito")
                    .param("external_reference", "uuid-123")
                    .param("payment_id", "pay-456")
                    .param("collection_status", "approved"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("plan/exito"));
        }

        @Test
        @WithMockUser
        @DisplayName("sin parámetros muestra fallo")
        void sinParametros_fallo() throws Exception {
            mockMvc.perform(get("/checkout/exito"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("plan/fallo"));
        }
    }

    @Nested
    @DisplayName("GET /checkout/pendiente y /fallo")
    class PendienteYFallo {
        @Test
        @WithMockUser
        void pendiente_retorna200() throws Exception {
            mockMvc.perform(get("/checkout/pendiente"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("plan/pendiente"));
        }

        @Test
        @WithMockUser
        void fallo_retorna200() throws Exception {
            mockMvc.perform(get("/checkout/fallo"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("plan/fallo"));
        }
    }

    @Nested
    @DisplayName("POST /checkout/trial")
    class Trial {
        @Test
        @WithMockUser
        @DisplayName("subdominio inválido muestra error en planes")
        void subdominioInvalido_retornaError() throws Exception {
            mockMvc.perform(post("/checkout/trial").with(csrf())
                    .param("nombreCerveceria", "Mi Cervecería")
                    .param("subdominio", "a")
                    .param("email", "test@test.com"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("plan/planes"))
                    .andExpect(model().attributeExists("error"));
        }

        @Test
        @WithMockUser
        @DisplayName("subdominio ya existente muestra error")
        void subdominioOcupado_retornaError() throws Exception {
            when(tenantRepo.existsById("ocupado")).thenReturn(true);

            mockMvc.perform(post("/checkout/trial").with(csrf())
                    .param("nombreCerveceria", "Mi Cervecería")
                    .param("subdominio", "ocupado")
                    .param("email", "test@test.com"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("plan/planes"))
                    .andExpect(model().attributeExists("error"));
        }

        @Test
        @WithMockUser
        @DisplayName("email inválido muestra error")
        void emailInvalido_retornaError() throws Exception {
            when(tenantRepo.existsById("micerve")).thenReturn(false);
            when(checkoutRepo.findBySubdominioAndEstado("micerve", "PENDIENTE"))
                    .thenReturn(Optional.empty());

            mockMvc.perform(post("/checkout/trial").with(csrf())
                    .param("nombreCerveceria", "Mi Cervecería")
                    .param("subdominio", "micerve")
                    .param("email", "noesunemail"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("plan/planes"))
                    .andExpect(model().attributeExists("error"));
        }

        @Test
        @WithMockUser
        @DisplayName("datos válidos provisiona y muestra éxito con esTrial=true")
        void datosValidos_retornaExitoConTrial() throws Exception {
            when(tenantRepo.existsById("micerve")).thenReturn(false);
            when(checkoutRepo.findBySubdominioAndEstado("micerve", "PENDIENTE"))
                    .thenReturn(Optional.empty());
            when(provisioningService.provisionar(
                    eq("micerve"), eq("test@test.com"), eq("Mi Cervecería"),
                    eq(PlanNivel.FERMENTACION), any(), eq(14)))
                    .thenReturn(TenantProvisioningService.ResultadoProvision.ok(
                            "micerve", "http://micerve.zymos.app"));

            mockMvc.perform(post("/checkout/trial").with(csrf())
                    .param("nombreCerveceria", "Mi Cervecería")
                    .param("subdominio", "micerve")
                    .param("email", "test@test.com"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("plan/exito"))
                    .andExpect(model().attribute("esTrial", true))
                    .andExpect(model().attributeExists("checkout", "loginUrl", "trialFin"));
        }
    }

    @Nested
    @DisplayName("POST /webhook/mp")
    class WebhookMp {

        @Test
        @WithMockUser
        @DisplayName("firma invalida retorna 401")
        void firmaInvalida_retorna401() throws Exception {
            when(pasarela.verificarFirmaWebhook(any(), any(), any(), anyMap())).thenReturn(false);

            mockMvc.perform(post("/webhook/mp").with(csrf())
                    .contentType("application/json")
                    .content("{\"type\":\"payment\",\"data\":{\"id\":\"pay-999\"}}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser
        @DisplayName("evento de pago aprobado retorna 200")
        void webhookPagoAprobado_retorna200() throws Exception {
            when(pasarela.resolverPaymentIdDesdeWebhook(any(), anyMap()))
                    .thenReturn(Optional.of("pay-789"));
            when(pasarela.pagoAprobado("pay-789")).thenReturn(true);
            when(pasarela.obtenerExternalReference("pay-789")).thenReturn("uuid-chk");
            when(checkoutRepo.findById("uuid-chk")).thenReturn(Optional.empty());

            mockMvc.perform(post("/webhook/mp").with(csrf())
                    .contentType("application/json")
                    .content("{\"type\":\"payment\",\"data\":{\"id\":\"pay-789\"}}"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser
        @DisplayName("evento irrelevante retorna 200 sin procesar")
        void webhookIrrelevante_retorna200() throws Exception {
            when(pasarela.resolverPaymentIdDesdeWebhook(any(), anyMap()))
                    .thenReturn(Optional.empty());

            mockMvc.perform(post("/webhook/mp").with(csrf())
                    .contentType("application/json")
                    .content("{\"type\":\"plan\",\"action\":\"plan.created\"}"))
                    .andExpect(status().isOk());
        }
    }
}
