package com.alera.controller;

import com.alera.model.RolTenant;
import com.alera.model.enums.ModuloApp;
import com.alera.service.RolTenantService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
@RequestMapping("/admin/roles")
public class RolTenantController {

    private final RolTenantService service;

    public RolTenantController(RolTenantService service) {
        this.service = service;
    }

    @GetMapping
    public String lista(Model model) {
        List<RolTenant> roles = service.listarTodos();
        model.addAttribute("roles", roles);
        model.addAttribute("totalRoles", roles.size());
        model.addAttribute("rolesActivos", roles.stream().filter(RolTenant::isActivo).count());
        return "admin/roles/lista";
    }

    @GetMapping("/nuevo")
    public String formularioNuevo(Model model) {
        model.addAttribute("modulos", ModuloApp.values());
        model.addAttribute("modoEdicion", false);
        return "admin/roles/formulario";
    }

    @GetMapping("/editar/{id}")
    public String formularioEditar(@PathVariable Long id, Model model, RedirectAttributes ra) {
        return service.buscarPorId(id).map(rol -> {
            model.addAttribute("rol", rol);
            model.addAttribute("modulos", ModuloApp.values());
            model.addAttribute("modoEdicion", true);
            return "admin/roles/formulario";
        }).orElseGet(() -> {
            ra.addFlashAttribute("mensaje", "Rol no encontrado");
            ra.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/admin/roles";
        });
    }

    @PostMapping("/guardar")
    public String guardar(@RequestParam String nombre,
                          @RequestParam(required = false) String descripcion,
                          @RequestParam(required = false) List<String> modulosVer,
                          @RequestParam(required = false) List<String> modulosCrear,
                          @RequestParam(required = false) List<String> modulosEditar,
                          @RequestParam(required = false) List<String> modulosEliminar,
                          RedirectAttributes ra) {
        if (nombre == null || nombre.isBlank()) {
            ra.addFlashAttribute("mensaje", "El nombre del rol es obligatorio");
            ra.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/admin/roles/nuevo";
        }
        service.guardar(nombre, descripcion, modulosVer, modulosCrear, modulosEditar, modulosEliminar);
        ra.addFlashAttribute("mensaje", "Rol '" + nombre.trim() + "' creado");
        ra.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/admin/roles";
    }

    @PostMapping("/actualizar/{id}")
    public String actualizar(@PathVariable Long id,
                             @RequestParam String nombre,
                             @RequestParam(required = false) String descripcion,
                             @RequestParam(defaultValue = "false") boolean activo,
                             @RequestParam(required = false) List<String> modulosVer,
                             @RequestParam(required = false) List<String> modulosCrear,
                             @RequestParam(required = false) List<String> modulosEditar,
                             @RequestParam(required = false) List<String> modulosEliminar,
                             RedirectAttributes ra) {
        if (nombre == null || nombre.isBlank()) {
            ra.addFlashAttribute("mensaje", "El nombre del rol es obligatorio");
            ra.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/admin/roles/editar/" + id;
        }
        service.actualizar(id, nombre, descripcion, activo, modulosVer, modulosCrear, modulosEditar, modulosEliminar);
        ra.addFlashAttribute("mensaje", "Rol actualizado");
        ra.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/admin/roles";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        service.toggleActivo(id);
        ra.addFlashAttribute("mensaje", "Estado del rol actualizado");
        ra.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/admin/roles";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            service.eliminar(id);
            ra.addFlashAttribute("mensaje", "Rol eliminado");
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("mensaje", e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/admin/roles";
    }
}
