package com.alera.controller;

import com.alera.model.Tenant;
import com.alera.service.TenantService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/tenants")
public class TenantAdminController {

    private final TenantService tenantService;

    public TenantAdminController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping
    public String lista(Model model) {
        model.addAttribute("tenants", tenantService.listarTodos());
        return "admin/tenants";
    }

    @GetMapping("/nuevo")
    public String formularioNuevo(Model model) {
        model.addAttribute("tenant", new Tenant());
        model.addAttribute("esNuevo", true);
        return "admin/tenant-formulario";
    }

    @GetMapping("/editar/{subdomain}")
    public String formularioEditar(@PathVariable String subdomain, Model model) {
        Tenant tenant = tenantService.buscarPorSubdomain(subdomain)
                .orElseThrow(() -> new RuntimeException("Tenant no encontrado: " + subdomain));
        model.addAttribute("tenant", tenant);
        model.addAttribute("esNuevo", false);
        return "admin/tenant-formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Tenant tenant,
                          @RequestParam(defaultValue = "false") boolean esNuevo,
                          RedirectAttributes ra) {
        tenantService.guardar(tenant);
        String accion = esNuevo ? "creado" : "actualizado";
        ra.addFlashAttribute("mensaje", "Tenant '" + tenant.getSubdomain() + "' " + accion + " correctamente");
        ra.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/admin/tenants";
    }

    @PostMapping("/{subdomain}/toggle")
    public String toggleActivo(@PathVariable String subdomain, RedirectAttributes ra) {
        tenantService.toggleActivo(subdomain);
        ra.addFlashAttribute("mensaje", "Estado del tenant actualizado");
        ra.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/admin/tenants";
    }
}