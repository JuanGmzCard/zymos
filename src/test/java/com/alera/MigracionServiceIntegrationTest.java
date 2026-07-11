package com.alera;

import com.alera.service.MigracionService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests de integración para MigracionService.
 * Verifica que cada módulo importa datos correctamente contra una BD PostgreSQL real.
 *
 * Cada test crea su propio Excel en memoria, importa y valida via JDBC.
 * @AfterEach limpia todas las tablas del tenant de prueba.
 */
@DisplayName("MigracionService — integración")
class MigracionServiceIntegrationTest extends AbstractIntegrationTest {

    static final String TENANT = "mig-test";
    static final String USUARIO = "test-runner";

    @Autowired MigracionService migracionService;
    @Autowired JdbcTemplate     jdbc;

    @AfterEach
    void cleanup() {
        // Orden: hijos antes que padres
        jdbc.update("DELETE FROM lote_items_factura        WHERE tenant_id=?", TENANT);
        jdbc.update("DELETE FROM lecturas_fermentacion      WHERE tenant_id=?", TENANT);
        jdbc.update("DELETE FROM evaluaciones_sensoriales   WHERE tenant_id=?", TENANT);
        jdbc.update("DELETE FROM ingredientes               WHERE tenant_id=?", TENANT);
        jdbc.update("DELETE FROM lotes_cerveza              WHERE tenant_id=?", TENANT);
        jdbc.update("DELETE FROM elaboraciones_planificadas WHERE tenant_id=?", TENANT);
        jdbc.update("DELETE FROM adiciones_hervor           WHERE tenant_id=?", TENANT);
        jdbc.update("DELETE FROM escalones_macerado         WHERE tenant_id=?", TENANT);
        jdbc.update("DELETE FROM receta_ingredientes        WHERE tenant_id=?", TENANT);
        jdbc.update("DELETE FROM recetas                    WHERE tenant_id=?", TENANT);
        jdbc.update("DELETE FROM mantenimientos_equipo      WHERE tenant_id=?", TENANT);
        jdbc.update("DELETE FROM equipos                    WHERE tenant_id=?", TENANT);
        jdbc.update("DELETE FROM movimientos_inventario     WHERE tenant_id=?", TENANT);
        jdbc.update("DELETE FROM insumos_inventario         WHERE tenant_id=?", TENANT);
        jdbc.update("DELETE FROM factura_items              WHERE tenant_id=?", TENANT);
        jdbc.update("DELETE FROM facturas_proveedor         WHERE tenant_id=?", TENANT);
        jdbc.update("DELETE FROM proveedores                WHERE tenant_id=?", TENANT);
        jdbc.update("DELETE FROM venta_items                WHERE tenant_id=?", TENANT);
        jdbc.update("DELETE FROM ventas                     WHERE tenant_id=?", TENANT);
        jdbc.update("DELETE FROM clientes                   WHERE tenant_id=?", TENANT);
        jdbc.update("DELETE FROM orden_compra_items         WHERE tenant_id=?", TENANT);
        jdbc.update("DELETE FROM ordenes_compra             WHERE tenant_id=?", TENANT);
        jdbc.update("DELETE FROM barriles                   WHERE tenant_id=?", TENANT);
        jdbc.update("DELETE FROM tipos_cerveza              WHERE tenant_id=?", TENANT);
        jdbc.update("DELETE FROM tipos_insumo               WHERE tenant_id=?", TENANT);
        jdbc.update("DELETE FROM tipos_equipo               WHERE tenant_id=?", TENANT);
        jdbc.update("DELETE FROM migracion_log              WHERE tenant_id=?", TENANT);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Convierte un workbook a MockMultipartFile listo para pasar a MigracionService */
    private MockMultipartFile toFile(XSSFWorkbook wb, String modulo) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();
        return new MockMultipartFile("archivo", "test-" + modulo + ".xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                out.toByteArray());
    }

    /** Crea la fila de datos (índice 3+) saltando header/leyenda/ejemplo */
    private Row fila(Sheet sh, int offset) {
        return sh.createRow(3 + offset);
    }

    /** Escribe un valor en una celda; soporta Number, Boolean y String */
    private void set(Row r, int col, Object val) {
        Cell c = r.createCell(col);
        if (val == null) return;
        if (val instanceof Number n) c.setCellValue(n.doubleValue());
        else if (val instanceof Boolean b) c.setCellValue(b);
        else c.setCellValue(val.toString());
    }

    /** Crea las 3 filas de cabecera que el importador salta (getRowNum < 3) */
    private Sheet hoja(XSSFWorkbook wb, String nombre) {
        Sheet sh = wb.createSheet(nombre);
        sh.createRow(0); // header
        sh.createRow(1); // leyenda
        sh.createRow(2); // ejemplo (skipped)
        return sh;
    }

    private long count(String tabla) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + tabla + " WHERE tenant_id=?", Long.class, TENANT);
    }

    // ── Almacén ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("importarAlmacen — 2 insumos importados correctamente")
    void almacen_dosInsumos() throws Exception {
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sh = hoja(wb, "Insumos");

        Row r1 = fila(sh, 0);
        set(r1, 0, "Malta Pilsner Test"); set(r1, 1, "MALTA");
        set(r1, 2, 5000); set(r1, 3, "gr");
        set(r1, 4, 500); set(r1, 5, "Maltería Sur");
        set(r1, 6, "2025-12-31"); set(r1, 7, "Lote fresco");

        Row r2 = fila(sh, 1);
        set(r2, 0, "Cascade Test"); set(r2, 1, "LUPULO");
        set(r2, 2, 300); set(r2, 3, "gr"); set(r2, 4, 50);

        MigracionService.Resultado res = migracionService.importarAlmacen(toFile(wb, "almacen"), TENANT, USUARIO);

        assertThat(res.estado()).isEqualTo("EXITOSO");
        assertThat(res.exitosas()).isEqualTo(2);
        assertThat(res.errores()).isZero();
        assertThat(count("insumos_inventario")).isEqualTo(2);

        Double cant = jdbc.queryForObject(
                "SELECT cantidad FROM insumos_inventario WHERE nombre='Malta Pilsner Test' AND tenant_id=?",
                Double.class, TENANT);
        assertThat(cant).isEqualTo(5000.0);
    }

    @Test
    @DisplayName("importarAlmacen — fila con tipo inválido genera error parcial")
    void almacen_tipoInvalido_errorParcial() throws Exception {
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sh = hoja(wb, "Insumos");

        Row r1 = fila(sh, 0);
        set(r1, 0, "Insumo OK"); set(r1, 1, "MALTA"); set(r1, 2, 100); set(r1, 3, "gr");

        Row r2 = fila(sh, 1);
        set(r2, 0, "Insumo Malo"); set(r2, 1, "TIPO_INVALIDO");

        MigracionService.Resultado res = migracionService.importarAlmacen(toFile(wb, "almacen"), TENANT, USUARIO);

        assertThat(res.estado()).isEqualTo("PARCIAL");
        assertThat(res.exitosas()).isEqualTo(1);
        assertThat(res.errores()).isEqualTo(1);
        assertThat(count("insumos_inventario")).isEqualTo(1);
    }

    // ── Equipos ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("importarEquipos — incluye fecha_ultimo_mant (campo nuevo)")
    void equipos_conFechaUltimoMant() throws Exception {
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sh = hoja(wb, "Equipos");

        Row r1 = fila(sh, 0);
        set(r1, 0, "Fermentador Test 1"); set(r1, 1, "FERMENTADOR"); set(r1, 2, "OPERATIVO");
        set(r1, 3, 300); set(r1, 4, "L");
        set(r1, 5, "2022-03-10"); // fecha_adquisicion
        set(r1, 6, "2025-06-01"); // fecha_proximo_mant
        set(r1, 7, "2024-12-15"); // fecha_ultimo_mant  ← campo nuevo
        set(r1, 8, "Tanque cónico acero inox");

        Row r2 = fila(sh, 1);
        set(r2, 0, "Olla Macerado Test"); set(r2, 1, "OLLA_MACERADO"); set(r2, 2, "OPERATIVO");
        set(r2, 3, 100); set(r2, 4, "L");

        MigracionService.Resultado res = migracionService.importarEquipos(toFile(wb, "equipos"), TENANT, USUARIO);

        assertThat(res.estado()).isEqualTo("EXITOSO");
        assertThat(res.exitosas()).isEqualTo(2);
        assertThat(count("equipos")).isEqualTo(2);

        String fechaUlt = jdbc.queryForObject(
                "SELECT fecha_ultimo_mantenimiento::text FROM equipos WHERE nombre='Fermentador Test 1' AND tenant_id=?",
                String.class, TENANT);
        assertThat(fechaUlt).isEqualTo("2024-12-15");
    }

    // ── Catálogos (módulo nuevo) ───────────────────────────────────────────────

    @Test
    @DisplayName("importarCatalogos — tipos cerveza, insumo y equipo; idempotente en duplicado")
    void catalogos_tresHojas_idempotente() throws Exception {
        XSSFWorkbook wb = new XSSFWorkbook();

        Sheet shTC = hoja(wb, "Tipos_Cerveza");
        Row tc1 = fila(shTC, 0); set(tc1, 0, "IPA Test"); set(tc1, 1, "India Pale Ale"); set(tc1, 2, "TRUE");
        Row tc2 = fila(shTC, 1); set(tc2, 0, "Stout Test"); set(tc2, 1, "Cerveza oscura");

        Sheet shTI = hoja(wb, "Tipos_Insumo");
        Row ti1 = fila(shTI, 0); set(ti1, 0, "Extracto de malta especial");
        Row ti2 = fila(shTI, 1); set(ti2, 0, "Lúpulo seco"); set(ti2, 1, "TRUE");

        Sheet shTE = hoja(wb, "Tipos_Equipo");
        Row te1 = fila(shTE, 0); set(te1, 0, "Conical fermenter"); set(te1, 1, "TRUE");

        MigracionService.Resultado res = migracionService.importarCatalogos(toFile(wb, "catalogos"), TENANT, USUARIO);

        assertThat(res.estado()).isEqualTo("EXITOSO");
        assertThat(res.exitosas()).isEqualTo(5);
        assertThat(count("tipos_cerveza")).isEqualTo(2);
        assertThat(count("tipos_insumo")).isEqualTo(2);
        assertThat(count("tipos_equipo")).isEqualTo(1);

        // Segunda importación del mismo archivo — idempotente: no crea duplicados
        XSSFWorkbook wb2 = new XSSFWorkbook();
        Sheet shTC2 = hoja(wb2, "Tipos_Cerveza");
        Row tc1b = fila(shTC2, 0); set(tc1b, 0, "IPA Test"); // ya existe

        MigracionService.Resultado res2 = migracionService.importarCatalogos(toFile(wb2, "catalogos"), TENANT, USUARIO);
        assertThat(res2.exitosas()).isEqualTo(1);
        assertThat(count("tipos_cerveza")).isEqualTo(2); // sin cambios
    }

    // ── Comercial ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("importarComercial — proveedor + factura con iva_incluido + ítem con impuesto_consumo")
    void comercial_facturaConIvaIncluidoEImpuestoConsumo() throws Exception {
        XSSFWorkbook wb = new XSSFWorkbook();

        Sheet shProv = hoja(wb, "Proveedores");
        Row p = fila(shProv, 0);
        set(p, 0, "Maltería Sur Test"); set(p, 1, "900111222-3");
        set(p, 2, "3001234567"); set(p, 3, "ventas@maltsur.co");

        Sheet shFac = hoja(wb, "Facturas");
        Row f = fila(shFac, 0);
        set(f, 0, "FAC-TEST-001"); set(f, 1, "Maltería Sur Test");
        set(f, 2, "2024-03-15"); set(f, 3, "Compra malta marzo");
        set(f, 4, 12000); set(f, 5, "PAGADA");
        set(f, 6, "TRUE"); // iva_incluido ← campo nuevo

        Sheet shItems = hoja(wb, "Factura_Items");
        Row i = fila(shItems, 0);
        set(i, 0, "FAC-TEST-001"); set(i, 1, "INSUMO");
        set(i, 2, "Malta Pilsner Import"); set(i, 3, "MALTA"); set(i, 4, "");
        set(i, 5, 25); set(i, 6, "kg");
        set(i, 7, 8500); set(i, 8, 0); set(i, 9, 19);
        set(i, 10, 490); // impuesto_consumo ← campo nuevo

        MigracionService.Resultado res = migracionService.importarComercial(toFile(wb, "comercial"), TENANT, USUARIO);

        assertThat(res.estado()).isEqualTo("EXITOSO");
        assertThat(count("proveedores")).isEqualTo(1);
        assertThat(count("facturas_proveedor")).isEqualTo(1);
        assertThat(count("factura_items")).isEqualTo(1);

        Boolean ivaIncl = jdbc.queryForObject(
                "SELECT iva_incluido FROM facturas_proveedor WHERE numero_factura='FAC-TEST-001' AND tenant_id=?",
                Boolean.class, TENANT);
        assertThat(ivaIncl).isTrue();

        Double impConsumo = jdbc.queryForObject(
                "SELECT impuesto_consumo FROM factura_items WHERE nombre='Malta Pilsner Import' AND tenant_id=?",
                Double.class, TENANT);
        assertThat(impConsumo).isEqualTo(490.0);
    }

    @Test
    @DisplayName("importarComercial — proveedor inexistente en Facturas genera error por fila")
    void comercial_proveedorInexistente_errorFila() throws Exception {
        XSSFWorkbook wb = new XSSFWorkbook();

        hoja(wb, "Proveedores"); // hoja vacía

        Sheet shFac = hoja(wb, "Facturas");
        Row f = fila(shFac, 0);
        set(f, 0, "FAC-ERROR-001"); set(f, 1, "Proveedor Inexistente");
        set(f, 2, "2024-03-15");

        MigracionService.Resultado res = migracionService.importarComercial(toFile(wb, "comercial"), TENANT, USUARIO);

        // La fila de factura falla (proveedor no encontrado)
        assertThat(res.errores()).isGreaterThan(0);
        assertThat(count("facturas_proveedor")).isZero();
    }

    // ── Producción ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("importarProduccion — receta + lote con brix, fermentador y fechas de fases")
    void produccion_loteConCamposNuevos() throws Exception {
        // Pre-requisito: equipo para el lookup de fermentador_nombre
        jdbc.update("INSERT INTO equipos (nombre,tipo,estado,tenant_id,created_at,created_by,last_modified_at,last_modified_by) " +
                "VALUES ('Fermentador Prod Test','Fermentador','OPERATIVO',?,NOW(),?,NOW(),?)",
                TENANT, USUARIO, USUARIO);

        XSSFWorkbook wb = new XSSFWorkbook();

        Sheet shRec = hoja(wb, "Recetas");
        Row rec = fila(shRec, 0);
        set(rec, 0, "IPA Test Recipe"); set(rec, 1, "IPA");
        set(rec, 2, "Receta prueba integración"); set(rec, 3, "TRUE");
        set(rec, 4, 20); set(rec, 5, 60);
        set(rec, 6, 1058); set(rec, 7, 1012);
        set(rec, 8, 18); set(rec, 9, "L");
        set(rec, 10, 8); set(rec, 11, "L");
        set(rec, 12, 5.2);

        Sheet shRI = hoja(wb, "Receta_Ingredientes");
        Row ri = fila(shRI, 0);
        set(ri, 0, "IPA Test Recipe"); set(ri, 1, "MALTA");
        set(ri, 2, "Pale Ale 2-Row"); set(ri, 3, "5000 gr");

        Sheet shES = hoja(wb, "Receta_Escalones");
        Row es = fila(shES, 0);
        set(es, 0, "IPA Test Recipe"); set(es, 1, "Sacarificación");
        set(es, 2, 67.5); set(es, 3, 60); set(es, 4, 1);

        Sheet shAd = hoja(wb, "Receta_Adiciones");
        Row ad = fila(shAd, 0);
        set(ad, 0, "IPA Test Recipe"); set(ad, 1, "Cascade");
        set(ad, 2, 60); set(ad, 3, 30); set(ad, 4, "gr"); set(ad, 5, 1);

        Sheet shLotes = hoja(wb, "Lotes");
        Row lote = fila(shLotes, 0);
        // cols 0-11: base
        set(lote, 0, "IPA-TEST-001"); set(lote, 1, "IPA");
        set(lote, 2, "2024-01-20"); set(lote, 3, 19.5);
        set(lote, 4, 1058); set(lote, 5, 1012);
        set(lote, 6, 25); set(lote, 7, 5.3);
        set(lote, 8, ""); set(lote, 9, "Lote de prueba integración");
        set(lote, 10, ""); set(lote, 11, "IPA Test Recipe");
        // cols 12-21: carbonatación
        set(lote, 12, "NATURAL"); set(lote, 13, 2.5); set(lote, 14, 2.4);
        set(lote, 15, "dextrosa"); set(lote, 16, 120.5);
        set(lote, 17, ""); set(lote, 18, ""); set(lote, 19, ""); set(lote, 20, "ADECUADA");
        set(lote, 21, "Botella 330mL");
        // cols 22-25: brix + fermentador + densidad_final_fecha ← CAMPOS NUEVOS
        set(lote, 22, 14.5); // og_brix
        set(lote, 23, 3.8);  // fg_brix
        set(lote, 24, "Fermentador Prod Test"); // fermentador_nombre
        set(lote, 25, "2024-02-03"); // densidad_final_fecha
        // cols 26-29: ferm ← CAMPOS NUEVOS
        set(lote, 26, "2024-01-21"); // ferm_fecha_inicial
        set(lote, 27, "2024-02-04"); // ferm_fecha_final_ideal
        set(lote, 28, 18.0);         // ferm_temperatura
        set(lote, 29, "2024-02-03"); // ferm_fecha_final
        // cols 30-33: acond
        set(lote, 30, "2024-02-04"); set(lote, 31, "2024-02-11");
        set(lote, 32, 12.0);         set(lote, 33, "2024-02-11");
        // cols 34-37: madur
        set(lote, 34, "2024-02-12"); set(lote, 35, "2024-03-12");
        set(lote, 36, 5.0);          set(lote, 37, "2024-03-10");
        // cols 38-41: carb
        set(lote, 38, "2024-03-11"); set(lote, 39, "2024-03-18");
        set(lote, 40, 4.0);          set(lote, 41, "2024-03-17");

        Sheet shLI = hoja(wb, "Lote_Ingredientes");
        Row li = fila(shLI, 0);
        set(li, 0, "IPA-TEST-001"); set(li, 1, "MALTA");
        set(li, 2, "Pale Ale 2-Row"); set(li, 3, "5000 gr");

        MigracionService.Resultado res = migracionService.importarProduccion(toFile(wb, "produccion"), TENANT, USUARIO);

        assertThat(res.estado()).isEqualTo("EXITOSO");
        assertThat(count("recetas")).isEqualTo(1);
        assertThat(count("lotes_cerveza")).isEqualTo(1);
        assertThat(count("receta_ingredientes")).isEqualTo(1);
        assertThat(count("escalones_macerado")).isEqualTo(1);
        assertThat(count("adiciones_hervor")).isEqualTo(1);
        assertThat(count("ingredientes")).isEqualTo(1);

        // Verificar campos nuevos del lote
        var row = jdbc.queryForMap(
                "SELECT og_brix, fg_brix, equipo_fermentador_id, ferm_fecha_inicial, " +
                "ferm_temperatura, carb_fecha_final FROM lotes_cerveza WHERE codigo_lote='IPA-TEST-001' AND tenant_id=?",
                TENANT);

        assertThat(row.get("og_brix")).isNotNull();
        assertThat(((Number) row.get("og_brix")).doubleValue()).isEqualTo(14.5);
        assertThat(row.get("fg_brix")).isNotNull();
        assertThat(row.get("equipo_fermentador_id")).isNotNull(); // fermentador vinculado
        assertThat(row.get("ferm_fecha_inicial").toString()).isEqualTo("2024-01-21");
        assertThat(((Number) row.get("ferm_temperatura")).doubleValue()).isEqualTo(18.0);
        assertThat(row.get("carb_fecha_final").toString()).isEqualTo("2024-03-17");
    }

    @Test
    @DisplayName("importarProduccion — re-importar lote duplicado es idempotente (sin error)")
    void produccion_codigoDuplicado_idempotente() throws Exception {
        // Primera importación
        XSSFWorkbook wb1 = new XSSFWorkbook();
        Sheet sh1 = hoja(wb1, "Lotes");
        Row r1 = fila(sh1, 0);
        set(r1, 0, "IPA-DUP-001"); set(r1, 1, "IPA"); set(r1, 2, "2024-01-20");
        hoja(wb1, "Recetas"); hoja(wb1, "Receta_Ingredientes");
        hoja(wb1, "Receta_Escalones"); hoja(wb1, "Receta_Adiciones"); hoja(wb1, "Lote_Ingredientes");
        migracionService.importarProduccion(toFile(wb1, "prod1"), TENANT, USUARIO);

        // Segunda importación — mismo código
        XSSFWorkbook wb2 = new XSSFWorkbook();
        Sheet sh2 = hoja(wb2, "Lotes");
        Row r2 = fila(sh2, 0);
        set(r2, 0, "IPA-DUP-001"); set(r2, 1, "IPA"); set(r2, 2, "2024-02-01");
        hoja(wb2, "Recetas"); hoja(wb2, "Receta_Ingredientes");
        hoja(wb2, "Receta_Escalones"); hoja(wb2, "Receta_Adiciones"); hoja(wb2, "Lote_Ingredientes");

        MigracionService.Resultado res2 = migracionService.importarProduccion(toFile(wb2, "prod2"), TENANT, USUARIO);

        assertThat(res2.errores()).isEqualTo(0);
        assertThat(res2.estado()).isEqualTo("EXITOSO");
        assertThat(count("lotes_cerveza")).isEqualTo(1); // no se duplicó
    }

    // ── Clientes ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("importarClientes — 2 clientes; duplicado por NIT es idempotente")
    void clientes_dosRegistros_idempotente() throws Exception {
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sh = hoja(wb, "Clientes");

        Row c1 = fila(sh, 0);
        set(c1, 0, "Cervecería El Mosto Test"); set(c1, 1, "El Mosto SAS");
        set(c1, 2, "900111222-9");             set(c1, 3, "RESPONSABLE_IVA");
        set(c1, 4, "mosto@test.co");           set(c1, 5, "3001234567");
        set(c1, 6, "Calle 45 #12-34");         set(c1, 7, "Bogotá");
        set(c1, 8, "Cundinamarca");             set(c1, 9, "DISTRIBUIDOR");
        set(c1, 10, "TRUE");

        Row c2 = fila(sh, 1);
        set(c2, 0, "Bar La Espuma Test");
        set(c2, 9, "BAR"); set(c2, 10, "TRUE");

        migracionService.importarClientes(toFile(wb, "clientes"), TENANT, USUARIO);
        assertThat(count("clientes")).isEqualTo(2);

        // Misma importación → idempotente por NIT
        XSSFWorkbook wb2 = new XSSFWorkbook();
        Sheet sh2 = hoja(wb2, "Clientes");
        Row c1b = fila(sh2, 0);
        set(c1b, 0, "Otro nombre"); set(c1b, 2, "900111222-9"); // mismo NIT

        migracionService.importarClientes(toFile(wb2, "clientes"), TENANT, USUARIO);
        assertThat(count("clientes")).isEqualTo(2); // sin duplicado
    }

    // ── Ventas ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("importarVentas — cotización con cotizacion_expira_en (campo nuevo)")
    void ventas_cotizacionConExpiracion() throws Exception {
        XSSFWorkbook wb = new XSSFWorkbook();

        Sheet shV = hoja(wb, "Ventas");
        Row v = fila(shV, 0);
        set(v, 0, "V-TEST-001");           // referencia_venta
        set(v, 1, "Cliente Libre Test");   // cliente_nombre
        set(v, 2, "");                     // cliente_nit
        set(v, 3, "2024-04-01");           // fecha_despacho
        set(v, 4, "COTIZACION");           // estado
        set(v, 5, "Cotización mensual");   // notas
        set(v, 6, "");                     // remision_numero
        set(v, 7, "2024-04-16");           // cotizacion_expira_en ← campo nuevo

        Sheet shVI = hoja(wb, "Venta_Items");
        Row vi = fila(shVI, 0);
        set(vi, 0, "V-TEST-001"); set(vi, 1, ""); // sin lote
        set(vi, 2, "Botella IPA 330ml"); set(vi, 3, 48);
        set(vi, 4, "Botella 330ml");     set(vi, 5, 8500); set(vi, 6, 0);

        MigracionService.Resultado res = migracionService.importarVentas(toFile(wb, "ventas"), TENANT, USUARIO);

        assertThat(res.estado()).isEqualTo("EXITOSO");
        assertThat(count("ventas")).isEqualTo(1);
        assertThat(count("venta_items")).isEqualTo(1);

        String expira = jdbc.queryForObject(
                "SELECT cotizacion_expira_en::text FROM ventas WHERE cliente='Cliente Libre Test' AND tenant_id=?",
                String.class, TENANT);
        assertThat(expira).isEqualTo("2024-04-16");
    }

    // ── Barriles ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("importarBarriles — 2 barriles con distintos estados")
    void barriles_dosBarriles() throws Exception {
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sh = hoja(wb, "Barriles");

        Row r1 = fila(sh, 0);
        set(r1, 0, "BAR-TEST-001"); set(r1, 1, "Acero inox");
        set(r1, 2, 20); set(r1, 3, "DISPONIBLE");

        Row r2 = fila(sh, 1);
        set(r2, 0, "BAR-TEST-002"); set(r2, 1, "Plástico");
        set(r2, 2, 50); set(r2, 3, "LLENO");
        set(r2, 4, ""); // sin lote
        set(r2, 5, "Bar La Espuma"); set(r2, 6, "2024-09-15");
        set(r2, 7, "Despacho test");

        MigracionService.Resultado res = migracionService.importarBarriles(toFile(wb, "barriles"), TENANT, USUARIO);

        assertThat(res.estado()).isEqualTo("EXITOSO");
        assertThat(res.exitosas()).isEqualTo(2);
        assertThat(count("barriles")).isEqualTo(2);
    }

    // ── Órdenes de Compra ─────────────────────────────────────────────────────

    @Test
    @DisplayName("importarOrdenes — OC con 2 ítems insumo/equipo")
    void ordenes_ocConDosItems() throws Exception {
        XSSFWorkbook wb = new XSSFWorkbook();

        Sheet shOC = hoja(wb, "OC");
        Row oc = fila(shOC, 0);
        set(oc, 0, "OC-TEST-001"); set(oc, 1, "Maltería Sur Test");
        set(oc, 2, "2024-08-10"); set(oc, 3, "2024-08-25");
        set(oc, 4, "RECIBIDA"); set(oc, 5, "Pedido mensual");

        Sheet shItems = hoja(wb, "OC_Items");
        Row i1 = fila(shItems, 0);
        set(i1, 0, "OC-TEST-001"); set(i1, 1, "INSUMO");
        set(i1, 2, "Malta Pilsner OC"); set(i1, 3, "MALTA"); set(i1, 4, "");
        set(i1, 5, 50); set(i1, 6, "kg"); set(i1, 7, 4800); set(i1, 8, 19);

        Row i2 = fila(shItems, 1);
        set(i2, 0, "OC-TEST-001"); set(i2, 1, "INSUMO");
        set(i2, 2, "Cascade OC"); set(i2, 3, "LUPULO"); set(i2, 4, "");
        set(i2, 5, 500); set(i2, 6, "gr"); set(i2, 7, 12000); set(i2, 8, 19);

        MigracionService.Resultado res = migracionService.importarOrdenes(toFile(wb, "ordenes"), TENANT, USUARIO);

        assertThat(res.estado()).isEqualTo("EXITOSO");
        assertThat(count("ordenes_compra")).isEqualTo(1);
        assertThat(count("orden_compra_items")).isEqualTo(2);
    }

    // ── Seguimiento ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("importarSeguimiento — lecturas, evaluaciones y planificación")
    void seguimiento_tresHojas() throws Exception {
        // Pre-requisito: lote existente
        jdbc.update("INSERT INTO lotes_cerveza " +
                "(codigo_lote,estilo,fecha_elaboracion,tenant_id,created_at,created_by,last_modified_at,last_modified_by) " +
                "VALUES ('SEG-LOTE-001','IPA','2024-01-20',?,NOW(),?,NOW(),?)", TENANT, USUARIO, USUARIO);

        XSSFWorkbook wb = new XSSFWorkbook();

        Sheet shLect = hoja(wb, "Lote_Lecturas");
        Row l1 = fila(shLect, 0);
        set(l1, 0, "SEG-LOTE-001"); set(l1, 1, "2024-01-25");
        set(l1, 2, 1045); set(l1, 3, 18.5); set(l1, 4, "Fermentación activa");

        Row l2 = fila(shLect, 1);
        set(l2, 0, "SEG-LOTE-001"); set(l2, 1, "2024-02-01");
        set(l2, 2, 1015); set(l2, 3, 17.0); set(l2, 4, "Densidad estable");

        Sheet shEval = hoja(wb, "Lote_Evaluaciones");
        Row ev = fila(shEval, 0);
        set(ev, 0, "SEG-LOTE-001"); set(ev, 1, "2024-02-15");
        set(ev, 2, "Juan P."); set(ev, 3, 10); set(ev, 4, 3);
        set(ev, 5, 18); set(ev, 6, 4); set(ev, 7, 9);
        set(ev, 8, "Excelente balance lúpulo floral");

        Sheet shPlan = hoja(wb, "Planificacion");
        Row pl = fila(shPlan, 0);
        set(pl, 0, "2024-10-01"); set(pl, 1, "Lote Otoño Test");
        set(pl, 2, ""); set(pl, 3, 300); set(pl, 4, "PLANIFICADA");
        set(pl, 5, "Preparar grist con 48h de anticipación");

        MigracionService.Resultado res = migracionService.importarSeguimiento(toFile(wb, "seguimiento"), TENANT, USUARIO);

        assertThat(res.estado()).isEqualTo("EXITOSO");
        assertThat(count("lecturas_fermentacion")).isEqualTo(2);
        assertThat(count("evaluaciones_sensoriales")).isEqualTo(1);
        assertThat(count("elaboraciones_planificadas")).isEqualTo(1);
    }

    @Test
    @DisplayName("importarSeguimiento — lote inexistente en lecturas genera error por fila")
    void seguimiento_loteInexistente_errorFila() throws Exception {
        XSSFWorkbook wb = new XSSFWorkbook();

        Sheet shLect = hoja(wb, "Lote_Lecturas");
        Row l = fila(shLect, 0);
        set(l, 0, "LOTE-NO-EXISTE"); set(l, 1, "2024-01-25");
        set(l, 2, 1045); set(l, 3, 18.5);

        hoja(wb, "Lote_Evaluaciones");
        hoja(wb, "Planificacion");

        MigracionService.Resultado res = migracionService.importarSeguimiento(toFile(wb, "seguimiento"), TENANT, USUARIO);

        assertThat(res.errores()).isEqualTo(1);
        assertThat(count("lecturas_fermentacion")).isZero();
    }

    // ── Mantenimientos (módulo nuevo) ─────────────────────────────────────────

    @Test
    @DisplayName("importarMantenimientos — registra mantenimiento y actualiza fecha_ultimo_mant del equipo")
    void mantenimientos_actualizaFechaEquipo() throws Exception {
        // Pre-requisito: equipo existente
        jdbc.update("INSERT INTO equipos " +
                "(nombre,tipo,estado,tenant_id,created_at,created_by,last_modified_at,last_modified_by) " +
                "VALUES ('Fermentador Mant Test','Fermentador','OPERATIVO',?,NOW(),?,NOW(),?)",
                TENANT, USUARIO, USUARIO);

        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sh = hoja(wb, "Mantenimientos");

        Row r1 = fila(sh, 0);
        set(r1, 0, "Fermentador Mant Test"); // nombre_equipo
        set(r1, 1, "2024-06-10");            // fecha
        set(r1, 2, "PREVENTIVO");            // tipo
        set(r1, 3, "Limpieza y sanitización mensual");
        set(r1, 4, "Carlos M."); set(r1, 5, 45000); set(r1, 6, "2024-07-10");

        // Segundo mantenimiento más reciente → debe actualizar fecha_ultimo_mant
        Row r2 = fila(sh, 1);
        set(r2, 0, "Fermentador Mant Test");
        set(r2, 1, "2024-09-05");
        set(r2, 2, "CORRECTIVO");
        set(r2, 3, "Reemplazo sello");
        set(r2, 4, "Técnico externo"); set(r2, 5, 120000);

        MigracionService.Resultado res = migracionService.importarMantenimientos(toFile(wb, "mantenimientos"), TENANT, USUARIO);

        assertThat(res.estado()).isEqualTo("EXITOSO");
        assertThat(res.exitosas()).isEqualTo(2);
        assertThat(count("mantenimientos_equipo")).isEqualTo(2);

        // Verifica que fecha_ultimo_mant apunta al mantenimiento más reciente
        String fechaUlt = jdbc.queryForObject(
                "SELECT fecha_ultimo_mantenimiento::text FROM equipos " +
                "WHERE nombre='Fermentador Mant Test' AND tenant_id=?",
                String.class, TENANT);
        assertThat(fechaUlt).isEqualTo("2024-09-05");
    }

    @Test
    @DisplayName("importarMantenimientos — equipo inexistente genera error por fila")
    void mantenimientos_equipoInexistente_errorFila() throws Exception {
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sh = hoja(wb, "Mantenimientos");

        Row r = fila(sh, 0);
        set(r, 0, "Equipo Fantasma"); set(r, 1, "2024-06-10"); set(r, 2, "PREVENTIVO");

        MigracionService.Resultado res = migracionService.importarMantenimientos(toFile(wb, "mantenimientos"), TENANT, USUARIO);

        assertThat(res.errores()).isEqualTo(1);
        assertThat(count("mantenimientos_equipo")).isZero();
    }

}
