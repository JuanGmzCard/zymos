package com.alera.controller;

import com.alera.service.UsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/perfil")
public class PerfilController {

    private static final int MIN_PASSWORD_LENGTH = 6;

    private final UsuarioService usuarioService;

    public PerfilController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/password")
    public String mostrarFormulario() {
        return "perfil/password";
    }

    @PostMapping("/password")
    public String cambiarPassword(@RequestParam String nuevaPassword,
                                   @RequestParam String confirmarPassword,
                                   Authentication auth,
                                   RedirectAttributes ra) {
        if (nuevaPassword.length() < MIN_PASSWORD_LENGTH) {
            ra.addFlashAttribute("mensaje", "La contraseña debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres.");
            ra.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/perfil/password";
        }
        if (!nuevaPassword.equals(confirmarPassword)) {
            ra.addFlashAttribute("mensaje", "Las contraseñas no coinciden.");
            ra.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/perfil/password";
        }
        usuarioService.buscarPorUsername(auth.getName()).ifPresent(u ->
            usuarioService.cambiarPassword(u.getId(), nuevaPassword)
        );
        ra.addFlashAttribute("mensaje", "Contraseña actualizada correctamente.");
        ra.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/dashboard";
    }
}
