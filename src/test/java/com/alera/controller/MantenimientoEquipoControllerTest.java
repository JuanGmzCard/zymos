package com.alera.controller;

import com.alera.config.*;
import com.alera.model.Equipo;
import com.alera.model.enums.EstadoEquipo;

import com.alera.repository.TenantRepository;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MantenimientoEquipoController.class)
@DisplayName("MantenimientoEquipoController")
class MantenimientoEquipoControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean TenantRepository               tenantRepo;
    @MockBean BrandingProperties             brandingProperties;
    @MockBean ZymosAuthSuccessHandler        successHandler;
    @MockBean ZymosAuthFailureHandler        failureHandler;
    @MockBean ZymosAccessDeniedHandler       accessDeniedHandler;
    @MockBean UsuarioService                 usuarioService;
    @MockBean LogAccesoService               logAccesoService;
    @MockBean LoginAttemptService            loginAttemptService;
    @MockBean JwtService                     jwtService;
    @MockBean MantenimientoEquipoService     mantenimientoService;
    @MockBean EquipoService                  equipoService;

    @BeforeEach
    void setUp() {
        WebMvcTestHelper.configureTenantMock(tenantRepo);
        Equipo equipo = new Equipo();
        equipo.setId(1L);
        equipo.setNombre("Fermentador 1");
        equipo.setTipo("Fermentador");
        equipo.setEstado(EstadoEquipo.OPERATIVO);
        when(equipoService.buscarPorId(1L)).thenReturn(Optional.of(equipo));
        when(mantenimientoService.listarPorEquipo(1L)).thenReturn(List.of());
        when(mantenimientoService.sumCostoPorEquipo(1L)).thenReturn(BigDecimal.ZERO);
        when(mantenimientoService.countPorEquipo(1L)).thenReturn(0L);
    }

    @Test
    @DisplayName("GET /equipos/1/mantenimientos sin autenticar retorna 401")
    void mantenimientos_sinAuth_retorna401() throws Exception {
        mockMvc.perform(get("/equipos/1/mantenimientos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /equipos/1/mantenimientos con ADMIN retorna 200")
    void mantenimientos_conAdmin_retorna200() throws Exception {
        mockMvc.perform(get("/equipos/1/mantenimientos"))
                .andExpect(status().isOk())
                .andExpect(view().name("equipos/mantenimientos"));
    }
}
