package com.alera.controller;

import com.alera.model.TipoCerveza;
import com.alera.service.TipoCervezaService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/tipos-cerveza")
public class TipoCervezaController {

    private final TipoCervezaService service;

    public TipoCervezaController(TipoCervezaService service) {
        this.service = service;
    }

    @GetMapping
    public String lista(Model model) {
        model.addAttribute("tipos", service.listarTodos());
        model.addAttribute("tipoCerveza", new TipoCerveza());
        return "tipos-cerveza/lista";
    }

    @PostMapping("/guardar-rapido")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> guardarRapido(@RequestParam String nombre) {
        Map<String, Object> resp = new LinkedHashMap<>();
        try {
            String nombreTrim = nombre.trim();
            if (service.existePorNombre(nombreTrim)) {
                resp.put("success", false);
                resp.put("error", "Ya existe un tipo con ese nombre");
                return ResponseEntity.badRequest().body(resp);
            }
            TipoCerveza tipo = new TipoCerveza();
            tipo.setNombre(nombreTrim);
            TipoCerveza saved = service.guardar(tipo);
            resp.put("success", true);
            resp.put("id", saved.getId());
            resp.put("nombre", saved.getNombre());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            resp.put("success", false);
            resp.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        }
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute TipoCerveza tipo, RedirectAttributes ra) {
        try {
            service.guardar(tipo);
            ra.addFlashAttribute("mensaje", "Tipo guardado");
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("mensaje", "Error: " + e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/tipos-cerveza";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        service.toggleActivo(id);
        ra.addFlashAttribute("mensaje", "Estado actualizado");
        ra.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/tipos-cerveza";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        service.eliminar(id);
        ra.addFlashAttribute("mensaje", "Tipo eliminado");
        ra.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/tipos-cerveza";
    }
}
