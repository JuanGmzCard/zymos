package com.alera.controller;

import com.alera.model.Proveedor;
import com.alera.service.ProveedorService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/proveedores")
public class ProveedorController {

    private final ProveedorService service;

    public ProveedorController(ProveedorService service) {
        this.service = service;
    }

    @GetMapping
    public String lista(Model model) {
        var proveedores = service.listarTodos();
        model.addAttribute("proveedores", proveedores);
        return "proveedores/lista";
    }

    @GetMapping(value = "/suggest", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Map<String, Object>> suggest(@RequestParam(defaultValue = "") String q) {
        return service.suggest(q);
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("proveedor", new Proveedor());
        return "proveedores/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Proveedor proveedor, RedirectAttributes ra) {
        try {
            service.guardar(proveedor);
            ra.addFlashAttribute("mensaje", "Proveedor guardado correctamente");
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("mensaje", "Error: " + e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/proveedores";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("proveedor", service.buscarPorId(id).orElseThrow());
        model.addAttribute("proveedorId", id);
        model.addAttribute("totalFacturas", service.totalFacturas(id));
        model.addAttribute("countFacturas", service.contarFacturas(id));
        return "proveedores/formulario";
    }

    @PostMapping("/actualizar/{id}")
    public String actualizar(@PathVariable Long id, @ModelAttribute Proveedor proveedor, RedirectAttributes ra) {
        proveedor.setId(id);
        try {
            service.guardar(proveedor);
            ra.addFlashAttribute("mensaje", "Proveedor actualizado");
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("mensaje", "Error: " + e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/proveedores";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            service.eliminar(id);
            ra.addFlashAttribute("mensaje", "Proveedor eliminado");
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("mensaje", "No se puede eliminar: tiene facturas asociadas");
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/proveedores";
    }
}
