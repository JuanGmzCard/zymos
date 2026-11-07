package com.alera.controller;

import com.alera.repository.LoteCervezaRepository;
import com.alera.service.RecetaService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/bienvenida")
public class BienvenidaController {

    static final String SESSION_SKIPPED = "onboardingSkipped";

    private final RecetaService recetaService;
    private final LoteCervezaRepository loteRepo;

    public BienvenidaController(RecetaService recetaService,
                                 LoteCervezaRepository loteRepo) {
        this.recetaService = recetaService;
        this.loteRepo      = loteRepo;
    }

    @GetMapping
    public String index(Model model) {
        boolean tieneRecetas = !recetaService.listarActivas().isEmpty();
        boolean tieneLotes   = loteRepo.count() > 0;
        if (tieneRecetas && tieneLotes) return "redirect:/";
        model.addAttribute("tieneRecetas", tieneRecetas);
        model.addAttribute("tieneLotes",   tieneLotes);
        return "bienvenida";
    }

    @PostMapping("/saltar")
    public String saltar(HttpServletRequest request) {
        request.getSession(true).setAttribute(SESSION_SKIPPED, true);
        return "redirect:/";
    }
}
