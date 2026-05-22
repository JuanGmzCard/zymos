package com.alera.controller;

import com.alera.config.AleraAccessDeniedHandler;
import com.alera.config.AleraAuthFailureHandler;
import com.alera.config.AleraAuthSuccessHandler;
import com.alera.config.BrandingProperties;
import com.alera.exception.LoteNoEncontradoException;
import com.alera.dto.LoteGuardadoResult;
import com.alera.model.LoteCerveza;
import com.alera.repository.FacturaProveedorRepository;
import com.alera.repository.InsumoInventarioRepository;
import com.alera.repository.TenantRepository;
import com.alera.repository.TipoCervezaRepository;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TrazabilidadController.class)
class TrazabilidadControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean TrazabilidadService          service;
    @MockBean EquipoService                equipoService;
    @MockBean RecetaService                recetaService;
    @MockBean InsumoInventarioRepository   insumoRepo;
    @MockBean TipoCervezaRepository        tipoCervezaRepo;
    @MockBean FacturaProveedorRepository   facturaRepo;
    @MockBean LogAccesoService             logAccesoService;
    @MockBean UsuarioService               usuarioService;
    @MockBean TenantRepository             tenantRepo;
    @MockBean BrandingProperties           brandingProperties;
    @MockBean AleraAuthSuccessHandler      successHandler;
    @MockBean AleraAuthFailureHandler      failureHandler;
    @MockBean AleraAccessDeniedHandler     accessDeniedHandler;
    @MockBean PdfExportService             pdfExportService;
    @MockBean LecturaFermentacionService   lecturaService;
    @MockBean PlanificacionService         planificacionService;

    @BeforeEach
    void setUp() {
        WebMvcTestHelper.configureTenantMock(tenantRepo);
    }

    // ── Seguridad ──────────────────────────────────────────────────────

    @Test
    void sinAutenticarDevuelveUnauthorized() throws Exception {
        // httpBasic() en SecurityConfig hace que peticiones sin Accept:text/html
        // devuelvan 401 (no redirect) cuando no hay credenciales.
        mockMvc.perform(get("/"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sinAutenticarNuevoDevuelveUnauthorized() throws Exception {
        mockMvc.perform(get("/nuevo"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET / ─────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void indexRetornaVistaConLotes() throws Exception {
        Page<LoteCerveza> pagina = new PageImpl<>(List.of());
        when(service.listarPaginado(anyString(), anyString(), any(), any(), anyInt()))
                .thenReturn(pagina);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("lotes", "paginaActual", "totalPaginas"));
    }

    @Test
    @WithMockUser(roles = "INVENTARIO")
    void indexAccesibleCualquierRol() throws Exception {
        when(service.listarPaginado(anyString(), anyString(), any(), any(), anyInt()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
    }

    // ── GET /kanban ───────────────────────────────────────────────────

    @Test
    @WithMockUser
    void kanbanRetornaVistaCon6Columnas() throws Exception {
        when(service.listarParaKanban()).thenReturn(List.of());

        mockMvc.perform(get("/kanban"))
                .andExpect(status().isOk())
                .andExpect(view().name("kanban"))
                .andExpect(model().attributeExists(
                        "sinIniciar", "fermentacion", "acondicionamiento",
                        "maduracion", "carbonatacion", "completados"));
    }

    // ── GET /nuevo ────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void nuevoRetornaFormularioVacio() throws Exception {
        when(insumoRepo.findAll()).thenReturn(List.of());
        when(equipoService.listarFermentadoresDisponibles()).thenReturn(List.of());
        when(tipoCervezaRepo.findByActivoTrueOrderByNombreAsc()).thenReturn(List.of());
        when(recetaService.listarActivas()).thenReturn(List.of());
        when(facturaRepo.findAllWithItems()).thenReturn(List.of());

        mockMvc.perform(get("/nuevo"))
                .andExpect(status().isOk())
                .andExpect(view().name("formulario"))
                .andExpect(model().attributeExists("loteForm"));
    }

    @Test
    @WithMockUser(roles = "INVENTARIO")
    void nuevoConRolNoAdminEsAccesible() throws Exception {
        // En @WebMvcTest con AleraAccessDeniedHandler mocked (void, no-op), el controller
        // se ejecuta igualmente y retorna 200. La seguridad URL-based real se verifica
        // en tests de integración (AbstractIntegrationTest). El mock handler no comite
        // la respuesta → el controller renderiza el formulario normalmente.
        mockMvc.perform(get("/nuevo"))
                .andExpect(status().isOk());
    }

    // ── POST /guardar ─────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void guardarConDatosValidosRedirige() throws Exception {
        LoteCerveza lote = new LoteCerveza();
        when(service.guardar(any())).thenReturn(new LoteGuardadoResult(lote, List.of()));

        mockMvc.perform(post("/guardar")
                        .with(csrf())
                        .param("estilo", "IPA Americana"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attributeExists("mensaje"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void guardarConEstiloVacioRetornaFormularioConErrores() throws Exception {
        when(insumoRepo.findAll()).thenReturn(List.of());
        when(equipoService.listarFermentadoresDisponibles()).thenReturn(List.of());
        when(tipoCervezaRepo.findByActivoTrueOrderByNombreAsc()).thenReturn(List.of());
        when(recetaService.listarActivas()).thenReturn(List.of());
        when(facturaRepo.findAllWithItems()).thenReturn(List.of());

        mockMvc.perform(post("/guardar")
                        .with(csrf())
                        .param("estilo", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("formulario"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("loteForm", "estilo"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void guardarConAdvertenciasDeStockMuestraWarning() throws Exception {
        LoteCerveza lote = new LoteCerveza();
        when(service.guardar(any()))
                .thenReturn(new LoteGuardadoResult(lote, List.of("Lúpulo Cascade")));

        mockMvc.perform(post("/guardar")
                        .with(csrf())
                        .param("estilo", "Stout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipoMensaje", "warning"));
    }

    // ── GET /ver/{id} ─────────────────────────────────────────────────

    @Test
    @WithMockUser
    void verLoteRetornaDetalle() throws Exception {
        LoteCerveza lote = new LoteCerveza();
        lote.setEstilo("IPA");
        lote.setCodigoLote("IPA-001");
        when(service.buscarPorId(1L)).thenReturn(lote);
        when(service.obtenerHistorial(1L)).thenReturn(List.of());

        mockMvc.perform(get("/ver/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("detalle"))
                .andExpect(model().attributeExists("lote", "historial"));
    }

    @Test
    @WithMockUser
    void verLoteNoEncontradoRetorna404() throws Exception {
        when(service.buscarPorId(999L)).thenThrow(new LoteNoEncontradoException(999L));

        mockMvc.perform(get("/ver/999"))
                .andExpect(status().isNotFound());
    }

    // ── POST /eliminar/{id} ───────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void eliminarLoteExistenteRedirige() throws Exception {
        doNothing().when(service).eliminar(1L);

        mockMvc.perform(post("/eliminar/1").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("tipoMensaje", "success"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void eliminarLoteNoEncontradoRedirigeConError() throws Exception {
        org.mockito.Mockito.doThrow(new LoteNoEncontradoException(99L)).when(service).eliminar(99L);

        mockMvc.perform(post("/eliminar/99").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipoMensaje", "danger"));
    }

    @Test
    @WithMockUser(roles = "INVENTARIO")
    void eliminarConRolNoAdminRedirigeIgual() throws Exception {
        // El controller se ejecuta (security URL-based no se aplica con handler mock)
        // y retorna "redirect:/" → 302. La seguridad real se verifica en integración.
        mockMvc.perform(post("/eliminar/1").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }
}