package com.alera.controller;

import com.alera.config.*;
import com.alera.dto.FacturaFormDto;
import com.alera.model.FacturaProveedor;
import com.alera.model.enums.EstadoFactura;
import com.alera.repository.EquipoRepository;
import com.alera.repository.FacturaItemRepository;
import com.alera.repository.InsumoInventarioRepository;
import com.alera.repository.TenantRepository;
import com.alera.service.*;
import com.alera.service.CategoriaInsumoService;
import com.alera.service.CategoriaEquipoService;
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
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FacturaProveedorController.class)
@DisplayName("FacturaProveedorController")
class FacturaProveedorControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean TenantRepository              tenantRepo;
    @MockBean BrandingProperties            brandingProperties;
    @MockBean ZymosAuthSuccessHandler       successHandler;
    @MockBean ZymosAuthFailureHandler       failureHandler;
    @MockBean ZymosAccessDeniedHandler      accessDeniedHandler;
    @MockBean UsuarioService                usuarioService;
    @MockBean LogAccesoService              logAccesoService;
    @MockBean LoginAttemptService           loginAttemptService;
    @MockBean JwtService                    jwtService;
    @MockBean FacturaProveedorService       facturaService;
    @MockBean ProveedorService              proveedorService;
    @MockBean InsumoInventarioRepository    insumoRepo;
    @MockBean EquipoRepository              equipoRepo;
    @MockBean InsumoInventarioService       insumoService;
    @MockBean EquipoService                 equipoService;
    @MockBean ExcelExportService            excelService;
    @MockBean CategoriaInsumoService        categoriaInsumoService;
    @MockBean CategoriaEquipoService        categoriaEquipoService;
    @MockBean FacturaItemRepository         facturaItemRepo;

    @BeforeEach
    void setUp() {
        WebMvcTestHelper.configureTenantMock(tenantRepo);
        when(facturaService.listarPaginado(any(), any(), any(), anyInt())).thenReturn(new PageImpl<>(List.of()));
        when(facturaService.suggest(anyString())).thenReturn(List.of());
        when(facturaService.sumTotal(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(facturaService.sumPendiente(any(), any())).thenReturn(BigDecimal.ZERO);
        when(facturaService.countPendiente(any(), any())).thenReturn(0L);
        when(proveedorService.listarActivos()).thenReturn(List.of());
        when(insumoRepo.findAll()).thenReturn(List.of());
        when(equipoRepo.findAll()).thenReturn(List.of());
        when(categoriaInsumoService.listarNombresActivos()).thenReturn(List.of());
        when(categoriaEquipoService.listarNombresActivos()).thenReturn(List.of());
    }

    @Test
    @DisplayName("GET /facturas sin autenticar retorna 401")
    void lista_sinAuth_retorna401() throws Exception {
        mockMvc.perform(get("/facturas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /facturas con ADMIN retorna 200")
    void lista_conAdmin_retorna200() throws Exception {
        mockMvc.perform(get("/facturas"))
                .andExpect(status().isOk())
                .andExpect(view().name("facturas/lista"));
    }

    @Test
    @WithMockUser(roles = "FACTURACION")
    @DisplayName("GET /facturas/suggest retorna JSON")
    void suggest_retornaJson() throws Exception {
        mockMvc.perform(get("/facturas/suggest").param("q", "001"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /facturas/ver/{id} con id inexistente redirige con mensaje de error")
    void ver_noExiste_redirige() throws Exception {
        when(facturaService.buscarPorId(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/facturas/ver/99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/facturas"))
                .andExpect(flash().attribute("tipoMensaje", "danger"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /facturas/editar/{id} con id inexistente redirige con mensaje de error")
    void editar_noExiste_redirige() throws Exception {
        when(facturaService.buscarPorId(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/facturas/editar/99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/facturas"))
                .andExpect(flash().attribute("tipoMensaje", "danger"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /facturas/nueva retorna 200 con el formulario vacío")
    void nueva_conAdmin_retorna200() throws Exception {
        mockMvc.perform(get("/facturas/nueva"))
                .andExpect(status().isOk())
                .andExpect(view().name("facturas/formulario"));
    }

    @Test
    @WithMockUser(roles = "FACTURACION")
    @DisplayName("GET /facturas/ver/{id} con factura existente retorna 200 con la vista de detalle")
    void ver_existe_retorna200() throws Exception {
        FacturaProveedor factura = new FacturaProveedor();
        factura.setId(1L);
        when(facturaService.buscarPorId(1L)).thenReturn(Optional.of(factura));
        when(facturaService.listarHistorial(1L)).thenReturn(List.of());

        mockMvc.perform(get("/facturas/ver/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("facturas/detalle"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /facturas/guardar con datos válidos redirige a /facturas con éxito")
    void guardar_datosValidos_redirige() throws Exception {
        mockMvc.perform(post("/facturas/guardar").with(csrf())
                        .param("numeroFactura", "FAC-001")
                        .param("proveedor", "Proveedor Test")
                        .param("fechaFactura", "2025-03-01"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/facturas"))
                .andExpect(flash().attribute("tipoMensaje", "success"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /facturas/guardar con datos inválidos retorna el formulario")
    void guardar_datosInvalidos_retornaFormulario() throws Exception {
        mockMvc.perform(post("/facturas/guardar").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("facturas/formulario"));
    }

    @Test
    @WithMockUser(roles = "FACTURACION")
    @DisplayName("POST /facturas/actualizar/{id} con datos válidos redirige a /facturas con éxito")
    void actualizar_datosValidos_redirige() throws Exception {
        mockMvc.perform(post("/facturas/actualizar/1").with(csrf())
                        .param("numeroFactura", "FAC-001")
                        .param("proveedor", "Proveedor Test")
                        .param("fechaFactura", "2025-03-01"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/facturas"))
                .andExpect(flash().attribute("tipoMensaje", "success"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /facturas/{id}/estado redirige a /facturas/ver/{id} con éxito")
    void cambiarEstado_redirige() throws Exception {
        mockMvc.perform(post("/facturas/5/estado").with(csrf())
                        .param("estado", EstadoFactura.VERIFICADA.name()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/facturas/ver/5"))
                .andExpect(flash().attribute("tipoMensaje", "success"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /facturas/eliminar/{id} redirige a /facturas con mensaje de éxito")
    void eliminar_redirige() throws Exception {
        mockMvc.perform(post("/facturas/eliminar/1").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/facturas"))
                .andExpect(flash().attribute("tipoMensaje", "success"));
    }
}
