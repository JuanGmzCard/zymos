package com.alera.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Siembra datos realistas de demostración en el tenant "demo".
 * Usa JdbcTemplate directamente para evitar el filtro @TenantId de Hibernate.
 * Se invoca desde DataInitializer al arrancar si el tenant tiene cero lotes.
 */
@Component
public class DemoDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String TENANT = "demo";

    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;

    public DemoDataSeeder(JdbcTemplate jdbc, PasswordEncoder encoder) {
        this.jdbc    = jdbc;
        this.encoder = encoder;
    }

    public void seed() {
        if (contarLotes() > 0) {
            log.debug("Tenant demo ya tiene lotes — omitiendo seed");
            return;
        }
        log.info("Sembrando datos demo para tenant '{}'", TENANT);

        crearUsuarioDemo();

        Long ferm1Id = crearEquipo("Fermentador Cónico 100L", 100.0, "L");
        Long ferm2Id = crearEquipo("Fermentador Cónico 60L",  60.0,  "L");

        Long recetaPaleId = crearReceta(
            "Pale Ale Clásica", "APA",
            "Receta clásica americana — cítrica, equilibrada, refrescante.",
            60.0, "L", 20.0, "L", 60, 1050, 1012, 20.0);

        Long recetaIpaId = crearReceta(
            "IPA Americana", "IPA",
            "India Pale Ale con Cascade y Centennial, perfil cítrico intenso y lupulado.",
            65.0, "L", 25.0, "L", 75, 1065, 1014, 20.0);

        crearRecetaIngredientes(recetaPaleId, new Object[][]{
            {"MALTA",       "Malta Pale Ale",    "9000 gr"},
            {"MALTA",       "Malta Caramel 60L", "1000 gr"},
            {"LUPULO",      "Lúpulo Cascade",    "60 gr"},
            {"LEVADURA",    "Levadura US-05",    "11 gr"},
            {"CLARIFICANTE","Irish Moss",         "5 gr"},
        });
        crearRecetaIngredientes(recetaIpaId, new Object[][]{
            {"MALTA",    "Malta Pale Ale",    "11000 gr"},
            {"MALTA",    "Malta Caramel 60L", "1000 gr"},
            {"LUPULO",   "Lúpulo Cascade",    "80 gr"},
            {"LUPULO",   "Lúpulo Centennial", "50 gr"},
            {"LEVADURA", "Levadura US-05",    "11 gr"},
        });

        crearInsumos();
        Long provId    = crearProveedor();
        crearFactura(provId);
        Long clienteId = crearCliente();

        LocalDate hoy = LocalDate.now();

        // Lote 1 — Completado (todas las fases terminadas)
        Long l1 = crearLote("PALE-001", "APA", hoy.minusDays(45), 60.0, 6.5, 55.0,
            ferm1Id, recetaPaleId, 1050, 1012, hoy.minusDays(16),
            hoy.minusDays(44), hoy.minusDays(30), 18.0, hoy.minusDays(30),
            hoy.minusDays(30), hoy.minusDays(16), 10.0, hoy.minusDays(16),
            hoy.minusDays(16), hoy.minusDays(2),  8.0,  hoy.minusDays(2),
            hoy.minusDays(2),  hoy.plusDays(5),   8.0,  hoy.minusDays(1));
        crearIngredientes(l1, new Object[][]{
            {"MALTA",       "Malta Pale Ale",    "9000 gr"},
            {"MALTA",       "Malta Caramel 60L", "1000 gr"},
            {"LUPULO",      "Lúpulo Cascade",    "60 gr"},
            {"LEVADURA",    "Levadura US-05",    "11 gr"},
            {"CLARIFICANTE","Irish Moss",         "5 gr"},
        });

        // Lote 2 — En Carbonatación
        Long l2 = crearLote("PALE-002", "APA", hoy.minusDays(30), 60.0, 6.5, 55.0,
            ferm1Id, recetaPaleId, 1050, 1013, hoy.minusDays(5),
            hoy.minusDays(29), hoy.minusDays(15), 18.0, hoy.minusDays(15),
            hoy.minusDays(15), hoy.minusDays(1),  10.0, hoy.minusDays(1),
            hoy.minusDays(1),  hoy.plusDays(13),  8.0,  null,
            hoy.minusDays(1),  hoy.plusDays(6),   8.0,  null);
        crearIngredientes(l2, new Object[][]{
            {"MALTA",   "Malta Pale Ale",    "9000 gr"},
            {"MALTA",   "Malta Caramel 60L", "1000 gr"},
            {"LUPULO",  "Lúpulo Cascade",    "60 gr"},
            {"LEVADURA","Levadura US-05",    "11 gr"},
        });

        // Lote 3 — En Fermentación (ocupa ferm1)
        Long l3 = crearLote("IPA-001", "IPA", hoy.minusDays(10), 65.0, 6.3, 60.0,
            ferm1Id, recetaIpaId, 1065, null, null,
            hoy.minusDays(9), hoy.plusDays(5), 20.0, null,
            null, null, null, null,
            null, null, null, null,
            null, null, null, null);
        crearIngredientes(l3, new Object[][]{
            {"MALTA",   "Malta Pale Ale",    "11000 gr"},
            {"MALTA",   "Malta Caramel 60L", "1000 gr"},
            {"LUPULO",  "Lúpulo Cascade",    "80 gr"},
            {"LUPULO",  "Lúpulo Centennial", "50 gr"},
            {"LEVADURA","Levadura US-05",    "11 gr"},
        });

        // Lote 4 — Recién elaborado (esperando fermentador ferm2)
        Long l4 = crearLote("IPA-002", "IPA", hoy.minusDays(1), 65.0, 6.3, 62.0,
            ferm2Id, recetaIpaId, 1066, null, null,
            null, null, null, null,
            null, null, null, null,
            null, null, null, null,
            null, null, null, null);
        crearIngredientes(l4, new Object[][]{
            {"MALTA",   "Malta Pale Ale",    "11000 gr"},
            {"MALTA",   "Malta Caramel 60L", "1000 gr"},
            {"LUPULO",  "Lúpulo Cascade",    "80 gr"},
            {"LUPULO",  "Lúpulo Centennial", "50 gr"},
            {"LEVADURA","Levadura US-05",    "11 gr"},
        });

        crearVenta(l1, "PALE-001", clienteId);

        // Costos de producción — vincula ítems de factura a lotes para activar reporte de rentabilidad
        vincularCostosLotes(l1, l2, l3);

        // Tareas operativas con ítems y referencias a entidades del sistema
        crearTareas(l1, l2, l3);

        // BPM — salud diaria (hoy sin síntomas, para que BpmSaludFilter no bloquee) + limpieza + control
        crearRegistrosBpm();

        // Notificaciones pre-cargadas: bajo stock + vencimiento + tarea próxima
        crearNotificaciones(l2);

        // Lecturas de fermentación para gráficas densidad + temperatura
        crearLecturas(l1, new Object[][]{
            {-44, 1050, 20.0, "OG inicial"},
            {-43, 1044, 20.0, null},
            {-42, 1036, 20.0, "Fermentación activa"},
            {-41, 1028, 20.0, null},
            {-39, 1022, 19.5, null},
            {-38, 1018, 19.0, null},
            {-36, 1015, 18.5, null},
            {-34, 1013, 18.0, null},
            {-30, 1012, 18.0, "FG final"},
        });
        crearLecturas(l2, new Object[][]{
            {-29, 1050, 18.0, "OG inicial"},
            {-28, 1046, 18.0, null},
            {-26, 1038, 18.0, null},
            {-24, 1028, 18.0, null},
            {-22, 1020, 18.0, null},
            {-20, 1016, 18.0, null},
            {-15, 1014, 18.0, null},
            { -1, 1013, 18.0, "FG final"},
        });
        crearLecturas(l3, new Object[][]{
            {-9, 1065, 20.0, "OG inicial"},
            {-8, 1058, 20.0, null},
            {-7, 1046, 20.0, "Fermentación muy activa"},
            {-6, 1035, 20.0, null},
            {-5, 1026, 20.0, null},
            {-4, 1021, 19.5, null},
            {-3, 1018, 19.0, null},
        });

        log.info("Demo listo: 2 equipos, 2 recetas, 4 lotes, 1 proveedor, 1 factura, 1 cliente, 1 venta, 20 lecturas, costos asignados, 4 tareas, BPM, 4 notificaciones");
    }

    @Scheduled(cron = "${app.demo.reset-cron:0 0 3 * * *}")
    public void autoReset() {
        Integer existe = jdbc.queryForObject(
            "SELECT COUNT(*) FROM tenants WHERE subdomain = 'demo' AND active = true",
            Integer.class);
        if (existe == null || existe == 0) {
            log.debug("Tenant demo no existe o inactivo — omitiendo auto-reset");
            return;
        }
        log.info("Auto-reset diario del tenant demo (3 AM)");
        resetear();
    }

    public void resetear() {
        log.info("Reseteando datos demo del tenant '{}'", TENANT);
        jdbc.update("DELETE FROM notificaciones                WHERE tenant_id = ?", TENANT);
        jdbc.update("DELETE FROM bpm_registros_sintomas        WHERE tenant_id = ?", TENANT);
        jdbc.update("DELETE FROM bpm_limpieza_desinfeccion     WHERE tenant_id = ?", TENANT);
        jdbc.update("DELETE FROM bpm_soluciones_desinfectantes WHERE tenant_id = ?", TENANT);
        jdbc.update("DELETE FROM bpm_avistamiento_plagas       WHERE tenant_id = ?", TENANT);
        jdbc.update("DELETE FROM bpm_evacuacion_residuos       WHERE tenant_id = ?", TENANT);
        jdbc.update("DELETE FROM tarea_referencias       WHERE tenant_id = ?", TENANT);
        jdbc.update("DELETE FROM tarea_items            WHERE tenant_id = ?", TENANT);
        jdbc.update("DELETE FROM tareas                 WHERE tenant_id = ?", TENANT);
        jdbc.update("DELETE FROM venta_items            WHERE tenant_id = ?", TENANT);
        jdbc.update("DELETE FROM ventas                 WHERE tenant_id = ?", TENANT);
        jdbc.update("DELETE FROM lote_items_factura     WHERE tenant_id = ?", TENANT);
        jdbc.update("DELETE FROM factura_items          WHERE tenant_id = ?", TENANT);
        jdbc.update("DELETE FROM facturas_proveedor     WHERE tenant_id = ?", TENANT);
        jdbc.update("DELETE FROM proveedores            WHERE tenant_id = ?", TENANT);
        jdbc.update("DELETE FROM clientes               WHERE tenant_id = ?", TENANT);
        jdbc.update("DELETE FROM historial_lotes        WHERE tenant_id = ?", TENANT);
        jdbc.update("DELETE FROM lecturas_fermentacion  WHERE tenant_id = ?", TENANT);
        jdbc.update("DELETE FROM lote_escalones_temperatura WHERE tenant_id = ?", TENANT);
        jdbc.update("DELETE FROM ingredientes           WHERE tenant_id = ?", TENANT);
        jdbc.update("DELETE FROM lotes_cerveza          WHERE tenant_id = ?", TENANT);
        jdbc.update("DELETE FROM adiciones_hervor       WHERE tenant_id = ?", TENANT);
        jdbc.update("DELETE FROM escalones_macerado     WHERE tenant_id = ?", TENANT);
        jdbc.update("DELETE FROM receta_ingredientes    WHERE tenant_id = ?", TENANT);
        jdbc.update("DELETE FROM recetas                WHERE tenant_id = ?", TENANT);
        jdbc.update("DELETE FROM insumos_inventario     WHERE tenant_id = ?", TENANT);
        jdbc.update("DELETE FROM equipos                WHERE tenant_id = ?", TENANT);
        seed();
    }

    // ── privados ──────────────────────────────────────────────────────────────

    private int contarLotes() {
        Integer n = jdbc.queryForObject(
            "SELECT COUNT(*) FROM lotes_cerveza WHERE tenant_id = ? AND deleted_at IS NULL",
            Integer.class, TENANT);
        return n != null ? n : 0;
    }

    private void crearUsuarioDemo() {
        Integer existe = jdbc.queryForObject(
            "SELECT COUNT(*) FROM usuarios WHERE username = ? AND tenant_id = ?",
            Integer.class, "demo", TENANT);
        if (existe != null && existe > 0) return;
        jdbc.update(
            "INSERT INTO usuarios (username, password, rol, activo, tenant_id) VALUES (?, ?, 'ADMIN', true, ?)",
            "demo", encoder.encode("demo"), TENANT);
        log.info("Usuario demo/demo creado en tenant '{}'", TENANT);
    }

    private Long crearEquipo(String nombre, double capacidad, String unidad) {
        return jdbc.queryForObject(
            "INSERT INTO equipos (nombre, tipo, estado, capacidad, unidad_capacidad, created_at, tenant_id)"
            + " VALUES (?, 'Fermentador', 'OPERATIVO', ?, ?, NOW(), ?) RETURNING id",
            Long.class, nombre, capacidad, unidad, TENANT);
    }

    private Long crearReceta(String nombre, String estilo, String descripcion,
                              double aguaMac, String unidadMac,
                              double aguaSp, String unidadSp,
                              int hervorMin, int ogObj, int fgObj, double volBase) {
        return jdbc.queryForObject(
            "INSERT INTO recetas (nombre, estilo, descripcion, activa,"
            + " agua_macerado, unidad_agua_macerado, agua_sparge, unidad_agua_sparge,"
            + " tiempo_hervor_minutos, og_objetivo, fg_objetivo, volumen_base, version,"
            + " created_at, tenant_id)"
            + " VALUES (?,?,?,true, ?,?,?,?, ?,?,?,?,1, NOW(),?) RETURNING id",
            Long.class,
            nombre, estilo, descripcion,
            aguaMac, unidadMac, aguaSp, unidadSp,
            hervorMin, ogObj, fgObj, volBase, TENANT);
    }

    private void crearRecetaIngredientes(Long recetaId, Object[][] ingredientes) {
        for (Object[] ing : ingredientes) {
            jdbc.update(
                "INSERT INTO receta_ingredientes (receta_id, tipo, nombre, cantidad, tenant_id)"
                + " VALUES (?,?,?,?,?)",
                recetaId, ing[0], ing[1], ing[2], TENANT);
        }
    }

    private void crearInsumos() {
        LocalDate hoy = LocalDate.now();
        // nombre, tipo, cantidad, unidad, stock_minimo, fecha_vencimiento (null = sin vencimiento)
        Object[][] insumos = {
            {"Malta Pale Ale",    "Malta",        20000.0, "gr",  5000.0, null},
            {"Malta Caramel 60L", "Malta",         5000.0, "gr",  1000.0, null},
            {"Lúpulo Cascade",    "Lúpulo",         500.0, "gr",   100.0, null},
            {"Lúpulo Centennial", "Lúpulo",         300.0, "gr",    50.0, null},
            // Levadura próxima a vencer — dispara alerta VENCIMIENTO
            {"Levadura US-05",    "Levadura",       100.0, "gr",    20.0, hoy.plusDays(18)},
            // Irish Moss por debajo del mínimo — dispara alerta BAJO_STOCK
            {"Irish Moss",        "Clarificante",     8.0, "gr",    10.0, null},
        };
        for (Object[] row : insumos) {
            jdbc.update(
                "INSERT INTO insumos_inventario"
                + " (nombre, tipo, cantidad, unidad, stock_minimo, fecha_vencimiento, tenant_id)"
                + " VALUES (?,?,?,?,?,?,?)",
                row[0], row[1], row[2], row[3], row[4], row[5], TENANT);
        }
    }

    private Long crearProveedor() {
        return jdbc.queryForObject(
            "INSERT INTO proveedores (nombre, nit, telefono, email, activo, created_at, tenant_id)"
            + " VALUES ('Maltería del Norte','900.123.456-7','310 555 0100',"
            + "'ventas@malterianorte.co', true, NOW(), ?) RETURNING id",
            Long.class, TENANT);
    }

    private void crearFactura(Long proveedorId) {
        Long facturaId = jdbc.queryForObject(
            "INSERT INTO facturas_proveedor"
            + " (numero_factura, proveedor, proveedor_id, fecha_factura, descripcion,"
            + "  subtotal, porcentaje_iva, valor_iva, costo_envio, valor_total,"
            + "  iva_incluido, tenant_id)"
            + " VALUES ('FAC-2024-001','Maltería del Norte',?, CURRENT_DATE - 30,"
            + "  'Compra de materias primas Q4 2024',"
            + "  1150000, 19, 218500, 25000, 1393500, false, ?) RETURNING id",
            Long.class, proveedorId, TENANT);

        jdbc.update(
            "INSERT INTO factura_items"
            + " (tipo_item, nombre, tipo_insumo, cantidad, unidad, valor_unitario,"
            + "  porcentaje_descuento, porcentaje_iva_item, valor_linea, factura_id, tenant_id)"
            + " VALUES ('INSUMO','Malta Pale Ale','MALTA', 25,'kg', 36000, 0, 19, 900000, ?,?)",
            facturaId, TENANT);

        jdbc.update(
            "INSERT INTO factura_items"
            + " (tipo_item, nombre, tipo_insumo, cantidad, unidad, valor_unitario,"
            + "  porcentaje_descuento, porcentaje_iva_item, valor_linea, factura_id, tenant_id)"
            + " VALUES ('INSUMO','Lúpulo Cascade','LUPULO', 1,'kg', 250000, 0, 19, 250000, ?,?)",
            facturaId, TENANT);
    }

    private Long crearCliente() {
        return jdbc.queryForObject(
            "INSERT INTO clientes"
            + " (tenant_id, nombre, razon_social, nit, regimen_tributario, email, telefono,"
            + "  ciudad, departamento, activo, created_at)"
            + " VALUES (?,'Bar Los Artesanos','Bar Los Artesanos S.A.S.','900.654.321-0',"
            + "  'RESPONSABLE_IVA','pedidos@losartesanos.co','314 777 8888',"
            + "  'Medellín','Antioquia', true, NOW()) RETURNING id",
            Long.class, TENANT);
    }

    @SuppressWarnings("java:S107")
    private Long crearLote(String codigo, String estilo, LocalDate fechaElab,
                            double agua, double ph, double litros,
                            Long fermentadorId, Long recetaId,
                            Integer og, Integer fg, LocalDate fgFecha,
                            LocalDate fermInicio, LocalDate fermIdeal, Double fermTemp, LocalDate fermFinal,
                            LocalDate acondInicio, LocalDate acondIdeal, Double acondTemp, LocalDate acondFinal,
                            LocalDate madurInicio, LocalDate madurIdeal, Double madurTemp, LocalDate madurFinal,
                            LocalDate carbInicio, LocalDate carbIdeal, Double carbTemp, LocalDate carbFinal) {
        return jdbc.queryForObject(
            "INSERT INTO lotes_cerveza"
            + " (codigo_lote, estilo, fecha_elaboracion, agua_utilizada, ph_agua, litros_finales,"
            + "  equipo_fermentador_id, receta_id, densidad_inicial, densidad_final, densidad_final_fecha,"
            + "  ferm_fecha_inicial, ferm_fecha_final_ideal, ferm_temperatura, ferm_fecha_final,"
            + "  acond_fecha_inicial, acond_fecha_final_ideal, acond_temperatura, acond_fecha_final,"
            + "  madur_fecha_inicial, madur_fecha_final_ideal, madur_temperatura, madur_fecha_final,"
            + "  carb_fecha_inicial, carb_fecha_final_ideal, carb_temperatura, carb_fecha_final,"
            + "  created_at, tenant_id)"
            + " VALUES (?,?,?,?,?,?, ?,?,?,?,?, ?,?,?,?, ?,?,?,?, ?,?,?,?, ?,?,?,?, NOW(),?)"
            + " RETURNING id",
            Long.class,
            codigo, estilo, fechaElab, agua, ph, litros,
            fermentadorId, recetaId, og, fg, fgFecha,
            fermInicio, fermIdeal, fermTemp, fermFinal,
            acondInicio, acondIdeal, acondTemp, acondFinal,
            madurInicio, madurIdeal, madurTemp, madurFinal,
            carbInicio, carbIdeal, carbTemp, carbFinal,
            TENANT);
    }

    private void crearIngredientes(Long loteId, Object[][] ingredientes) {
        for (Object[] ing : ingredientes) {
            jdbc.update(
                "INSERT INTO ingredientes (tipo, nombre, cantidad, lote_id, tenant_id) VALUES (?,?,?,?,?)",
                ing[0], ing[1], ing[2], loteId, TENANT);
        }
    }

    private void vincularCostosLotes(Long l1, Long l2, Long l3) {
        Long maltaId = jdbc.queryForObject(
            "SELECT fi.id FROM factura_items fi"
            + " JOIN facturas_proveedor f ON fi.factura_id = f.id"
            + " WHERE f.numero_factura = 'FAC-2024-001' AND fi.nombre = 'Malta Pale Ale' AND fi.tenant_id = ?",
            Long.class, TENANT);
        Long lupuloId = jdbc.queryForObject(
            "SELECT fi.id FROM factura_items fi"
            + " JOIN facturas_proveedor f ON fi.factura_id = f.id"
            + " WHERE f.numero_factura = 'FAC-2024-001' AND fi.nombre = 'Lúpulo Cascade' AND fi.tenant_id = ?",
            Long.class, TENANT);

        // PALE-001: 7 kg Malta + 0.050 kg Cascade → costo $264.5k | ingresos $319.2k → margen ~17%
        insertarItemLote(l1, maltaId,  new BigDecimal("7.000"));
        insertarItemLote(l1, lupuloId, new BigDecimal("0.050"));
        // PALE-002: 9 kg Malta + 0.060 kg Cascade → costo $339k (sin venta aún)
        insertarItemLote(l2, maltaId,  new BigDecimal("9.000"));
        insertarItemLote(l2, lupuloId, new BigDecimal("0.060"));
        // IPA-001: 9 kg Malta + 0.080 kg Cascade → costo $344k (en fermentación)
        insertarItemLote(l3, maltaId,  new BigDecimal("9.000"));
        insertarItemLote(l3, lupuloId, new BigDecimal("0.080"));
    }

    private void insertarItemLote(Long loteId, Long facturaItemId, BigDecimal cantidad) {
        jdbc.update(
            "INSERT INTO lote_items_factura (tenant_id, lote_id, factura_item_id, cantidad_asignada)"
            + " VALUES (?, ?, ?, ?)",
            TENANT, loteId, facturaItemId, cantidad);
    }

    private void crearLecturas(Long loteId, Object[][] lecturas) {
        LocalDate hoy = LocalDate.now();
        for (Object[] l : lecturas) {
            int offset    = (int) l[0];
            Integer dens  = (Integer) l[1];
            Double tempD  = (Double) l[2];
            String notas  = (String) l[3];
            jdbc.update(
                "INSERT INTO lecturas_fermentacion (tenant_id, lote_id, fecha, densidad, temperatura, notas)"
                + " VALUES (?, ?, ?, ?, ?, ?)",
                TENANT, loteId, hoy.plusDays(offset), dens,
                tempD != null ? new BigDecimal(tempD.toString()) : null, notas);
        }
    }

    private void crearTareas(Long l1, Long l2, Long l3) {
        LocalDate hoy = LocalDate.now();

        Long equipoId = jdbc.queryForObject(
            "SELECT id FROM equipos WHERE nombre = ? AND tenant_id = ?",
            Long.class, "Fermentador Cónico 100L", TENANT);
        Long proveedorId = jdbc.queryForObject(
            "SELECT id FROM proveedores WHERE nombre = ? AND tenant_id = ?",
            Long.class, "Maltería del Norte", TENANT);
        Long insumoId = jdbc.queryForObject(
            "SELECT id FROM insumos_inventario WHERE nombre = ? AND tenant_id = ?",
            Long.class, "Malta Pale Ale", TENANT);
        Long recetaId = jdbc.queryForObject(
            "SELECT id FROM recetas WHERE nombre = ? AND tenant_id = ? AND deleted_at IS NULL",
            Long.class, "IPA Americana", TENANT);

        // Tarea 1 — Sanitizar fermentador (EN_PROGRESO, ALTA, vence hoy+2, 2/4 ítems completados)
        Long t1 = insertarTarea(
            "Sanitizar fermentador Cónico 100L",
            "Proceso de CIP completo tras finalizar la fermentación de IPA-001.",
            hoy.plusDays(2), "ALTA", "EN_PROGRESO");
        insertarTareaItem(t1, "Vaciar y enjuagar con agua caliente", true,  1);
        insertarTareaItem(t1, "Aplicar solución cáustica 2%",        true,  2);
        insertarTareaItem(t1, "Sanitizar con StarSan",                false, 3);
        insertarTareaItem(t1, "Verificar sello hermético",            false, 4);
        insertarTareaRef(t1, "EQUIPO", equipoId, "Fermentador Cónico 100L",
            "/equipos/ver/" + equipoId, 1);
        insertarTareaRef(t1, "LOTE", l3, "IPA-001", "/ver/" + l3, 2);

        // Tarea 2 — Controlar carbonatación PALE-002 (PENDIENTE, ALTA, vence hoy+1)
        Long t2 = insertarTarea(
            "Controlar carbonatación PALE-002",
            "Verificar presión y ajustar niveles de CO₂ para PALE-002 en acondicionamiento.",
            hoy.plusDays(1), "ALTA", "PENDIENTE");
        insertarTareaItem(t2, "Medir presión con manómetro",         false, 1);
        insertarTareaItem(t2, "Registrar lectura en sistema",         false, 2);
        insertarTareaItem(t2, "Ajustar presión si es necesario",      false, 3);
        insertarTareaRef(t2, "LOTE", l2, "PALE-002", "/ver/" + l2, 1);

        // Tarea 3 — Preparar pedido materias primas (PENDIENTE, MEDIA, vence hoy+7, 1/3 completado)
        Long t3 = insertarTarea(
            "Preparar pedido a Maltería del Norte",
            "Reabastecer materias primas para los próximos 2 lotes de Pale Ale e IPA.",
            hoy.plusDays(7), "MEDIA", "PENDIENTE");
        insertarTareaItem(t3, "Revisar stock actual en inventario",        true,  1);
        insertarTareaItem(t3, "Calcular necesidades para próximos lotes",  false, 2);
        insertarTareaItem(t3, "Enviar orden de compra",                    false, 3);
        insertarTareaRef(t3, "PROVEEDOR", proveedorId, "Maltería del Norte",
            "/proveedores/editar/" + proveedorId, 1);
        insertarTareaRef(t3, "INSUMO", insumoId, "Malta Pale Ale",
            "/inventario/" + insumoId + "/historial", 2);

        // Tarea 4 — Ajustar receta IPA Americana (COMPLETADA, BAJA, venció hoy-3)
        Long t4 = insertarTarea(
            "Ajustar receta IPA Americana",
            "Revisar los resultados del lote IPA-001 y actualizar la receta con mejoras de lupulado.",
            hoy.minusDays(3), "BAJA", "COMPLETADA");
        insertarTareaItem(t4, "Revisar balance lúpulo/malta del último lote",  true, 1);
        insertarTareaItem(t4, "Comparar con resultados del lote IPA-001",      true, 2);
        insertarTareaItem(t4, "Actualizar notas y observaciones en receta",    true, 3);
        insertarTareaRef(t4, "RECETA", recetaId, "IPA Americana",
            "/recetas/ver/" + recetaId, 1);
        insertarTareaRef(t4, "LOTE", l3, "IPA-001", "/ver/" + l3, 2);
    }

    private Long insertarTarea(String titulo, String descripcion,
                                LocalDate vencimiento, String prioridad, String estado) {
        return jdbc.queryForObject(
            "INSERT INTO tareas"
            + " (tenant_id, titulo, descripcion, fecha_vencimiento, prioridad, estado,"
            + "  asignado_a, creado_por, created_at, updated_at)"
            + " VALUES (?,?,?,?,?,?, ?,?, NOW(), NOW()) RETURNING id",
            Long.class, TENANT, titulo, descripcion, vencimiento, prioridad, estado,
            "demo", "demo");
    }

    private void insertarTareaItem(Long tareaId, String descripcion, boolean completado, int orden) {
        jdbc.update(
            "INSERT INTO tarea_items (tenant_id, tarea_id, descripcion, completado, orden_item)"
            + " VALUES (?,?,?,?,?)",
            TENANT, tareaId, descripcion, completado, orden);
    }

    private void insertarTareaRef(Long tareaId, String tipo, Long entidadId,
                                   String label, String url, int orden) {
        jdbc.update(
            "INSERT INTO tarea_referencias (tenant_id, tarea_id, tipo, entidad_id, label, url, orden)"
            + " VALUES (?,?,?,?,?,?,?)",
            TENANT, tareaId, tipo, entidadId, label, url, orden);
    }

    private void crearNotificaciones(Long l2) {
        LocalDate hoy = LocalDate.now();
        // 1 — Bajo stock: Irish Moss (cantidad 8 gr < mínimo 10 gr)
        insertarNotif("BAJO_STOCK",
            "Bajo stock: Irish Moss",
            "Stock actual 8 gr — por debajo del mínimo (10 gr). Considera reabastecer.",
            "/inventario?filtroBajoStock=true");
        // 2 — Vencimiento próximo: Levadura US-05 vence en 18 días
        insertarNotif("VENCIMIENTO",
            "Vencimiento próximo: Levadura US-05",
            "Vence el " + hoy.plusDays(18) + " — quedan 18 días para su consumo.",
            "/inventario?filtroPorVencer=true");
        // 3 — Tarea urgente mañana
        insertarNotif("TAREA_VENCIMIENTO",
            "Tarea urgente: Controlar carbonatación PALE-002",
            "La tarea vence mañana. Recuerda medir la presión y registrar la lectura.",
            "/tareas");
        // 4 — Lote en carbonatación lleva más tiempo del ideal
        insertarNotif("LOTE_FASE_VENCIDA",
            "Carbonatación en curso: PALE-002",
            "El lote PALE-002 lleva " + hoy.minusDays(1).until(hoy.plusDays(6), java.time.temporal.ChronoUnit.DAYS)
                + " días de los 7 ideales en carbonatación. Revisa el avance.",
            "/ver/" + l2);
    }

    private void insertarNotif(String tipo, String titulo, String mensaje, String urlAccion) {
        jdbc.update(
            "INSERT INTO notificaciones (tenant_id, tipo, titulo, mensaje, url_accion, leida, created_at)"
            + " VALUES (?,?,?,?,?,false,NOW())",
            TENANT, tipo, titulo, mensaje, urlAccion);
    }

    private void crearRegistrosBpm() {
        LocalDate hoy = LocalDate.now();

        // Salud diaria — HOY sin síntomas: crítico para que BpmSaludFilter no bloquee al demo user
        jdbc.update(
            "INSERT INTO bpm_registros_sintomas"
            + " (tenant_id, fecha, nombre_manipulador, diarrea, vomito, fiebre,"
            + "  infeccion_respiratoria, lesion_piel, autorizado_por_admin, created_at)"
            + " VALUES (?,?,?,false,false,false,false,false,false,NOW())",
            TENANT, hoy, "Demo Usuario");
        // Dos días anteriores para que el historial no luzca vacío
        jdbc.update(
            "INSERT INTO bpm_registros_sintomas"
            + " (tenant_id, fecha, nombre_manipulador, diarrea, vomito, fiebre,"
            + "  infeccion_respiratoria, lesion_piel, autorizado_por_admin, created_at)"
            + " VALUES (?,?,?,false,false,false,false,false,false,NOW())",
            TENANT, hoy.minusDays(1), "Demo Usuario");
        jdbc.update(
            "INSERT INTO bpm_registros_sintomas"
            + " (tenant_id, fecha, nombre_manipulador, diarrea, vomito, fiebre,"
            + "  infeccion_respiratoria, lesion_piel, autorizado_por_admin, created_at)"
            + " VALUES (?,?,?,false,false,false,false,false,false,NOW())",
            TENANT, hoy.minusDays(2), "Demo Usuario");

        // Limpieza y desinfección — 3 registros en días distintos
        jdbc.update(
            "INSERT INTO bpm_limpieza_desinfeccion"
            + " (tenant_id, fecha, dia, area_utensilio, detergente_usado, sanitizador_usado,"
            + "  concentracion, responsable, visto_bueno, created_at)"
            + " VALUES (?,?,?,?,?,?,?,?,?,NOW())",
            TENANT, hoy.minusDays(1), hoy.minusDays(1).getDayOfWeek().name().substring(0, 3),
            "Sala de elaboración", "Soda Cáustica 2%", "StarSan 0.1%",
            "100 ppm", "demo", true);
        jdbc.update(
            "INSERT INTO bpm_limpieza_desinfeccion"
            + " (tenant_id, fecha, dia, area_utensilio, detergente_usado, sanitizador_usado,"
            + "  concentracion, responsable, visto_bueno, created_at)"
            + " VALUES (?,?,?,?,?,?,?,?,?,NOW())",
            TENANT, hoy.minusDays(3), hoy.minusDays(3).getDayOfWeek().name().substring(0, 3),
            "Fermentadores", "Caustic Clean 1.5%", "Iodophor 25 ppm",
            "25 ppm", "demo", true);
        jdbc.update(
            "INSERT INTO bpm_limpieza_desinfeccion"
            + " (tenant_id, fecha, dia, area_utensilio, detergente_usado, sanitizador_usado,"
            + "  concentracion, responsable, visto_bueno, created_at)"
            + " VALUES (?,?,?,?,?,?,?,?,?,NOW())",
            TENANT, hoy.minusDays(7), hoy.minusDays(7).getDayOfWeek().name().substring(0, 3),
            "Área de envasado y almacén", "PBW 1%", "StarSan 0.1%",
            "100 ppm", "demo", true);

        // Solución desinfectante — preparación reciente
        jdbc.update(
            "INSERT INTO bpm_soluciones_desinfectantes"
            + " (tenant_id, fecha, hora, producto, cantidad_agua, unidad_agua,"
            + "  cantidad_producto, unidad_producto, concentracion_final, responsable, created_at)"
            + " VALUES (?,?,?,?,?,?,?,?,?,?,NOW())",
            TENANT, hoy.minusDays(1), LocalTime.of(7, 30),
            "StarSan", new BigDecimal("20.000"), "L",
            new BigDecimal("2.000"), "mL", new BigDecimal("0.10"), "demo");

        // Control de plagas — última inspección sin novedades
        jdbc.update(
            "INSERT INTO bpm_avistamiento_plagas"
            + " (tenant_id, fecha, presencia_plagas, tipo_plagas,"
            + "  estado_ventanas_puertas, accion_tomada, created_at)"
            + " VALUES (?,?,false,null,'OK','Sin novedades. Área limpia y sellada.',NOW())",
            TENANT, hoy.minusDays(7));
    }

    private void crearVenta(Long loteId, String codigoLote, Long clienteId) {
        Long ventaId = jdbc.queryForObject(
            "INSERT INTO ventas"
            + " (tenant_id, cliente, cliente_id, fecha_despacho, notas, estado, created_at)"
            + " VALUES (?,'Bar Los Artesanos',?, CURRENT_DATE - 5,"
            + "  'Pedido mensual. Entregar en horario nocturno.','DESPACHADO', NOW()) RETURNING id",
            Long.class, TENANT, clienteId);

        jdbc.update(
            "INSERT INTO venta_items"
            + " (tenant_id, venta_id, lote_id, codigo_lote, descripcion,"
            + "  cantidad, unidad, precio_unitario, descuento_pct)"
            + " VALUES (?,?,?,?,'Pale Ale Clásica 330ml', 48,'48 × Botella 330ml', 7000, 5)",
            TENANT, ventaId, loteId, codigoLote);
    }
}
