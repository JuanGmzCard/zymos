package com.alera.controller;

import com.alera.model.enums.PlanNivel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LandingController {

    @Value("${app.base-domain:}") private String baseDomain;

    @GetMapping("/landing")
    public String landing(Model model) {
        model.addAttribute("planes", PlanNivel.values());
        // Si hay dominio base configurado, el demo vive en demo.<dominio>
        String demoUrl = baseDomain != null && !baseDomain.isBlank()
                ? "https://demo." + baseDomain + "/demo"
                : "/demo";
        model.addAttribute("demoUrl", demoUrl);
        return "landing";
    }
}
