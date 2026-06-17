package com.alera.controller;

import com.alera.config.ExportBranding;
import com.alera.config.TenantContext;
import com.alera.dto.RentabilidadLoteDto;
import com.alera.model.LoteCerveza;
import com.alera.model.Tenant;
import com.alera.model.Venta;
import com.alera.model.enums.EstadoVenta;
import com.alera.repository.LoteCervezaRepository;
import com.alera.repository.LoteItemFacturaRepository;
import com.alera.repository.VentaItemRepository;
import com.alera.service.ExcelExportService;
import com.alera.service.PdfExportService;
import com.alera.service.VentaService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Comparator;
import java.util.HashMap;

@Controller
@RequestMapping("/reportes")
public class ReporteController {

    private final LoteCervezaRepository     loteRepo;
    private final ExcelExportService        excelExportService;
    private final PdfExportService          pdfExportService;
    private final VentaService              ventaService;
    private final LoteItemFacturaRepository loteItemFacturaRepo;
    private final VentaItemRepository       ventaItemRepo;

    public ReporteController(LoteCervezaRepository loteRepo,
                              ExcelExportService excelExportService,
                              PdfExportService pdfExportService,
                              VentaService ventaService,
                              LoteItemFacturaRepository loteItemFacturaRepo,
                              VentaItemRepository ventaItemRepo) {
        this.loteRepo            = loteRepo;
        this.excelExportService  = excelExportService;
        this.pdfExportService    = pdfExportService;
        this.ventaService        = ventaService;
        this.loteItemFacturaRepo = loteItemFacturaRepo;
        this.ventaItemRepo       = ventaItemRepo;
    }

