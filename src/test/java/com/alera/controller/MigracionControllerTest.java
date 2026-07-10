package com.alera.controller;

import com.alera.config.*;
import com.alera.model.MigracionLog;
import com.alera.model.Tenant;
import com.alera.repository.TenantRepository;
import com.alera.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MigracionController.class)
@DisplayName("MigracionController")
class MigracionControllerTest {

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
    @MockBean MigracionTemplateService   templateService;
    @MockBean MigracionService           migracionService;

    private Tenant tenantMosto;

    @BeforeEach
    void setUp() {
        WebMvcTestHelper.configureTenantMock(tenantRepo);

        tenantMosto = new Tenant();
        tenantMosto.setSubdomain("mosto");
        tenantMosto.setName("Cervecería Mosto");

        when(tenantRepo.findById("mosto")).thenReturn(Optional.of(tenantMosto));
        when(tenantRepo.findById("noexiste")).thenReturn(Optional.empty());
        when(migracionService.historial(anyString())).thenReturn(List.of());
    }

    // ── Autenticación ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /admin/migracion/{sub} sin autenticar retorna 401")
    void detalle_sinAuth_retorna401() throws Exception {
        mockMvc.perform(get("/admin/migracion/mosto"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /admin/migracion/{sub}/plantilla/{mod} sin autenticar retorna 401")
    void plantilla_sinAuth_retorna401() throws Exception {
        mockMvc.perform(get("/admin/migracion/mosto/plantilla/almacen"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /admin/migracion/{sub}/importar/{mod} sin autenticar retorna 401")
    void importar_sinAuth_retorna401() throws Exception {
        mockMvc.perform(multipart("/admin/migracion/mosto/importar/almacen")
                        .file(archivoXlsx("test.xlsx", new byte[]{1, 2, 3}))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /{subdomain} — página de detalle ──────────────────────────────────

    @Nested
    @DisplayName("GET /{subdomain}")
    class Detalle {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("con tenant existente retorna 200 y vista correcta")
        void tenantExiste_retorna200() throws Exception {
            mockMvc.perform(get("/admin/migracion/mosto"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/migracion/detalle"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("con tenant existente expone tenant e historial en el modelo")
        void tenantExiste_exponeModeloBase() throws Exception {
            mockMvc.perform(get("/admin/migracion/mosto"))
                    .andExpect(model().attributeExists("tenant", "historial",
                            "totalImportaciones", "importacionesExitosas", "importacionesParciales"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("con historial vacío las estadísticas son cero")
        void historialVacio_estadisticasCero() throws Exception {
            when(migracionService.historial("mosto")).thenReturn(List.of());

            mockMvc.perform(get("/admin/migracion/mosto"))
                    .andExpect(model().attribute("totalImportaciones",    0))
                    .andExpect(model().attribute("importacionesExitosas", 0L))
                    .andExpect(model().attribute("importacionesParciales",0L));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("con historial mixto calcula estadísticas correctamente")
        void historialMixto_calculaEstadisticas() throws Exception {
            List<MigracionLog> logs = List.of(
                    logDeEstado("EXITOSO"),
                    logDeEstado("EXITOSO"),
                    logDeEstado("PARCIAL"),
                    logDeEstado("FALLIDO")
            );
            when(migracionService.historial("mosto")).thenReturn(logs);

            mockMvc.perform(get("/admin/migracion/mosto"))
                    .andExpect(model().attribute("totalImportaciones",    4))
                    .andExpect(model().attribute("importacionesExitosas", 2L))
                    .andExpect(model().attribute("importacionesParciales",1L));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("con tenant inexistente redirige a /admin/tenants con flash danger")
        void tenantNoExiste_redirige() throws Exception {
            mockMvc.perform(get("/admin/migracion/noexiste"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/tenants"))
                    .andExpect(flash().attribute("tipoMensaje", "danger"));
        }
    }

    // ── GET /{subdomain}/plantilla/{modulo} ───────────────────────────────────

    @Nested
    @DisplayName("GET /{subdomain}/plantilla/{modulo}")
    class DescargaPlantilla {

        private static final byte[] XLSX_MAGIC = {0x50, 0x4B, 0x03, 0x04, 1, 2, 3};

        @BeforeEach
        void stubPlantillas() throws Exception {
            when(templateService.plantillaAlmacen()).thenReturn(XLSX_MAGIC);
            when(templateService.plantillaEquipos()).thenReturn(XLSX_MAGIC);
            when(templateService.plantillaComercial()).thenReturn(XLSX_MAGIC);
            when(templateService.plantillaProduccion()).thenReturn(XLSX_MAGIC);
            when(templateService.plantillaClientes()).thenReturn(XLSX_MAGIC);
            when(templateService.plantillaVentas()).thenReturn(XLSX_MAGIC);
            when(templateService.plantillaBarriles()).thenReturn(XLSX_MAGIC);
            when(templateService.plantillaOrdenes()).thenReturn(XLSX_MAGIC);
            when(templateService.plantillaSeguimiento()).thenReturn(XLSX_MAGIC);
            when(templateService.plantillaCatalogos()).thenReturn(XLSX_MAGIC);
            when(templateService.plantillaMantenimientos()).thenReturn(XLSX_MAGIC);
        }

        @ParameterizedTest(name = "módulo {0} retorna XLSX")
        @ValueSource(strings = {
                "almacen", "equipos", "comercial", "produccion",
                "clientes", "ventas", "barriles", "ordenes",
                "seguimiento", "catalogos", "mantenimientos"
        })
        @WithMockUser(roles = "ADMIN")
        void modulo_conocido_retornaXlsx(String modulo) throws Exception {
            mockMvc.perform(get("/admin/migracion/mosto/plantilla/" + modulo))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        }

        @ParameterizedTest(name = "módulo {0} incluye nombre de tenant en Content-Disposition")
        @ValueSource(strings = {"almacen", "equipos", "produccion"})
        @WithMockUser(roles = "ADMIN")
        void modulo_conocido_incluyeTenantEnFilename(String modulo) throws Exception {
            mockMvc.perform(get("/admin/migracion/mosto/plantilla/" + modulo))
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                            containsString("mosto")));
        }

        @ParameterizedTest(name = "módulo {0} incluye nombre de módulo en Content-Disposition")
        @ValueSource(strings = {"almacen", "equipos", "produccion"})
        @WithMockUser(roles = "ADMIN")
        void modulo_conocido_incluyeModuloEnFilename(String modulo) throws Exception {
            mockMvc.perform(get("/admin/migracion/mosto/plantilla/" + modulo))
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                            containsString(modulo)));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("módulo desconocido retorna 400 (RuntimeException → GlobalExceptionHandler)")
        void moduloDesconocido_retornaError() throws Exception {
            mockMvc.perform(get("/admin/migracion/mosto/plantilla/invalido"))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("plantilla almacén llama al templateService correctamente")
        void plantillaAlmacen_llamaTemplateService() throws Exception {
            mockMvc.perform(get("/admin/migracion/mosto/plantilla/almacen"))
                    .andExpect(status().isOk());
            verify(templateService).plantillaAlmacen();
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("plantilla producción llama al templateService correctamente")
        void plantillaProduccion_llamaTemplateService() throws Exception {
            mockMvc.perform(get("/admin/migracion/mosto/plantilla/produccion"))
                    .andExpect(status().isOk());
            verify(templateService).plantillaProduccion();
        }
    }

    // ── POST /{subdomain}/importar/{modulo} ───────────────────────────────────

    @Nested
    @DisplayName("POST /{subdomain}/importar/{modulo}")
    class Importar {

        @Test
        @WithMockUser(roles = "ADMIN", username = "admin")
        @DisplayName("archivo vacío redirige con tipoMensaje warning sin llamar al servicio")
        void archivoVacio_redirige_conWarning() throws Exception {
            MockMultipartFile vacio = archivoXlsx("vacio.xlsx", new byte[0]);

            mockMvc.perform(multipart("/admin/migracion/mosto/importar/almacen")
                            .file(vacio)
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/migracion/mosto"))
                    .andExpect(flash().attribute("tipoMensaje", "warning"));

            verifyNoInteractions(migracionService);
        }

        @Test
        @WithMockUser(roles = "ADMIN", username = "admin")
        @DisplayName("importación exitosa redirige con tipoMensaje success")
        void importacionExitosa_redirige_conSuccess() throws Exception {
            MigracionService.Resultado exitoso = new MigracionService.Resultado(
                    5, 5, 0, List.of(), "EXITOSO");
            when(migracionService.importarAlmacen(any(), eq("mosto"), eq("admin")))
                    .thenReturn(exitoso);

            mockMvc.perform(multipart("/admin/migracion/mosto/importar/almacen")
                            .file(archivoXlsx("datos.xlsx", new byte[]{1, 2, 3}))
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/migracion/mosto"))
                    .andExpect(flash().attribute("tipoMensaje", "success"));
        }

        @Test
        @WithMockUser(roles = "ADMIN", username = "admin")
        @DisplayName("importación parcial redirige con tipoMensaje warning")
        void importacionParcial_redirige_conWarning() throws Exception {
            MigracionService.Resultado parcial = new MigracionService.Resultado(
                    10, 7, 3, List.of("Error fila 4", "Error fila 8"), "PARCIAL");
            when(migracionService.importarAlmacen(any(), eq("mosto"), eq("admin")))
                    .thenReturn(parcial);

            mockMvc.perform(multipart("/admin/migracion/mosto/importar/almacen")
                            .file(archivoXlsx("datos.xlsx", new byte[]{1, 2, 3}))
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/migracion/mosto"))
                    .andExpect(flash().attribute("tipoMensaje", "warning"));
        }

        @Test
        @WithMockUser(roles = "ADMIN", username = "admin")
        @DisplayName("importación fallida redirige con tipoMensaje danger")
        void importacionFallida_redirige_conDanger() throws Exception {
            MigracionService.Resultado fallido = new MigracionService.Resultado(
                    5, 0, 5, List.of("Formato inválido"), "FALLIDO");
            when(migracionService.importarAlmacen(any(), eq("mosto"), eq("admin")))
                    .thenReturn(fallido);

            mockMvc.perform(multipart("/admin/migracion/mosto/importar/almacen")
                            .file(archivoXlsx("datos.xlsx", new byte[]{1, 2, 3}))
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/migracion/mosto"))
                    .andExpect(flash().attribute("tipoMensaje", "danger"));
        }

        @Test
        @WithMockUser(roles = "ADMIN", username = "admin")
        @DisplayName("excepción en servicio redirige con tipoMensaje danger")
        void excepcionEnServicio_redirige_conDanger() throws Exception {
            when(migracionService.importarAlmacen(any(), any(), any()))
                    .thenThrow(new RuntimeException("Error inesperado"));

            mockMvc.perform(multipart("/admin/migracion/mosto/importar/almacen")
                            .file(archivoXlsx("datos.xlsx", new byte[]{1, 2, 3}))
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/migracion/mosto"))
                    .andExpect(flash().attribute("tipoMensaje", "danger"));
        }

        @Test
        @WithMockUser(roles = "ADMIN", username = "admin")
        @DisplayName("módulo desconocido redirige con tipoMensaje danger")
        void moduloDesconocido_redirige_conDanger() throws Exception {
            mockMvc.perform(multipart("/admin/migracion/mosto/importar/invalido")
                            .file(archivoXlsx("datos.xlsx", new byte[]{1, 2, 3}))
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/migracion/mosto"))
                    .andExpect(flash().attribute("tipoMensaje", "danger"));
        }

        @ParameterizedTest(name = "módulo {0} enruta al método de servicio correcto")
        @ValueSource(strings = {
                "almacen", "equipos", "comercial", "produccion",
                "clientes", "ventas", "barriles", "ordenes",
                "seguimiento", "catalogos", "mantenimientos"
        })
        @WithMockUser(roles = "ADMIN", username = "admin")
        void todosLosModulos_enrutanAlServicio(String modulo) throws Exception {
            MigracionService.Resultado ok = new MigracionService.Resultado(
                    1, 1, 0, List.of(), "EXITOSO");
            stubImportar(modulo, ok);

            mockMvc.perform(multipart("/admin/migracion/mosto/importar/" + modulo)
                            .file(archivoXlsx("datos.xlsx", new byte[]{1, 2, 3}))
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/migracion/mosto"))
                    .andExpect(flash().attribute("tipoMensaje", "success"));
        }

        @Test
        @WithMockUser(roles = "ADMIN", username = "admin")
        @DisplayName("el usuario autenticado se pasa como parámetro al servicio")
        void usuarioAutenticado_pasa_alServicio() throws Exception {
            MigracionService.Resultado ok = new MigracionService.Resultado(
                    1, 1, 0, List.of(), "EXITOSO");
            when(migracionService.importarEquipos(any(), eq("mosto"), eq("admin")))
                    .thenReturn(ok);

            mockMvc.perform(multipart("/admin/migracion/mosto/importar/equipos")
                            .file(archivoXlsx("datos.xlsx", new byte[]{1, 2, 3}))
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection());

            verify(migracionService).importarEquipos(any(), eq("mosto"), eq("admin"));
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static MockMultipartFile archivoXlsx(String filename, byte[] content) {
        return new MockMultipartFile("archivo", filename,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                content);
    }

    private static MigracionLog logDeEstado(String estado) {
        return MigracionLog.of("mosto", "almacen", "test.xlsx",
                5, "EXITOSO".equals(estado) ? 5 : 0,
                "EXITOSO".equals(estado) ? 0 : 5,
                estado, null, "admin");
    }

    private void stubImportar(String modulo,
                               MigracionService.Resultado resultado) throws Exception {
        switch (modulo) {
            case "almacen"        -> when(migracionService.importarAlmacen(any(), any(), any())).thenReturn(resultado);
            case "equipos"        -> when(migracionService.importarEquipos(any(), any(), any())).thenReturn(resultado);
            case "comercial"      -> when(migracionService.importarComercial(any(), any(), any())).thenReturn(resultado);
            case "produccion"     -> when(migracionService.importarProduccion(any(), any(), any())).thenReturn(resultado);
            case "clientes"       -> when(migracionService.importarClientes(any(), any(), any())).thenReturn(resultado);
            case "ventas"         -> when(migracionService.importarVentas(any(), any(), any())).thenReturn(resultado);
            case "barriles"       -> when(migracionService.importarBarriles(any(), any(), any())).thenReturn(resultado);
            case "ordenes"        -> when(migracionService.importarOrdenes(any(), any(), any())).thenReturn(resultado);
            case "seguimiento"    -> when(migracionService.importarSeguimiento(any(), any(), any())).thenReturn(resultado);
            case "catalogos"      -> when(migracionService.importarCatalogos(any(), any(), any())).thenReturn(resultado);
            case "mantenimientos" -> when(migracionService.importarMantenimientos(any(), any(), any())).thenReturn(resultado);
        }
    }
}
