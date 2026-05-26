package com.alera.controller;

import com.alera.config.ZymosAccessDeniedHandler;
import com.alera.config.ZymosAuthFailureHandler;
import com.alera.config.ZymosAuthSuccessHandler;
import com.alera.config.BrandingProperties;
import com.alera.config.LoginAttemptService;
import com.alera.repository.TenantRepository;
import com.alera.service.JwtService;
import com.alera.service.LogAccesoService;
import com.alera.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * addFilters=false bypasa la cadena de filtros de Spring Security en MockMvc.
 * Esto evita el conflicto entre @MockBean AuthenticationManager y la
 * configuración interna de Spring Security, permitiendo testear solo la
 * lógica del controller (flujo de autenticación, generación de token, validación).
 * La configuración de seguridad (permitAll, CSRF) se verifica en tests de integración.
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean AuthenticationManager    authManager;
    @MockBean JwtService               jwtService;
    @MockBean LogAccesoService         logAccesoService;
    @MockBean UsuarioService           usuarioService;
    @MockBean TenantRepository         tenantRepo;
    @MockBean BrandingProperties       brandingProperties;
    @MockBean ZymosAuthSuccessHandler  successHandler;
    @MockBean ZymosAuthFailureHandler  failureHandler;
    @MockBean ZymosAccessDeniedHandler accessDeniedHandler;
    @MockBean LoginAttemptService      loginAttemptService;

    @BeforeEach
    void setUp() {
        WebMvcTestHelper.configureTenantMock(tenantRepo);
    }

    @Test
    void loginConCredencialesValidasRetornaToken() throws Exception {
        var userDetails = new User("admin", "pw",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        var auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        when(authManager.authenticate(any())).thenReturn(auth);
        when(jwtService.generarToken(any(), any())).thenReturn("eyJhbGciOiJIUzI1NiJ9.test.sig");
        when(jwtService.getTtlSegundos()).thenReturn(86400L);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("eyJhbGciOiJIUzI1NiJ9.test.sig"))
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(86400))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.rol").value("ROLE_ADMIN"));
    }

    @Test
    void loginConCredencialesInvalidasRetorna401() throws Exception {
        when(authManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void loginConCuerpoVacioRetorna400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
