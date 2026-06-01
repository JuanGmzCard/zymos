package com.alera.controller;

import com.alera.config.*;
import com.alera.model.Cliente;
import com.alera.repository.TenantRepository;
import com.alera.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClienteController.class)
@DisplayName("ClienteController")
class ClienteControllerTest {

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
    @MockBean ClienteService           clienteService;

    @BeforeEach
    void setUp() {
        WebMvcTestHelper.configureTenantMock(tenantRepo);
        when(clienteService.listarPaginado(any(), any(), anyInt()))
                .thenReturn(new PageImpl<>(List.of()));
        when(clienteService.suggest(anyString())).thenReturn(List.of());
    }

    @Test
    @DisplayName("GET /clientes sin autenticar retorna 401")
    void lista_sinAuth_retorna401() throws Exception {
        mockMvc.perform(get("/clientes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /clientes con ADMIN retorna 200 y vista correcta")
    void lista_conAdmin_retorna200() throws Exception {
        mockMvc.perform(get("/clientes"))
                .andExpect(status().isOk())
                .andExpect(view().name("clientes/lista"))
                .andExpect(model().attributeExists("clientes", "totalClientes"));
    }

    @Test
    @WithMockUser(roles = "FACTURACION")
    @DisplayName("GET /clientes con FACTURACION retorna 200")
    void lista_conFacturacion_retorna200() throws Exception {
        mockMvc.perform(get("/clientes"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /clientes/suggest retorna JSON")
    void suggest_retornaJson() throws Exception {
        when(clienteService.suggest("mosto")).thenReturn(List.of(
                Map.of("id", 1L, "nombre", "Cervecería Mosto", "nit", "900-1", "ciudad", "Bogotá")
        ));
        mockMvc.perform(get("/clientes/suggest").param("q", "mosto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Cervecería Mosto"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /clientes/nuevo retorna formulario con listas de enums")
    void nuevo_retornaFormulario() throws Exception {
        mockMvc.perform(get("/clientes/nuevo"))
                .andExpect(status().isOk())
                .andExpect(view().name("clientes/formulario"))
                .andExpect(model().attributeExists("cliente", "listasPrecio", "regimenes"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /clientes/ver/{id} retorna detalle del cliente")
    void ver_retornaDetalle() throws Exception {
        Cliente c = new Cliente();
        c.setNombre("Distribuidora Norte");
        when(clienteService.buscarPorId(1L)).thenReturn(Optional.of(c));

        mockMvc.perform(get("/clientes/ver/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("clientes/detalle"))
                .andExpect(model().attributeExists("cliente"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /clientes/guardar con NIT duplicado redirige con flash danger")
    void guardar_nitDuplicado_redirige() throws Exception {
        doThrow(new RuntimeException("NIT ya registrado para otro cliente"))
                .when(clienteService).guardar(any());

        mockMvc.perform(post("/clientes/guardar").with(csrf())
                        .param("nombre", "Cliente Test"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clientes"))
                .andExpect(flash().attribute("tipoMensaje", "danger"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /clientes/{id}/toggle redirige a /clientes")
    void toggle_redirige() throws Exception {
        mockMvc.perform(post("/clientes/1/toggle").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clientes"));
    }
}
