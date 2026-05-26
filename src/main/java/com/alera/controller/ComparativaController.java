package com.alera.controller;

import com.alera.model.LoteCerveza;
import com.alera.model.Tenant;
import com.alera.repository.LoteCervezaRepository;
import com.alera.service.PdfExportService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/comparativa")
public class ComparativaController {

    private final LoteCervezaRepository loteRepo;
    private final PdfExportService pdfService;

    public ComparativaController(LoteCervezaRepository loteRepo, PdfExportService pdfService) {
        this.loteRepo   = loteRepo;
        this.pdfService = pdfService;
    }

    @GetMapping
    public String seleccion(@RequestParam(defaultValue = "") String q, Model model) {
        List<LoteCerveza> lotes = loteRepo.search(
                q.isBlank() ? "" : q.trim(),
                PageRequest.of(0, 100, Sort.by("createdAt").descending()));
        model.addAttribute("lotes", lotes);
        model.addAttribute("qFiltro", q);
        return "comparativa/seleccion";
    }

    @GetMapping("/resultado")
    public String resultado(@RequestParam(required = false) List<Long> ids,
                             RedirectAttributes ra, Model model) {
        if (ids == null || ids.size() < 2) {
            ra.addFlashAttribute("mensaje", "Seleccioná al menos 2 lotes para comparar");
            ra.addFlashAttribute("tipoMensaje", "warning");
            return "redirect:/comparativa";
        }
        if (ids.size() > 6) ids = ids.subList(0, 6);

        List<LoteCerveza> lotes = loteRepo.findByIds(ids);

        Map<String, Long> mejores = new LinkedHashMap<>();
        mejorMax(lotes, "abv",        LoteCerveza::getAbv,                mejores);
        mejorMax(lotes, "atenuacion", LoteCerveza::getAtenuacionAparente, mejores);
        mejorMax(lotes, "eficiencia", LoteCerveza::getEficienciaMacerado, mejores);
        mejorMax(lotes, "litros",     LoteCerveza::getLitrosFinales,      mejores);
        mejorMin(lotes, "cpl",        LoteCerveza::getCostoPorLitro,      mejores);

        model.addAttribute("lotes",          lotes);
        model.addAttribute("mejores",        mejores);
        model.addAttribute("chartLabels",    lotes.stream().map(LoteCerveza::getCodigoLote).collect(Collectors.toList()));
        model.addAttribute("chartAbv",       chartValues(lotes, LoteCerveza::getAbv));
        model.addAttribute("chartAtenuacion",chartValues(lotes, LoteCerveza::getAtenuacionAparente));
        model.addAttribute("chartEficiencia",chartValues(lotes, LoteCerveza::getEficienciaMacerado));
        return "comparativa/resultado";
    }

    @GetMapping("/resultado/pdf")
    public ResponseEntity<byte[]> pdf(@RequestParam(required = false) List<Long> ids,
                                       HttpServletRequest request) {
        if (ids == null || ids.size() < 2) {
            return ResponseEntity.badRequest().build();
        }
        if (ids.size() > 6) ids = ids.subList(0, 6);
        List<LoteCerveza> lotes = loteRepo.findByIds(ids);

        Map<String, Long> mejoresMap = new LinkedHashMap<>();
        mejorMax(lotes, "abv",        LoteCerveza::getAbv,                mejoresMap);
        mejorMax(lotes, "atenuacion", LoteCerveza::getAtenuacionAparente, mejoresMap);
        mejorMax(lotes, "eficiencia", LoteCerveza::getEficienciaMacerado, mejoresMap);
        mejorMax(lotes, "litros",     LoteCerveza::getLitrosFinales,      mejoresMap);
        Map<String, Long> cplMap = new LinkedHashMap<>();
        mejorMin(lotes, "cpl",        LoteCerveza::getCostoPorLitro,      cplMap);
        Long mejorCpl = cplMap.get("cpl");

        Tenant tenant = (Tenant) request.getAttribute("currentTenant");
        com.alera.config.ExportBranding branding = com.alera.config.ExportBranding.from(tenant);

        byte[] pdf = pdfService.generarPdfComparativa(lotes, mejoresMap, mejorCpl, branding);
        String filename = "comparativa-" + lotes.stream()
                .map(LoteCerveza::getCodigoLote).collect(Collectors.joining("-")) + ".pdf";

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private void mejorMax(List<LoteCerveza> lotes, String key,
                           Function<LoteCerveza, BigDecimal> fn, Map<String, Long> out) {
        lotes.stream().filter(l -> fn.apply(l) != null)
                .max(Comparator.comparing(fn))
                .ifPresent(l -> out.put(key, l.getId()));
    }

    private void mejorMin(List<LoteCerveza> lotes, String key,
                           Function<LoteCerveza, BigDecimal> fn, Map<String, Long> out) {
        lotes.stream().filter(l -> fn.apply(l) != null)
                .min(Comparator.comparing(fn))
                .ifPresent(l -> out.put(key, l.getId()));
    }

    private List<Double> chartValues(List<LoteCerveza> lotes, Function<LoteCerveza, BigDecimal> fn) {
        return lotes.stream()
                .map(l -> fn.apply(l) != null ? fn.apply(l).doubleValue() : null)
                .collect(Collectors.toList());
    }
}