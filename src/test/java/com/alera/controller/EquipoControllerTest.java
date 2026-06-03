package com.alera.controller;

import com.alera.config.*;
import com.alera.repository.TenantRepository;
import com.alera.service.CategoriaEquipoService;
import com.alera.service.EquipoService;
import com.alera.service.JwtService;
import com.alera.service.LogAccesoService;
import com.alera.service.MantenimientoEquipoService;
import com.alera.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EquipoController.class)
@DisplayName("EquipoController")
class EquipoControllerTest {

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
    @MockBean EquipoService              equipoService;
    @MockBean MantenimientoEquipoService mantenimientoService;
    @MockBean CategoriaEquipoService     categoriaEquipoService;

    @BeforeEach
    void setUp() {
        WebMvcTestHelper.configureTenantMock(tenantRepo);
        doReturn(new PageImpl<>(Collections.emptyList()))
                .when(equipoService).listarPaginado(any(), anyInt());
        when(equipoService.suggest(anyString(), any())).thenReturn(List.of());
        when(equipoService.listarFermentadoresDisponibles()).thenReturn(List.of());
        when(equipoService.countTotal()).thenReturn(0L);
        when(equipoService.countByEstado(any())).thenReturn(0L);
        when(equipoService.countMantenimientoPendiente()).thenReturn(0L);
        when(categoriaEquipoService.listarNombresActivos()).thenReturn(List.of());
    }

    @Test
    @DisplayName("GET /equipos sin autenticar retorna 401")
    void lista_sinAuth_retorna401() throws Exception {
        mockMvc.perform(get("/equipos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /equipos con ADMIN retorna 200")
    void lista_conAdmin_retorna200() throws Exception {
        mockMvc.perform(get("/equipos"))
                .andExpect(status().isOk())
                .andExpect(view().name("equipos/lista"));
    }

    @Test
    @WithMockUser(roles = "EQUIPOS")
    @DisplayName("GET /equipos/suggest retorna JSON")
    void suggest_retornaJson() throws Exception {
        mockMvc.perform(get("/equipos/suggest").param("q", "fer"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /equipos/ver/{id} retorna detalle")
    void ver_retornaDetalle() throws Exception {
        com.alera.model.Equipo equipo = new com.alera.model.Equipo();
        equipo.setId(1L);
        equipo.setNombre("Fermentador A");
        equipo.setTipo("Fermentador");
        equipo.setEstado(com.alera.model.enums.EstadoEquipo.OPERATIVO);
        when(equipoService.buscarPorId(1L)).thenReturn(Optional.of(equipo));
        when(mantenimientoService.listarPorEquipo(1L)).thenReturn(List.of());
        when(mantenimientoService.sumCostoPorEquipo(1L)).thenReturn(BigDecimal.ZERO);
        when(mantenimientoService.countPorEquipo(1L)).thenReturn(0L);

        mockMvc.perform(get("/equipos/ver/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("equipos/detalle"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /equipos/ver/{id} con id inexistente redirige con mensaje de error")
    void ver_noExiste_redirige() throws Exception {
        when(equipoService.buscarPorId(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/equipos/ver/99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipos"))
                .andExpect(flash().attribute("tipoMensaje", "danger"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /equipos/editar/{id} con id inexistente redirige con mensaje de error")
    void editar_noExiste_redirige() throws Exception {
        when(equipoService.buscarPorId(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/equipos/editar/99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipos"))
                .andExpect(flash().attribute("tipoMensaje", "danger"));
    }
}
