package com.alera.controller;

import com.alera.model.Barril;
import com.alera.model.enums.EstadoBarril;
import com.alera.service.BarrilService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/barriles")
public class BarrilController {

    private static final List<String> TIPOS_BARRIL = List.of(
            "Keg 20L", "Keg 30L", "Keg 50L",
            "Barril 30L", "Barril 60L",
            "Otro"
    );

    private final BarrilService service;

    public BarrilController(BarrilService service) {
        this.service = service;
    }

    // ── Lista ──────────────────────────────────────────────────────────────

    @GetMapping
    public String lista(@RequestParam(defaultValue = "") String codigo,
                        @RequestParam(required = false) EstadoBarril estado,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        Page<Barril> pagina = service.listarPaginado(codigo, estado, page);
        model.addAttribute("barriles",      pagina.getContent());
        model.addAttribute("paginaActual",  page);
        model.addAttribute("totalPaginas",  pagina.getTotalPages());
        model.addAttribute("codigo",        codigo);
        model.addAttribute("estadoFiltro",  estado);
        model.addAttribute("estados",       EstadoBarril.values());
        model.addAttribute("statsTotal",      service.countTotal());
        model.addAttribute("statsDisponibles",service.countByEstado(EstadoBarril.DISPONIBLE));
        model.addAttribute("statsLlenos",     service.countByEstado(EstadoBarril.LLENO));
        model.addAttribute("statsDespachados",service.countByEstado(EstadoBarril.DESPACHADO));
        return "barriles/lista";
    }

    // ── Formulario nuevo ───────────────────────────────────────────────────

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("barril",      new Barril());
        model.addAttribute("tiposBarril", TIPOS_BARRIL);
        model.addAttribute("estados",     EstadoBarril.values());
        return "barriles/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("barril") Barril barril,
                          BindingResult result,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDespacho,
                          Model model,
                          RedirectAttributes ra) {
        if (barril.getCodigo() == null || barril.getCodigo().isBlank()) {
            result.rejectValue("codigo", "required", "El código es obligatorio");
        }
        if (result.hasErrors()) {
            model.addAttribute("tiposBarril", TIPOS_BARRIL);
            model.addAttribute("estados", EstadoBarril.values());
            return "barriles/formulario";
        }
        barril.setFechaDespacho(fechaDespacho);
        try {
            service.guardar(barril);
            ra.addFlashAttribute("mensaje",     "Barril registrado correctamente.");
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("mensaje",     e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/barriles";
    }

    // ── Formulario editar ──────────────────────────────────────────────────

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            model.addAttribute("barril",      service.buscarPorId(id));
            model.addAttribute("tiposBarril", TIPOS_BARRIL);
            model.addAttribute("estados",     EstadoBarril.values());
            return "barriles/formulario";
        } catch (RuntimeException e) {
            ra.addFlashAttribute("mensaje",     "Barril no encontrado.");
            ra.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/barriles";
        }
    }

    @PostMapping("/actualizar/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("barril") Barril barril,
                             BindingResult result,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDespacho,
                             Model model,
                             RedirectAttributes ra) {
        if (barril.getCodigo() == null || barril.getCodigo().isBlank()) {
            result.rejectValue("codigo", "required", "El código es obligatorio");
        }
        if (result.hasErrors()) {
            model.addAttribute("tiposBarril", TIPOS_BARRIL);
            model.addAttribute("estados", EstadoBarril.values());
            return "barriles/formulario";
        }
        barril.setFechaDespacho(fechaDespacho);
        try {
            service.actualizar(id, barril);
            ra.addFlashAttribute("mensaje",     "Barril actualizado correctamente.");
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("mensaje",     e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/barriles";
    }

    // ── Detalle ────────────────────────────────────────────────────────────

    @GetMapping("/ver/{id}")
    public String ver(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            Barril barril = service.buscarPorId(id);
            model.addAttribute("barril",      barril);
            model.addAttribute("movimientos", service.listarMovimientos(id));
            model.addAttribute("estados",     EstadoBarril.values());
            return "barriles/detalle";
        } catch (RuntimeException e) {
            ra.addFlashAttribute("mensaje",     "Barril no encontrado.");
            ra.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/barriles";
        }
    }

    // ── Cambio de estado ───────────────────────────────────────────────────

    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable Long id,
                                @RequestParam EstadoBarril estado,
                                @RequestParam(required = false) String notas,
                                RedirectAttributes ra) {
        try {
            service.cambiarEstado(id, estado, notas);
            ra.addFlashAttribute("mensaje",     "Estado actualizado.");
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("mensaje",     e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/barriles/ver/" + id;
    }

    // ── Eliminar ───────────────────────────────────────────────────────────

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            service.eliminar(id);
            ra.addFlashAttribute("mensaje",     "Barril eliminado.");
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("mensaje",     e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/barriles";
    }
}
