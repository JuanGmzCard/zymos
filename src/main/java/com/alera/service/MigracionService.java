package com.alera.service;

import com.alera.model.MigracionLog;
import com.alera.repository.MigracionLogRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class MigracionService {

    private static final Map<String, String> TIPO_INSUMO_DISPLAY = Map.of(
            "MALTA", "Malta",
            "LUPULO", "Lúpulo",
            "LEVADURA", "Levadura",
            "CLARIFICANTE", "Clarificante",
            "AGENTE_CARBONATACION", "Agente de Carbonatación",
            "AGUA", "Agua",
            "QUIMICO", "Químico",
            "ENVASE", "Envase",
            "OTRO", "Otro"
    );

    private static final Map<String, String> TIPO_EQUIPO_DISPLAY = Map.ofEntries(
            Map.entry("FERMENTADOR",  "Fermentador"),
            Map.entry("OLLA_MACERADO","Olla de Macerado"),
            Map.entry("OLLA_HERVOR",  "Olla de Hervor"),
            Map.entry("ENFRIADOR",    "Enfriador"),
            Map.entry("BOMBA",        "Bomba"),
            Map.entry("FILTRO",       "Filtro"),
            Map.entry("MEDIDOR_PH",   "Medidor de pH"),
            Map.entry("DENSIMETRO",   "Densímetro"),
            Map.entry("BASCULA",      "Báscula"),
            Map.entry("COMPRESOR",    "Compresor"),
            Map.entry("OTRO",         "Otro")
    );

    private final JdbcTemplate jdbc;
    private final MigracionLogRepository logRepo;

    public MigracionService(JdbcTemplate jdbc, MigracionLogRepository logRepo) {
        this.jdbc    = jdbc;
        this.logRepo = logRepo;
    }

    public record Resultado(int procesadas, int exitosas, int errores,
                             List<String> mensajes, String estado) {
        static String estadoDe(int exitosas, int errores) {
            if (errores == 0 && exitosas > 0) return "EXITOSO";
            if (exitosas == 0) return "FALLIDO";
            return "PARCIAL";
        }
    }

    // ── Almacén ───────────────────────────────────────────────────────────────

    public Resultado importarAlmacen(MultipartFile file, String tenantId, String usuario)
            throws IOException {
        List<String> errores = new ArrayList<>();
        int ok = 0, total = 0;

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sh = wb.getSheet("Insumos");
            if (sh == null) throw new IllegalArgumentException("No se encontró la hoja 'Insumos'");

            for (Row row : sh) {
                if (row.getRowNum() < 3) continue;   // skip header + leyenda + ejemplo
                if (vacio(row, 0)) continue;
                total++;
                try {
                    String nombre = texto(row, 0);
                    String tipo   = texto(row, 1).toUpperCase();
                    BigDecimal cantidad  = decimal(row, 2);
                    String unidad        = texto(row, 3);
                    BigDecimal stockMin  = decimal(row, 4);
                    String proveedor     = texto(row, 5);
                    LocalDate fecVenc    = fecha(row, 6);
                    String obs           = texto(row, 7);

                    if (nombre.isBlank()) throw new IllegalArgumentException("nombre es obligatorio");
                    validarEnum(tipo, "tipo", "MALTA","LUPULO","LEVADURA","CLARIFICANTE","AGENTE_CARBONATACION","AGUA","QUIMICO","ENVASE","OTRO");
                    String tipoDisplay = TIPO_INSUMO_DISPLAY.getOrDefault(tipo, "Otro");

                    jdbc.update("INSERT INTO insumos_inventario " +
                            "(nombre,tipo,cantidad,unidad,stock_minimo,proveedor,fecha_vencimiento,observaciones," +
                            "tenant_id,created_at,created_by,last_modified_at,last_modified_by) " +
                            "VALUES (?,?,?,?,?,?,?,?,?,NOW(),?,NOW(),?)",
                            nombre, tipoDisplay,
                            cantidad != null ? cantidad : BigDecimal.ZERO,
                            unidadNula(unidad), stockMin != null ? stockMin : BigDecimal.ZERO, proveedorNulo(proveedor),
                            fecVenc, obsNula(obs), tenantId, usuario, usuario);
                    ok++;
                } catch (Exception e) {
                    errores.add("Fila " + (row.getRowNum() + 1) + ": " + e.getMessage());
                }
            }
        }
        return guardarLog(tenantId, "almacen", file.getOriginalFilename(),
                          total, ok, errores, usuario);
    }

    // ── Equipos ───────────────────────────────────────────────────────────────

    public Resultado importarEquipos(MultipartFile file, String tenantId, String usuario)
            throws IOException {
        List<String> errores = new ArrayList<>();
        int ok = 0, total = 0;

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sh = wb.getSheet("Equipos");
            if (sh == null) throw new IllegalArgumentException("No se encontró la hoja 'Equipos'");

            for (Row row : sh) {
                if (row.getRowNum() < 3) continue;
                if (vacio(row, 0)) continue;
                total++;
                try {
                    String nombre      = texto(row, 0);
                    String tipo        = texto(row, 1).toUpperCase();
                    String estado      = textoODefault(row, 2, "OPERATIVO").toUpperCase();
                    BigDecimal cap     = decimal(row, 3);
                    String unidCap     = texto(row, 4);
                    LocalDate fecAdq   = fecha(row, 5);
                    LocalDate fecProx  = fecha(row, 6);
                    LocalDate fecUltMant = fecha(row, 7);
                    String obs         = texto(row, 8);

                    if (nombre.isBlank()) throw new IllegalArgumentException("nombre es obligatorio");
                    validarEnum(tipo, "tipo",
                            "FERMENTADOR","OLLA_MACERADO","OLLA_HERVOR","ENFRIADOR","BOMBA",
                            "FILTRO","MEDIDOR_PH","DENSIMETRO","BASCULA","COMPRESOR","OTRO");
                    validarEnum(estado, "estado", "OPERATIVO","MANTENIMIENTO","INACTIVO");
                    String tipoDisplay = TIPO_EQUIPO_DISPLAY.getOrDefault(tipo, tipo);

                    jdbc.update("INSERT INTO equipos " +
                            "(nombre,tipo,estado,capacidad,unidad_capacidad,fecha_adquisicion,proximo_mantenimiento," +
                            "fecha_ultimo_mantenimiento,observaciones," +
                            "tenant_id,created_at,created_by,last_modified_at,last_modified_by) " +
                            "VALUES (?,?,?,?,?,?,?,?,?,?,NOW(),?,NOW(),?)",
                            nombre, tipoDisplay, estado, cap, unidadNula(unidCap),
                            fecAdq, fecProx, fecUltMant, obsNula(obs), tenantId, usuario, usuario);
                    ok++;
                } catch (Exception e) {
                    errores.add("Fila " + (row.getRowNum() + 1) + ": " + e.getMessage());
                }
            }
        }
        return guardarLog(tenantId, "equipos", file.getOriginalFilename(),
                          total, ok, errores, usuario);
    }

    // ── Comercial ─────────────────────────────────────────────────────────────

    public Resultado importarComercial(MultipartFile file, String tenantId, String usuario)
            throws IOException {
        List<String> errores = new ArrayList<>();
        int ok = 0, total = 0;

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            // 1) Proveedores
            Sheet shProv = wb.getSheet("Proveedores");
            if (shProv != null) {
                for (Row row : shProv) {
                    if (row.getRowNum() < 3 || vacio(row, 0)) continue;
                    total++;
                    try {
                        String nombre = texto(row, 0);
                        if (nombre.isBlank()) throw new IllegalArgumentException("nombre es obligatorio");
                        String nit   = texto(row, 1);
                        String tel   = texto(row, 2);
                        String email = texto(row, 3);
                        String dir   = texto(row, 4);

                        long existe = jdbc.queryForObject(
                                "SELECT COUNT(*) FROM proveedores WHERE LOWER(nombre)=LOWER(?) AND tenant_id=?",
                                Long.class, nombre, tenantId);
                        if (existe > 0) { ok++; continue; }   // idempotente: skip duplicado

                        jdbc.update("INSERT INTO proveedores " +
                                "(nombre,nit,telefono,email,direccion,activo," +
                                "tenant_id,created_at,created_by,last_modified_at,last_modified_by) " +
                                "VALUES (?,?,?,?,?,TRUE,?,NOW(),?,NOW(),?)",
                                nombre, nulaSiBlank(nit), nulaSiBlank(tel),
                                nulaSiBlank(email), nulaSiBlank(dir),
                                tenantId, usuario, usuario);
                        ok++;
                    } catch (Exception e) {
                        errores.add("Proveedores fila " + (row.getRowNum() + 1) + ": " + e.getMessage());
                    }
                }
            }

            // 2) Facturas — mapa numeroFactura → id generado
            Map<String, Long> facturaIds = new HashMap<>();
            Sheet shFac = wb.getSheet("Facturas");
            if (shFac != null) {
                for (Row row : shFac) {
                    if (row.getRowNum() < 3 || vacio(row, 1)) continue;
                    total++;
                    try {
                        String numFac      = texto(row, 0);
                        String provNombre  = texto(row, 1);
                        LocalDate fecha    = fecha(row, 2);
                        String desc        = texto(row, 3);
                        BigDecimal envio   = decimal(row, 4);
                        String estado      = textoODefault(row, 5, "RECIBIDA").toUpperCase();
                        String ivaInclStr  = textoODefault(row, 6, "FALSE").toUpperCase();

                        if (provNombre.isBlank()) throw new IllegalArgumentException("proveedor_nombre es obligatorio");
                        if (fecha == null)        throw new IllegalArgumentException("fecha_factura es obligatoria");
                        validarEnum(estado, "estado", "RECIBIDA","VERIFICADA","PAGADA");
                        boolean ivaIncluido = "TRUE".equals(ivaInclStr) || "SI".equals(ivaInclStr) || "1".equals(ivaInclStr);

                        List<Long> provIds = jdbc.queryForList(
                                "SELECT id FROM proveedores WHERE LOWER(nombre)=LOWER(?) AND tenant_id=? LIMIT 1",
                                Long.class, provNombre, tenantId);
                        if (provIds.isEmpty())
                            throw new IllegalArgumentException("Proveedor no encontrado: " + provNombre);
                        Long provId = provIds.get(0);

                        long factId = insertarYRetornarId(
                                "INSERT INTO facturas_proveedor " +
                                "(numero_factura,proveedor,proveedor_id,fecha_factura,descripcion," +
                                "costo_envio,subtotal,porcentaje_iva,valor_iva,valor_total,estado,iva_incluido," +
                                "tenant_id,created_at,created_by,last_modified_at,last_modified_by) " +
                                "VALUES (?,?,?,?,?,?,0,19,0,0,?,?,?,NOW(),?,NOW(),?)",
                                nulaSiBlank(numFac), provNombre, provId, fecha,
                                nulaSiBlank(desc),
                                envio != null ? envio : BigDecimal.ZERO,
                                estado, ivaIncluido, tenantId, usuario, usuario);

                        if (!numFac.isBlank()) facturaIds.put(numFac, factId);
                        ok++;
                    } catch (Exception e) {
                        errores.add("Facturas fila " + (row.getRowNum() + 1) + ": " + e.getMessage());
                    }
                }
            }

            // 3) Factura_Items
            Sheet shItems = wb.getSheet("Factura_Items");
            if (shItems != null) {
                for (Row row : shItems) {
                    if (row.getRowNum() < 3 || vacio(row, 0)) continue;
                    total++;
                    try {
                        String numFac     = texto(row, 0);
                        String tipoItem   = texto(row, 1).toUpperCase();
                        String nombre     = texto(row, 2);
                        String tipoIns    = texto(row, 3).toUpperCase();
                        String tipoEq     = texto(row, 4).toUpperCase();
                        BigDecimal cant   = decimal(row, 5);
                        String unidad     = texto(row, 6);
                        BigDecimal valU   = decimal(row, 7);
                        BigDecimal desc   = decimal(row, 8);
                        BigDecimal iva    = decimal(row, 9);
                        BigDecimal impCon = decimal(row, 10);

                        if (nombre.isBlank()) throw new IllegalArgumentException("nombre es obligatorio");
                        if (valU == null)     throw new IllegalArgumentException("valor_unitario es obligatorio");
                        validarEnum(tipoItem, "tipo_item", "INSUMO","EQUIPO");

                        Long factId = facturaIds.get(numFac);
                        if (factId == null && !numFac.isBlank())
                            throw new IllegalArgumentException("numero_factura '" + numFac + "' no encontrado en la hoja Facturas");

                        // factura_items.tipo_insumo/tipo_equipo almacenan display name (V47)
                        String tipoInsDisplay = tipoIns.isBlank() ? null : TIPO_INSUMO_DISPLAY.getOrDefault(tipoIns, tipoIns);
                        String tipoEqDisplay  = tipoEq.isBlank()  ? null : TIPO_EQUIPO_DISPLAY.getOrDefault(tipoEq, tipoEq);

                        BigDecimal cantidad = cant != null ? cant : BigDecimal.ONE;
                        BigDecimal descPct  = desc != null ? desc : BigDecimal.ZERO;
                        BigDecimal ivaPct   = iva  != null ? iva  : BigDecimal.ZERO;
                        BigDecimal valBase  = cantidad.multiply(valU)
                                .multiply(BigDecimal.ONE.subtract(
                                        descPct.divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP)));
                        BigDecimal valLinea = valBase.add(
                                valBase.multiply(ivaPct.divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP)));

                        BigDecimal impConVal = impCon != null ? impCon : BigDecimal.ZERO;

                        jdbc.update("INSERT INTO factura_items " +
                                "(tipo_item,nombre,tipo_insumo,tipo_equipo,cantidad,unidad," +
                                "valor_unitario,porcentaje_descuento,porcentaje_iva_item,valor_linea," +
                                "impuesto_consumo,factura_id,tenant_id) " +
                                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                                tipoItem, nombre,
                                tipoInsDisplay, tipoEqDisplay,
                                cantidad, unidadNula(unidad),
                                valU, descPct, ivaPct, valLinea,
                                impConVal, factId, tenantId);

                        // Actualizar subtotal y total de la factura si tenemos id
                        if (factId != null) {
                            jdbc.update("UPDATE facturas_proveedor SET " +
                                    "subtotal = COALESCE((SELECT SUM(valor_linea) FROM factura_items WHERE factura_id=? AND tenant_id=?),0), " +
                                    "valor_total = COALESCE((SELECT SUM(valor_linea) FROM factura_items WHERE factura_id=? AND tenant_id=?),0) + costo_envio " +
                                    "WHERE id=? AND tenant_id=?",
                                    factId, tenantId, factId, tenantId, factId, tenantId);
                        }
                        ok++;
                    } catch (Exception e) {
                        errores.add("Factura_Items fila " + (row.getRowNum() + 1) + ": " + e.getMessage());
                    }
                }
            }
        }
        return guardarLog(tenantId, "comercial", file.getOriginalFilename(),
                          total, ok, errores, usuario);
    }

    // ── Producción ────────────────────────────────────────────────────────────

    public Resultado importarProduccion(MultipartFile file, String tenantId, String usuario)
            throws IOException {
        List<String> errores = new ArrayList<>();
        int ok = 0, total = 0;

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {

            // 1) Recetas
            Map<String, Long> recetaIds = new HashMap<>();
            Set<String> recetasExistentes = new HashSet<>();
            Sheet shRec = wb.getSheet("Recetas");
            if (shRec != null) {
                for (Row row : shRec) {
                    if (row.getRowNum() < 3 || vacio(row, 0)) continue;
                    total++;
                    try {
                        String nombre  = texto(row, 0);
                        String estilo  = texto(row, 1);
                        if (nombre.isBlank()) throw new IllegalArgumentException("nombre es obligatorio");
                        if (estilo.isBlank()) throw new IllegalArgumentException("estilo es obligatorio");

                        List<Long> existentes = jdbc.queryForList(
                                "SELECT id FROM recetas WHERE nombre=? AND tenant_id=? LIMIT 1",
                                Long.class, nombre, tenantId);
                        if (!existentes.isEmpty()) {
                            recetaIds.put(nombre, existentes.get(0));
                            recetasExistentes.add(nombre);
                            ok++;
                            continue;
                        }

                        String desc     = texto(row, 2);
                        boolean activa  = textoODefault(row, 3, "TRUE").equalsIgnoreCase("TRUE") || textoODefault(row, 3,"TRUE").equalsIgnoreCase("SI");
                        BigDecimal volBase   = decimal(row, 4);
                        Integer hervor       = entero(row, 5);
                        Integer ogObj        = entero(row, 6);
                        Integer fgObj        = entero(row, 7);
                        BigDecimal aguaMac   = decimal(row, 8);
                        String unidMac       = texto(row, 9);
                        BigDecimal aguaSp    = decimal(row, 10);
                        String unidSp        = texto(row, 11);
                        BigDecimal phAgua    = decimal(row, 12);
                        String notas         = texto(row, 13);

                        long recetaId = insertarYRetornarId(
                                "INSERT INTO recetas " +
                                "(nombre,estilo,descripcion,activa,volumen_base,tiempo_hervor_minutos," +
                                "og_objetivo,fg_objetivo,agua_macerado,unidad_agua_macerado,agua_sparge,unidad_agua_sparge," +
                                "ph_agua,notas,version," +
                                "tenant_id,created_at,created_by,last_modified_at,last_modified_by) " +
                                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,1,?,NOW(),?,NOW(),?)",
                                nombre, estilo, nulaSiBlank(desc), activa,
                                volBase, hervor, ogObj, fgObj,
                                aguaMac, unidadNula(unidMac), aguaSp, unidadNula(unidSp),
                                phAgua, obsNula(notas),
                                tenantId, usuario, usuario);

                        recetaIds.put(nombre, recetaId);
                        ok++;
                    } catch (Exception e) {
                        errores.add("Recetas fila " + (row.getRowNum() + 1) + ": " + e.getMessage());
                    }
                }
            }

            // 2) Receta_Ingredientes
            Sheet shRI = wb.getSheet("Receta_Ingredientes");
            if (shRI != null) {
                for (Row row : shRI) {
                    if (row.getRowNum() < 3 || vacio(row, 0)) continue;
                    total++;
                    try {
                        String recNom   = texto(row, 0);
                        String tipo     = texto(row, 1).toUpperCase();
                        String nombre   = texto(row, 2);
                        String cantUni  = texto(row, 3);

                        if (recetasExistentes.contains(recNom)) { ok++; continue; }
                        Long recId = resolverRecetaId(recetaIds, recNom, tenantId);
                        validarEnum(tipo,"tipo","MALTA","LUPULO","LEVADURA","CLARIFICANTE");
                        if (nombre.isBlank()) throw new IllegalArgumentException("nombre_ingrediente es obligatorio");
                        if (cantUni.isBlank()) throw new IllegalArgumentException("cantidad_con_unidad es obligatorio");

                        jdbc.update("INSERT INTO receta_ingredientes (receta_id,tipo,nombre,cantidad,tenant_id) VALUES (?,?,?,?,?)",
                                recId, tipo, nombre, cantUni, tenantId);
                        ok++;
                    } catch (Exception e) {
                        errores.add("Receta_Ingredientes fila " + (row.getRowNum() + 1) + ": " + e.getMessage());
                    }
                }
            }

            // 3) Receta_Escalones
            Sheet shES = wb.getSheet("Receta_Escalones");
            if (shES != null) {
                for (Row row : shES) {
                    if (row.getRowNum() < 3 || vacio(row, 0)) continue;
                    total++;
                    try {
                        String recNom   = texto(row, 0);
                        String escNom   = texto(row, 1);
                        BigDecimal temp = decimal(row, 2);
                        Integer durMin  = entero(row, 3);
                        Integer orden   = enteroODefault(row, 4, 0);

                        if (recetasExistentes.contains(recNom)) { ok++; continue; }
                        Long recId = resolverRecetaId(recetaIds, recNom, tenantId);
                        if (escNom.isBlank()) throw new IllegalArgumentException("nombre_escalon es obligatorio");
                        if (temp == null)     throw new IllegalArgumentException("temperatura_c es obligatoria");
                        if (durMin == null)   throw new IllegalArgumentException("duracion_minutos es obligatoria");

                        jdbc.update("INSERT INTO escalones_macerado (receta_id,nombre,temperatura_c,duracion_minutos,orden,tenant_id) VALUES (?,?,?,?,?,?)",
                                recId, escNom, temp, durMin, orden, tenantId);
                        ok++;
                    } catch (Exception e) {
                        errores.add("Receta_Escalones fila " + (row.getRowNum() + 1) + ": " + e.getMessage());
                    }
                }
            }

            // 4) Receta_Adiciones
            Sheet shAd = wb.getSheet("Receta_Adiciones");
            if (shAd != null) {
                for (Row row : shAd) {
                    if (row.getRowNum() < 3 || vacio(row, 0)) continue;
                    total++;
                    try {
                        String recNom  = texto(row, 0);
                        String nombre  = texto(row, 1);
                        Integer minRest= entero(row, 2);
                        BigDecimal cant= decimal(row, 3);
                        String unidad  = texto(row, 4);
                        Integer orden  = enteroODefault(row, 5, 0);

                        if (recetasExistentes.contains(recNom)) { ok++; continue; }
                        Long recId = resolverRecetaId(recetaIds, recNom, tenantId);
                        if (nombre.isBlank())  throw new IllegalArgumentException("nombre es obligatorio");
                        if (minRest == null)   throw new IllegalArgumentException("minutos_restantes es obligatorio");
                        if (cant == null)      throw new IllegalArgumentException("cantidad es obligatoria");

                        jdbc.update("INSERT INTO adiciones_hervor (receta_id,nombre,minutos_restantes,cantidad,unidad,orden,tenant_id) VALUES (?,?,?,?,?,?,?)",
                                recId, nombre, minRest, cant, unidadNula(unidad), orden, tenantId);
                        ok++;
                    } catch (Exception e) {
                        errores.add("Receta_Adiciones fila " + (row.getRowNum() + 1) + ": " + e.getMessage());
                    }
                }
            }

            // 5) Lotes
            Map<String, Long> loteIds = new HashMap<>();
            Set<String> lotesExistentes = new HashSet<>();
            Sheet shLotes = wb.getSheet("Lotes");
            if (shLotes != null) {
                for (Row row : shLotes) {
                    if (row.getRowNum() < 3 || vacio(row, 0)) continue;
                    total++;
                    try {
                        String codigo   = texto(row, 0);
                        String estilo   = texto(row, 1);
                        LocalDate fecEl = fecha(row, 2);

                        if (codigo.isBlank())  throw new IllegalArgumentException("codigo_lote es obligatorio");
                        if (estilo.isBlank())  throw new IllegalArgumentException("estilo es obligatorio");
                        if (fecEl == null)     throw new IllegalArgumentException("fecha_elaboracion es obligatoria");

                        List<Long> lotesExist = jdbc.queryForList(
                                "SELECT id FROM lotes_cerveza WHERE codigo_lote=? AND tenant_id=? LIMIT 1",
                                Long.class, codigo, tenantId);
                        if (!lotesExist.isEmpty()) {
                            loteIds.put(codigo, lotesExist.get(0));
                            lotesExistentes.add(codigo);
                            ok++;
                            continue;
                        }

                        BigDecimal litros  = decimal(row, 3);
                        Integer ogObj      = entero(row, 4);
                        Integer fgObj      = entero(row, 5);
                        BigDecimal agua    = decimal(row, 6);
                        BigDecimal phAgua  = decimal(row, 7);
                        String clar        = texto(row, 8);
                        String obs         = texto(row, 9);
                        String notasCata   = texto(row, 10);
                        String recNom      = texto(row, 11);
                        // Carbonatación avanzada
                        String carbMetodo        = nulaSiBlank(texto(row, 12));
                        BigDecimal carbCo2Obj    = decimal(row, 13);
                        BigDecimal carbCo2Real   = decimal(row, 14);
                        String carbAzucarTipo    = nulaSiBlank(texto(row, 15));
                        BigDecimal carbAzucarGr  = decimal(row, 16);
                        BigDecimal carbPresionPsi = decimal(row, 17);
                        Integer carbTiempoHoras  = entero(row, 18);
                        String carbTecnica       = nulaSiBlank(texto(row, 19));
                        String carbValidacion    = nulaSiBlank(texto(row, 20));
                        String carbDestino       = obsNula(texto(row, 21));
                        // Brix / Terrill
                        BigDecimal ogBrix        = decimal(row, 22);
                        BigDecimal fgBrix        = decimal(row, 23);
                        // Fermentador (por nombre)
                        String fermNombre        = nulaSiBlank(texto(row, 24));
                        // Densidad final fecha
                        LocalDate densFinalFecha = fecha(row, 25);
                        // Fechas de fases
                        LocalDate fermFecIni   = fecha(row, 26);
                        LocalDate fermFecIdeal = fecha(row, 27);
                        BigDecimal fermTemp    = decimal(row, 28);
                        LocalDate fermFecFin   = fecha(row, 29);
                        LocalDate acondFecIni  = fecha(row, 30);
                        LocalDate acondFecIdeal= fecha(row, 31);
                        BigDecimal acondTemp   = decimal(row, 32);
                        LocalDate acondFecFin  = fecha(row, 33);
                        LocalDate madurFecIni  = fecha(row, 34);
                        LocalDate madurFecIdeal= fecha(row, 35);
                        BigDecimal madurTemp   = decimal(row, 36);
                        LocalDate madurFecFin  = fecha(row, 37);
                        LocalDate carbFecIni   = fecha(row, 38);
                        LocalDate carbFecIdeal = fecha(row, 39);
                        BigDecimal carbTemp    = decimal(row, 40);
                        LocalDate carbFecFin   = fecha(row, 41);
                        // Multi-sesión (cols 42-70)
                        Integer numElab = enteroODefault(row, 42, 1);
                        Integer ogS1              = entero(row, 43);
                        BigDecimal volFinalS1     = decimal(row, 44);
                        java.time.LocalTime horaIniS1 = hora(row, 45);
                        java.time.LocalTime horaFinS1 = hora(row, 46);
                        LocalDate fechaS2         = fecha(row, 47);
                        BigDecimal aguaS2         = decimal(row, 48);
                        Integer ogS2              = entero(row, 49);
                        BigDecimal ogBrixS2       = decimal(row, 50);
                        BigDecimal volFinalS2     = decimal(row, 51);
                        java.time.LocalTime horaIniS2 = hora(row, 52);
                        java.time.LocalTime horaFinS2 = hora(row, 53);
                        String recNomS2           = texto(row, 54);
                        LocalDate fechaS3         = fecha(row, 55);
                        BigDecimal aguaS3         = decimal(row, 56);
                        Integer ogS3              = entero(row, 57);
                        BigDecimal ogBrixS3       = decimal(row, 58);
                        BigDecimal volFinalS3     = decimal(row, 59);
                        java.time.LocalTime horaIniS3 = hora(row, 60);
                        java.time.LocalTime horaFinS3 = hora(row, 61);
                        String recNomS3           = texto(row, 62);
                        LocalDate fechaS4         = fecha(row, 63);
                        BigDecimal aguaS4         = decimal(row, 64);
                        Integer ogS4              = entero(row, 65);
                        BigDecimal ogBrixS4       = decimal(row, 66);
                        BigDecimal volFinalS4     = decimal(row, 67);
                        java.time.LocalTime horaIniS4 = hora(row, 68);
                        java.time.LocalTime horaFinS4 = hora(row, 69);
                        String recNomS4           = texto(row, 70);

                        Long recId = null;
                        if (!recNom.isBlank()) {
                            recId = recetaIds.get(recNom);
                            if (recId == null) {
                                List<Long> ids = jdbc.queryForList(
                                        "SELECT id FROM recetas WHERE LOWER(nombre)=LOWER(?) AND tenant_id=? LIMIT 1",
                                        Long.class, recNom, tenantId);
                                if (!ids.isEmpty()) recId = ids.get(0);
                            }
                        }

                        Long fermId = null;
                        if (fermNombre != null) {
                            List<Long> ids = jdbc.queryForList(
                                    "SELECT id FROM equipos WHERE LOWER(nombre)=LOWER(?) AND tenant_id=? LIMIT 1",
                                    Long.class, fermNombre, tenantId);
                            if (!ids.isEmpty()) fermId = ids.get(0);
                        }

                        Long recId2 = resolverRecetaOpcional(recNomS2, recetaIds, tenantId);
                        Long recId3 = resolverRecetaOpcional(recNomS3, recetaIds, tenantId);
                        Long recId4 = resolverRecetaOpcional(recNomS4, recetaIds, tenantId);

                        long loteId = insertarYRetornarId(
                                "INSERT INTO lotes_cerveza " +
                                "(codigo_lote,estilo,fecha_elaboracion,litros_finales,densidad_inicial,densidad_final," +
                                "agua_utilizada,ph_agua,clarificante,observaciones,notas_cata,receta_id," +
                                "carb_metodo,carb_co2_objetivo,carb_co2_real,carb_azucar_tipo,carb_azucar_gramos," +
                                "carb_presion_psi,carb_tiempo_horas,carb_tecnica,carb_validacion,carb_destino," +
                                "og_brix,fg_brix,equipo_fermentador_id,densidad_final_fecha," +
                                "ferm_fecha_inicial,ferm_fecha_final_ideal,ferm_temperatura,ferm_fecha_final," +
                                "acond_fecha_inicial,acond_fecha_final_ideal,acond_temperatura,acond_fecha_final," +
                                "madur_fecha_inicial,madur_fecha_final_ideal,madur_temperatura,madur_fecha_final," +
                                "carb_fecha_inicial,carb_fecha_final_ideal,carb_temperatura,carb_fecha_final," +
                                "numero_elaboraciones," +
                                "og_primera_elaboracion,volumen_final_primera_elaboracion," +
                                "hora_inicio_primera_elaboracion,hora_fin_primera_elaboracion," +
                                "fecha_segunda_elaboracion,agua_segunda_elaboracion," +
                                "og_segunda_elaboracion,og_brix_segunda_elaboracion,volumen_final_segunda_elaboracion," +
                                "hora_inicio_segunda_elaboracion,hora_fin_segunda_elaboracion,receta2_id," +
                                "fecha_tercera_elaboracion,agua_tercera_elaboracion," +
                                "og_tercera_elaboracion,og_brix_tercera_elaboracion,volumen_final_tercera_elaboracion," +
                                "hora_inicio_tercera_elaboracion,hora_fin_tercera_elaboracion,receta3_id," +
                                "fecha_cuarta_elaboracion,agua_cuarta_elaboracion," +
                                "og_cuarta_elaboracion,og_brix_cuarta_elaboracion,volumen_final_cuarta_elaboracion," +
                                "hora_inicio_cuarta_elaboracion,hora_fin_cuarta_elaboracion,receta4_id," +
                                "tenant_id,created_at,created_by,last_modified_at,last_modified_by) " +
                                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?," +
                                "?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW(),?,NOW(),?)",
                                codigo, estilo, fecEl, litros, ogObj, fgObj,
                                agua, phAgua, nulaSiBlank(clar), obsNula(obs), obsNula(notasCata),
                                recId,
                                carbMetodo, carbCo2Obj, carbCo2Real, carbAzucarTipo, carbAzucarGr,
                                carbPresionPsi, carbTiempoHoras, carbTecnica, carbValidacion, carbDestino,
                                ogBrix, fgBrix, fermId, densFinalFecha,
                                fermFecIni, fermFecIdeal, fermTemp, fermFecFin,
                                acondFecIni, acondFecIdeal, acondTemp, acondFecFin,
                                madurFecIni, madurFecIdeal, madurTemp, madurFecFin,
                                carbFecIni, carbFecIdeal, carbTemp, carbFecFin,
                                numElab,
                                ogS1, volFinalS1, horaIniS1, horaFinS1,
                                fechaS2, aguaS2, ogS2, ogBrixS2, volFinalS2, horaIniS2, horaFinS2, recId2,
                                fechaS3, aguaS3, ogS3, ogBrixS3, volFinalS3, horaIniS3, horaFinS3, recId3,
                                fechaS4, aguaS4, ogS4, ogBrixS4, volFinalS4, horaIniS4, horaFinS4, recId4,
                                tenantId, usuario, usuario);

                        loteIds.put(codigo, loteId);
                        ok++;
                    } catch (Exception e) {
                        errores.add("Lotes fila " + (row.getRowNum() + 1) + ": " + e.getMessage());
                    }
                }
            }

            // 6) Lote_Ingredientes
            Sheet shLI = wb.getSheet("Lote_Ingredientes");
            if (shLI != null) {
                for (Row row : shLI) {
                    if (row.getRowNum() < 3 || vacio(row, 0)) continue;
                    total++;
                    try {
                        String codigo  = texto(row, 0);
                        String tipo    = texto(row, 1).toUpperCase();
                        String nombre  = texto(row, 2);
                        String cantUni = texto(row, 3);

                        if (lotesExistentes.contains(codigo)) { ok++; continue; }
                        Long loteId = loteIds.get(codigo);
                        if (loteId == null) {
                            List<Long> ids = jdbc.queryForList(
                                    "SELECT id FROM lotes_cerveza WHERE codigo_lote=? AND tenant_id=? LIMIT 1",
                                    Long.class, codigo, tenantId);
                            if (ids.isEmpty()) throw new IllegalArgumentException("codigo_lote '" + codigo + "' no encontrado");
                            loteId = ids.get(0);
                        }
                        validarEnum(tipo,"tipo","MALTA","LUPULO","LEVADURA","CLARIFICANTE");
                        if (nombre.isBlank())  throw new IllegalArgumentException("nombre es obligatorio");
                        if (cantUni.isBlank()) throw new IllegalArgumentException("cantidad_con_unidad es obligatorio");

                        jdbc.update("INSERT INTO ingredientes (tipo,nombre,cantidad,lote_id,tenant_id) VALUES (?,?,?,?,?)",
                                tipo, nombre, cantUni, loteId, tenantId);
                        ok++;
                    } catch (Exception e) {
                        errores.add("Lote_Ingredientes fila " + (row.getRowNum() + 1) + ": " + e.getMessage());
                    }
                }
            }
        }
        return guardarLog(tenantId, "produccion", file.getOriginalFilename(),
                          total, ok, errores, usuario);
    }

    // ── Clientes ──────────────────────────────────────────────────────────────

    public Resultado importarClientes(MultipartFile file, String tenantId, String usuario)
            throws IOException {
        List<String> errores = new ArrayList<>();
        int ok = 0, total = 0;

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sh = wb.getSheet("Clientes");
            if (sh == null) throw new IllegalArgumentException("No se encontró la hoja 'Clientes'");

            for (Row row : sh) {
                if (row.getRowNum() < 3) continue;
                if (vacio(row, 0)) continue;
                total++;
                try {
                    String nombre       = texto(row, 0);
                    String razonSocial  = texto(row, 1);
                    String nit          = texto(row, 2);
                    String regimen      = texto(row, 3).toUpperCase();
                    String email        = texto(row, 4);
                    String telefono     = texto(row, 5);
                    String direccion    = texto(row, 6);
                    String ciudad       = texto(row, 7);
                    String departamento = texto(row, 8);
                    String listaPrecio  = texto(row, 9).toUpperCase();
                    String activoStr    = textoODefault(row, 10, "TRUE").toUpperCase();
                    String notas        = texto(row, 11);

                    if (nombre.isBlank()) throw new IllegalArgumentException("nombre es obligatorio");
                    if (!regimen.isBlank())
                        validarEnum(regimen, "regimen_tributario", "SIMPLIFICADO", "RESPONSABLE_IVA");
                    if (!listaPrecio.isBlank())
                        validarEnum(listaPrecio, "lista_precio",
                                "VENTA_DIRECTA","DISTRIBUIDOR","BAR","MAYORISTA","EXPORTACION","EMPLEADO");
                    boolean activo = !"FALSE".equals(activoStr) && !"NO".equals(activoStr);

                    // Idempotencia por NIT cuando está presente
                    String nitNulo = nulaSiBlank(nit);
                    if (nitNulo != null) {
                        long existe = jdbc.queryForObject(
                                "SELECT COUNT(*) FROM clientes WHERE nit=? AND tenant_id=?",
                                Long.class, nitNulo, tenantId);
                        if (existe > 0) { ok++; continue; }
                    }

                    jdbc.update("INSERT INTO clientes " +
                            "(nombre,razon_social,nit,regimen_tributario,email,telefono," +
                            "direccion_despacho,ciudad,departamento,lista_precio,activo,notas," +
                            "tenant_id,created_at,created_by,last_modified_at,last_modified_by) " +
                            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,NOW(),?,NOW(),?)",
                            nombre, nulaSiBlank(razonSocial), nitNulo,
                            nulaSiBlank(regimen), nulaSiBlank(email), nulaSiBlank(telefono),
                            nulaSiBlank(direccion), nulaSiBlank(ciudad), nulaSiBlank(departamento),
                            nulaSiBlank(listaPrecio), activo, nulaSiBlank(notas),
                            tenantId, usuario, usuario);
                    ok++;
                } catch (Exception e) {
                    errores.add("Fila " + (row.getRowNum() + 1) + ": " + e.getMessage());
                }
            }
        }
        return guardarLog(tenantId, "clientes", file.getOriginalFilename(),
                          total, ok, errores, usuario);
    }

    // ── Ventas ────────────────────────────────────────────────────────────────

    public Resultado importarVentas(MultipartFile file, String tenantId, String usuario)
            throws IOException {
        List<String> errores = new ArrayList<>();
        int ok = 0, total = 0;

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {

            // 1) Ventas — mapa referencia → id generado
            Map<String, Long> ventaIds = new HashMap<>();
            Sheet shVentas = wb.getSheet("Ventas");
            if (shVentas != null) {
                for (Row row : shVentas) {
                    if (row.getRowNum() < 3 || vacio(row, 0)) continue;
                    total++;
                    try {
                        String refVenta      = texto(row, 0);
                        String clienteNombre = texto(row, 1);
                        String clienteNit    = texto(row, 2);
                        LocalDate fechaDesp  = fecha(row, 3);
                        String estado        = textoODefault(row, 4, "DESPACHADO").toUpperCase();
                        String notas         = texto(row, 5);
                        String remisionNum   = texto(row, 6);
                        LocalDate cotizExpira = fecha(row, 7);

                        if (refVenta.isBlank())       throw new IllegalArgumentException("referencia_venta es obligatorio");
                        if (clienteNombre.isBlank())  throw new IllegalArgumentException("cliente_nombre es obligatorio");
                        if (fechaDesp == null)        throw new IllegalArgumentException("fecha_despacho es obligatoria");
                        validarEnum(estado, "estado", "COTIZACION","PENDIENTE","DESPACHADO","CANCELADO");

                        // Resolver cliente_id — NIT primero, nombre como fallback
                        Long clienteId = null;
                        String nitNulo = nulaSiBlank(clienteNit);
                        if (nitNulo != null) {
                            List<Long> ids = jdbc.queryForList(
                                    "SELECT id FROM clientes WHERE nit=? AND tenant_id=? LIMIT 1",
                                    Long.class, nitNulo, tenantId);
                            if (!ids.isEmpty()) clienteId = ids.get(0);
                        }
                        if (clienteId == null) {
                            List<Long> ids = jdbc.queryForList(
                                    "SELECT id FROM clientes WHERE LOWER(nombre)=LOWER(?) AND tenant_id=? LIMIT 1",
                                    Long.class, clienteNombre, tenantId);
                            if (!ids.isEmpty()) clienteId = ids.get(0);
                        }

                        long ventaId = insertarYRetornarId(
                                "INSERT INTO ventas " +
                                "(cliente,cliente_id,fecha_despacho,estado,notas,remision_numero," +
                                "cotizacion_expira_en," +
                                "tenant_id,created_at,created_by,last_modified_at,last_modified_by) " +
                                "VALUES (?,?,?,?,?,?,?,?,NOW(),?,NOW(),?)",
                                clienteNombre, clienteId, fechaDesp, estado,
                                nulaSiBlank(notas), nulaSiBlank(remisionNum),
                                cotizExpira,
                                tenantId, usuario, usuario);

                        ventaIds.put(refVenta, ventaId);
                        ok++;
                    } catch (Exception e) {
                        errores.add("Ventas fila " + (row.getRowNum() + 1) + ": " + e.getMessage());
                    }
                }
            }

            // 2) Venta_Items
            Sheet shItems = wb.getSheet("Venta_Items");
            if (shItems != null) {
                for (Row row : shItems) {
                    if (row.getRowNum() < 3 || vacio(row, 0)) continue;
                    total++;
                    try {
                        String refVenta    = texto(row, 0);
                        String codigoLote  = texto(row, 1);
                        String descripcion = texto(row, 2);
                        BigDecimal cantidad  = decimal(row, 3);
                        String unidad        = texto(row, 4);
                        BigDecimal precioU   = decimal(row, 5);
                        BigDecimal descPct   = decimal(row, 6);

                        if (refVenta.isBlank()) throw new IllegalArgumentException("referencia_venta es obligatorio");
                        if (cantidad == null)   throw new IllegalArgumentException("cantidad es obligatoria");
                        if (precioU == null)    throw new IllegalArgumentException("precio_unitario es obligatorio");

                        Long ventaId = ventaIds.get(refVenta);
                        if (ventaId == null)
                            throw new IllegalArgumentException("referencia_venta '" + refVenta + "' no encontrada en la hoja Ventas");

                        // Resolver lote — tolerante: si no existe deja lote_id null
                        Long loteId = null;
                        String codigoLoteNulo = nulaSiBlank(codigoLote);
                        if (codigoLoteNulo != null) {
                            List<Long> ids = jdbc.queryForList(
                                    "SELECT id FROM lotes_cerveza WHERE codigo_lote=? AND tenant_id=? AND deleted_at IS NULL LIMIT 1",
                                    Long.class, codigoLoteNulo, tenantId);
                            if (!ids.isEmpty()) loteId = ids.get(0);
                        }

                        jdbc.update("INSERT INTO venta_items " +
                                "(venta_id,lote_id,codigo_lote,descripcion,cantidad,unidad,precio_unitario,descuento_pct,tenant_id) " +
                                "VALUES (?,?,?,?,?,?,?,?,?)",
                                ventaId, loteId, codigoLoteNulo,
                                nulaSiBlank(descripcion),
                                cantidad, nulaSiBlank(unidad),
                                precioU, descPct != null ? descPct : BigDecimal.ZERO,
                                tenantId);
                        ok++;
                    } catch (Exception e) {
                        errores.add("Venta_Items fila " + (row.getRowNum() + 1) + ": " + e.getMessage());
                    }
                }
            }
        }
        return guardarLog(tenantId, "ventas", file.getOriginalFilename(),
                          total, ok, errores, usuario);
    }

    // ── Barriles ──────────────────────────────────────────────────────────────

    public Resultado importarBarriles(MultipartFile file, String tenantId, String usuario)
            throws IOException {
        List<String> errores = new ArrayList<>();
        int ok = 0, total = 0;

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sh = wb.getSheet("Barriles");
            if (sh == null) throw new IllegalArgumentException("No se encontró la hoja 'Barriles'");

            for (Row row : sh) {
                if (row.getRowNum() < 3) continue;
                if (vacio(row, 0)) continue;
                total++;
                try {
                    String codigo         = texto(row, 0);
                    String tipo           = texto(row, 1);
                    BigDecimal capacidad  = decimal(row, 2);
                    String estado         = textoODefault(row, 3, "DISPONIBLE").toUpperCase();
                    String codigoLote     = texto(row, 4);
                    String clienteNombre  = texto(row, 5);
                    LocalDate fechaDesp   = fecha(row, 6);
                    String obs            = texto(row, 7);

                    if (codigo.isBlank()) throw new IllegalArgumentException("codigo es obligatorio");
                    validarEnum(estado, "estado", "DISPONIBLE","LLENO","DESPACHADO","VACIO","LIMPIEZA","BAJA");

                    // Resolver lote_id por codigo_lote (tolerante)
                    Long loteId = null;
                    String codigoLoteN = nulaSiBlank(codigoLote);
                    if (codigoLoteN != null) {
                        List<Long> ids = jdbc.queryForList(
                                "SELECT id FROM lotes_cerveza WHERE codigo_lote=? AND tenant_id=? AND deleted_at IS NULL LIMIT 1",
                                Long.class, codigoLoteN, tenantId);
                        if (!ids.isEmpty()) loteId = ids.get(0);
                    }

                    jdbc.update("INSERT INTO barriles " +
                            "(codigo,tipo,capacidad_litros,estado,lote_id,codigo_lote,cliente_nombre,fecha_despacho,observaciones," +
                            "tenant_id,created_at,created_by,last_modified_at,last_modified_by) " +
                            "VALUES (?,?,?,?,?,?,?,?,?,?,NOW(),?,NOW(),?)",
                            codigo, nulaSiBlank(tipo), capacidad, estado,
                            loteId, codigoLoteN, nulaSiBlank(clienteNombre), fechaDesp, nulaSiBlank(obs),
                            tenantId, usuario, usuario);
                    ok++;
                } catch (Exception e) {
                    errores.add("Barriles fila " + (row.getRowNum() + 1) + ": " + e.getMessage());
                }
            }
        }
        return guardarLog(tenantId, "barriles", file.getOriginalFilename(),
                          total, ok, errores, usuario);
    }

    // ── Órdenes de Compra ─────────────────────────────────────────────────────

    public Resultado importarOrdenes(MultipartFile file, String tenantId, String usuario)
            throws IOException {
        List<String> errores = new ArrayList<>();
        int ok = 0, total = 0;

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {

            // 1) OC — mapa numeroOc → id generado
            Map<String, Long> ocIds = new HashMap<>();
            Sheet shOc = wb.getSheet("OC");
            if (shOc != null) {
                for (Row row : shOc) {
                    if (row.getRowNum() < 3 || vacio(row, 0)) continue;
                    total++;
                    try {
                        String numeroOc       = texto(row, 0);
                        String proveedor      = texto(row, 1);
                        LocalDate fechaEmis   = fecha(row, 2);
                        LocalDate fechaReq    = fecha(row, 3);
                        String estado         = textoODefault(row, 4, "BORRADOR").toUpperCase();
                        String notas          = texto(row, 5);

                        if (numeroOc.isBlank())   throw new IllegalArgumentException("numero_oc es obligatorio");
                        if (fechaEmis == null)     throw new IllegalArgumentException("fecha_emision es obligatoria");
                        validarEnum(estado, "estado", "BORRADOR","ENVIADA","RECIBIDA_PARCIAL","RECIBIDA","CANCELADA");

                        // Resolver proveedor_id (tolerante)
                        Long proveedorId = null;
                        String proveedorN = nulaSiBlank(proveedor);
                        if (proveedorN != null) {
                            List<Long> ids = jdbc.queryForList(
                                    "SELECT id FROM proveedores WHERE LOWER(nombre)=LOWER(?) AND tenant_id=? LIMIT 1",
                                    Long.class, proveedorN, tenantId);
                            if (!ids.isEmpty()) proveedorId = ids.get(0);
                        }

                        long ocId = insertarYRetornarId(
                                "INSERT INTO ordenes_compra " +
                                "(numero_oc,proveedor,proveedor_id,fecha_emision,fecha_requerida,estado,notas," +
                                "tenant_id,created_at,created_by,last_modified_at,last_modified_by) " +
                                "VALUES (?,?,?,?,?,?,?,?,NOW(),?,NOW(),?)",
                                numeroOc, proveedorN, proveedorId, fechaEmis, fechaReq, estado,
                                nulaSiBlank(notas), tenantId, usuario, usuario);

                        ocIds.put(numeroOc, ocId);
                        ok++;
                    } catch (Exception e) {
                        errores.add("OC fila " + (row.getRowNum() + 1) + ": " + e.getMessage());
                    }
                }
            }

            // 2) OC_Items
            Sheet shItems = wb.getSheet("OC_Items");
            if (shItems != null) {
                for (Row row : shItems) {
                    if (row.getRowNum() < 3 || vacio(row, 0)) continue;
                    total++;
                    try {
                        String numeroOc   = texto(row, 0);
                        String tipoItem   = texto(row, 1).toUpperCase();
                        String nombre     = texto(row, 2);
                        String tipoInsumo = texto(row, 3);
                        String tipoEquipo = texto(row, 4);
                        BigDecimal cant   = decimal(row, 5);
                        String unidad     = texto(row, 6);
                        BigDecimal precio = decimal(row, 7);
                        BigDecimal iva    = decimal(row, 8);
                        String desc       = texto(row, 9);

                        if (numeroOc.isBlank()) throw new IllegalArgumentException("numero_oc es obligatorio");
                        if (nombre.isBlank())   throw new IllegalArgumentException("nombre es obligatorio");
                        if (cant == null)        throw new IllegalArgumentException("cantidad es obligatoria");
                        if (!tipoItem.isBlank()) validarEnum(tipoItem, "tipo_item", "INSUMO","EQUIPO");

                        Long ocId = ocIds.get(numeroOc);
                        if (ocId == null)
                            throw new IllegalArgumentException("numero_oc '" + numeroOc + "' no encontrado en hoja OC");

                        jdbc.update("INSERT INTO orden_compra_items " +
                                "(orden_id,tipo_item,nombre,descripcion,cantidad,unidad,precio_unitario_estimado," +
                                "porcentaje_iva_item,tipo_insumo,tipo_equipo,tenant_id) " +
                                "VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                                ocId,
                                nulaSiBlank(tipoItem), nombre, nulaSiBlank(desc),
                                cant, nulaSiBlank(unidad), precio,
                                iva != null ? iva : BigDecimal.ZERO,
                                nulaSiBlank(tipoInsumo), nulaSiBlank(tipoEquipo),
                                tenantId);
                        ok++;
                    } catch (Exception e) {
                        errores.add("OC_Items fila " + (row.getRowNum() + 1) + ": " + e.getMessage());
                    }
                }
            }
        }
        return guardarLog(tenantId, "ordenes", file.getOriginalFilename(),
                          total, ok, errores, usuario);
    }

    // ── Seguimiento (Lecturas + Evaluaciones + Planificación) ─────────────────

    public Resultado importarSeguimiento(MultipartFile file, String tenantId, String usuario)
            throws IOException {
        List<String> errores = new ArrayList<>();
        int ok = 0, total = 0;

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {

            // 1) Lote_Lecturas → lecturas_fermentacion
            Sheet shLect = wb.getSheet("Lote_Lecturas");
            if (shLect != null) {
                for (Row row : shLect) {
                    if (row.getRowNum() < 3 || vacio(row, 0)) continue;
                    total++;
                    try {
                        String codigoLote  = texto(row, 0);
                        LocalDate fechaL   = fecha(row, 1);
                        Integer densidad   = entero(row, 2);
                        BigDecimal temp    = decimal(row, 3);
                        String notas       = texto(row, 4);

                        if (codigoLote.isBlank()) throw new IllegalArgumentException("codigo_lote es obligatorio");
                        if (fechaL == null)        throw new IllegalArgumentException("fecha es obligatoria");

                        List<Long> ids = jdbc.queryForList(
                                "SELECT id FROM lotes_cerveza WHERE codigo_lote=? AND tenant_id=? AND deleted_at IS NULL LIMIT 1",
                                Long.class, codigoLote, tenantId);
                        if (ids.isEmpty())
                            throw new IllegalArgumentException("Lote '" + codigoLote + "' no encontrado");
                        long loteId = ids.get(0);

                        jdbc.update("INSERT INTO lecturas_fermentacion " +
                                "(lote_id,fecha,densidad,temperatura,notas,tenant_id) " +
                                "VALUES (?,?,?,?,?,?)",
                                loteId, fechaL, densidad, temp, nulaSiBlank(notas), tenantId);
                        ok++;
                    } catch (Exception e) {
                        errores.add("Lote_Lecturas fila " + (row.getRowNum() + 1) + ": " + e.getMessage());
                    }
                }
            }

            // 2) Lote_Evaluaciones → evaluaciones_sensoriales
            Sheet shEval = wb.getSheet("Lote_Evaluaciones");
            if (shEval != null) {
                for (Row row : shEval) {
                    if (row.getRowNum() < 3 || vacio(row, 0)) continue;
                    total++;
                    try {
                        String codigoLote = texto(row, 0);
                        LocalDate fechaE  = fecha(row, 1);
                        String catador    = texto(row, 2);
                        Integer aroma     = entero(row, 3);
                        Integer apariencia= entero(row, 4);
                        Integer sabor     = entero(row, 5);
                        Integer sensacion = entero(row, 6);
                        Integer impresion = entero(row, 7);
                        String notas      = texto(row, 8);

                        if (codigoLote.isBlank()) throw new IllegalArgumentException("codigo_lote es obligatorio");
                        if (fechaE == null)        throw new IllegalArgumentException("fecha es obligatoria");

                        List<Long> ids = jdbc.queryForList(
                                "SELECT id FROM lotes_cerveza WHERE codigo_lote=? AND tenant_id=? AND deleted_at IS NULL LIMIT 1",
                                Long.class, codigoLote, tenantId);
                        if (ids.isEmpty())
                            throw new IllegalArgumentException("Lote '" + codigoLote + "' no encontrado");
                        long loteId = ids.get(0);

                        jdbc.update("INSERT INTO evaluaciones_sensoriales " +
                                "(lote_id,fecha,catador,aroma,apariencia,sabor,sensacion_boca,impresion_general,notas,creado_at,tenant_id) " +
                                "VALUES (?,?,?,?,?,?,?,?,?,NOW(),?)",
                                loteId, fechaE, nulaSiBlank(catador),
                                aroma, apariencia, sabor, sensacion, impresion,
                                nulaSiBlank(notas), tenantId);
                        ok++;
                    } catch (Exception e) {
                        errores.add("Lote_Evaluaciones fila " + (row.getRowNum() + 1) + ": " + e.getMessage());
                    }
                }
            }

            // 3) Planificacion → elaboraciones_planificadas
            Sheet shPlan = wb.getSheet("Planificacion");
            if (shPlan != null) {
                Map<String, Long> recetaCache = new HashMap<>();
                for (Row row : shPlan) {
                    if (row.getRowNum() < 3 || vacio(row, 0)) continue;
                    total++;
                    try {
                        LocalDate fechaPlan   = fecha(row, 0);
                        String nombreElab     = texto(row, 1);
                        String nombreReceta   = texto(row, 2);
                        BigDecimal volumen    = decimal(row, 3);
                        String estado         = textoODefault(row, 4, "PLANIFICADA").toUpperCase();
                        String notas          = texto(row, 5);

                        if (fechaPlan == null)     throw new IllegalArgumentException("fecha_planeada es obligatoria");
                        if (nombreElab.isBlank())  throw new IllegalArgumentException("nombre_elaboracion es obligatorio");
                        validarEnum(estado, "estado", "PLANIFICADA","EN_PROCESO","COMPLETADA","CANCELADA");

                        Long recetaId = null;
                        if (!nombreReceta.isBlank()) {
                            recetaId = recetaCache.computeIfAbsent(nombreReceta, n -> {
                                List<Long> ids2 = jdbc.queryForList(
                                        "SELECT id FROM recetas WHERE LOWER(nombre)=LOWER(?) AND tenant_id=? AND deleted_at IS NULL LIMIT 1",
                                        Long.class, n, tenantId);
                                return ids2.isEmpty() ? null : ids2.get(0);
                            });
                        }

                        jdbc.update("INSERT INTO elaboraciones_planificadas " +
                                "(fecha_planeada,nombre_elaboracion,receta_id,volumen_estimado,estado,notas,creado_at,tenant_id) " +
                                "VALUES (?,?,?,?,?,?,NOW(),?)",
                                fechaPlan, nombreElab, recetaId, volumen, estado,
                                nulaSiBlank(notas), tenantId);
                        ok++;
                    } catch (Exception e) {
                        errores.add("Planificacion fila " + (row.getRowNum() + 1) + ": " + e.getMessage());
                    }
                }
            }
        }
        return guardarLog(tenantId, "seguimiento", file.getOriginalFilename(),
                          total, ok, errores, usuario);
    }

    // ── Catálogos ─────────────────────────────────────────────────────────────

    public Resultado importarCatalogos(MultipartFile file, String tenantId, String usuario)
            throws IOException {
        List<String> errores = new ArrayList<>();
        int ok = 0, total = 0;

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {

            // 1) Tipos de Cerveza
            Sheet shTC = wb.getSheet("Tipos_Cerveza");
            if (shTC != null) {
                for (Row row : shTC) {
                    if (row.getRowNum() < 3 || vacio(row, 0)) continue;
                    total++;
                    try {
                        String nombre = texto(row, 0);
                        if (nombre.isBlank()) throw new IllegalArgumentException("nombre es obligatorio");
                        String desc   = texto(row, 1);
                        boolean activo = !"FALSE".equalsIgnoreCase(textoODefault(row, 2, "TRUE"))
                                      && !"NO".equalsIgnoreCase(textoODefault(row, 2, "TRUE"));

                        long existe = jdbc.queryForObject(
                                "SELECT COUNT(*) FROM tipos_cerveza WHERE LOWER(nombre)=LOWER(?) AND tenant_id=?",
                                Long.class, nombre, tenantId);
                        if (existe > 0) { ok++; continue; }

                        jdbc.update("INSERT INTO tipos_cerveza (nombre,descripcion,activo,tenant_id) VALUES (?,?,?,?)",
                                nombre, nulaSiBlank(desc), activo, tenantId);
                        ok++;
                    } catch (Exception e) {
                        errores.add("Tipos_Cerveza fila " + (row.getRowNum() + 1) + ": " + e.getMessage());
                    }
                }
            }

            // 2) Tipos de Insumo
            Sheet shTI = wb.getSheet("Tipos_Insumo");
            if (shTI != null) {
                for (Row row : shTI) {
                    if (row.getRowNum() < 3 || vacio(row, 0)) continue;
                    total++;
                    try {
                        String nombre = texto(row, 0);
                        if (nombre.isBlank()) throw new IllegalArgumentException("nombre es obligatorio");
                        boolean activo = !"FALSE".equalsIgnoreCase(textoODefault(row, 1, "TRUE"))
                                      && !"NO".equalsIgnoreCase(textoODefault(row, 1, "TRUE"));

                        long existe = jdbc.queryForObject(
                                "SELECT COUNT(*) FROM tipos_insumo WHERE LOWER(nombre)=LOWER(?) AND tenant_id=?",
                                Long.class, nombre, tenantId);
                        if (existe > 0) { ok++; continue; }

                        jdbc.update("INSERT INTO tipos_insumo (nombre,activo,tenant_id) VALUES (?,?,?)",
                                nombre, activo, tenantId);
                        ok++;
                    } catch (Exception e) {
                        errores.add("Tipos_Insumo fila " + (row.getRowNum() + 1) + ": " + e.getMessage());
                    }
                }
            }

            // 3) Tipos de Equipo
            Sheet shTE = wb.getSheet("Tipos_Equipo");
            if (shTE != null) {
                for (Row row : shTE) {
                    if (row.getRowNum() < 3 || vacio(row, 0)) continue;
                    total++;
                    try {
                        String nombre = texto(row, 0);
                        if (nombre.isBlank()) throw new IllegalArgumentException("nombre es obligatorio");
                        boolean activo = !"FALSE".equalsIgnoreCase(textoODefault(row, 1, "TRUE"))
                                      && !"NO".equalsIgnoreCase(textoODefault(row, 1, "TRUE"));

                        long existe = jdbc.queryForObject(
                                "SELECT COUNT(*) FROM tipos_equipo WHERE LOWER(nombre)=LOWER(?) AND tenant_id=?",
                                Long.class, nombre, tenantId);
                        if (existe > 0) { ok++; continue; }

                        jdbc.update("INSERT INTO tipos_equipo (nombre,activo,tenant_id) VALUES (?,?,?)",
                                nombre, activo, tenantId);
                        ok++;
                    } catch (Exception e) {
                        errores.add("Tipos_Equipo fila " + (row.getRowNum() + 1) + ": " + e.getMessage());
                    }
                }
            }
        }
        return guardarLog(tenantId, "catalogos", file.getOriginalFilename(),
                          total, ok, errores, usuario);
    }

    // ── Mantenimientos ────────────────────────────────────────────────────────

    public Resultado importarMantenimientos(MultipartFile file, String tenantId, String usuario)
            throws IOException {
        List<String> errores = new ArrayList<>();
        int ok = 0, total = 0;

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sh = wb.getSheet("Mantenimientos");
            if (sh == null) throw new IllegalArgumentException("No se encontró la hoja 'Mantenimientos'");

            for (Row row : sh) {
                if (row.getRowNum() < 3 || vacio(row, 0)) continue;
                total++;
                try {
                    String nombreEquipo = texto(row, 0);
                    LocalDate fecha     = fecha(row, 1);
                    String tipo         = texto(row, 2).toUpperCase();
                    String descripcion  = texto(row, 3);
                    String tecnico      = texto(row, 4);
                    BigDecimal costo    = decimal(row, 5);
                    LocalDate proxMant  = fecha(row, 6);

                    if (nombreEquipo.isBlank()) throw new IllegalArgumentException("nombre_equipo es obligatorio");
                    if (fecha == null)          throw new IllegalArgumentException("fecha es obligatoria");
                    if (tipo.isBlank())         throw new IllegalArgumentException("tipo es obligatorio");
                    validarEnum(tipo, "tipo", "PREVENTIVO","CORRECTIVO","CALIBRACION","LIMPIEZA");

                    List<Long> ids = jdbc.queryForList(
                            "SELECT id FROM equipos WHERE LOWER(nombre)=LOWER(?) AND tenant_id=? LIMIT 1",
                            Long.class, nombreEquipo, tenantId);
                    if (ids.isEmpty())
                        throw new IllegalArgumentException("Equipo '" + nombreEquipo + "' no encontrado");
                    long equipoId = ids.get(0);

                    jdbc.update("INSERT INTO mantenimientos_equipo " +
                            "(equipo_id,fecha,tipo,descripcion,tecnico,costo,proximo_mantenimiento,created_at,tenant_id) " +
                            "VALUES (?,?,?,?,?,?,?,NOW(),?)",
                            equipoId, fecha, tipo, nulaSiBlank(descripcion),
                            nulaSiBlank(tecnico), costo, proxMant, tenantId);

                    // Actualizar fecha_ultimo_mantenimiento en el equipo
                    jdbc.update("UPDATE equipos SET fecha_ultimo_mantenimiento=? " +
                            "WHERE id=? AND tenant_id=? AND (fecha_ultimo_mantenimiento IS NULL OR fecha_ultimo_mantenimiento < ?)",
                            fecha, equipoId, tenantId, fecha);
                    ok++;
                } catch (Exception e) {
                    errores.add("Fila " + (row.getRowNum() + 1) + ": " + e.getMessage());
                }
            }
        }
        return guardarLog(tenantId, "mantenimientos", file.getOriginalFilename(),
                          total, ok, errores, usuario);
    }

    // ── Log ───────────────────────────────────────────────────────────────────

    public List<MigracionLog> historial(String tenantId) {
        return logRepo.findByTenantIdOrderByFechaDesc(tenantId);
    }

    // ── Privados utilitarios ──────────────────────────────────────────────────

    private Resultado guardarLog(String tenantId, String modulo, String archivo,
                                  int total, int ok, List<String> errores, String usuario) {
        int nErr = errores.size();
        String estado = Resultado.estadoDe(ok, nErr);
        String detalles = errores.isEmpty() ? null : String.join("\n", errores.stream().limit(50).toList());
        logRepo.save(MigracionLog.of(tenantId, modulo, archivo, total, ok, nErr, estado, detalles, usuario));
        return new Resultado(total, ok, nErr, errores, estado);
    }

    private long insertarYRetornarId(String sql, Object... params) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            // PostgreSQL con RETURN_GENERATED_KEYS devuelve todas las columnas;
            // especificar "id" garantiza que getKey() recibe exactamente una clave.
            PreparedStatement ps = conn.prepareStatement(sql, new String[]{"id"});
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            return ps;
        }, kh);
        return kh.getKey().longValue();
    }

    private Long resolverRecetaId(Map<String, Long> cache, String nombreReceta, String tenantId) {
        if (nombreReceta.isBlank()) throw new IllegalArgumentException("nombre_receta es obligatorio");
        Long id = cache.get(nombreReceta);
        if (id != null) return id;
        List<Long> ids = jdbc.queryForList(
                "SELECT id FROM recetas WHERE LOWER(nombre)=LOWER(?) AND tenant_id=? LIMIT 1",
                Long.class, nombreReceta, tenantId);
        if (ids.isEmpty()) throw new IllegalArgumentException("Receta '" + nombreReceta + "' no existe en el sistema para este tenant");
        id = ids.get(0);
        cache.put(nombreReceta, id);
        return id;
    }

    private void validarEnum(String valor, String campo, String... validos) {
        if (valor.isBlank()) return;
        for (String v : validos) if (v.equalsIgnoreCase(valor)) return;
        throw new IllegalArgumentException(campo + " '" + valor + "' no válido. Válidos: " + Arrays.toString(validos));
    }

    private boolean vacio(Row row, int colIdx) {
        Cell c = row.getCell(colIdx);
        if (c == null) return true;
        return switch (c.getCellType()) {
            case STRING  -> c.getStringCellValue().isBlank();
            case BLANK   -> true;
            default      -> false;
        };
    }

    private String texto(Row row, int col) {
        Cell c = row.getCell(col);
        if (c == null) return "";
        return switch (c.getCellType()) {
            case STRING  -> c.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) c.getNumericCellValue());
            case BOOLEAN -> String.valueOf(c.getBooleanCellValue());
            default      -> "";
        };
    }

    private String textoODefault(Row row, int col, String def) {
        String v = texto(row, col);
        return v.isBlank() ? def : v;
    }

    private BigDecimal decimal(Row row, int col) {
        Cell c = row.getCell(col);
        if (c == null) return null;
        return switch (c.getCellType()) {
            case NUMERIC -> BigDecimal.valueOf(c.getNumericCellValue());
            case STRING  -> {
                try { yield new BigDecimal(c.getStringCellValue().trim()); }
                catch (NumberFormatException e) { yield null; }
            }
            default -> null;
        };
    }

    private Integer entero(Row row, int col) {
        BigDecimal v = decimal(row, col);
        return v != null ? v.intValue() : null;
    }

    private Integer enteroODefault(Row row, int col, int def) {
        Integer v = entero(row, col);
        return v != null ? v : def;
    }

    private LocalDate fecha(Row row, int col) {
        Cell c = row.getCell(col);
        if (c == null) return null;
        if (c.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(c)) {
            return c.getLocalDateTimeCellValue().toLocalDate();
        }
        String s = texto(row, col);
        if (s.isBlank()) return null;
        try { return LocalDate.parse(s); }
        catch (DateTimeParseException e) { return null; }
    }

    private java.time.LocalTime hora(Row row, int col) {
        String s = texto(row, col);
        if (s.isBlank()) return null;
        try { return java.time.LocalTime.parse(s); }
        catch (DateTimeParseException e) { return null; }
    }

    private Long resolverRecetaOpcional(String nombre, Map<String, Long> recetaIds, String tenantId) {
        if (nombre == null || nombre.isBlank()) return null;
        Long id = recetaIds.get(nombre);
        if (id != null) return id;
        List<Long> ids = jdbc.queryForList(
                "SELECT id FROM recetas WHERE LOWER(nombre)=LOWER(?) AND tenant_id=? LIMIT 1",
                Long.class, nombre, tenantId);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private String nulaSiBlank(String s) { return (s == null || s.isBlank()) ? null : s; }
    private String unidadNula(String s)  { return (s == null || s.isBlank()) ? null : s; }
    private String proveedorNulo(String s) { return nulaSiBlank(s); }
    private String obsNula(String s)     { return nulaSiBlank(s); }
}