    @GetMapping("/produccion")
    public String produccion(
            @RequestParam(required = false) String estilo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Model model) {

        // desde/hasta null = sin restricción (muestra todo el historial)

        var todosLotes = loteRepo.findByPeriodo(desde, hasta);

        // Estilos disponibles para el select (del período completo, sin filtrar)
        List<String> estilosDisponibles = todosLotes.stream()
                .map(LoteCerveza::getEstilo)
                .filter(e -> e != null && !e.isBlank())
                .distinct().sorted()
                .collect(Collectors.toList());

        // Aplicar filtro de estilo si viene en el request
        List<LoteCerveza> lotes = (estilo != null && !estilo.isBlank())
                ? todosLotes.stream().filter(l -> estilo.equals(l.getEstilo())).collect(Collectors.toList())
                : todosLotes;

        // ── Estadísticas ──────────────────────────────────────────────
        long totalLotes = lotes.size();
        BigDecimal totalLitros = lotes.stream()
                .map(l -> l.getLitrosFinales() != null ? l.getLitrosFinales() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long estilosUnicos = lotes.stream().map(LoteCerveza::getEstilo).distinct().count();
        long completados   = lotes.stream().filter(LoteCerveza::isCompletado).count();

        BigDecimal avgLitros = totalLotes > 0
                ? totalLitros.divide(BigDecimal.valueOf(totalLotes), 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        OptionalDouble avgAbvOpt = lotes.stream()
                .filter(l -> l.getAbv() != null)
                .mapToDouble(l -> l.getAbv().doubleValue()).average();
        BigDecimal avgAbv = avgAbvOpt.isPresent()
                ? BigDecimal.valueOf(avgAbvOpt.getAsDouble()).setScale(2, RoundingMode.HALF_UP) : null;

        OptionalDouble avgEfOpt = lotes.stream()
                .filter(l -> l.getEficienciaMacerado() != null)
                .mapToDouble(l -> l.getEficienciaMacerado().doubleValue()).average();
        BigDecimal avgEficiencia = avgEfOpt.isPresent()
                ? BigDecimal.valueOf(avgEfOpt.getAsDouble()).setScale(1, RoundingMode.HALF_UP) : null;

        boolean hayCostos = lotes.stream().anyMatch(l -> l.getCostoTotal() != null);
        BigDecimal costoTotalPeriodo = hayCostos
                ? lotes.stream().map(l -> l.getCostoTotal() != null ? l.getCostoTotal() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                : null;

        int tasaCompletados = totalLotes > 0 ? (int) (completados * 100 / totalLotes) : 0;

        // ── Gráfico: tendencia mensual (from lotes — respeta filtro de estilo) ──
        String[] mesesNombres = {"Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic"};
        Map<String, Number> chartTendencia = new LinkedHashMap<>();
        lotes.stream()
                .filter(l -> l.getFechaElaboracion() != null)
                .sorted(Comparator.comparing(LoteCerveza::getFechaElaboracion))
                .collect(Collectors.groupingBy(
                        l -> {
                            LocalDate f = l.getFechaElaboracion();
                            return mesesNombres[f.getMonthValue() - 1] + " " + f.getYear();
                        },
                        LinkedHashMap::new,
                        Collectors.summingDouble(l -> l.getLitrosFinales() != null
                                ? l.getLitrosFinales().doubleValue() : 0.0)
                ))
                .forEach(chartTendencia::put);

        // ── Gráfico: litros por estilo (from lotes — respeta filtro) ──
        Map<String, Number> chartEstilosLitros = new LinkedHashMap<>();
        lotes.stream()
                .filter(l -> l.getLitrosFinales() != null)
                .collect(Collectors.groupingBy(
                        l -> l.getEstilo() != null ? l.getEstilo() : "Sin estilo",
                        LinkedHashMap::new,
                        Collectors.summingDouble(l -> l.getLitrosFinales().doubleValue())
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .forEach(e -> chartEstilosLitros.put(e.getKey(), e.getValue()));

        // ── Gráfico: distribución ABV ──────────────────────────────────
        String[] abvLabels = {"<3%","3-4%","4-5%","5-6%","6-7%","7-8%","≥8%"};
        int[] abvCounts = new int[7];
        for (var lote : lotes) {
            BigDecimal abv = lote.getAbv();
            if (abv == null) continue;
            double v = abv.doubleValue();
            if (v < 3)      abvCounts[0]++;
            else if (v < 4) abvCounts[1]++;
            else if (v < 5) abvCounts[2]++;
            else if (v < 6) abvCounts[3]++;
            else if (v < 7) abvCounts[4]++;
            else if (v < 8) abvCounts[5]++;
            else            abvCounts[6]++;
        }
        Map<String, Integer> chartAbvDist = new LinkedHashMap<>();
        boolean anyAbv = false;
        for (int i = 0; i < 7; i++) {
            chartAbvDist.put(abvLabels[i], abvCounts[i]);
            if (abvCounts[i] > 0) anyAbv = true;
        }
        if (!anyAbv) chartAbvDist.clear();

        // ── Resumen por estilo ─────────────────────────────────────────
        Map<String, BigDecimal[]> resumenAgg = new LinkedHashMap<>();
        for (var lote : lotes) {
            String est = lote.getEstilo() != null ? lote.getEstilo() : "Sin estilo";
            BigDecimal[] agg = resumenAgg.computeIfAbsent(est, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            agg[0] = agg[0].add(BigDecimal.ONE);
            agg[1] = agg[1].add(lote.getLitrosFinales() != null ? lote.getLitrosFinales() : BigDecimal.ZERO);
        }
        List<Map<String, Object>> resumenEstilos = new ArrayList<>();
        resumenAgg.entrySet().stream()
                .sorted((a, b) -> b.getValue()[1].compareTo(a.getValue()[1]))
                .forEach(e -> {
                    BigDecimal litros = e.getValue()[1];
                    int pct = totalLitros.compareTo(BigDecimal.ZERO) > 0
                            ? litros.multiply(BigDecimal.valueOf(100))
                                    .divide(totalLitros, 0, RoundingMode.HALF_UP).intValue()
                            : 0;
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("estilo",   e.getKey());
                    m.put("cantidad", e.getValue()[0].longValue());
                    m.put("litros",   litros);
                    m.put("pct",      pct);
                    resumenEstilos.add(m);
                });

        // ── Shortcuts de período ───────────────────────────────────────
        LocalDate hoy = LocalDate.now();
        model.addAttribute("urlEsteMes",   "/reportes/produccion?desde=" + hoy.withDayOfMonth(1) + "&hasta=" + hoy);
        model.addAttribute("urlUltimoMes", "/reportes/produccion?desde=" + hoy.withDayOfMonth(1).minusMonths(1)
                + "&hasta=" + hoy.withDayOfMonth(1).minusDays(1));
        model.addAttribute("urlEsteAnio",  "/reportes/produccion?desde=" + hoy.withDayOfYear(1) + "&hasta=" + hoy);
        model.addAttribute("urlUltimos3",  "/reportes/produccion?desde=" + hoy.minusMonths(3) + "&hasta=" + hoy);

        model.addAttribute("lotes",              lotes);
        model.addAttribute("desde",              desde);
        model.addAttribute("hasta",              hasta);
        model.addAttribute("estilo",             estilo);
        model.addAttribute("estilosDisponibles", estilosDisponibles);
        model.addAttribute("totalLotes",         totalLotes);
        model.addAttribute("totalLitros",        totalLitros);
        model.addAttribute("estilosUnicos",      estilosUnicos);
        model.addAttribute("completados",        completados);
        model.addAttribute("avgLitros",          avgLitros);
        model.addAttribute("avgAbv",             avgAbv);
        model.addAttribute("avgEficiencia",      avgEficiencia);
        model.addAttribute("costoTotalPeriodo",  costoTotalPeriodo);
        model.addAttribute("tasaCompletados",    tasaCompletados);
        model.addAttribute("chartEstilosLitros", chartEstilosLitros);
        model.addAttribute("chartTendencia",     chartTendencia);
        model.addAttribute("chartAbvDist",       chartAbvDist);
        model.addAttribute("resumenEstilos",     resumenEstilos);
        return "reportes/produccion";
    }

    @GetMapping("/produccion/excel")
    public ResponseEntity<byte[]> produccionExcel(
            @RequestParam(required = false) String estilo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            HttpServletRequest request) {

        if (desde == null) desde = LocalDate.now().minusMonths(3);
        if (hasta == null) hasta = LocalDate.now();

        var todosLotes = loteRepo.findByPeriodo(desde, hasta);
        List<LoteCerveza> lotes = (estilo != null && !estilo.isBlank())
                ? todosLotes.stream().filter(l -> estilo.equals(l.getEstilo())).collect(Collectors.toList())
                : todosLotes;
        var resumen = loteRepo.findResumenPorEstilo(desde, hasta, TenantContext.getCurrentTenant());

        Tenant tenant = (Tenant) request.getAttribute("currentTenant");
        ExportBranding branding = ExportBranding.from(tenant);

        byte[] excel = excelExportService.generarExcelReporteProduccion(lotes, resumen, desde, hasta, branding);
        String filename = "reporte-produccion-" + desde + "-" + hasta + ".xlsx";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(excel);
    }

    @GetMapping("/produccion/pdf")
    public ResponseEntity<byte[]> produccionPdf(
            @RequestParam(required = false) String estilo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            HttpServletRequest request) {

        if (desde == null) desde = LocalDate.now().minusMonths(3);
        if (hasta == null) hasta = LocalDate.now();

        var todosLotes = loteRepo.findByPeriodo(desde, hasta);
        List<LoteCerveza> lotes = (estilo != null && !estilo.isBlank())
                ? todosLotes.stream().filter(l -> estilo.equals(l.getEstilo())).collect(Collectors.toList())
                : todosLotes;

        Tenant tenant = (Tenant) request.getAttribute("currentTenant");
        ExportBranding branding = ExportBranding.from(tenant);

        byte[] pdf = pdfExportService.generarPdfReporteProduccion(lotes, desde, hasta, estilo, branding);
        String filename = "reporte-produccion-" + desde + "-" + hasta + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }

    // ── Reporte de Ventas ──────────────────────────────────────────────────────

    @GetMapping("/ventas")
    public String ventasReporte(
            @RequestParam(required = false) EstadoVenta estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Model model) {

        if (desde == null) desde = LocalDate.now().minusMonths(3);
        if (hasta == null) hasta = LocalDate.now();

        List<Venta> ventas = ventaService.listarParaExport(estado, desde, hasta);

        // ── Estadísticas ──────────────────────────────────────────────
        long totalVentas      = ventas.size();
        long totalDespachadas = ventas.stream().filter(v -> v.getEstado() == EstadoVenta.DESPACHADO).count();
        long totalPendientes  = ventas.stream().filter(v -> v.getEstado() == EstadoVenta.PENDIENTE).count();
        long totalCanceladas  = ventas.stream().filter(v -> v.getEstado() == EstadoVenta.CANCELADO).count();

        BigDecimal ingresosTotales = ventas.stream()
                .filter(v -> v.getEstado() == EstadoVenta.DESPACHADO)
                .map(Venta::getValorTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long clientesUnicos = ventas.stream()
                .map(Venta::getCliente).filter(Objects::nonNull)
                .distinct().count();

        // ── Gráfico: ingresos por mes ──────────────────────────────────
        String[] mesesNombres = {"Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic"};
        Map<String, Number> chartIngresosMes = new LinkedHashMap<>();
        ventas.stream()
                .filter(v -> v.getFechaDespacho() != null && v.getEstado() == EstadoVenta.DESPACHADO)
                .sorted(Comparator.comparing(Venta::getFechaDespacho))
                .collect(Collectors.groupingBy(
                        v -> {
                            LocalDate f = v.getFechaDespacho();
                            return mesesNombres[f.getMonthValue() - 1] + " " + f.getYear();
                        },
                        LinkedHashMap::new,
                        Collectors.summingDouble(v -> v.getValorTotal() != null
                                ? v.getValorTotal().doubleValue() : 0.0)
                ))
                .forEach(chartIngresosMes::put);

        // ── Gráfico: ventas por estado ────────────────────────────────
        Map<String, Integer> chartEstados = new LinkedHashMap<>();
        chartEstados.put(EstadoVenta.DESPACHADO.getDisplayName(), (int) totalDespachadas);
        chartEstados.put(EstadoVenta.PENDIENTE.getDisplayName(),  (int) totalPendientes);
        chartEstados.put(EstadoVenta.CANCELADO.getDisplayName(),  (int) totalCanceladas);

        // ── Top clientes (para tabla resumen) ─────────────────────────
        Map<String, double[]> clienteAgg = new LinkedHashMap<>();
        for (var v : ventas) {
            if (v.getCliente() == null) continue;
            double[] agg = clienteAgg.computeIfAbsent(v.getCliente(), k -> new double[]{0, 0});
            agg[0]++;
            if (v.getValorTotal() != null) agg[1] += v.getValorTotal().doubleValue();
        }
        List<Map<String, Object>> resumenClientes = new ArrayList<>();
        clienteAgg.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue()[1], a.getValue()[1]))
                .limit(15)
                .forEach(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("cliente", e.getKey());
                    m.put("ventas",  (long) e.getValue()[0]);
                    m.put("total",   BigDecimal.valueOf(e.getValue()[1]).setScale(0, RoundingMode.HALF_UP));
                    resumenClientes.add(m);
                });

        // ── Shortcuts de período ───────────────────────────────────────
        LocalDate hoy = LocalDate.now();
        model.addAttribute("urlEsteMes",   "/reportes/ventas?desde=" + hoy.withDayOfMonth(1) + "&hasta=" + hoy);
        model.addAttribute("urlUltimoMes", "/reportes/ventas?desde=" + hoy.withDayOfMonth(1).minusMonths(1)
                + "&hasta=" + hoy.withDayOfMonth(1).minusDays(1));
        model.addAttribute("urlEsteAnio",  "/reportes/ventas?desde=" + hoy.withDayOfYear(1) + "&hasta=" + hoy);
        model.addAttribute("urlUltimos3",  "/reportes/ventas");

        model.addAttribute("ventas",           ventas);
        model.addAttribute("desde",            desde);
        model.addAttribute("hasta",            hasta);
        model.addAttribute("estadoFiltro",     estado);
        model.addAttribute("estados",          EstadoVenta.values());
        model.addAttribute("totalVentas",      totalVentas);
        model.addAttribute("totalDespachadas", totalDespachadas);
        model.addAttribute("totalPendientes",  totalPendientes);
        model.addAttribute("totalCanceladas",  totalCanceladas);
        model.addAttribute("ingresosTotales",  ingresosTotales);
        model.addAttribute("clientesUnicos",   clientesUnicos);
        model.addAttribute("chartIngresosMes", chartIngresosMes);
        model.addAttribute("chartEstados",     chartEstados);
        model.addAttribute("resumenClientes",  resumenClientes);
        return "reportes/ventas";
    }

    // ── Reporte de Rentabilidad por Lote ──────────────────────────────────────

    @GetMapping("/rentabilidad")
    public String rentabilidad(
            @RequestParam(required = false) String estilo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false, defaultValue = "margen") String ordenar,
            Model model) {

        // Costos por lote (una sola query agregada)
        Map<Long, BigDecimal> costosPorLote = new HashMap<>();
        for (Object[] row : loteItemFacturaRepo.sumCostosPorLote()) {
            costosPorLote.put(((Number) row[0]).longValue(), toBigDecimal(row[1]));
        }

        // Ingresos despachados por lote (una sola query agregada)
        Map<Long, BigDecimal> ingresosPorLote = new HashMap<>();
        for (Object[] row : ventaItemRepo.sumIngresosDespachadosPorLote()) {
            ingresosPorLote.put(((Number) row[0]).longValue(), toBigDecimal(row[1]));
        }

        // Lotes completados con filtro de período
        var todosLotes = loteRepo.findAllCompletados(
                org.springframework.data.domain.PageRequest.of(0, 1000));

        // Filtro de período por fechaCompletado (carbFechaFinal)
        var lotesFiltrados = todosLotes.stream()
                .filter(l -> desde == null || (l.getCarbFechaFinal() != null && !l.getCarbFechaFinal().isBefore(desde)))
                .filter(l -> hasta == null || (l.getCarbFechaFinal() != null && !l.getCarbFechaFinal().isAfter(hasta)))
                .filter(l -> estilo == null || estilo.isBlank() || estilo.equals(l.getEstilo()))
                .collect(Collectors.toList());

        // Estilos disponibles para el filtro
        List<String> estilosDisponibles = todosLotes.stream()
                .map(LoteCerveza::getEstilo).filter(e -> e != null && !e.isBlank())
                .distinct().sorted().collect(Collectors.toList());

        // Construir DTOs
        List<RentabilidadLoteDto> filas = new ArrayList<>();
        for (LoteCerveza lote : lotesFiltrados) {
            BigDecimal costo    = costosPorLote.getOrDefault(lote.getId(), null);
            BigDecimal ingresos = ingresosPorLote.getOrDefault(lote.getId(), null);

            // Si el costo es 0 y no hay entrada, lo tratamos como sin datos
            if (costo != null && costo.compareTo(BigDecimal.ZERO) == 0) costo = null;
            if (ingresos != null && ingresos.compareTo(BigDecimal.ZERO) == 0) ingresos = null;

            BigDecimal margen    = null;
            BigDecimal margenPct = null;
            if (costo != null && ingresos != null) {
                margen    = ingresos.subtract(costo).setScale(0, RoundingMode.HALF_UP);
                margenPct = ingresos.compareTo(BigDecimal.ZERO) > 0
                        ? margen.multiply(BigDecimal.valueOf(100))
                                .divide(ingresos, 1, RoundingMode.HALF_UP)
                        : null;
            } else if (ingresos != null) {
                margen = ingresos.setScale(0, RoundingMode.HALF_UP); // sin costo registrado
            }

            BigDecimal litros = lote.getLitrosFinales();
            BigDecimal costoPorLitro = (costo != null && litros != null && litros.compareTo(BigDecimal.ZERO) > 0)
                    ? costo.divide(litros, 2, RoundingMode.HALF_UP) : null;
            BigDecimal ingresoPorLitro = (ingresos != null && litros != null && litros.compareTo(BigDecimal.ZERO) > 0)
                    ? ingresos.divide(litros, 2, RoundingMode.HALF_UP) : null;

            filas.add(new RentabilidadLoteDto(
                    lote.getId(), lote.getCodigoLote(), lote.getEstilo(),
                    lote.getCarbFechaFinal(), litros,
                    costo, ingresos, margen, margenPct,
                    costoPorLitro, ingresoPorLitro));
        }

        // Ordenamiento
        Comparator<RentabilidadLoteDto> comp = switch (ordenar) {
            case "ingreso" -> Comparator.comparing(r -> r.ingresos() != null ? r.ingresos() : BigDecimal.ZERO,
                    Comparator.reverseOrder());
            case "pct"     -> Comparator.comparing(r -> r.margenPct() != null ? r.margenPct() : BigDecimal.valueOf(-999),
                    Comparator.reverseOrder());
            case "fecha"   -> Comparator.comparing(r -> r.fechaCompletado() != null ? r.fechaCompletado() : LocalDate.MIN,
                    Comparator.reverseOrder());
            default        -> Comparator.comparing(r -> r.margen() != null ? r.margen() : BigDecimal.valueOf(-999_999),
                    Comparator.reverseOrder());
        };
        filas.sort(comp);

        // Totales y estadísticas
        BigDecimal totalCosto     = filas.stream().filter(r -> r.costo() != null)
                .map(RentabilidadLoteDto::costo).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalIngresos  = filas.stream().filter(r -> r.ingresos() != null)
                .map(RentabilidadLoteDto::ingresos).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalMargen    = totalIngresos.subtract(totalCosto).setScale(0, RoundingMode.HALF_UP);
        BigDecimal totalMargenPct = totalIngresos.compareTo(BigDecimal.ZERO) > 0
                ? totalMargen.multiply(BigDecimal.valueOf(100))
                        .divide(totalIngresos, 1, RoundingMode.HALF_UP)
                : null;

        long lotesConDatos   = filas.stream().filter(r -> !r.sinCosto() && !r.sinVentas()).count();
        long lotesRentables  = filas.stream().filter(RentabilidadLoteDto::rentable).count();
        long lotesEnRojo     = filas.stream().filter(RentabilidadLoteDto::enRojo).count();

        // Gráfico: top 10 por margen (barras)
        Map<String, Number> chartMargen = new LinkedHashMap<>();
        filas.stream()
                .filter(r -> r.margen() != null)
                .limit(10)
                .forEach(r -> chartMargen.put(r.codigoLote(), r.margen()));

        // Shortcuts de período
        LocalDate hoy = LocalDate.now();
        model.addAttribute("urlEsteMes",   "/reportes/rentabilidad?desde=" + hoy.withDayOfMonth(1) + "&hasta=" + hoy);
        model.addAttribute("urlUltimoMes", "/reportes/rentabilidad?desde="
                + hoy.withDayOfMonth(1).minusMonths(1) + "&hasta=" + hoy.withDayOfMonth(1).minusDays(1));
        model.addAttribute("urlEsteAnio",  "/reportes/rentabilidad?desde=" + hoy.withDayOfYear(1) + "&hasta=" + hoy);
        model.addAttribute("urlUltimos3",  "/reportes/rentabilidad?desde=" + hoy.minusMonths(3) + "&hasta=" + hoy);

        model.addAttribute("filas",              filas);
        model.addAttribute("desde",              desde);
        model.addAttribute("hasta",              hasta);
        model.addAttribute("estilo",             estilo);
        model.addAttribute("ordenar",            ordenar);
        model.addAttribute("estilosDisponibles", estilosDisponibles);
        model.addAttribute("totalCosto",         totalCosto);
        model.addAttribute("totalIngresos",      totalIngresos);
        model.addAttribute("totalMargen",        totalMargen);
        model.addAttribute("totalMargenPct",     totalMargenPct);
        model.addAttribute("lotesConDatos",      lotesConDatos);
        model.addAttribute("lotesRentables",     lotesRentables);
        model.addAttribute("lotesEnRojo",        lotesEnRojo);
        model.addAttribute("chartMargen",        chartMargen);
        return "reportes/rentabilidad";
    }

    @GetMapping("/ventas/excel")
    public ResponseEntity<byte[]> ventasExcel(
            @RequestParam(required = false) EstadoVenta estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            HttpServletRequest request) {

        if (desde == null) desde = LocalDate.now().minusMonths(3);
        if (hasta == null) hasta = LocalDate.now();

        List<Venta> ventas = ventaService.listarParaExport(estado, desde, hasta);
        Tenant tenant = (Tenant) request.getAttribute("currentTenant");
        ExportBranding branding = ExportBranding.from(tenant);

        byte[] excel = excelExportService.generarExcelVentas(ventas, estado, desde, hasta, branding);
        String filename = "reporte-ventas-" + desde + "-" + hasta + ".xlsx";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(excel);
    }

    // JPQL SUM sobre BigDecimal puede retornar Double en Hibernate — conversión segura
    private static BigDecimal toBigDecimal(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal bd) return bd;
        return BigDecimal.valueOf(((Number) o).doubleValue());
    }
}
