package com.alera.controller;

import com.alera.config.*;
import com.alera.model.InsumoInventario;
import com.alera.model.enums.TipoMovimiento;
import com.alera.repository.FacturaItemRepository;
import com.alera.repository.TenantRepository;
import com.alera.service.ExcelExportService;
import com.alera.service.CategoriaInsumoService;
import com.alera.service.InsumoInventarioService;
import com.alera.service.JwtService;
import com.alera.service.LogAccesoService;
import com.alera.service.ProveedorService;
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

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InsumoInventarioController.class)
@DisplayName("InsumoInventarioController")
class InsumoInventarioControllerTest {

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
    @MockBean InsumoInventarioService    insumoService;
    @MockBean CategoriaInsumoService     categoriaInsumoService;
    @MockBean FacturaItemRepository      facturaItemRepo;
    @MockBean ExcelExportService         excelService;
    @MockBean ProveedorService           proveedorService;

    @BeforeEach
    void setUp() {
        WebMvcTestHelper.configureTenantMock(tenantRepo);
        when(insumoService.listarPaginado(any(), any(), anyInt())).thenReturn(new PageImpl<>(List.of()));
        when(insumoService.listarBajoStock()).thenReturn(List.of());
        when(insumoService.listarProximosAVencer(anyInt())).thenReturn(List.of());
        when(facturaItemRepo.findNombresDistintos()).thenReturn(List.of());
        when(proveedorService.listarActivos()).thenReturn(List.of());
        when(categoriaInsumoService.listarNombresActivos()).thenReturn(List.of());
    }

    @Test
    @DisplayName("GET /inventario sin autenticar retorna 401")
    void lista_sinAuth_retorna401() throws Exception {
        mockMvc.perform(get("/inventario"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /inventario con ADMIN retorna 200")
    void lista_conAdmin_retorna200() throws Exception {
        mockMvc.perform(get("/inventario"))
                .andExpect(status().isOk())
                .andExpect(view().name("inventario/lista"));
    }

    @Test
    @WithMockUser(roles = "INVENTARIO")
    @DisplayName("GET /inventario/suggest retorna JSON")
    void suggest_retornaJson() throws Exception {
        when(insumoService.listarPaginado(any(), any(), anyInt())).thenReturn(new PageImpl<>(List.of()));
        mockMvc.perform(get("/inventario/suggest").param("nombre", "ma"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /inventario/editar/{id} con id inexistente redirige con mensaje de error")
    void editar_noExiste_redirige() throws Exception {
        when(insumoService.buscarPorId(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/inventario/editar/99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inventario"))
                .andExpect(flash().attribute("tipoMensaje", "danger"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /inventario/{id}/historial con id inexistente redirige con mensaje de error")
    void historial_noExiste_redirige() throws Exception {
        when(insumoService.buscarPorId(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/inventario/99/historial"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inventario"))
                .andExpect(flash().attribute("tipoMensaje", "danger"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /inventario/nuevo retorna 200 con el formulario vacío")
    void nuevo_conAdmin_retorna200() throws Exception {
        mockMvc.perform(get("/inventario/nuevo"))
                .andExpect(status().isOk())
                .andExpect(view().name("inventario/formulario"));
    }

    @Test
    @WithMockUser(roles = "INVENTARIO")
    @DisplayName("GET /inventario/editar/{id} con insumo existente retorna 200 con el formulario")
    void editar_existe_retorna200() throws Exception {
        InsumoInventario insumo = new InsumoInventario();
        insumo.setId(1L);
        insumo.setNombre("Malta Pilsen");
        when(insumoService.buscarPorId(1L)).thenReturn(Optional.of(insumo));

        mockMvc.perform(get("/inventario/editar/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("inventario/formulario"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /inventario/guardar redirige a /inventario con mensaje de éxito")
    void guardar_redirige() throws Exception {
        InsumoInventario saved = new InsumoInventario();
        when(insumoService.guardar(any())).thenReturn(saved);

        mockMvc.perform(post("/inventario/guardar").with(csrf())
                        .param("nombre", "Malta Vienna")
                        .param("tipo", "Malta")
                        .param("unidad", "gr")
                        .param("cantidad", "0")
                        .param("stockMinimo", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inventario"))
                .andExpect(flash().attribute("tipoMensaje", "success"));
    }

    @Test
    @WithMockUser(roles = "INVENTARIO")
    @DisplayName("POST /inventario/actualizar/{id} redirige a /inventario con mensaje de éxito")
    void actualizar_redirige() throws Exception {
        InsumoInventario saved = new InsumoInventario();
        when(insumoService.guardar(any())).thenReturn(saved);

        mockMvc.perform(post("/inventario/actualizar/1").with(csrf())
                        .param("nombre", "Malta Vienna")
                        .param("tipo", "Malta")
                        .param("unidad", "gr"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inventario"))
                .andExpect(flash().attribute("tipoMensaje", "success"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /inventario/eliminar/{id} redirige a /inventario con mensaje de éxito")
    void eliminar_redirige() throws Exception {
        mockMvc.perform(post("/inventario/eliminar/1").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inventario"))
                .andExpect(flash().attribute("tipoMensaje", "success"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /inventario/{id}/ajuste redirige a /inventario con mensaje de éxito")
    void ajuste_redirige() throws Exception {
        mockMvc.perform(post("/inventario/1/ajuste").with(csrf())
                        .param("tipo", TipoMovimiento.ENTRADA.name())
                        .param("cantidad", "500")
                        .param("motivo", "Compra"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inventario"));
    }
}
