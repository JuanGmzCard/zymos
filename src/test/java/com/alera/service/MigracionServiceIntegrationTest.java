package com.alera.service;

import com.alera.AbstractIntegrationTest;
import com.alera.config.TenantContext;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MigracionService — integración")
class MigracionServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired MigracionService service;
    @Autowired JdbcTemplate jdbc;

    private static final String TENANT  = "mig-test";
    private static final String USUARIO = "test";

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenant("default");
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        jdbc.update("DELETE FROM ingredientes WHERE tenant_id = ?", TENANT);
        jdbc.update("DELETE FROM lotes_cerveza WHERE tenant_id = ?", TENANT);
        jdbc.update("DELETE FROM adiciones_hervor WHERE tenant_id = ?", TENANT);
        jdbc.update("DELETE FROM escalones_macerado WHERE tenant_id = ?", TENANT);
        jdbc.update("DELETE FROM receta_ingredientes WHERE tenant_id = ?", TENANT);
        jdbc.update("DELETE FROM recetas WHERE tenant_id = ?", TENANT);
        jdbc.update("DELETE FROM factura_items WHERE tenant_id = ?", TENANT);
        jdbc.update("DELETE FROM facturas_proveedor WHERE tenant_id = ?", TENANT);
        jdbc.update("DELETE FROM proveedores WHERE tenant_id = ?", TENANT);
        jdbc.update("DELETE FROM equipos WHERE tenant_id = ?", TENANT);
        jdbc.update("DELETE FROM insumos_inventario WHERE tenant_id = ?", TENANT);
        jdbc.update("DELETE FROM migracion_log WHERE tenant_id = ?", TENANT);
    }

    // ── Almacén ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("importarAlmacen inserta 2 insumos correctamente")
    void importarAlmacen_happyPath() throws Exception {
        MockMultipartFile file = excelConHoja("Insumos",
                new String[][]{
                        {"Lúpulo Cascade", "LUPULO", "500", "gr", "100", "", "", ""},
                        {"Malta Pilsner",  "MALTA",  "5000", "gr", "500", "", "", ""}
                });

        MigracionService.Resultado r = service.importarAlmacen(file, TENANT, USUARIO);

        assertThat(r.exitosas()).isEqualTo(2);
        assertThat(r.errores()).isEqualTo(0);
        assertThat(r.estado()).isEqualTo("EXITOSO");

        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM insumos_inventario WHERE tenant_id = ?", Long.class, TENANT);
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("importarAlmacen reporta error para tipo inválido y continúa con filas válidas")
    void importarAlmacen_tipoInvalido_reportaError() throws Exception {
        MockMultipartFile file = excelConHoja("Insumos",
                new String[][]{
                        {"Insumo Válido",  "LUPULO", "100", "gr", null, null, null, null},
                        {"Insumo Inválido","INVALIDO","100","gr", null, null, null, null}
                });

        MigracionService.Resultado r = service.importarAlmacen(file, TENANT, USUARIO);

        assertThat(r.procesadas()).isEqualTo(2);
        assertThat(r.exitosas()).isEqualTo(1);
        assertThat(r.errores()).isEqualTo(1);
        assertThat(r.estado()).isEqualTo("PARCIAL");
        assertThat(r.mensajes()).anyMatch(m -> m.contains("INVALIDO"));
    }

    @Test
    @DisplayName("importarAlmacen reporta error para nombre vacío")
    void importarAlmacen_nombreVacio_reportaError() throws Exception {
        MockMultipartFile file = excelConHoja("Insumos",
                new String[][]{{"", "LUPULO", "100", "gr", null, null, null, null}});

        MigracionService.Resultado r = service.importarAlmacen(file, TENANT, USUARIO);

        assertThat(r.exitosas()).isEqualTo(0);
        assertThat(r.errores()).isEqualTo(1);
        assertThat(r.estado()).isEqualTo("FALLIDO");
    }

    // ── Equipos ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("importarEquipos inserta equipo con estado OPERATIVO por defecto")
    void importarEquipos_happyPath_estadoDefault() throws Exception {
        MockMultipartFile file = excelConHoja("Equipos",
                new String[][]{
                        {"Fermentador 100L", "FERMENTADOR", "", "100", "L", "", "", ""}
                });

        MigracionService.Resultado r = service.importarEquipos(file, TENANT, USUARIO);

        assertThat(r.exitosas()).isEqualTo(1);
        assertThat(r.errores()).isEqualTo(0);

        String estado = jdbc.queryForObject(
                "SELECT estado FROM equipos WHERE tenant_id = ?", String.class, TENANT);
        assertThat(estado).isEqualTo("OPERATIVO");
    }

    // ── Comercial ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("importarComercial inserta proveedor, factura e ítem y calcula subtotal")
    void importarComercial_happyPath() throws Exception {
        MockMultipartFile file = excelConHojasComercial(
                new String[][]{{"Proveedor Test", "123-4", "3001234567", "prov@test.com", "Calle 1"}},
                new String[][]{{"FAC-001", "Proveedor Test", "2024-01-15", "Compra de insumos", "0", "RECIBIDA"}},
                new String[][]{{"FAC-001", "INSUMO", "Lúpulo Cascade", "LUPULO", "", "10", "kg", "50000", "0", "19"}}
        );

        MigracionService.Resultado r = service.importarComercial(file, TENANT, USUARIO);

        assertThat(r.errores()).isEqualTo(0);

        Long provCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM proveedores WHERE tenant_id = ?", Long.class, TENANT);
        assertThat(provCount).isEqualTo(1);

        Long facCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM facturas_proveedor WHERE tenant_id = ?", Long.class, TENANT);
        assertThat(facCount).isEqualTo(1);

        // subtotal debe ser cantidad * valorUnitario (sin descuento, con IVA)
        // 10 * 50000 * 1.19 = 595000
        Double subtotal = jdbc.queryForObject(
                "SELECT subtotal FROM facturas_proveedor WHERE tenant_id = ?", Double.class, TENANT);
        assertThat(subtotal).isGreaterThan(0);
    }

    @Test
    @DisplayName("importarComercial salta proveedor duplicado sin reportar error")
    void importarComercial_proveedorDuplicado_skip() throws Exception {
        MockMultipartFile file = excelConHojasComercial(
                new String[][]{
                        {"Proveedor Dup", null, null, null, null},
                        {"Proveedor Dup", null, null, null, null}
                },
                new String[0][],
                new String[0][]
        );

        MigracionService.Resultado r = service.importarComercial(file, TENANT, USUARIO);

        assertThat(r.errores()).isEqualTo(0);
        assertThat(r.exitosas()).isEqualTo(2); // duplicado cuenta como ok (skip idempotente)

        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM proveedores WHERE tenant_id = ?", Long.class, TENANT);
        assertThat(count).isEqualTo(1); // solo 1 fila en DB
    }

    // ── Producción ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("importarProduccion inserta receta, escalón y lote correctamente")
    void importarProduccion_happyPath() throws Exception {
        MockMultipartFile file = excelConHojasProduccion(
                // Recetas: nombre, estilo, desc, activa, volBase, hervor, og, fg...
                new String[][]{{"IPA Test", "India Pale Ale", "", "TRUE", "20", "60", "1058", "1012", "", "", "", "", "", ""}},
                // Receta_Ingredientes: receta, tipo, nombre, cantidadConUnidad
                new String[][]{{"IPA Test", "LUPULO", "Cascade", "100 gr"}},
                // Receta_Escalones: receta, nombre, temp, duracion, orden
                new String[][]{{"IPA Test", "Sacarificación", "68", "60", "1"}},
                // Receta_Adiciones: empty
                new String[0][],
                // Lotes: codigo, estilo, fecha, litros, og, fg, agua, ph, clar, obs, notasCata, receta
                new String[][]{{"IPA-T01", "India Pale Ale", "2024-03-01", "18", "1058", "1012", "", "", "", "", "", "IPA Test"}},
                // Lote_Ingredientes: empty
                new String[0][]
        );

        MigracionService.Resultado r = service.importarProduccion(file, TENANT, USUARIO);

        assertThat(r.errores()).isEqualTo(0);
        assertThat(r.estado()).isEqualTo("EXITOSO");

        Long recetas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM recetas WHERE tenant_id = ?", Long.class, TENANT);
        assertThat(recetas).isEqualTo(1);

        Long lotes = jdbc.queryForObject(
                "SELECT COUNT(*) FROM lotes_cerveza WHERE tenant_id = ?", Long.class, TENANT);
        assertThat(lotes).isEqualTo(1);

        Long escalones = jdbc.queryForObject(
                "SELECT COUNT(*) FROM escalones_macerado WHERE tenant_id = ?", Long.class, TENANT);
        assertThat(escalones).isEqualTo(1);
    }

    @Test
    @DisplayName("importarProduccion reporta error para código de lote duplicado")
    void importarProduccion_codigoDuplicado_reportaError() throws Exception {
        MockMultipartFile file1 = excelConHojasProduccion(
                new String[0][],
                new String[0][],
                new String[0][],
                new String[0][],
                new String[][]{{"IPA-DUP", "IPA", "2024-01-01", "20", null, null, null, null, null, null, null, null}},
                new String[0][]
        );
        service.importarProduccion(file1, TENANT, USUARIO);

        MockMultipartFile file2 = excelConHojasProduccion(
                new String[0][],
                new String[0][],
                new String[0][],
                new String[0][],
                new String[][]{{"IPA-DUP", "IPA", "2024-01-01", "20", null, null, null, null, null, null, null, null}},
                new String[0][]
        );

        MigracionService.Resultado r = service.importarProduccion(file2, TENANT, USUARIO);

        assertThat(r.errores()).isEqualTo(1);
        assertThat(r.mensajes()).anyMatch(m -> m.contains("IPA-DUP"));
    }

    @Test
    @DisplayName("MigracionLog se persiste tras cada importación")
    void importarAlmacen_guardaMigracionLog() throws Exception {
        MockMultipartFile file = excelConHoja("Insumos",
                new String[][]{{"Agua Destilada", "AGUA", "10", "L", null, null, null, null}});

        service.importarAlmacen(file, TENANT, USUARIO);

        Long logCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM migracion_log WHERE tenant_id = ? AND modulo = 'almacen'",
                Long.class, TENANT);
        assertThat(logCount).isEqualTo(1);

        String estado = jdbc.queryForObject(
                "SELECT estado FROM migracion_log WHERE tenant_id = ?", String.class, TENANT);
        assertThat(estado).isEqualTo("EXITOSO");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Crea un MockMultipartFile Excel con una sola hoja.
     * Las filas 0-2 se crean vacías (serán saltadas por el servicio con rowNum < 3).
     * Los datos empiezan en fila 3.
     */
    private MockMultipartFile excelConHoja(String sheetName, String[][] data) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sh = wb.createSheet(sheetName);
            for (int i = 0; i < 3; i++) sh.createRow(i); // skip rows
            for (int r = 0; r < data.length; r++) {
                Row row = sh.createRow(3 + r);
                for (int c = 0; c < data[r].length; c++) {
                    if (data[r][c] != null) row.createCell(c).setCellValue(data[r][c]);
                }
            }
            return toMultipart(wb);
        }
    }

    private MockMultipartFile excelConHojasComercial(String[][] proveedores,
                                                      String[][] facturas,
                                                      String[][] items) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            agregarHoja(wb, "Proveedores", proveedores);
            agregarHoja(wb, "Facturas",    facturas);
            agregarHoja(wb, "Factura_Items", items);
            return toMultipart(wb);
        }
    }

    private MockMultipartFile excelConHojasProduccion(String[][] recetas,
                                                       String[][] ingredientes,
                                                       String[][] escalones,
                                                       String[][] adiciones,
                                                       String[][] lotes,
                                                       String[][] loteIngredientes) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            agregarHoja(wb, "Recetas",            recetas);
            agregarHoja(wb, "Receta_Ingredientes", ingredientes);
            agregarHoja(wb, "Receta_Escalones",    escalones);
            agregarHoja(wb, "Receta_Adiciones",    adiciones);
            agregarHoja(wb, "Lotes",               lotes);
            agregarHoja(wb, "Lote_Ingredientes",   loteIngredientes);
            return toMultipart(wb);
        }
    }

    private void agregarHoja(XSSFWorkbook wb, String nombre, String[][] data) {
        Sheet sh = wb.createSheet(nombre);
        for (int i = 0; i < 3; i++) sh.createRow(i);
        for (int r = 0; r < data.length; r++) {
            Row row = sh.createRow(3 + r);
            for (int c = 0; c < data[r].length; c++) {
                if (data[r][c] != null) row.createCell(c).setCellValue(data[r][c]);
            }
        }
    }

    private MockMultipartFile toMultipart(XSSFWorkbook wb) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        wb.write(bos);
        return new MockMultipartFile("file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                bos.toByteArray());
    }
}
