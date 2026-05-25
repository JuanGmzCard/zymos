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

        var lotes = loteRepo.findByPeriodo(desde, hasta);
        var resumen = loteRepo.findResumenPorEstilo(desde, hasta, TenantContext.getCurrentTenant());

        // Estadísticas del período
        long totalLotes = lotes.size();
        BigDecimal totalLitros = lotes.stream()
                .map(l -> l.getLitrosFinales() != null ? l.getLitrosFinales() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long estilosUnicos = lotes.stream().map(l -> l.getEstilo()).distinct().count();
        long completados   = lotes.stream().filter(l -> l.isCompletado()).count();

        // Datos para gráfica Chart.js
        Map<String, Number> chartEstilosLitros = new LinkedHashMap<>();
        for (Object[] row : resumen) {
            chartEstilosLitros.put((String) row[0], (Number) row[2]);
        }

        model.addAttribute("lotes",            lotes);
        model.addAttribute("desde",            desde);
        model.addAttribute("hasta",            hasta);
        model.addAttribute("totalLotes",       totalLotes);
        model.addAttribute("totalLitros",      totalLitros);
        model.addAttribute("estilosUnicos",    estilosUnicos);
        model.addAttribute("completados",      completados);
        model.addAttribute("chartEstilosLitros", chartEstilosLitros);
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
        String brandName = tenant != null ? tenant.getName() : "Alera";

        byte[] excel = excelExportService.generarExcelReporteProduccion(lotes, resumen, desde, hasta, brandName);
        String filename = "reporte-produccion-" + desde + "-" + hasta + ".xlsx";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(excel);
    }
}
