package com.alera.controller;

import com.alera.service.LogAccesoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final LogAccesoService logService;

    public AdminController(LogAccesoService logService) {
        this.logService = logService;
    }

    @GetMapping("/logs")
    public String logs(@RequestParam(defaultValue = "") String tipo,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        var pagina = logService.listarPaginado(tipo, page);
        model.addAttribute("logs",           pagina.getContent());
        model.addAttribute("paginaActual",   page);
        model.addAttribute("totalPaginas",   pagina.getTotalPages());
        model.addAttribute("totalRegistros", pagina.getTotalElements());
        model.addAttribute("tipoFiltro",     tipo);
        model.addAttribute("fallidosHora",   logService.fallidosUltimaHora());
        model.addAttribute("baseUrl",        "/admin/logs");
        model.addAttribute("extraParams",    tipo.isBlank() ? "" : "&tipo=" + tipo);
        return "admin/logs";
    }
}
