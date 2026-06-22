package com.alera.util;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Genera archivos Excel de prueba para todos los módulos de migración.
 * Ejecutar como test JUnit para producir los .xlsx en src/test/resources/migracion-test/.
 * Los datos son consistentes entre archivos (mismo tenant ficticio "Cervecería El Aguila"):
 *   - comercial usa proveedores definidos en el mismo archivo
 *   - ventas y barriles referencian el lote IPA-2024-001 del archivo produccion
 *   - seguimiento y mantenimientos requieren que produccion y equipos ya estén importados
 * Orden de importación recomendado: 01 → 02 → 03 → 04 → 05 → 06 → 07 → 08 → 09 → 10 → 11
 */
class MigracionTestDataGenerator {

    private static final String OUT_DIR = "src/test/resources/migracion-test";

    @Test
    void generarTodos() throws IOException {
        Path dir = Paths.get(OUT_DIR);
        Files.createDirectories(dir);

        generarCatalogos(dir);
        generarAlmacen(dir);
        generarEquipos(dir);
        generarComercial(dir);
        generarProduccion(dir);
        generarClientes(dir);
        generarVentas(dir);
        generarBarriles(dir);
        generarOrdenes(dir);
        generarSeguimiento(dir);
        generarMantenimientos(dir);

        System.out.println("Archivos generados en: " + dir.toAbsolutePath());
    }

    // ── 01 – Catálogos ────────────────────────────────────────────────────────

    private void generarCatalogos(Path dir) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // Tipos_Cerveza — col: [0]nombre [1]descripcion [2]activo
            Sheet tc = wb.createSheet("Tipos_Cerveza");
            hdr(tc, "nombre", "descripcion", "activo");
            data(tc, 0, "American IPA",       "IPA con lúpulos americanos, amargor moderado-alto", "TRUE");
            data(tc, 1, "Imperial Stout",      "Stout oscura, alta graduación, sabores a chocolate y café", "TRUE");
            data(tc, 2, "Munich Lager",        "Lager alemana, limpia y refrescante", "TRUE");
            data(tc, 3, "Robust Porter",       "Porter de cuerpo medio, notas tostadas", "TRUE");
            data(tc, 4, "Hefeweizen",          "Trigo alemán, esteres de plátano y clavo", "TRUE");
            data(tc, 5, "Belgian Witbier",     "Trigo belga, coriandro y cáscara de naranja", "TRUE");
            data(tc, 6, "Saison",              "Ale belga artesanal, seca y especiada", "FALSE");

            // Tipos_Insumo — col: [0]nombre [1]activo
            Sheet ti = wb.createSheet("Tipos_Insumo");
            hdr(ti, "nombre", "activo");
            data(ti, 0, "Lúpulo fresco",       "TRUE");
            data(ti, 1, "Extracto de malta",   "TRUE");
            data(ti, 2, "Adjunto maíz",        "TRUE");
            data(ti, 3, "Adjunto trigo",       "TRUE");
            data(ti, 4, "Enzima amilasa",      "FALSE");

            // Tipos_Equipo — col: [0]nombre [1]activo
            Sheet te = wb.createSheet("Tipos_Equipo");
            hdr(te, "nombre", "activo");
            data(te, 0, "Tanque de presión",  "TRUE");
            data(te, 1, "Chiller de placas",  "TRUE");
            data(te, 2, "Molino de malta",    "TRUE");

