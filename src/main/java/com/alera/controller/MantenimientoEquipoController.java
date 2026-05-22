package com.alera.controller;

import com.alera.dto.MantenimientoDto;
import com.alera.model.enums.TipoMantenimiento;
import com.alera.service.EquipoService;
import com.alera.service.MantenimientoEquipoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/equipos/{equipoId}/mantenimientos")
public class MantenimientoEquipoController {

    private final MantenimientoEquipoService service;
    private final EquipoService equipoService;

    public MantenimientoEquipoController(MantenimientoEquipoService service, EquipoService equipoService) {
        this.service = service;
        this.equipoService = equipoService;
    }

    @GetMapping
    public String lista(@PathVariable Long equipoId, Model model) {
        var equipo = equipoService.buscarPorId(equipoId).orElseThrow();
        model.addAttribute("equipo", equipo);
        model.addAttribute("mantenimientos", service.listarPorEquipo(equipoId));
        model.addAttribute("mantenimientoForm", new MantenimientoDto());
        model.addAttribute("tiposMantenimiento", TipoMantenimiento.values());
        return "equipos/mantenimientos";
    }

    @PostMapping("/registrar")
    public String registrar(@PathVariable Long equipoId,
                            @ModelAttribute MantenimientoDto dto,
                            RedirectAttributes ra) {
        try {
            service.registrar(equipoId, dto);
            ra.addFlashAttribute("mensaje", "Mantenimiento registrado");
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("mensaje", "Error: " + e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/equipos/" + equipoId + "/mantenimientos";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long equipoId,
                           @PathVariable Long id,
                           RedirectAttributes ra) {
        service.eliminar(id);
        ra.addFlashAttribute("mensaje", "Mantenimiento eliminado");
        ra.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/equipos/" + equipoId + "/mantenimientos";
    }
}
