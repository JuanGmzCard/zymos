package com.alera.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PlanController {

    @GetMapping("/plan-vencido")
    public String planVencido() {
        return "plan/vencido";
    }
}
