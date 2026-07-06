package com.alera.controller;

import com.alera.config.ExportBranding;
import com.alera.model.*;
import com.alera.model.enums.TipoResiduo;
import com.alera.service.BpmPdfService;
import com.alera.service.BpmService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Controller
@RequestMapping("/bpm")
public class BpmController {

    private final BpmService service;
    private final BpmPdfService pdfService;

    public BpmController(BpmService service, BpmPdfService pdfService) {
        this.service = service;
        this.pdfService = pdfService;
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @GetMapping
    public String index(Model model) {
        LocalDate inicio = LocalDate.now().withDayOfMonth(1);
        LocalDate fin    = LocalDate.now();
        model.addAttribute("cntSintomas",  service.contarSintomasMes(inicio, fin));
        model.addAttribute("cntSoluciones", service.contarSolucionesMes(inicio, fin));
        model.addAttribute("cntPlagas",    service.contarPlagasMes(inicio, fin));
        model.addAttribute("cntResiduos",  service.contarResiduosMes(inicio, fin));
        model.addAttribute("cntLimpieza",  service.contarLimpiezaMes(inicio, fin));
        return "bpm/index";
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SÍNTOMAS
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/sintomas")
    public String listaSintomas(Model model) {
        model.addAttribute("registros", service.listarSintomas());
        return "bpm/sintomas/lista";
    }

    @GetMapping("/sintomas/nuevo")
    public String nuevoSintoma(Model model) {
        model.addAttribute("registro", new RegistroSintomas());
        return "bpm/sintomas/formulario";
    }

    @GetMapping("/sintomas/editar/{id}")
    public String editarSintoma(@PathVariable Long id, Model model) {
        model.addAttribute("registro", service.buscarSintoma(id));
        return "bpm/sintomas/formulario";
    }

    @PostMapping("/sintomas/guardar")
    public String guardarSintoma(@Valid @ModelAttribute("registro") RegistroSintomas r,
                                  BindingResult br, Model model,
                                  RedirectAttributes flash) {
        if (br.hasErrors()) return "bpm/sintomas/formulario";
        service.guardarSintoma(r);
        flash.addFlashAttribute("mensaje", "Registro de síntomas guardado correctamente");
        flash.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/bpm/sintomas";
    }

    @PostMapping("/sintomas/eliminar/{id}")
    public String eliminarSintoma(@PathVariable Long id, RedirectAttributes flash) {
        service.eliminarSintoma(id);
        flash.addFlashAttribute("mensaje", "Registro eliminado");
        flash.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/bpm/sintomas";
    }

    @GetMapping("/sintomas/pdf")
    public ResponseEntity<byte[]> pdfSintomas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            HttpServletRequest request, Locale locale) {
        if (desde == null) desde = LocalDate.now().withDayOfMonth(1);
        if (hasta == null) hasta = LocalDate.now();
        var registros = service.listarSintomasEntre(desde, hasta);
        Tenant tenant = (Tenant) request.getAttribute("currentTenant");
        ExportBranding b = ExportBranding.from(tenant);
        String logo = tenant != null ? tenant.getLogoUrl() : null;
        String sub = "Período: " + desde.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + " — " + hasta.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        byte[] bytes = pdfService.generarSintomas(registros, b, logo,
                "Control Estado de Salud", sub);
        String fn = "bpm-sintomas-" + LocalDate.now() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fn + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SOLUCIONES DESINFECTANTES
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/soluciones")
    public String listaSoluciones(Model model) {
        model.addAttribute("registros", service.listarSoluciones());
        return "bpm/soluciones/lista";
    }

    @GetMapping("/soluciones/nuevo")
    public String nuevaSolucion(Model model) {
        model.addAttribute("registro", new SolucionDesinfectante());
        return "bpm/soluciones/formulario";
    }

    @GetMapping("/soluciones/editar/{id}")
    public String editarSolucion(@PathVariable Long id, Model model) {
        model.addAttribute("registro", service.buscarSolucion(id));
        return "bpm/soluciones/formulario";
    }

    @PostMapping("/soluciones/guardar")
    public String guardarSolucion(@Valid @ModelAttribute("registro") SolucionDesinfectante r,
                                   BindingResult br, Model model,
                                   RedirectAttributes flash) {
        if (br.hasErrors()) return "bpm/soluciones/formulario";
        service.guardarSolucion(r);
        flash.addFlashAttribute("mensaje", "Solución desinfectante guardada correctamente");
        flash.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/bpm/soluciones";
    }

    @PostMapping("/soluciones/eliminar/{id}")
    public String eliminarSolucion(@PathVariable Long id, RedirectAttributes flash) {
        service.eliminarSolucion(id);
        flash.addFlashAttribute("mensaje", "Registro eliminado");
        flash.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/bpm/soluciones";
    }

    @GetMapping("/soluciones/pdf")
    public ResponseEntity<byte[]> pdfSoluciones(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            HttpServletRequest request, Locale locale) {
        if (desde == null) desde = LocalDate.now().withDayOfMonth(1);
        if (hasta == null) hasta = LocalDate.now();
        var registros = service.listarSolucionesEntre(desde, hasta);
        Tenant tenant = (Tenant) request.getAttribute("currentTenant");
        ExportBranding b = ExportBranding.from(tenant);
        String logo = tenant != null ? tenant.getLogoUrl() : null;
        String sub = "Período: " + desde.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + " — " + hasta.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        byte[] bytes = pdfService.generarSoluciones(registros, b, logo,
                "Soluciones Desinfectantes", sub);
        String fn = "bpm-soluciones-" + LocalDate.now() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fn + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PLAGAS
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/plagas")
    public String listaPlagas(Model model) {
        model.addAttribute("registros", service.listarPlagas());
        return "bpm/plagas/lista";
    }

    @GetMapping("/plagas/nuevo")
    public String nuevaPlaga(Model model) {
        model.addAttribute("registro", new AvistamientoPlagas());
        return "bpm/plagas/formulario";
    }

    @GetMapping("/plagas/editar/{id}")
    public String editarPlaga(@PathVariable Long id, Model model) {
        model.addAttribute("registro", service.buscarPlaga(id));
        return "bpm/plagas/formulario";
    }

    @PostMapping("/plagas/guardar")
    public String guardarPlaga(@Valid @ModelAttribute("registro") AvistamientoPlagas r,
                                BindingResult br, Model model,
                                RedirectAttributes flash) {
        if (br.hasErrors()) return "bpm/plagas/formulario";
        service.guardarPlaga(r);
        flash.addFlashAttribute("mensaje", "Registro de plagas guardado correctamente");
        flash.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/bpm/plagas";
    }

    @PostMapping("/plagas/eliminar/{id}")
    public String eliminarPlaga(@PathVariable Long id, RedirectAttributes flash) {
        service.eliminarPlaga(id);
        flash.addFlashAttribute("mensaje", "Registro eliminado");
        flash.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/bpm/plagas";
    }

    @GetMapping("/plagas/pdf")
    public ResponseEntity<byte[]> pdfPlagas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            HttpServletRequest request, Locale locale) {
        if (desde == null) desde = LocalDate.now().withDayOfMonth(1);
        if (hasta == null) hasta = LocalDate.now();
        var registros = service.listarPlagasEntre(desde, hasta);
        Tenant tenant = (Tenant) request.getAttribute("currentTenant");
        ExportBranding b = ExportBranding.from(tenant);
        String logo = tenant != null ? tenant.getLogoUrl() : null;
        String sub = "Período: " + desde.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + " — " + hasta.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        byte[] bytes = pdfService.generarPlagas(registros, b, logo,
                "Control de Plagas", sub);
        String fn = "bpm-plagas-" + LocalDate.now() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fn + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // RESIDUOS
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/residuos")
    public String listaResiduos(Model model) {
        model.addAttribute("registros", service.listarResiduos());
        model.addAttribute("tiposResiduo", TipoResiduo.values());
        return "bpm/residuos/lista";
    }

    @GetMapping("/residuos/nuevo")
    public String nuevoResiduo(Model model) {
        model.addAttribute("registro", new EvacuacionResiduos());
        model.addAttribute("tiposResiduo", TipoResiduo.values());
        return "bpm/residuos/formulario";
    }

    @GetMapping("/residuos/editar/{id}")
    public String editarResiduo(@PathVariable Long id, Model model) {
        model.addAttribute("registro", service.buscarResiduo(id));
        model.addAttribute("tiposResiduo", TipoResiduo.values());
        return "bpm/residuos/formulario";
    }

    @PostMapping("/residuos/guardar")
    public String guardarResiduo(@Valid @ModelAttribute("registro") EvacuacionResiduos r,
                                  BindingResult br, Model model,
                                  RedirectAttributes flash) {
        if (br.hasErrors()) {
            model.addAttribute("tiposResiduo", TipoResiduo.values());
            return "bpm/residuos/formulario";
        }
        service.guardarResiduo(r);
        flash.addFlashAttribute("mensaje", "Registro de residuos guardado correctamente");
        flash.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/bpm/residuos";
    }

    @PostMapping("/residuos/eliminar/{id}")
    public String eliminarResiduo(@PathVariable Long id, RedirectAttributes flash) {
        service.eliminarResiduo(id);
        flash.addFlashAttribute("mensaje", "Registro eliminado");
        flash.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/bpm/residuos";
    }

    @GetMapping("/residuos/pdf")
    public ResponseEntity<byte[]> pdfResiduos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            HttpServletRequest request, Locale locale) {
        if (desde == null) desde = LocalDate.now().withDayOfMonth(1);
        if (hasta == null) hasta = LocalDate.now();
        var registros = service.listarResiduosEntre(desde, hasta);
        Tenant tenant = (Tenant) request.getAttribute("currentTenant");
        ExportBranding b = ExportBranding.from(tenant);
        String logo = tenant != null ? tenant.getLogoUrl() : null;
        String sub = "Período: " + desde.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + " — " + hasta.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        byte[] bytes = pdfService.generarResiduos(registros, b, logo,
                "Evacuación de Residuos", sub);
        String fn = "bpm-residuos-" + LocalDate.now() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fn + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // LIMPIEZA Y DESINFECCIÓN
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/limpieza")
    public String listaLimpieza(Model model) {
        model.addAttribute("registros", service.listarLimpieza());
        return "bpm/limpieza/lista";
    }

    @GetMapping("/limpieza/nuevo")
    public String nuevoLimpieza(Model model) {
        model.addAttribute("registro", new LimpiezaDesinfeccion());
        return "bpm/limpieza/formulario";
    }

    @GetMapping("/limpieza/editar/{id}")
    public String editarLimpieza(@PathVariable Long id, Model model) {
        model.addAttribute("registro", service.buscarLimpieza(id));
        return "bpm/limpieza/formulario";
    }

    @PostMapping("/limpieza/guardar")
    public String guardarLimpieza(@Valid @ModelAttribute("registro") LimpiezaDesinfeccion r,
                                   BindingResult br, Model model,
                                   RedirectAttributes flash) {
        if (br.hasErrors()) return "bpm/limpieza/formulario";
        service.guardarLimpieza(r);
        flash.addFlashAttribute("mensaje", "Registro de limpieza guardado correctamente");
        flash.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/bpm/limpieza";
    }

    @PostMapping("/limpieza/eliminar/{id}")
    public String eliminarLimpieza(@PathVariable Long id, RedirectAttributes flash) {
        service.eliminarLimpieza(id);
        flash.addFlashAttribute("mensaje", "Registro eliminado");
        flash.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/bpm/limpieza";
    }

    @GetMapping("/limpieza/pdf")
    public ResponseEntity<byte[]> pdfLimpieza(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            HttpServletRequest request, Locale locale) {
        if (desde == null) desde = LocalDate.now().withDayOfMonth(1);
        if (hasta == null) hasta = LocalDate.now();
        var registros = service.listarLimpiezaEntre(desde, hasta);
        Tenant tenant = (Tenant) request.getAttribute("currentTenant");
        ExportBranding b = ExportBranding.from(tenant);
        String logo = tenant != null ? tenant.getLogoUrl() : null;
        String sub = "Período: " + desde.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + " — " + hasta.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        byte[] bytes = pdfService.generarLimpieza(registros, b, logo,
                "Limpieza y Desinfección", sub);
        String fn = "bpm-limpieza-" + LocalDate.now() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fn + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }
}
