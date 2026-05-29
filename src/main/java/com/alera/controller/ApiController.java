package com.alera.controller;

import com.alera.model.LoteCerveza;
import com.alera.service.*;
import com.alera.repository.LoteCervezaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping(value = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "API v1", description = "REST API para integración con apps móviles y herramientas externas. Autenticación: HTTP Basic.")
public class ApiController {

    private final TrazabilidadService trazabilidadService;
    private final LoteCervezaRepository loteRepo;
    private final RecetaService recetaService;
    private final InsumoInventarioService insumoService;
    private final DashboardService dashboardService;

    public ApiController(TrazabilidadService trazabilidadService,
                         LoteCervezaRepository loteRepo,
                         RecetaService recetaService,
                         InsumoInventarioService insumoService,
                         DashboardService dashboardService) {
        this.trazabilidadService = trazabilidadService;
        this.loteRepo = loteRepo;
        this.recetaService = recetaService;
        this.insumoService = insumoService;
        this.dashboardService = dashboardService;
    }

    // ── Lotes ────────────────────────────────────────────────────────

    @Operation(summary = "Listar lotes paginados", description = "Soporta filtros opcionales por estilo y fase de producción")
    @GetMapping("/lotes")
    public Map<String, Object> lotes(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "") String estilo,
                                     @RequestParam(defaultValue = "") String fase) {
        var pagina = trazabilidadService.listarPaginado(estilo, fase, page);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("total",        pagina.getTotalElements());
        res.put("totalPaginas", pagina.getTotalPages());
        res.put("pagina",       page);
        res.put("lotes",        pagina.getContent().stream().map(this::loteResumen).toList());
        return res;
    }

    @Operation(summary = "Detalle de un lote", description = "Incluye ingredientes, densidades, ABV, costo y auditoría")
    @GetMapping("/lotes/{id}")
    public Map<String, Object> lote(@PathVariable Long id) {
        var lote = trazabilidadService.buscarPorId(id);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id",            lote.getId());
        res.put("codigoLote",    lote.getCodigoLote());
        res.put("estilo",        lote.getEstilo());
        res.put("fase",          lote.getFaseActual());
        res.put("completado",    lote.isCompletado());
        res.put("fechaElaboracion", lote.getFechaElaboracion());
        res.put("fermentador",   lote.getEquipoFermentador() != null ? lote.getEquipoFermentador().getNombre() : null);
        res.put("litrosFinales", lote.getLitrosFinales());
        res.put("densidadInicial", lote.getDensidadInicial());
        res.put("densidadFinal",   lote.getDensidadFinal());
        res.put("abv",             lote.getAbv());
        res.put("atenuacionAparente", lote.getAtenuacionAparente());
        res.put("eficienciaMacerado", lote.getEficienciaMacerado());
        res.put("notasCata",     lote.getNotasCata());
        res.put("observaciones", lote.getObservaciones());
        res.put("ingredientes",  lote.getIngredientes().stream().map(i -> Map.of(
                "tipo", i.getTipo().name(),
                "nombre", i.getNombre(),
                "cantidad", Objects.requireNonNullElse(i.getCantidad(), "")
        )).toList());
        res.put("costoTotal",    lote.getCostoTotal());
        res.put("costoPorLitro", lote.getCostoPorLitro());
        Map<String, Object> carb = new LinkedHashMap<>();
        carb.put("metodo",       lote.getCarbMetodo());
        carb.put("co2Objetivo",  lote.getCarbCo2Objetivo());
        carb.put("co2Real",      lote.getCarbCo2Real());
        carb.put("azucarTipo",   lote.getCarbAzucarTipo());
        carb.put("azucarGramos", lote.getCarbAzucarGramos());
        carb.put("presionPsi",   lote.getCarbPresionPsi());
        carb.put("tiempoHoras",  lote.getCarbTiempoHoras());
        carb.put("tecnica",      lote.getCarbTecnica());
        carb.put("validacion",   lote.getCarbValidacion());
        carb.put("destino",      lote.getCarbDestino());
        res.put("carbonatacion", carb);
        res.put("lastModifiedAt", lote.getLastModifiedAt());
        res.put("lastModifiedBy", lote.getLastModifiedBy());
        return res;
    }

    @Operation(summary = "Historial de un lote", description = "Acciones CREADO / EDITADO / ELIMINADO ordenadas por fecha desc")
    @GetMapping("/lotes/{id}/historial")
    public List<Map<String, Object>> historialLote(@PathVariable Long id) {
        return trazabilidadService.obtenerHistorial(id).stream().map(h -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("accion",  h.getAccion());
            m.put("usuario", h.getUsuario());
            m.put("fecha",   h.getFecha());
            m.put("notas",   h.getNotas());
            return m;
        }).toList();
    }

    // ── Recetas ──────────────────────────────────────────────────────

    @GetMapping("/recetas")
    public List<Map<String, Object>> recetas() {
        return recetaService.listarActivas().stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",              r.getId());
            m.put("nombre",          r.getNombre());
            m.put("estilo",          r.getEstilo());
            m.put("ogObjetivo",      r.getOgObjetivo());
            m.put("fgObjetivo",      r.getFgObjetivo());
            m.put("volumenBase",     r.getVolumenBase());
            m.put("tiempoHervorMin", r.getTiempoHervorMinutos());
            m.put("ingredientes",    r.getIngredientes().size());
            return m;
        }).toList();
    }

    @GetMapping("/recetas/{id}")
    public Map<String, Object> receta(@PathVariable Long id) {
        var r = recetaService.buscarPorId(id);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id",          r.getId());
        res.put("nombre",      r.getNombre());
        res.put("estilo",      r.getEstilo());
        res.put("ogObjetivo",  r.getOgObjetivo());
        res.put("fgObjetivo",  r.getFgObjetivo());
        res.put("volumenBase", r.getVolumenBase());
        res.put("maltas",       r.getMaltas().stream().map(i -> Map.of("nombre", i.getNombre(), "cantidad", Objects.requireNonNullElse(i.getCantidad(), ""))).toList());
        res.put("lupulos",      r.getLupulos().stream().map(i -> Map.of("nombre", i.getNombre(), "cantidad", Objects.requireNonNullElse(i.getCantidad(), ""))).toList());
        res.put("levaduras",    r.getLevaduras().stream().map(i -> Map.of("nombre", i.getNombre(), "cantidad", Objects.requireNonNullElse(i.getCantidad(), ""))).toList());
        res.put("clarificantes",r.getClarificantes().stream().map(i -> Map.of("nombre", i.getNombre(), "cantidad", Objects.requireNonNullElse(i.getCantidad(), ""))).toList());
        res.put("escalones",    r.getEscalones().stream().map(e -> Map.of("nombre", e.getNombre(), "minutos", Objects.requireNonNullElse(e.getDuracionMinutos(), 0), "tempC", Objects.requireNonNullElse(e.getTemperaturaC(), ""))).toList());
        res.put("notas",        r.getNotas());
        return res;
    }

    // ── Inventario ───────────────────────────────────────────────────

    @Operation(summary = "Alertas de inventario", description = "Insumos con bajo stock y próximos a vencer en los próximos 30 días")
    @GetMapping("/inventario/alertas")
    public Map<String, Object> alertasInventario() {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("bajoStock", insumoService.listarBajoStock().stream().map(i -> Map.of(
                "nombre", i.getNombre(),
                "tipo",   i.getTipo().name(),
                "stock",  i.getCantidad(),
                "minimo", i.getStockMinimo(),
                "unidad", Objects.requireNonNullElse(i.getUnidad(), "")
        )).toList());
        res.put("proximosAVencer", insumoService.listarProximosAVencer(30).stream().map(i -> Map.of(
                "nombre",    i.getNombre(),
                "vencimiento", i.getFechaVencimiento()
        )).toList());
        return res;
    }

    // ── Dashboard ────────────────────────────────────────────────────

    @Operation(summary = "Estadísticas del dashboard", description = "Conteos de lotes, inventario, equipos y totales financieros")
    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        var stats = dashboardService.obtenerEstadisticas();
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("totalLotes",          stats.getTotalLotes());
        res.put("lotesEnProceso",      stats.getEnProceso());
        res.put("lotesCompletados",    stats.getCompletados());
        res.put("estilosDistintos",    stats.getEstilosDistintos());
        res.put("totalInsumos",        stats.getTotalInsumos());
        res.put("insumosbajoStock",    stats.getBajoStock());
        res.put("proximosAVencer",     stats.getProximosAVencer());
        res.put("totalEquipos",        stats.getTotalEquipos());
        res.put("mantenimientoPendiente", stats.getMantenimientoPendiente());
        res.put("totalFacturas",       stats.getTotalFacturas());
        res.put("totalGastado",        stats.getTotalGastado());
        return res;
    }

    // ── Utilidades ───────────────────────────────────────────────────

    private Map<String, Object> loteResumen(LoteCerveza l) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",           l.getId());
        m.put("codigoLote",   l.getCodigoLote());
        m.put("estilo",       l.getEstilo());
        m.put("fase",         l.getFaseActual());
        m.put("completado",   l.isCompletado());
        m.put("fechaElaboracion", l.getFechaElaboracion());
        m.put("litrosFinales", l.getLitrosFinales());
        m.put("abv",          l.getAbv());
        return m;
    }
}
