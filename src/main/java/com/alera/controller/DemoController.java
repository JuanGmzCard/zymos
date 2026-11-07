package com.alera.controller;

import com.alera.config.DemoDataSeeder;
import com.alera.config.TenantContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class DemoController {

    private final DemoDataSeeder demoDataSeeder;

    public DemoController(DemoDataSeeder demoDataSeeder) {
        this.demoDataSeeder = demoDataSeeder;
    }

    /** Página pública de acceso directo al tenant demo — auto-submit del login. */
    @GetMapping("/demo")
    public String demo() {
        return "demo";
    }

    @PostMapping("/demo/reset")
    public String reset(RedirectAttributes ra) {
        if (!"demo".equals(TenantContext.getCurrentTenant())) {
            return "redirect:/";
        }
        demoDataSeeder.resetear();
        ra.addFlashAttribute("mensaje", "Datos de demostración restablecidos correctamente");
        ra.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/";
    }
}
