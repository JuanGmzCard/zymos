package com.alera.controller;

import com.alera.model.CategoriaEquipo;
import com.alera.model.CategoriaInsumo;
import com.alera.service.CategoriaEquipoService;
import com.alera.service.CategoriaInsumoService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/admin/categorias")
public class CategoriaController {

    private final CategoriaInsumoService insumoService;
    private final CategoriaEquipoService equipoService;

    public CategoriaController(CategoriaInsumoService insumoService,
                                CategoriaEquipoService equipoService) {
        this.insumoService = insumoService;
        this.equipoService = equipoService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("categoriasInsumo", insumoService.listarTodos());
        model.addAttribute("categoriasEquipo", equipoService.listarTodos());
        return "admin/categorias";
    }

    // ── Insumo ──────────────────────────────────────────────────────────────

    @PostMapping("/insumo/guardar")
    public String guardarInsumo(@RequestParam String nombre, RedirectAttributes ra) {
        try {
            insumoService.guardar(nombre);
            ra.addFlashAttribute("mensaje", "Categoría de insumo creada");
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("mensaje", e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/admin/categorias";
    }

    @PostMapping("/insumo/{id}/toggle")
    public String toggleInsumo(@PathVariable Long id, RedirectAttributes ra) {
        insumoService.toggleActivo(id);
        ra.addFlashAttribute("mensaje", "Categoría actualizada");
        ra.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/admin/categorias";
    }

    @PostMapping("/insumo/{id}/eliminar")
    public String eliminarInsumo(@PathVariable Long id, RedirectAttributes ra) {
        try {
            insumoService.eliminar(id);
            ra.addFlashAttribute("mensaje", "Categoría eliminada");
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("mensaje", "No se puede eliminar: " + e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/admin/categorias";
    }

    @PostMapping(value = "/insumo/guardar-rapido", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> guardarInsumoRapido(@RequestParam String nombre) {
        try {
            CategoriaInsumo cat = insumoService.guardar(nombre);
            return Map.of("success", true, "id", cat.getId(), "nombre", cat.getNombre());
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    // ── Equipo ───────────────────────────────────────────────────────────────

    @PostMapping("/equipo/guardar")
    public String guardarEquipo(@RequestParam String nombre, RedirectAttributes ra) {
        try {
            equipoService.guardar(nombre);
            ra.addFlashAttribute("mensaje", "Categoría de equipo creada");
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("mensaje", e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/admin/categorias";
    }

    @PostMapping("/equipo/{id}/toggle")
    public String toggleEquipo(@PathVariable Long id, RedirectAttributes ra) {
        equipoService.toggleActivo(id);
        ra.addFlashAttribute("mensaje", "Categoría actualizada");
        ra.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/admin/categorias";
    }

    @PostMapping("/equipo/{id}/eliminar")
    public String eliminarEquipo(@PathVariable Long id, RedirectAttributes ra) {
        try {
            equipoService.eliminar(id);
            ra.addFlashAttribute("mensaje", "Categoría eliminada");
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("mensaje", "No se puede eliminar: " + e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/admin/categorias";
    }

    @PostMapping(value = "/equipo/guardar-rapido", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> guardarEquipoRapido(@RequestParam String nombre) {
        try {
            CategoriaEquipo cat = equipoService.guardar(nombre);
            return Map.of("success", true, "id", cat.getId(), "nombre", cat.getNombre());
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }
}
