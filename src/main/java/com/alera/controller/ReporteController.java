package com.alera.controller;

import com.alera.config.ExportBranding;
import com.alera.config.TenantContext;
import com.alera.model.LoteCerveza;
import com.alera.model.Tenant;
import com.alera.repository.LoteCervezaRepository;
import com.alera.service.ExcelExportService;
import com.alera.service.PdfExportService;
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

@Controller
@RequestMapping("/reportes")
public class ReporteController {

    private final LoteCervezaRepository loteRepo;
    private final ExcelExportService    excelExportService;
    private final PdfExportService      pdfExportService;

    public ReporteController(LoteCervezaRepository loteRepo,
                              ExcelExportService excelExportService,
                              PdfExportService pdfExportService) {
        this.loteRepo           = loteRepo;
        this.excelExportService = excelExportService;
        this.pdfExportService   = pdfExportService;
    }

    @GetMapping("/produccion")
    public String produccion(
            @RequestParam(required = false) String estilo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Model model) {

        if (desde == null) desde = LocalDate.now().minusMonths(3);
        if (hasta == null) hasta = LocalDate.now();

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
        model.addAttribute("urlUltimos3",  "/reportes/produccion");

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
}