            save(wb, dir, "01-catalogos.xlsx");
        }
    }

    // ── 02 – Almacén ──────────────────────────────────────────────────────────

    private void generarAlmacen(Path dir) throws IOException {
        // cols: [0]nombre [1]tipo [2]cantidad [3]unidad [4]stock_minimo [5]proveedor [6]fecha_vencimiento [7]obs
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sh = wb.createSheet("Insumos");
            hdr(sh, "nombre", "tipo", "cantidad", "unidad", "stock_minimo",
                    "proveedor", "fecha_vencimiento", "observaciones");
            int r = 0;
            data(sh, r++, "Pale Ale 2-Row",   "MALTA",      "25000", "gr", "2000",
                    "Maltería del Sur",    "2026-12-31", "Malta base colombiana");
            data(sh, r++, "Caramel 60L",      "MALTA",      "5000",  "gr", "500",
                    "Maltería del Sur",    "2026-12-31", "Crystal 60");
            data(sh, r++, "Munich 10L",       "MALTA",      "8000",  "gr", "800",
                    "Maltería del Sur",    "2026-12-31", "");
            data(sh, r++, "Roasted Barley",   "MALTA",      "3000",  "gr", "300",
                    "Maltería del Sur",    "2026-12-31", "Para stout");
            data(sh, r++, "Cascade",          "LUPULO",     "1000",  "gr", "100",
                    "Lúpulos Andinos",     "2026-06-30", "6.8% alfa-ácidos");
            data(sh, r++, "Citra",            "LUPULO",     "500",   "gr", "50",
                    "Lúpulos Andinos",     "2026-06-30", "12% alfa-ácidos");
            data(sh, r++, "Centennial",       "LUPULO",     "400",   "gr", "40",
                    "Lúpulos Andinos",     "2026-06-30", "10% alfa-ácidos");
            data(sh, r++, "US-05",            "LEVADURA",   "110",   "gr", "11",
                    "",                    "",            "Sachet 11g, almacenar < 8°C");
            data(sh, r++, "WY3068",           "LEVADURA",   "88",    "gr", "11",
                    "",                    "2026-03-01",  "Hefeweizen, líquida");
            data(sh, r++, "Whirlfloc",        "CLARIFICANTE","200",  "gr", "20",
                    "Distribuidora NorCo", "",            "Tabletas Irish Moss");
            data(sh, r++, "Gelatin",          "CLARIFICANTE","100",  "gr", "10",
                    "",                    "",            "Gelatina sin sabor");
            data(sh, r++, "Dextrosa",         "AGENTE_CARBONATACION","500","gr","50",
                    "Distribuidora NorCo", "",            "Para refermentación");
            data(sh, r++, "Botella 330mL",    "ENVASE",     "1200",  "und","100",
                    "Envases El Dorado",   "",            "Retornable marrón");
            data(sh, r++, "Botella 750mL",    "ENVASE",     "600",   "und","60",
                    "Envases El Dorado",   "",            "Para ediciones especiales");
            data(sh, r++, "Agua Tratada",     "AGUA",       "500",   "L",  "100",
                    "",                    "",            "pH 7.0, tratada con campden");

            save(wb, dir, "02-almacen.xlsx");
        }
    }

    // ── 03 – Equipos ──────────────────────────────────────────────────────────

    private void generarEquipos(Path dir) throws IOException {
        // cols: [0]nombre [1]tipo [2]estado [3]capacidad [4]unidad_cap [5]fec_adq
        //       [6]proximo_mant [7]fecha_ult_mant [8]observaciones
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sh = wb.createSheet("Equipos");
            hdr(sh, "nombre", "tipo", "estado", "capacidad", "unidad_capacidad",
                    "fecha_adquisicion", "proximo_mantenimiento",
                    "fecha_ultimo_mantenimiento", "observaciones");
            int r = 0;
            data(sh, r++,
                    "Fermentador A", "FERMENTADOR", "OPERATIVO", "300", "L",
                    "2022-01-10", "2026-07-10", "2025-12-20",
                    "Tanque cónico acero inox 304, termómetro digital");
            data(sh, r++,
                    "Fermentador B", "FERMENTADOR", "OPERATIVO", "150", "L",
                    "2023-03-15", "2026-09-15", "2025-11-15",
                    "Tanque cónico PET, airlock de agua");
            data(sh, r++,
                    "Olla Macerado",  "OLLA_MACERADO", "OPERATIVO", "100", "L",
                    "2021-05-20", "", "",
                    "Fondo falso de acero, válvula de bola 3/4\"");
            data(sh, r++,
                    "Olla Hervor",    "OLLA_HERVOR",   "MANTENIMIENTO", "120", "L",
                    "2021-05-20", "2026-07-01", "2026-06-10",
                    "Revisión mensual, termómetro descalibrado");
            data(sh, r++,
                    "Chiller Placas", "ENFRIADOR",     "OPERATIVO", "60", "L",
                    "2023-08-01", "", "",
                    "20 placas acero, enfría de 100°C a 18°C en 10 min");
            data(sh, r++,
                    "Báscula Digital","BASCULA",       "OPERATIVO", "30", "kg",
                    "2022-06-15", "2026-06-15", "2025-06-15",
                    "Precisión 1g, calibrada con pesas certificadas");

            save(wb, dir, "03-equipos.xlsx");
        }
    }

    // ── 04 – Comercial ────────────────────────────────────────────────────────

    private void generarComercial(Path dir) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {

            // Proveedores — cols: [0]nombre [1]nit [2]tel [3]email [4]dir
            Sheet shP = wb.createSheet("Proveedores");
            hdr(shP, "nombre", "nit", "telefono", "email", "direccion");
            int r = 0;
            data(shP, r++, "Maltería del Sur",    "900111222-3", "3001234567",
                    "ventas@malteriadelsur.co",    "Cra 15 #45-20, Bogotá");
            data(shP, r++, "Lúpulos Andinos",     "900333444-5", "3009876543",
                    "pedidos@lupulosandinos.co",   "Cll 80 #68-15, Medellín");
            data(shP, r++, "Distribuidora NorCo", "900555666-7", "6014445566",
                    "info@norcodist.co",           "Zona Industrial, Cali");
            data(shP, r++, "Envases El Dorado",   "900777888-1", "3101112233",
                    "envases@eldorado.co",         "Bogotá, Fontibón");

            // Facturas — cols: [0]num_fac [1]proveedor [2]fecha [3]desc [4]envio [5]estado [6]iva_incluido
            Sheet shF = wb.createSheet("Facturas");
            hdr(shF, "numero_factura", "proveedor_nombre", "fecha_factura", "descripcion",
                    "costo_envio", "estado", "iva_incluido");
            r = 0;
            data(shF, r++, "FAC-2024-001", "Maltería del Sur",    "2024-02-15",
                    "Compra insumos Q1 2024", "15000", "PAGADA", "FALSE");
            data(shF, r++, "FAC-2024-015", "Lúpulos Andinos",     "2024-03-01",
                    "Compra lúpulos primavera", "8000", "PAGADA", "TRUE");
            data(shF, r++, "FAC-2024-032", "Distribuidora NorCo", "2024-04-10",
                    "Clarificantes y adjuntos", "0", "VERIFICADA", "FALSE");
            data(shF, r++, "FAC-2024-048", "Envases El Dorado",   "2024-06-01",
                    "Botellas 330mL x1000", "25000", "RECIBIDA", "FALSE");

            // Factura_Items — cols: [0]num_fac [1]tipo_item [2]nombre [3]tipo_ins [4]tipo_eq
            //   [5]cantidad [6]unidad [7]val_unit [8]pct_desc [9]pct_iva [10]impuesto_consumo
            Sheet shI = wb.createSheet("Factura_Items");
            hdr(shI, "numero_factura", "tipo_item", "nombre", "tipo_insumo", "tipo_equipo",
                    "cantidad", "unidad", "valor_unitario", "porcentaje_descuento",
                    "porcentaje_iva", "impuesto_consumo");
            r = 0;
            data(shI, r++, "FAC-2024-001", "INSUMO", "Pale Ale 2-Row", "MALTA", "",
                    "50", "kg", "4800", "0", "19", "0");
            data(shI, r++, "FAC-2024-001", "INSUMO", "Caramel 60L",    "MALTA", "",
                    "10", "kg", "6500", "0", "19", "0");
            data(shI, r++, "FAC-2024-001", "INSUMO", "Munich 10L",     "MALTA", "",
                    "8",  "kg", "5900", "5", "19", "0");
            data(shI, r++, "FAC-2024-015", "INSUMO", "Cascade",        "LUPULO", "",
                    "2",  "kg", "55000", "0", "19", "490");
            data(shI, r++, "FAC-2024-015", "INSUMO", "Citra",          "LUPULO", "",
                    "1",  "kg", "72000", "0", "19", "490");
            data(shI, r++, "FAC-2024-032", "INSUMO", "Whirlfloc",      "CLARIFICANTE", "",
                    "500", "gr", "180", "0", "19", "0");
            data(shI, r++, "FAC-2024-032", "INSUMO", "Dextrosa",       "AGENTE_CARBONATACION", "",
                    "2",  "kg", "4200", "0", "19", "0");
            data(shI, r++, "FAC-2024-048", "INSUMO", "Botella 330mL",  "ENVASE", "",
                    "1000", "und", "450", "0", "19", "0");

            save(wb, dir, "04-comercial.xlsx");
        }
    }

    // ── 05 – Producción ───────────────────────────────────────────────────────

    private void generarProduccion(Path dir) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {

            // Recetas — cols: [0]nombre [1]estilo [2]desc [3]activa [4]vol_base [5]hervor
            //   [6]og_obj [7]fg_obj [8]agua_mac [9]unid_mac [10]agua_sp [11]unid_sp [12]ph [13]notas
            Sheet shR = wb.createSheet("Recetas");
            hdr(shR, "nombre", "estilo", "descripcion", "activa",
                    "volumen_base", "tiempo_hervor_minutos", "og_objetivo", "fg_objetivo",
                    "agua_macerado", "unidad_agua_macerado", "agua_sparge", "unidad_agua_sparge",
                    "ph_agua", "notas");
            int r = 0;
            data(shR, r++,
                    "IPA Clásica Aguila", "American IPA",
                    "Receta insignia con cascade y citra, amargor limpio y aroma frutal",
                    "TRUE", "20", "60", "1058", "1012",
                    "18", "L", "8", "L", "5.4",
                    "Dry hop 48h con citra adicional");
            data(shR, r++,
                    "Stout Imperial 2024", "Imperial Stout",
                    "Oscura e intensa, notas a café y chocolate amargo",
                    "TRUE", "18", "75", "1082", "1020",
                    "22", "L", "6", "L", "5.2",
                    "Mash escalonado, temp acondicionamiento 10°C x 4 semanas");
            data(shR, r++,
                    "Hefeweizen Dorada", "Hefeweizen",
                    "Trigo alemán con banana y clavo, levadura WY3068",
                    "TRUE", "20", "60", "1050", "1010",
                    "16", "L", "10", "L", "5.8",
                    "Sin clarificar, turbidez es parte del estilo");

            // Receta_Ingredientes — cols: [0]nombre_receta [1]tipo [2]nombre_ing [3]cant_con_unidad
            Sheet shRI = wb.createSheet("Receta_Ingredientes");
            hdr(shRI, "nombre_receta", "tipo", "nombre_ingrediente", "cantidad_con_unidad");
            r = 0;
            // IPA
            data(shRI, r++, "IPA Clásica Aguila", "MALTA",     "Pale Ale 2-Row",  "4500 gr");
            data(shRI, r++, "IPA Clásica Aguila", "MALTA",     "Caramel 60L",     "450 gr");
            data(shRI, r++, "IPA Clásica Aguila", "LUPULO",    "Cascade",         "45 gr");
            data(shRI, r++, "IPA Clásica Aguila", "LUPULO",    "Citra",           "20 gr");
            data(shRI, r++, "IPA Clásica Aguila", "LEVADURA",  "US-05",           "11 gr");
            data(shRI, r++, "IPA Clásica Aguila", "CLARIFICANTE", "Whirlfloc",    "1 tab");
            // Stout
            data(shRI, r++, "Stout Imperial 2024","MALTA",     "Pale Ale 2-Row",  "5500 gr");
            data(shRI, r++, "Stout Imperial 2024","MALTA",     "Munich 10L",      "800 gr");
            data(shRI, r++, "Stout Imperial 2024","MALTA",     "Roasted Barley",  "600 gr");
            data(shRI, r++, "Stout Imperial 2024","LUPULO",    "Centennial",      "30 gr");
            data(shRI, r++, "Stout Imperial 2024","LEVADURA",  "US-05",           "22 gr");
            // Hefeweizen
            data(shRI, r++, "Hefeweizen Dorada",  "MALTA",     "Pale Ale 2-Row",  "2000 gr");
            data(shRI, r++, "Hefeweizen Dorada",  "MALTA",     "Munich 10L",      "2200 gr");
            data(shRI, r++, "Hefeweizen Dorada",  "LEVADURA",  "WY3068",          "88 gr");

            // Receta_Escalones — cols: [0]nombre_receta [1]nombre_esc [2]temp_c [3]dur_min [4]orden
            Sheet shES = wb.createSheet("Receta_Escalones");
            hdr(shES, "nombre_receta", "nombre_escalon", "temperatura_c", "duracion_minutos", "orden");
            r = 0;
            data(shES, r++, "IPA Clásica Aguila", "Sacarificación",  "67.5", "60", "1");
            data(shES, r++, "IPA Clásica Aguila", "Mash-out",         "76",   "10", "2");
            data(shES, r++, "Stout Imperial 2024","Proteólisis",      "52",   "20", "1");
            data(shES, r++, "Stout Imperial 2024","Sacarificación",   "67",   "60", "2");
            data(shES, r++, "Stout Imperial 2024","Mash-out",         "76",   "10", "3");
            data(shES, r++, "Hefeweizen Dorada",  "Acid rest",        "45",   "15", "1");
            data(shES, r++, "Hefeweizen Dorada",  "Sacarificación",   "65",   "50", "2");
            data(shES, r++, "Hefeweizen Dorada",  "Mash-out",         "76",   "10", "3");

            // Receta_Adiciones — cols: [0]nombre_receta [1]nombre [2]min_rest [3]cant [4]unidad [5]orden
            Sheet shAD = wb.createSheet("Receta_Adiciones");
            hdr(shAD, "nombre_receta", "nombre", "minutos_restantes", "cantidad", "unidad", "orden");
            r = 0;
            data(shAD, r++, "IPA Clásica Aguila", "Cascade",    "60", "25", "gr", "1");
            data(shAD, r++, "IPA Clásica Aguila", "Cascade",    "15", "15", "gr", "2");
            data(shAD, r++, "IPA Clásica Aguila", "Whirlfloc",  "10", "1",  "tab","3");
            data(shAD, r++, "IPA Clásica Aguila", "Citra",      "0",  "10", "gr", "4");
            data(shAD, r++, "Stout Imperial 2024","Centennial", "60", "30", "gr", "1");
            data(shAD, r++, "Stout Imperial 2024","Whirlfloc",  "10", "1",  "tab","2");
            data(shAD, r++, "Hefeweizen Dorada",  "Hallertau",  "60", "15", "gr", "1");

            // Lotes — cols 0-41 (ver MigracionService.importarProduccion)
            // [0]codigo [1]estilo [2]fec_elab [3]litros [4]og [5]fg [6]agua [7]ph
            // [8]clar [9]obs [10]notas_cata [11]receta [12]carb_metodo [13]co2_obj [14]co2_real
            // [15]azucar_tipo [16]azucar_gr [17]presion_psi [18]tiempo_h [19]tecnica [20]validacion
            // [21]destino [22]og_brix [23]fg_brix [24]ferm_nombre [25]dens_final_fecha
            // [26]ferm_ini [27]ferm_ideal [28]ferm_temp [29]ferm_fin
            // [30]acond_ini [31]acond_ideal [32]acond_temp [33]acond_fin
            // [34]madur_ini [35]madur_ideal [36]madur_temp [37]madur_fin
            // [38]carb_ini [39]carb_ideal [40]carb_temp [41]carb_fin
            Sheet shL = wb.createSheet("Lotes");
            hdr(shL,
                    "codigo_lote","estilo","fecha_elaboracion","litros_finales",
                    "densidad_inicial","densidad_final","agua_utilizada","ph_agua",
                    "clarificante","observaciones","notas_cata","receta_nombre",
                    "carb_metodo","carb_co2_objetivo","carb_co2_real","carb_azucar_tipo",
                    "carb_azucar_gramos","carb_presion_psi","carb_tiempo_horas",
                    "carb_tecnica","carb_validacion","carb_destino",
                    "og_brix","fg_brix","fermentador_nombre","densidad_final_fecha",
                    "ferm_fecha_inicial","ferm_fecha_final_ideal","ferm_temperatura","ferm_fecha_final",
                    "acond_fecha_inicial","acond_fecha_final_ideal","acond_temperatura","acond_fecha_final",
                    "madur_fecha_inicial","madur_fecha_final_ideal","madur_temperatura","madur_fecha_final",
                    "carb_fecha_inicial","carb_fecha_final_ideal","carb_temperatura","carb_fecha_final");
            r = 0;
            // Lote 1 — IPA completo con todas las fases y campos nuevos
            data(shL, r++,
                    "IPA-2024-001","American IPA","2024-03-15","19.5",
                    "1057","1012","24","5.4",
                    "Whirlfloc","Primer lote IPA del año, muy buena atenuación","Aroma floral prominente, amargor limpio, retrogusto frutal","IPA Clásica Aguila",
                    "NATURAL","2.4","2.4","dextrosa",
                    "118","","","","ADECUADA","Botella 330mL",
                    "14.2","3.5","Fermentador A","2024-03-29",
                    "2024-03-16","2024-03-30","18.0","2024-03-29",
                    "2024-03-30","2024-04-06","12.0","2024-04-05",
                    "2024-04-06","2024-05-07","5.0","2024-05-05",
                    "2024-05-06","2024-05-13","4.0","2024-05-12");
            // Lote 2 — Stout en maduración (sin datos de carbonatación aún)
            data(shL, r++,
                    "STT-2024-001","Imperial Stout","2024-04-20","17.0",
                    "1082","1021","26","5.2",
                    "Gelatin","Segunda hornada, densidad final más alta de lo esperado","","Stout Imperial 2024",
                    "","","","",
                    "","","","","","",
                    "","","Fermentador B","2024-05-10",
                    "2024-04-21","2024-05-05","19.0","2024-05-05",
                    "2024-05-06","2024-05-13","12.0","2024-05-13",
                    "2024-05-14","2024-06-14","4.0","",
                    "","","","");
            // Lote 3 — Hefeweizen simple (campos mínimos)
            data(shL, r++,
                    "HEF-2024-001","Hefeweizen","2024-05-10","20.0",
                    "1050","","20","5.8",
                    "","Sin clarificar — turbidez natural del estilo","","Hefeweizen Dorada",
                    "","","","","","","","","","",
                    "","","Fermentador A","",
                    "2024-05-11","2024-05-21","18.0","",
                    "","","","",
                    "","","","",
                    "","","","");

            // Lote_Ingredientes — cols: [0]codigo [1]tipo [2]nombre [3]cant_con_unidad
            Sheet shLI = wb.createSheet("Lote_Ingredientes");
            hdr(shLI, "codigo_lote", "tipo", "nombre_ingrediente", "cantidad_con_unidad");
            r = 0;
            data(shLI, r++, "IPA-2024-001", "MALTA",        "Pale Ale 2-Row",  "4500 gr");
            data(shLI, r++, "IPA-2024-001", "MALTA",        "Caramel 60L",     "450 gr");
            data(shLI, r++, "IPA-2024-001", "LUPULO",       "Cascade",         "45 gr");
            data(shLI, r++, "IPA-2024-001", "LUPULO",       "Citra",           "20 gr");
            data(shLI, r++, "IPA-2024-001", "LEVADURA",     "US-05",           "11 gr");
            data(shLI, r++, "IPA-2024-001", "CLARIFICANTE", "Whirlfloc",       "1 tab");
            data(shLI, r++, "STT-2024-001", "MALTA",        "Pale Ale 2-Row",  "5500 gr");
            data(shLI, r++, "STT-2024-001", "MALTA",        "Munich 10L",      "800 gr");
            data(shLI, r++, "STT-2024-001", "MALTA",        "Roasted Barley",  "600 gr");
            data(shLI, r++, "STT-2024-001", "LUPULO",       "Centennial",      "30 gr");
            data(shLI, r++, "STT-2024-001", "LEVADURA",     "US-05",           "22 gr");
            data(shLI, r++, "HEF-2024-001", "MALTA",        "Pale Ale 2-Row",  "2000 gr");
            data(shLI, r++, "HEF-2024-001", "MALTA",        "Munich 10L",      "2200 gr");
            data(shLI, r++, "HEF-2024-001", "LEVADURA",     "WY3068",          "88 gr");

            save(wb, dir, "05-produccion.xlsx");
        }
    }

    // ── 06 – Clientes ─────────────────────────────────────────────────────────

    private void generarClientes(Path dir) throws IOException {
        // cols: [0]nombre [1]razon_social [2]nit [3]regimen [4]email [5]tel [6]dir
        //       [7]ciudad [8]depto [9]lista_precio [10]activo [11]notas
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sh = wb.createSheet("Clientes");
            hdr(sh, "nombre", "razon_social", "nit", "regimen_tributario",
                    "email", "telefono", "direccion_despacho", "ciudad",
                    "departamento", "lista_precio", "activo", "notas");
            int r = 0;
            data(sh, r++,
                    "Cervecería El Mosto", "El Mosto SAS", "900222333-7", "RESPONSABLE_IVA",
                    "compras@elmosto.co", "3001111222", "Calle 45 #12-34",
                    "Bogotá", "Cundinamarca", "DISTRIBUIDOR", "TRUE", "Paga a 30 días");
            data(sh, r++,
                    "Bar La Espuma", "", "", "",
                    "espuma@bar.co", "3009998887", "Carrera 70 #34-15",
                    "Medellín", "Antioquia", "BAR", "TRUE", "Pedidos cada quincena");
            data(sh, r++,
                    "Tienda Craft Beer", "Craft Beer Colombia SAS", "800999111-2", "RESPONSABLE_IVA",
                    "", "3115556677", "Zona Rosa, Cl 82 #11-40",
                    "Bogotá", "Cundinamarca", "VENTA_DIRECTA", "TRUE", "");
            data(sh, r++,
                    "Restaurante La Pinta", "", "", "",
                    "lapinta@rest.co", "6015551234", "Av Jiménez #4-32",
                    "Bogotá", "Cundinamarca", "BAR", "TRUE", "Solo recogen en planta");
            data(sh, r++,
                    "Exportadora Sur América", "SA Exports Ltda", "900444555-8", "RESPONSABLE_IVA",
                    "orders@saexports.co", "3209997788", "",
                    "Cali", "Valle del Cauca", "EXPORTACION", "FALSE", "Cuenta pausada 2024");

            save(wb, dir, "06-clientes.xlsx");
        }
    }

    // ── 07 – Ventas ───────────────────────────────────────────────────────────

    private void generarVentas(Path dir) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {

            // Ventas — cols: [0]ref [1]cliente [2]nit [3]fec_desp [4]estado [5]notas [6]remision [7]cotiz_expira
            Sheet shV = wb.createSheet("Ventas");
            hdr(shV, "referencia_venta", "cliente_nombre", "cliente_nit",
                    "fecha_despacho", "estado", "notas", "remision_numero", "cotizacion_expira_en");
            int r = 0;
            data(shV, r++,
                    "V-2024-001", "Cervecería El Mosto", "900222333-7",
                    "2024-05-20", "DESPACHADO", "Entrega mensual mayo", "REM-2024-001", "");
            data(shV, r++,
                    "V-2024-002", "Bar La Espuma", "",
                    "2024-06-15", "COTIZACION", "Cotización verano IPA", "", "2024-07-15");
            data(shV, r++,
                    "V-2024-003", "Tienda Craft Beer", "800999111-2",
                    "2024-06-18", "PENDIENTE", "Pedido especial stout", "", "");
            data(shV, r++,
                    "V-2024-004", "Restaurante La Pinta", "",
                    "2024-07-01", "DESPACHADO", "Venta directa julio", "REM-2024-002", "");

            // Venta_Items — cols: [0]ref [1]codigo_lote [2]desc [3]cant [4]unidad [5]precio_u [6]desc_pct
            Sheet shI = wb.createSheet("Venta_Items");
            hdr(shI, "referencia_venta", "codigo_lote", "descripcion",
                    "cantidad", "unidad", "precio_unitario", "descuento_pct");
            r = 0;
            data(shI, r++, "V-2024-001", "IPA-2024-001", "Botella IPA 330mL",   "96",  "und",    "8500",  "0");
            data(shI, r++, "V-2024-001", "",              "Caja cartón x12",      "8",   "und",    "3500",  "0");
            data(shI, r++, "V-2024-002", "IPA-2024-001", "Barril IPA 20L",       "2",   "barril", "185000","5");
            data(shI, r++, "V-2024-002", "STT-2024-001", "Barril Stout 20L",     "1",   "barril", "220000","5");
            data(shI, r++, "V-2024-003", "STT-2024-001", "Botella Stout 750mL",  "24",  "und",    "22000", "0");
            data(shI, r++, "V-2024-004", "IPA-2024-001", "Botella IPA 330mL",    "48",  "und",    "9000",  "10");
            data(shI, r++, "V-2024-004", "HEF-2024-001", "Botella Hefeweizen 330mL","24","und",   "8500",  "10");

            save(wb, dir, "07-ventas.xlsx");
        }
    }

    // ── 08 – Barriles ─────────────────────────────────────────────────────────

    private void generarBarriles(Path dir) throws IOException {
        // cols: [0]codigo [1]tipo [2]capacidad [3]estado [4]codigo_lote [5]cliente [6]fec_desp [7]obs
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sh = wb.createSheet("Barriles");
            hdr(sh, "codigo", "tipo", "capacidad_litros", "estado",
                    "codigo_lote", "cliente_nombre", "fecha_despacho", "observaciones");
            int r = 0;
            data(sh, r++, "BAR-001", "Acero inox", "20", "DISPONIBLE",
                    "", "", "", "Limpieza completada 2024-05-01");
            data(sh, r++, "BAR-002", "Acero inox", "20", "LLENO",
                    "IPA-2024-001", "", "", "Llenado 2024-05-06, carbonatación activa");
            data(sh, r++, "BAR-003", "Acero inox", "20", "DESPACHADO",
                    "IPA-2024-001", "Bar La Espuma", "2024-05-20", "");
            data(sh, r++, "BAR-004", "Acero inox", "50", "DISPONIBLE",
                    "", "", "", "Barril grande para eventos");
            data(sh, r++, "BAR-005", "Plástico",   "30", "LIMPIEZA",
                    "", "", "", "Requiere revisión de O-rings");
            data(sh, r++, "BAR-006", "Acero inox", "20", "LLENO",
                    "STT-2024-001", "", "", "Stout imperial, maduración en curso");
            data(sh, r++, "BAR-007", "Acero inox", "20", "VACIO",
                    "", "", "", "");
            data(sh, r++, "BAR-008", "Plástico",   "20", "BAJA",
                    "", "", "", "Grieta en la costura, fuera de servicio");

            save(wb, dir, "08-barriles.xlsx");
        }
    }

    // ── 09 – Órdenes de Compra ────────────────────────────────────────────────

    private void generarOrdenes(Path dir) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {

            // OC — cols: [0]numero_oc [1]proveedor [2]fec_emis [3]fec_req [4]estado [5]notas
            Sheet shOC = wb.createSheet("OC");
            hdr(shOC, "numero_oc", "proveedor", "fecha_emision", "fecha_requerida", "estado", "notas");
            int r = 0;
            data(shOC, r++, "OC-2024-001", "Maltería del Sur",    "2024-01-15", "2024-02-01",
                    "RECIBIDA",  "Pedido insumos Q1");
            data(shOC, r++, "OC-2024-002", "Lúpulos Andinos",     "2024-02-20", "2024-03-10",
                    "RECIBIDA",  "Lúpulos temporada primavera");
            data(shOC, r++, "OC-2024-003", "Distribuidora NorCo", "2024-04-01", "2024-04-20",
                    "RECIBIDA_PARCIAL", "Solo llegaron clarificantes, dextrosa pendiente");
            data(shOC, r++, "OC-2024-004", "Envases El Dorado",   "2024-05-15", "2024-06-01",
                    "ENVIADA",   "Orden botellas 330mL para segunda mitad del año");
            data(shOC, r++, "OC-2024-005", "Maltería del Sur",    "2024-06-01", "",
                    "BORRADOR",  "Pre-pedido Q3, sujeto a confirmación");

            // OC_Items — cols: [0]oc [1]tipo_item [2]nombre [3]tipo_ins [4]tipo_eq
            //   [5]cantidad [6]unidad [7]precio_est [8]pct_iva [9]descripcion
            Sheet shI = wb.createSheet("OC_Items");
            hdr(shI, "numero_oc", "tipo_item", "nombre", "tipo_insumo", "tipo_equipo",
                    "cantidad", "unidad", "precio_unitario_estimado", "porcentaje_iva", "descripcion");
            r = 0;
            data(shI, r++, "OC-2024-001", "INSUMO", "Pale Ale 2-Row", "MALTA",  "", "50", "kg", "4800", "19", "");
            data(shI, r++, "OC-2024-001", "INSUMO", "Caramel 60L",    "MALTA",  "", "10", "kg", "6500", "19", "");
            data(shI, r++, "OC-2024-001", "INSUMO", "Munich 10L",     "MALTA",  "",  "8", "kg", "5900", "19", "");
            data(shI, r++, "OC-2024-002", "INSUMO", "Cascade",        "LUPULO", "",  "2", "kg","55000", "19", "6.8% AA");
            data(shI, r++, "OC-2024-002", "INSUMO", "Citra",          "LUPULO", "",  "1", "kg","72000", "19", "12% AA");
            data(shI, r++, "OC-2024-002", "INSUMO", "Centennial",     "LUPULO", "",  "1", "kg","60000", "19", "10% AA");
            data(shI, r++, "OC-2024-003", "INSUMO", "Whirlfloc",      "CLARIFICANTE","","500","gr","180","19","");
            data(shI, r++, "OC-2024-003", "INSUMO", "Dextrosa",       "AGENTE_CARBONATACION","","2","kg","4200","19","");
            data(shI, r++, "OC-2024-004", "INSUMO", "Botella 330mL",  "ENVASE", ""," 1000","und","450","19","Marrón retornable");
            data(shI, r++, "OC-2024-004", "INSUMO", "Botella 750mL",  "ENVASE", "",  "500","und","650","19","Especial edición limitada");
            data(shI, r++, "OC-2024-005", "INSUMO", "Pale Ale 2-Row", "MALTA",  "", "80",  "kg","4850","19","Estimado Q3");

            save(wb, dir, "09-ordenes.xlsx");
        }
    }

    // ── 10 – Seguimiento ──────────────────────────────────────────────────────

    private void generarSeguimiento(Path dir) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {

            // Lote_Lecturas — cols: [0]codigo_lote [1]fecha [2]densidad [3]temp [4]notas
            Sheet shLect = wb.createSheet("Lote_Lecturas");
            hdr(shLect, "codigo_lote", "fecha", "densidad", "temperatura", "notas");
            int r = 0;
            // IPA-2024-001
            data(shLect, r++, "IPA-2024-001", "2024-03-16", "1057", "18.5", "Día 1 — fermentación iniciando, airlock activo");
            data(shLect, r++, "IPA-2024-001", "2024-03-18", "1042", "18.5", "Fermentación activa, espuma visible");
            data(shLect, r++, "IPA-2024-001", "2024-03-20", "1035", "18.0", "Desacelerando, buen progreso");
            data(shLect, r++, "IPA-2024-001", "2024-03-23", "1022", "18.0", "Casi final");
            data(shLect, r++, "IPA-2024-001", "2024-03-25", "1018", "17.5", "");
            data(shLect, r++, "IPA-2024-001", "2024-03-27", "1014", "17.0", "");
            data(shLect, r++, "IPA-2024-001", "2024-03-29", "1012", "17.0", "Densidad objetivo alcanzada — listo para acondicionamiento");
            // STT-2024-001
            data(shLect, r++, "STT-2024-001", "2024-04-22", "1082", "19.0", "Inicio fermentación, cabeza de espuma alta");
            data(shLect, r++, "STT-2024-001", "2024-04-25", "1058", "19.0", "Muy activa");
            data(shLect, r++, "STT-2024-001", "2024-04-29", "1040", "18.5", "Moderada");
            data(shLect, r++, "STT-2024-001", "2024-05-03", "1030", "18.0", "");
            data(shLect, r++, "STT-2024-001", "2024-05-07", "1024", "18.0", "");
            data(shLect, r++, "STT-2024-001", "2024-05-10", "1021", "17.5", "Densidad final, alta para el estilo — normal en imps");
            // HEF-2024-001
            data(shLect, r++, "HEF-2024-001", "2024-05-12", "1050", "18.0", "Inicio, levadura WY3068 muy activa");
            data(shLect, r++, "HEF-2024-001", "2024-05-15", "1030", "18.0", "Banana y clavo ya perceptibles");
            data(shLect, r++, "HEF-2024-001", "2024-05-19", "1014", "17.5", "Final esperado");

            // Lote_Evaluaciones — cols: [0]codigo [1]fecha [2]catador [3]aroma [4]apariencia
            //   [5]sabor [6]sensacion [7]impresion [8]notas
            Sheet shEval = wb.createSheet("Lote_Evaluaciones");
            hdr(shEval, "codigo_lote", "fecha", "catador", "aroma", "apariencia",
                    "sabor", "sensacion_boca", "impresion_general", "notas");
            r = 0;
            data(shEval, r++,
                    "IPA-2024-001", "2024-05-10", "Juan P.",
                    "10", "3", "18", "4", "9",
                    "Excelente balance, lúpulo floral y cítrico. Amargor limpio y persistente.");
            data(shEval, r++,
                    "IPA-2024-001", "2024-05-12", "María C.",
                    "9", "3", "17", "4", "8",
                    "Muy fresca, aroma a maracuyá. Levemente alta en amargor al final.");
            data(shEval, r++,
                    "STT-2024-001", "2024-06-01", "Juan P.",
                    "11", "4", "20", "5", "10",
                    "Complejidad notable: café, chocolate amargo, vainilla sutil. Cuerpo untuoso.");
            data(shEval, r++,
                    "HEF-2024-001", "2024-05-22", "Carlos R.",
                    "10", "3", "15", "3", "8",
                    "Banana prominente, clavo en segundo plano. Carbonatación alta, espuma cremosa.");

            // Planificacion — cols: [0]fecha_planeada [1]nombre_elab [2]nombre_receta
            //   [3]volumen [4]estado [5]notas
            Sheet shPlan = wb.createSheet("Planificacion");
            hdr(shPlan, "fecha_planeada", "nombre_elaboracion", "nombre_receta",
                    "volumen_estimado", "estado", "notas");
            r = 0;
            data(shPlan, r++,
                    "2024-07-15", "Lote Verano IPA",  "IPA Clásica Aguila", "300",
                    "PLANIFICADA", "Aumentar dry hop citra, reducir cascade bittering");
            data(shPlan, r++,
                    "2024-09-10", "Stout Otoño 2024", "Stout Imperial 2024", "200",
                    "PLANIFICADA", "Añadir café colombiano en acondicionamiento");
            data(shPlan, r++,
                    "2024-10-01", "Hefe Octubre",     "Hefeweizen Dorada", "250",
                    "PLANIFICADA", "");
            data(shPlan, r++,
                    "2024-11-15", "Navidad Porter",   "",                   "150",
                    "PLANIFICADA", "Nueva receta — aún en desarrollo");

            save(wb, dir, "10-seguimiento.xlsx");
        }
    }

    // ── 11 – Mantenimientos ───────────────────────────────────────────────────

    private void generarMantenimientos(Path dir) throws IOException {
        // cols: [0]nombre_equipo [1]fecha [2]tipo [3]descripcion [4]tecnico [5]costo [6]proximo_mant
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sh = wb.createSheet("Mantenimientos");
            hdr(sh, "nombre_equipo", "fecha", "tipo", "descripcion",
                    "tecnico", "costo", "proximo_mantenimiento");
            int r = 0;
            data(sh, r++,
                    "Fermentador A", "2024-06-15", "PREVENTIVO",
                    "Limpieza CIP completa, revisión sello inferior y termómetro",
                    "Carlos Méndez", "45000", "2024-07-15");
            data(sh, r++,
                    "Olla Hervor",   "2024-06-10", "CORRECTIVO",
                    "Reemplazo válvula de salida fondo, limpieza incrustaciones calcáreas",
                    "Técnico Externo SAS", "120000", "");
            data(sh, r++,
                    "Fermentador A", "2024-12-20", "CALIBRACION",
                    "Calibración termómetro y sonda pH con soluciones buffer",
                    "Carlos Méndez", "25000", "2025-03-20");
            data(sh, r++,
                    "Fermentador B", "2024-11-15", "PREVENTIVO",
                    "Revisión airlock, cambio O-ring tapa y limpieza general",
                    "Carlos Méndez", "18000", "2025-02-15");
            data(sh, r++,
                    "Báscula Digital","2025-06-15","CALIBRACION",
                    "Calibración con pesas certificadas 1kg y 5kg",
                    "Metrología SAS", "35000", "2026-06-15");
            data(sh, r++,
                    "Olla Macerado", "2024-03-01", "LIMPIEZA",
                    "Limpieza profunda con soda cáustica 2%, enjuague ácido",
                    "Carlos Méndez", "0", "");
            data(sh, r++,
                    "Chiller Placas","2024-08-20","PREVENTIVO",
                    "Desarmado y limpieza de placas con CIP ácido, revisión juntas",
                    "Técnico Externo SAS", "85000", "2025-02-20");

            save(wb, dir, "11-mantenimientos.xlsx");
        }
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    /** Escribe los nombres de columna en la fila 0 (cabecera visible). Filas 1 y 2 quedan vacías. */
    private void hdr(Sheet sh, String... cols) {
        Row hdrRow = sh.createRow(0);
        for (int i = 0; i < cols.length; i++) hdrRow.createCell(i).setCellValue(cols[i]);
    }

    /** Escribe una fila de datos a partir del índice 3 (las filas 0-2 las salta el importador). */
    private void data(Sheet sh, int offset, String... vals) {
        Row row = sh.createRow(3 + offset);
        for (int i = 0; i < vals.length; i++) row.createCell(i).setCellValue(vals[i]);
    }

    private void save(XSSFWorkbook wb, Path dir, String filename) throws IOException {
        Path out = dir.resolve(filename);
        try (FileOutputStream fos = new FileOutputStream(out.toFile())) {
            wb.write(fos);
        }
        System.out.println("  -> " + out.getFileName());
    }
}
