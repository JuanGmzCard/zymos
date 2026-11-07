package com.alera.controller;

import com.alera.model.enums.PlanNivel;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LandingController {

    @GetMapping("/landing")
    public String landing(Model model) {
        model.addAttribute("planes", PlanNivel.values());
        return "landing";
    }
}
