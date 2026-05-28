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
                    validarEnum(tipo, "tipo", "MALTA","LUPULO","LEVADURA","CLARIFICANTE","AGUA","QUIMICO","ENVASE","OTRO");

                    jdbc.update("INSERT INTO insumos_inventario " +
                            "(nombre,tipo,cantidad,unidad,stock_minimo,proveedor,fecha_vencimiento,observaciones," +
                            "tenant_id,created_at,created_by,last_modified_at,last_modified_by) " +
                            "VALUES (?,?,?,?,?,?,?,?,?,NOW(),?,NOW(),?)",
                            nombre, tipo,
                            cantidad != null ? cantidad : BigDecimal.ZERO,
                            unidadNula(unidad), stockMin, proveedorNulo(proveedor),
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
                    String nombre   = texto(row, 0);
                    String tipo     = texto(row, 1).toUpperCase();
                    String estado   = textoODefault(row, 2, "OPERATIVO").toUpperCase();
                    BigDecimal cap  = decimal(row, 3);
                    String unidCap  = texto(row, 4);
                    LocalDate fecAdq  = fecha(row, 5);
                    LocalDate fecProx = fecha(row, 6);
                    String obs        = texto(row, 7);

                    if (nombre.isBlank()) throw new IllegalArgumentException("nombre es obligatorio");
                    validarEnum(tipo, "tipo",
                            "FERMENTADOR","OLLA_MACERADO","OLLA_HERVOR","ENFRIADOR","BOMBA",
                            "FILTRO","MEDIDOR_PH","DENSIMETRO","BASCULA","COMPRESOR","OTRO");
                    validarEnum(estado, "estado", "OPERATIVO","MANTENIMIENTO","INACTIVO");

                    jdbc.update("INSERT INTO equipos " +
                            "(nombre,tipo,estado,capacidad,unidad_capacidad,fecha_adquisicion,proximo_mantenimiento,observaciones," +
                            "tenant_id,created_at,created_by,last_modified_at,last_modified_by) " +
                            "VALUES (?,?,?,?,?,?,?,?,?,NOW(),?,NOW(),?)",
                            nombre, tipo, estado, cap, unidadNula(unidCap),
                            fecAdq, fecProx, obsNula(obs), tenantId, usuario, usuario);
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
                        String numFac    = texto(row, 0);
                        String provNombre= texto(row, 1);
                        LocalDate fecha  = fecha(row, 2);
                        String desc      = texto(row, 3);
                        BigDecimal envio = decimal(row, 4);
                        String estado    = textoODefault(row, 5, "RECIBIDA").toUpperCase();

                        if (provNombre.isBlank()) throw new IllegalArgumentException("proveedor_nombre es obligatorio");
                        if (fecha == null)        throw new IllegalArgumentException("fecha_factura es obligatoria");
                        validarEnum(estado, "estado", "RECIBIDA","VERIFICADA","PAGADA");

                        Long provId = jdbc.queryForObject(
                                "SELECT id FROM proveedores WHERE LOWER(nombre)=LOWER(?) AND tenant_id=? LIMIT 1",
                                Long.class, provNombre, tenantId);

                        long factId = insertarYRetornarId(
                                "INSERT INTO facturas_proveedor " +
                                "(numero_factura,proveedor,proveedor_id,fecha_factura,descripcion," +
                                "costo_envio,subtotal,porcentaje_iva,valor_iva,valor_total,estado," +
                                "tenant_id,created_at,created_by,last_modified_at,last_modified_by) " +
                                "VALUES (?,?,?,?,?,?,0,19,0,0,?,?,NOW(),?,NOW(),?)",
                                nulaSiBlank(numFac), provNombre, provId, fecha,
                                nulaSiBlank(desc),
                                envio != null ? envio : BigDecimal.ZERO,
                                estado, tenantId, usuario, usuario);

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
                        String numFac   = texto(row, 0);
                        String tipoItem = texto(row, 1).toUpperCase();
                        String nombre   = texto(row, 2);
                        String tipoIns  = texto(row, 3).toUpperCase();
                        String tipoEq   = texto(row, 4).toUpperCase();
                        BigDecimal cant = decimal(row, 5);
                        String unidad   = texto(row, 6);
                        BigDecimal valU = decimal(row, 7);
                        BigDecimal desc = decimal(row, 8);
                        BigDecimal iva  = decimal(row, 9);

                        if (nombre.isBlank()) throw new IllegalArgumentException("nombre es obligatorio");
                        if (valU == null)     throw new IllegalArgumentException("valor_unitario es obligatorio");
                        validarEnum(tipoItem, "tipo_item", "INSUMO","EQUIPO");

                        Long factId = facturaIds.get(numFac);
                        if (factId == null && !numFac.isBlank())
                            throw new IllegalArgumentException("numero_factura '" + numFac + "' no encontrado en la hoja Facturas");

                        BigDecimal cantidad = cant != null ? cant : BigDecimal.ONE;
                        BigDecimal descPct  = desc != null ? desc : BigDecimal.ZERO;
                        BigDecimal ivaPct   = iva  != null ? iva  : BigDecimal.ZERO;
                        BigDecimal valBase  = cantidad.multiply(valU)
                                .multiply(BigDecimal.ONE.subtract(
                                        descPct.divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP)));
                        BigDecimal valLinea = valBase.add(
                                valBase.multiply(ivaPct.divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP)));

                        jdbc.update("INSERT INTO factura_items " +
                                "(tipo_item,nombre,tipo_insumo,tipo_equipo,cantidad,unidad," +
                                "valor_unitario,porcentaje_descuento,porcentaje_iva_item,valor_linea," +
                                "factura_id,tenant_id) " +
                                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                                tipoItem, nombre,
                                nulaSiBlank(tipoIns), nulaSiBlank(tipoEq),
                                cantidad, unidadNula(unidad),
                                valU, descPct, ivaPct, valLinea,
                                factId, tenantId);

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

                        long existe = jdbc.queryForObject(
                                "SELECT COUNT(*) FROM recetas WHERE LOWER(nombre)=LOWER(?) AND tenant_id=? AND deleted_at IS NULL",
                                Long.class, nombre, tenantId);
                        if (existe > 0) throw new IllegalArgumentException("Ya existe una receta con nombre '" + nombre + "'");

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

                        long existe = jdbc.queryForObject(
                                "SELECT COUNT(*) FROM lotes_cerveza WHERE codigo_lote=? AND tenant_id=? AND deleted_at IS NULL",
                                Long.class, codigo, tenantId);
                        if (existe > 0) throw new IllegalArgumentException("Ya existe un lote con código '" + codigo + "'");

                        BigDecimal litros = decimal(row, 3);
                        Integer ogObj     = entero(row, 4);
                        Integer fgObj     = entero(row, 5);
                        BigDecimal agua   = decimal(row, 6);
                        BigDecimal phAgua = decimal(row, 7);
                        String clar       = texto(row, 8);
                        String obs        = texto(row, 9);
                        String notasCata  = texto(row, 10);
                        String recNom     = texto(row, 11);

                        Long recId = null;
                        if (!recNom.isBlank()) {
                            recId = recetaIds.get(recNom);
                            if (recId == null) {
                                List<Long> ids = jdbc.queryForList(
                                        "SELECT id FROM recetas WHERE LOWER(nombre)=LOWER(?) AND tenant_id=? AND deleted_at IS NULL LIMIT 1",
                                        Long.class, recNom, tenantId);
                                if (!ids.isEmpty()) recId = ids.get(0);
                            }
                        }

                        long loteId = insertarYRetornarId(
                                "INSERT INTO lotes_cerveza " +
                                "(codigo_lote,estilo,fecha_elaboracion,litros_finales,densidad_inicial,densidad_final," +
                                "agua_utilizada,ph_agua,clarificante,observaciones,notas_cata,receta_id," +
                                "tenant_id,created_at,created_by,last_modified_at,last_modified_by) " +
                                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,NOW(),?,NOW(),?)",
                                codigo, estilo, fecEl, litros, ogObj, fgObj,
                                agua, phAgua, nulaSiBlank(clar), obsNula(obs), obsNula(notasCata),
                                recId, tenantId, usuario, usuario);

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

                        Long loteId = loteIds.get(codigo);
                        if (loteId == null) {
                            List<Long> ids = jdbc.queryForList(
                                    "SELECT id FROM lotes_cerveza WHERE codigo_lote=? AND tenant_id=? AND deleted_at IS NULL LIMIT 1",
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
                "SELECT id FROM recetas WHERE LOWER(nombre)=LOWER(?) AND tenant_id=? AND deleted_at IS NULL LIMIT 1",
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

    private String nulaSiBlank(String s) { return (s == null || s.isBlank()) ? null : s; }
    private String unidadNula(String s)  { return (s == null || s.isBlank()) ? null : s; }
    private String proveedorNulo(String s) { return nulaSiBlank(s); }
    private String obsNula(String s)     { return nulaSiBlank(s); }
}
