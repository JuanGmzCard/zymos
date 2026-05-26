package com.alera.controller;

import com.alera.config.TenantContext;
import com.alera.model.Tenant;
import com.alera.repository.LoteCervezaRepository;
import com.alera.service.ExcelExportService;
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

@Controller
@RequestMapping("/reportes")
public class ReporteController {

    private final LoteCervezaRepository loteRepo;
    private final ExcelExportService excelExportService;

    public ReporteController(LoteCervezaRepository loteRepo, ExcelExportService excelExportService) {
        this.loteRepo = loteRepo;
        this.excelExportService = excelExportService;
    }

    @GetMapping("/produccion")
    public String produccion(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Model model) {

        if (desde == null) desde = LocalDate.now().minusMonths(3);
        if (hasta == null) hasta = LocalDate.now();

        String tenantId = TenantContext.getCurrentTenant();
        var lotes   = loteRepo.findByPeriodo(desde, hasta);
        var resumen = loteRepo.findResumenPorEstilo(desde, hasta, tenantId);

        // Estadísticas del período
        long totalLotes = lotes.size();
        BigDecimal totalLitros = lotes.stream()
                .map(l -> l.getLitrosFinales() != null ? l.getLitrosFinales() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long estilosUnicos = lotes.stream().map(l -> l.getEstilo()).distinct().count();
        long completados   = lotes.stream().filter(l -> l.isCompletado()).count();

        // Promedio litros por lote
        BigDecimal avgLitros = totalLotes > 0
                ? totalLitros.divide(BigDecimal.valueOf(totalLotes), 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // ABV promedio (solo lotes con ambas densidades)
        OptionalDouble avgAbvOpt = lotes.stream()
                .filter(l -> l.getAbv() != null)
                .mapToDouble(l -> l.getAbv().doubleValue())
                .average();
        BigDecimal avgAbv = avgAbvOpt.isPresent()
                ? BigDecimal.valueOf(avgAbvOpt.getAsDouble()).setScale(2, RoundingMode.HALF_UP)
                : null;

        // Tasa de completados (porcentaje)
        int tasaCompletados = totalLotes > 0 ? (int) (completados * 100 / totalLotes) : 0;

        // Gráfico litros por estilo
        Map<String, Number> chartEstilosLitros = new LinkedHashMap<>();
        for (Object[] row : resumen) {
            chartEstilosLitros.put((String) row[0], (Number) row[2]);
        }

        // Gráfico tendencia mensual (litros por mes dentro del período)
        String[] mesesNombres = {"Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic"};
        var litrosPorMes = loteRepo.findLitrosPorMes(desde, tenantId);
        Map<String, Number> chartTendencia = new LinkedHashMap<>();
        LocalDate hastaInicio = hasta.withDayOfMonth(1);
        for (Object[] row : litrosPorMes) {
            int anio = ((Number) row[0]).intValue();
            int mes  = ((Number) row[1]).intValue();
            if (!LocalDate.of(anio, mes, 1).isAfter(hastaInicio)) {
                chartTendencia.put(mesesNombres[mes - 1] + " " + anio, (Number) row[2]);
            }
        }

        // Tabla resumen por estilo con porcentaje del volumen total
        List<Map<String, Object>> resumenEstilos = new ArrayList<>();
        for (Object[] row : resumen) {
            BigDecimal litros = row[2] instanceof BigDecimal bd ? bd
                    : BigDecimal.valueOf(((Number) row[2]).doubleValue());
            int pct = totalLitros.compareTo(BigDecimal.ZERO) > 0
                    ? litros.multiply(BigDecimal.valueOf(100))
                            .divide(totalLitros, 0, RoundingMode.HALF_UP).intValue()
                    : 0;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("estilo",   row[0]);
            m.put("cantidad", ((Number) row[1]).longValue());
            m.put("litros",   litros);
            m.put("pct",      pct);
            resumenEstilos.add(m);
        }

        // URLs de shortcuts de período
        LocalDate hoy = LocalDate.now();
        model.addAttribute("urlEsteMes",   "/reportes/produccion?desde=" + hoy.withDayOfMonth(1) + "&hasta=" + hoy);
        model.addAttribute("urlUltimoMes", "/reportes/produccion?desde=" + hoy.withDayOfMonth(1).minusMonths(1)
                + "&hasta=" + hoy.withDayOfMonth(1).minusDays(1));
        model.addAttribute("urlEsteAnio",  "/reportes/produccion?desde=" + hoy.withDayOfYear(1) + "&hasta=" + hoy);
        model.addAttribute("urlUltimos3",  "/reportes/produccion");

        model.addAttribute("lotes",              lotes);
        model.addAttribute("desde",              desde);
        model.addAttribute("hasta",              hasta);
        model.addAttribute("totalLotes",         totalLotes);
        model.addAttribute("totalLitros",        totalLitros);
        model.addAttribute("estilosUnicos",      estilosUnicos);
        model.addAttribute("completados",        completados);
        model.addAttribute("avgLitros",          avgLitros);
        model.addAttribute("avgAbv",             avgAbv);
        model.addAttribute("tasaCompletados",    tasaCompletados);
        model.addAttribute("chartEstilosLitros", chartEstilosLitros);
        model.addAttribute("chartTendencia",     chartTendencia);
        model.addAttribute("resumenEstilos",     resumenEstilos);
        return "reportes/produccion";
    }

    @GetMapping("/produccion/excel")
    public ResponseEntity<byte[]> produccionExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            HttpServletRequest request) {

        if (desde == null) desde = LocalDate.now().minusMonths(3);
        if (hasta == null) hasta = LocalDate.now();

        var lotes   = loteRepo.findByPeriodo(desde, hasta);
        var resumen = loteRepo.findResumenPorEstilo(desde, hasta, TenantContext.getCurrentTenant());

        Tenant tenant = (Tenant) request.getAttribute("currentTenant");
        com.alera.config.ExportBranding branding = com.alera.config.ExportBranding.from(tenant);

        byte[] excel = excelExportService.generarExcelReporteProduccion(lotes, resumen, desde, hasta, branding);
        String filename = "reporte-produccion-" + desde + "-" + hasta + ".xlsx";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(excel);
    }
}
