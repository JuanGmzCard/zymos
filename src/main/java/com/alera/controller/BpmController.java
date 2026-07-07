package com.alera.controller;

import com.alera.config.ExportBranding;
import com.alera.model.*;
import com.alera.model.enums.TipoResiduo;
import com.alera.service.BpmPdfService;
import com.alera.service.BpmService;
import com.alera.service.InsumoInventarioService;
import com.alera.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/bpm")
public class BpmController {

    private final BpmService service;
    private final BpmPdfService pdfService;
    private final UsuarioService usuarioService;
    private final InsumoInventarioService inventarioService;
    private final MessageSource messageSource;

    public BpmController(BpmService service, BpmPdfService pdfService,
                         UsuarioService usuarioService, InsumoInventarioService inventarioService,
                         MessageSource messageSource) {
        this.service = service;
        this.pdfService = pdfService;
        this.usuarioService = usuarioService;
        this.inventarioService = inventarioService;
        this.messageSource = messageSource;
    }

    private String msg(String key, Locale locale) {
        return messageSource.getMessage(key, null, key, locale);
    }

    private String msgf(String key, Locale locale, Object... args) {
        return messageSource.getMessage(key, args, key, locale);
    }

    @ModelAttribute("usuarios")
    public List<Usuario> populateUsuarios() {
        return usuarioService.listarTodos();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SALUD DIARIO
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/salud/diario")
    public String saludDiario(Model model, Principal principal) {
        String username = principal.getName();
        var existente = service.buscarHoyPorUsuario(username);
        model.addAttribute("hoy", LocalDate.now());
        if (existente.isPresent()) {
            model.addAttribute("registro", existente.get());
            model.addAttribute("yaCompletado", true);
        } else {
            RegistroSintomas nuevo = new RegistroSintomas();
            nuevo.setNombreManipulador(username);
            nuevo.setFecha(LocalDate.now());
            model.addAttribute("registro", nuevo);
            model.addAttribute("yaCompletado", false);
        }
        return "bpm/salud/diario";
    }

    @PostMapping("/salud/diario")
    public String guardarSaludDiario(@ModelAttribute("registro") RegistroSintomas r,
                                      Principal principal) {
        String username = principal.getName();
        if (service.buscarHoyPorUsuario(username).isPresent()) {
            return "redirect:/dashboard";
        }
        r.setNombreManipulador(username);
        r.setFecha(LocalDate.now());
        service.guardarSintoma(r);
        return r.tieneSintomas() ? "redirect:/bpm/salud/bloqueado" : "redirect:/dashboard";
    }

    @GetMapping("/salud/bloqueado")
    public String saludBloqueado(Model model, Principal principal) {
        model.addAttribute("registro",
                service.buscarHoyPorUsuario(principal.getName()).orElse(null));
        return "bpm/salud/bloqueado";
    }

    @GetMapping("/salud/autorizaciones")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public String autorizaciones(Model model) {
        model.addAttribute("registros", service.listarConSintomasHoy());
        model.addAttribute("hoy", LocalDate.now());
        return "bpm/salud/autorizaciones";
    }

    @PostMapping("/salud/autorizar/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public String autorizar(@PathVariable Long id, Principal principal,
                             @RequestParam(required = false) String firmaResponsable,
                             RedirectAttributes flash, Locale locale) {
        service.autorizarAcceso(id, principal.getName(), firmaResponsable);
        flash.addFlashAttribute("mensaje", msg("bpm.aut.autorizado", locale));
        flash.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/bpm/salud/autorizaciones";
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
                                  RedirectAttributes flash, Locale locale) {
        if (br.hasErrors()) return "bpm/sintomas/formulario";
        service.guardarSintoma(r);
        flash.addFlashAttribute("mensaje", msg("bpm.sint.guardado", locale));
        flash.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/bpm/sintomas";
    }

    @PostMapping("/sintomas/eliminar/{id}")
    public String eliminarSintoma(@PathVariable Long id, RedirectAttributes flash, Locale locale) {
        service.eliminarSintoma(id);
        flash.addFlashAttribute("mensaje", msg("bpm.eliminado", locale));
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
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String sub = msgf("bpm.pdf.periodo", locale, desde.format(fmt), hasta.format(fmt));
        byte[] bytes = pdfService.generarSintomas(registros, b, logo,
                msg("bpm.pdf.titulo.sintomas", locale), sub, locale);
        String fn = "bpm-sintomas-" + LocalDate.now() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fn + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    @GetMapping("/sintomas/{id}/pdf")
    public ResponseEntity<byte[]> pdfSintomaIndividual(@PathVariable Long id,
                                                        HttpServletRequest request, Locale locale) {
        var r = service.buscarSintoma(id);
        Tenant tenant = (Tenant) request.getAttribute("currentTenant");
        ExportBranding b = ExportBranding.from(tenant);
        String sub = r.getFecha() != null ? r.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
        byte[] bytes = pdfService.generarSintomas(List.of(r), b, tenant != null ? tenant.getLogoUrl() : null,
                msg("bpm.pdf.titulo.sintomas", locale), sub, locale);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bpm-sintoma-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF).body(bytes);
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
        model.addAttribute("productosQuimicos", inventarioService.listarPorTipo("Químico"));
        return "bpm/soluciones/formulario";
    }

    @GetMapping("/soluciones/editar/{id}")
    public String editarSolucion(@PathVariable Long id, Model model) {
        model.addAttribute("registro", service.buscarSolucion(id));
        model.addAttribute("productosQuimicos", inventarioService.listarPorTipo("Químico"));
        return "bpm/soluciones/formulario";
    }

    @PostMapping("/soluciones/guardar")
    public String guardarSolucion(@Valid @ModelAttribute("registro") SolucionDesinfectante r,
                                   BindingResult br, Model model,
                                   RedirectAttributes flash, Locale locale) {
        if (br.hasErrors()) {
            model.addAttribute("productosQuimicos", inventarioService.listarPorTipo("Químico"));
            return "bpm/soluciones/formulario";
        }
        service.guardarSolucion(r);
        flash.addFlashAttribute("mensaje", msg("bpm.sol.guardado", locale));
        flash.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/bpm/soluciones";
    }

    @PostMapping("/soluciones/eliminar/{id}")
    public String eliminarSolucion(@PathVariable Long id, RedirectAttributes flash, Locale locale) {
        service.eliminarSolucion(id);
        flash.addFlashAttribute("mensaje", msg("bpm.eliminado", locale));
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
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String sub = msgf("bpm.pdf.periodo", locale, desde.format(fmt), hasta.format(fmt));
        byte[] bytes = pdfService.generarSoluciones(registros, b, logo,
                msg("bpm.pdf.titulo.soluciones", locale), sub, locale);
        String fn = "bpm-soluciones-" + LocalDate.now() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fn + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    @GetMapping("/soluciones/{id}/pdf")
    public ResponseEntity<byte[]> pdfSolucionIndividual(@PathVariable Long id,
                                                         HttpServletRequest request, Locale locale) {
        var r = service.buscarSolucion(id);
        Tenant tenant = (Tenant) request.getAttribute("currentTenant");
        ExportBranding b = ExportBranding.from(tenant);
        String sub = r.getFecha() != null ? r.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
        byte[] bytes = pdfService.generarSoluciones(List.of(r), b, tenant != null ? tenant.getLogoUrl() : null,
                msg("bpm.pdf.titulo.soluciones", locale), sub, locale);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bpm-solucion-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF).body(bytes);
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
                                RedirectAttributes flash, Locale locale) {
        if (br.hasErrors()) return "bpm/plagas/formulario";
        service.guardarPlaga(r);
        flash.addFlashAttribute("mensaje", msg("bpm.plagas.guardado", locale));
        flash.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/bpm/plagas";
    }

    @PostMapping("/plagas/eliminar/{id}")
    public String eliminarPlaga(@PathVariable Long id, RedirectAttributes flash, Locale locale) {
        service.eliminarPlaga(id);
        flash.addFlashAttribute("mensaje", msg("bpm.eliminado", locale));
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
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String sub = msgf("bpm.pdf.periodo", locale, desde.format(fmt), hasta.format(fmt));
        byte[] bytes = pdfService.generarPlagas(registros, b, logo,
                msg("bpm.pdf.titulo.plagas", locale), sub, locale);
        String fn = "bpm-plagas-" + LocalDate.now() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fn + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    @GetMapping("/plagas/{id}/pdf")
    public ResponseEntity<byte[]> pdfPlagaIndividual(@PathVariable Long id,
                                                      HttpServletRequest request, Locale locale) {
        var r = service.buscarPlaga(id);
        Tenant tenant = (Tenant) request.getAttribute("currentTenant");
        ExportBranding b = ExportBranding.from(tenant);
        String sub = r.getFecha() != null ? r.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
        byte[] bytes = pdfService.generarPlagas(List.of(r), b, tenant != null ? tenant.getLogoUrl() : null,
                msg("bpm.pdf.titulo.plagas", locale), sub, locale);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bpm-plaga-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF).body(bytes);
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
                                  RedirectAttributes flash, Locale locale) {
        if (br.hasErrors()) {
            model.addAttribute("tiposResiduo", TipoResiduo.values());
            return "bpm/residuos/formulario";
        }
        service.guardarResiduo(r);
        flash.addFlashAttribute("mensaje", msg("bpm.res.guardado", locale));
        flash.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/bpm/residuos";
    }

    @PostMapping("/residuos/eliminar/{id}")
    public String eliminarResiduo(@PathVariable Long id, RedirectAttributes flash, Locale locale) {
        service.eliminarResiduo(id);
        flash.addFlashAttribute("mensaje", msg("bpm.eliminado", locale));
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
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String sub = msgf("bpm.pdf.periodo", locale, desde.format(fmt), hasta.format(fmt));
        byte[] bytes = pdfService.generarResiduos(registros, b, logo,
                msg("bpm.pdf.titulo.residuos", locale), sub, locale);
        String fn = "bpm-residuos-" + LocalDate.now() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fn + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    @GetMapping("/residuos/{id}/pdf")
    public ResponseEntity<byte[]> pdfResiduoIndividual(@PathVariable Long id,
                                                        HttpServletRequest request, Locale locale) {
        var r = service.buscarResiduo(id);
        Tenant tenant = (Tenant) request.getAttribute("currentTenant");
        ExportBranding b = ExportBranding.from(tenant);
        String sub = r.getFecha() != null ? r.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
        byte[] bytes = pdfService.generarResiduos(List.of(r), b, tenant != null ? tenant.getLogoUrl() : null,
                msg("bpm.pdf.titulo.residuos", locale), sub, locale);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bpm-residuo-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF).body(bytes);
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
                                   RedirectAttributes flash, Locale locale) {
        if (br.hasErrors()) return "bpm/limpieza/formulario";
        service.guardarLimpieza(r);
        flash.addFlashAttribute("mensaje", msg("bpm.limp.guardado", locale));
        flash.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/bpm/limpieza";
    }

    @PostMapping("/limpieza/eliminar/{id}")
    public String eliminarLimpieza(@PathVariable Long id, RedirectAttributes flash, Locale locale) {
        service.eliminarLimpieza(id);
        flash.addFlashAttribute("mensaje", msg("bpm.eliminado", locale));
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
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String sub = msgf("bpm.pdf.periodo", locale, desde.format(fmt), hasta.format(fmt));
        byte[] bytes = pdfService.generarLimpieza(registros, b, logo,
                msg("bpm.pdf.titulo.limpieza", locale), sub, locale);
        String fn = "bpm-limpieza-" + LocalDate.now() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fn + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    @GetMapping("/limpieza/{id}/pdf")
    public ResponseEntity<byte[]> pdfLimpiezaIndividual(@PathVariable Long id,
                                                         HttpServletRequest request, Locale locale) {
        var r = service.buscarLimpieza(id);
        Tenant tenant = (Tenant) request.getAttribute("currentTenant");
        ExportBranding b = ExportBranding.from(tenant);
        String sub = r.getFecha() != null ? r.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
        byte[] bytes = pdfService.generarLimpieza(List.of(r), b, tenant != null ? tenant.getLogoUrl() : null,
                msg("bpm.pdf.titulo.limpieza", locale), sub, locale);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bpm-limpieza-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF).body(bytes);
    }
}
