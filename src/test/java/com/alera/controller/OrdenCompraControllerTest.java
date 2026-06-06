package com.alera.controller;

import com.alera.config.BrandingProperties;
import com.alera.config.LoginAttemptService;
import com.alera.config.ZymosAccessDeniedHandler;
import com.alera.config.ZymosAuthFailureHandler;
import com.alera.config.ZymosAuthSuccessHandler;
import com.alera.model.OrdenCompra;
import com.alera.model.enums.EstadoOrdenCompra;
import com.alera.repository.TenantRepository;
import com.alera.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrdenCompraController.class)
class OrdenCompraControllerTest {

    @Autowired MockMvc mockMvc;

    // ── Mocks estándar @WebMvcTest ─────────────────────────────────────────
    @MockBean TenantRepository          tenantRepo;
    @MockBean BrandingProperties        brandingProperties;
    @MockBean ZymosAuthSuccessHandler   successHandler;
    @MockBean ZymosAuthFailureHandler   failureHandler;
    @MockBean ZymosAccessDeniedHandler  accessDeniedHandler;
    @MockBean LoginAttemptService       loginAttemptService;
    @MockBean JwtService                jwtService;
    @MockBean UsuarioService            usuarioService;
    @MockBean LogAccesoService          logAccesoService;

    // ── Mocks del controller ──────────────────────────────────────────────
    @MockBean OrdenCompraService        service;
    @MockBean FacturaProveedorService   facturaService;
    @MockBean ProveedorService          proveedorService;
    @MockBean CategoriaInsumoService    categoriaInsumoService;
    @MockBean CategoriaEquipoService    categoriaEquipoService;
    @MockBean PdfExportService          pdfExportService;

    @BeforeEach
    void setUp() {
        WebMvcTestHelper.configureTenantMock(tenantRepo);
        Page<OrdenCompra> vacia = new PageImpl<>(List.of());
        when(service.listarPaginado(any(), anyInt())).thenReturn(vacia);
        when(service.countTotal()).thenReturn(0L);
        when(service.countByEstado(any())).thenReturn(0L);
        when(service.suggest(anyString())).thenReturn(List.of());
        when(proveedorService.listarActivos()).thenReturn(List.of());
        when(categoriaInsumoService.listarNombresActivos()).thenReturn(List.of());
        when(categoriaEquipoService.listarNombresActivos()).thenReturn(List.of());
    }

    // ── Seguridad ──────────────────────────────────────────────────────────

    @Test
    void lista_sinAuth_retorna401() throws Exception {
        mockMvc.perform(get("/ordenes-compra"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /ordenes-compra ────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void lista_conAdmin_retorna200() throws Exception {
        mockMvc.perform(get("/ordenes-compra"))
                .andExpect(status().isOk())
                .andExpect(view().name("ordenes-compra/lista"))
                .andExpect(model().attributeExists("ordenes", "estados", "statsTotal"));
    }

    @Test
    @WithMockUser(roles = "FACTURACION")
    void lista_conFacturacion_retorna200() throws Exception {
        mockMvc.perform(get("/ordenes-compra"))
                .andExpect(status().isOk());
    }

    // ── GET /ordenes-compra/nueva ──────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void nueva_retornaFormulario() throws Exception {
        mockMvc.perform(get("/ordenes-compra/nueva"))
                .andExpect(status().isOk())
                .andExpect(view().name("ordenes-compra/formulario"))
                .andExpect(model().attributeExists("oc", "tiposInsumo", "tiposEquipo", "tiposItem"));
    }

    // ── GET /ordenes-compra/suggest ────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void suggest_retornaJson() throws Exception {
        when(service.suggest("OC-001")).thenReturn(List.of(
                Map.of("titulo", "OC-001", "sub", "Maltería", "estado", "Borrador", "url", "/ordenes-compra/ver/1")
        ));

        mockMvc.perform(get("/ordenes-compra/suggest").param("q", "OC-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titulo").value("OC-001"));
    }

    // ── GET /ordenes-compra/ver/{id} ───────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void ver_retornaDetalle() throws Exception {
        OrdenCompra oc = new OrdenCompra();
        oc.setId(1L);
        oc.setNumeroOc("OC-001");
        oc.setEstado(EstadoOrdenCompra.BORRADOR);
        oc.setFechaEmision(LocalDate.now());
        when(service.buscarPorId(1L)).thenReturn(oc);
        when(service.transicionesValidas(EstadoOrdenCompra.BORRADOR)).thenReturn(
                List.of(EstadoOrdenCompra.ENVIADA, EstadoOrdenCompra.CANCELADA));

        mockMvc.perform(get("/ordenes-compra/ver/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("ordenes-compra/detalle"))
                .andExpect(model().attributeExists("oc", "transicionesValidas"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void ver_noExiste_redirige() throws Exception {
        when(service.buscarPorId(99L)).thenThrow(new RuntimeException("Orden no encontrada: 99"));

        mockMvc.perform(get("/ordenes-compra/ver/99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ordenes-compra"))
                .andExpect(flash().attribute("tipoMensaje", "danger"));
    }

    // ── POST /ordenes-compra/{id}/estado ──────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void cambiarEstado_redirigaAlDetalle() throws Exception {
        OrdenCompra oc = new OrdenCompra();
        oc.setId(1L);
        oc.setEstado(EstadoOrdenCompra.ENVIADA);
        when(service.cambiarEstado(1L, EstadoOrdenCompra.ENVIADA)).thenReturn(oc);

        mockMvc.perform(post("/ordenes-compra/1/estado")
                        .param("nuevoEstado", "ENVIADA")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ordenes-compra/ver/1"));
    }

    // ── POST /ordenes-compra/{id}/eliminar ────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void eliminar_redirigaALista() throws Exception {
        OrdenCompra oc = new OrdenCompra();
        oc.setId(1L);
        oc.setNumeroOc("OC-001");
        when(service.buscarPorId(1L)).thenReturn(oc);

        mockMvc.perform(post("/ordenes-compra/1/eliminar")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ordenes-compra"));
    }

    // ── GET /ordenes-compra/{id}/pdf ──────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void pdf_retornaPdf() throws Exception {
        OrdenCompra oc = new OrdenCompra();
        oc.setId(1L);
        oc.setNumeroOc("OC-001");
        oc.setEstado(EstadoOrdenCompra.BORRADOR);
        oc.setFechaEmision(LocalDate.now());
        when(service.buscarPorId(1L)).thenReturn(oc);
        when(pdfExportService.generarPdfOrdenCompra(any(), any()))
                .thenReturn(new byte[]{0x25, 0x50, 0x44, 0x46});

        mockMvc.perform(get("/ordenes-compra/1/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("OC-001")));
    }
}
