package com.alera.controller;

import com.alera.model.Equipo;
import com.alera.model.enums.EstadoEquipo;
import com.alera.model.enums.TipoEquipo;
import com.alera.service.EquipoService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/equipos")
public class EquipoController {

    private final EquipoService service;

    public EquipoController(EquipoService service) {
        this.service = service;
    }

    @GetMapping
    public String lista(@RequestParam(required = false) EstadoEquipo estado,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        var pagina = service.listarPaginado(estado, page);
        model.addAttribute("equipos",      pagina.getContent());
        model.addAttribute("paginaActual", page);
        model.addAttribute("totalPaginas", pagina.getTotalPages());
        model.addAttribute("totalEquipos", pagina.getTotalElements());
        model.addAttribute("estadoFiltro", estado);
        model.addAttribute("baseUrl",      "/equipos");
        model.addAttribute("extraParams",  estado != null ? "&estado=" + estado.name() : "");
        model.addAttribute("tiposEquipo",  TipoEquipo.values());
        model.addAttribute("estadosEquipo", EstadoEquipo.values());
        return "equipos/lista";
    }

    @GetMapping(value = "/suggest", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Map<String, Object>> suggest(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) EstadoEquipo estado) {
        return service.suggest(q, estado);
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("equipo", new Equipo());
        model.addAttribute("tiposEquipo", TipoEquipo.values());
        model.addAttribute("estadosEquipo", EstadoEquipo.values());
        return "equipos/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Equipo equipo, RedirectAttributes ra) {
        try {
            service.guardar(equipo);
            ra.addFlashAttribute("mensaje", "Equipo guardado correctamente");
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("mensaje", "Error: " + e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/equipos";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        var equipo = service.buscarPorId(id).orElseThrow();
        model.addAttribute("equipo", equipo);
        model.addAttribute("tiposEquipo", TipoEquipo.values());
        model.addAttribute("estadosEquipo", EstadoEquipo.values());
        return "equipos/formulario";
    }

    @PostMapping("/actualizar/{id}")
    public String actualizar(@PathVariable Long id, @ModelAttribute Equipo equipo, RedirectAttributes ra) {
        equipo.setId(id);
        try {
            service.guardar(equipo);
            ra.addFlashAttribute("mensaje", "Equipo actualizado");
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("mensaje", "Error: " + e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/equipos";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            service.eliminar(id);  // lanza EquipoEnUsoException si tiene lotes activos
            ra.addFlashAttribute("mensaje", "Equipo eliminado");
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("mensaje", "Error: " + e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/equipos";
    }
}
