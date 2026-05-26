package com.alera.controller;

import com.alera.config.ZymosAccessDeniedHandler;
import com.alera.config.ZymosAuthFailureHandler;
import com.alera.config.ZymosAuthSuccessHandler;
import com.alera.config.BrandingProperties;
import com.alera.config.LoginAttemptService;
import com.alera.model.Notificacion;
import com.alera.model.enums.TipoNotificacion;
import com.alera.repository.TenantRepository;
import com.alera.service.JwtService;
import com.alera.service.LogAccesoService;
import com.alera.service.NotificacionService;
import com.alera.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificacionController.class)
class NotificacionControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean NotificacionService       notificacionService;
    @MockBean LogAccesoService          logAccesoService;
    @MockBean UsuarioService            usuarioService;
    @MockBean TenantRepository          tenantRepo;
    @MockBean BrandingProperties        brandingProperties;
    @MockBean ZymosAuthSuccessHandler   successHandler;
    @MockBean ZymosAuthFailureHandler   failureHandler;
    @MockBean ZymosAccessDeniedHandler  accessDeniedHandler;
    @MockBean LoginAttemptService       loginAttemptService;
    @MockBean JwtService                jwtService;

    @BeforeEach
    void setUp() {
        WebMvcTestHelper.configureTenantMock(tenantRepo);
    }

    @Test
    void sinAutenticarDevuelveUnauthorized() throws Exception {
        mockMvc.perform(get("/notificaciones"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void paginaIndexCargaCorrectamente() throws Exception {
        when(notificacionService.listarTodas(anyInt())).thenReturn(new PageImpl<>(List.of()));
        when(notificacionService.contarNoLeidas()).thenReturn(0L);

        mockMvc.perform(get("/notificaciones"))
                .andExpect(status().isOk())
                .andExpect(view().name("notificaciones/index"))
                .andExpect(model().attributeExists("notificaciones", "totalNoLeidas"));
    }

    @Test
    @WithMockUser
    void recientesRetornaJson() throws Exception {
        Notificacion n = Notificacion.of(
                TipoNotificacion.BAJO_STOCK,
                "2 insumos bajo stock",
                "Malta, Lúpulo.",
                "/inventario");
        n.setLeida(false);
        // Acceso reflexivo para setear createdAt (campo privado con @PrePersist)
        try {
            var f = Notificacion.class.getDeclaredField("createdAt");
            f.setAccessible(true);
            f.set(n, LocalDateTime.now().minusHours(1));
        } catch (Exception ignored) {}

        when(notificacionService.contarNoLeidas()).thenReturn(1L);
        when(notificacionService.listarRecientes()).thenReturn(List.of(n));

        mockMvc.perform(get("/notificaciones/recientes"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].titulo").value("2 insumos bajo stock"))
                .andExpect(jsonPath("$.items[0].tipo").value("BAJO_STOCK"));
    }

    @Test
    @WithMockUser
    void marcarLeidaDevuelveJsonConNoLeidas() throws Exception {
        doNothing().when(notificacionService).marcarLeida(anyLong());
        when(notificacionService.contarNoLeidas()).thenReturn(2L);

        mockMvc.perform(post("/notificaciones/1/leer").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.noLeidas").value(2));
    }

    @Test
    @WithMockUser
    void marcarTodasLeidasRedirige() throws Exception {
        doNothing().when(notificacionService).marcarTodasLeidas();

        mockMvc.perform(post("/notificaciones/leer-todas").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notificaciones"));
    }
}
