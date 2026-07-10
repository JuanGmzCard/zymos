package com.alera.controller;

import com.alera.config.PasswordPolicy;
import com.alera.model.enums.RolUsuario;
import com.alera.service.RolTenantService;
import com.alera.service.UsuarioService;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {


    private final UsuarioService service;
    private final RolTenantService rolTenantService;

    public UsuarioController(UsuarioService service, RolTenantService rolTenantService) {
        this.service = service;
        this.rolTenantService = rolTenantService;
    }

    @GetMapping
    public String lista(Model model) {
        model.addAttribute("usuarios", service.listarTodos());
        model.addAttribute("roles", RolUsuario.values());
        var rolesCustom = rolTenantService.listarActivos();
        model.addAttribute("rolesCustom", rolesCustom);
        model.addAttribute("rolesCustomNombres",
            rolesCustom.stream().collect(Collectors.toMap(r -> r.getId(), r -> r.getNombre())));
        return "usuarios";
    }

    @GetMapping(value = "/suggest", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Map<String, Object>> suggest(@RequestParam(defaultValue = "") String q) {
        return service.suggest(q);
    }

    @PostMapping("/guardar")
    public String guardar(@RequestParam String username,
                          @RequestParam String password,
                          @RequestParam String confirmPassword,
                          @RequestParam(defaultValue = "ADMIN") RolUsuario rol,
                          RedirectAttributes ra) {
        String errorPassword = PasswordPolicy.validar(password);
        if (errorPassword != null) {
            ra.addFlashAttribute("mensaje", errorPassword);
            ra.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/usuarios";
        }
        if (!password.equals(confirmPassword)) {
            ra.addFlashAttribute("mensaje", "Las contraseñas no coinciden");
            ra.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/usuarios";
        }
        if (service.existeUsername(username)) {
            ra.addFlashAttribute("mensaje", "El usuario ya existe");
            ra.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/usuarios";
        }
        service.guardar(username, password, rol);
        ra.addFlashAttribute("mensaje", "Usuario '" + username + "' creado con rol " + rol.getDisplayName());
        ra.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        if (service.esElMismoUsuario(id, auth.getName())) {
            ra.addFlashAttribute("mensaje", "No puedes desactivarte a ti mismo");
            ra.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/usuarios";
        }
        service.toggleActivo(id);
        ra.addFlashAttribute("mensaje", "Estado actualizado");
        ra.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        if (service.esElMismoUsuario(id, auth.getName())) {
            ra.addFlashAttribute("mensaje", "No puedes eliminarte a ti mismo");
            ra.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/usuarios";
        }
        service.eliminar(id);
        ra.addFlashAttribute("mensaje", "Usuario eliminado");
        ra.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}/password")
    public String cambiarPassword(@PathVariable Long id,
                                  @RequestParam String newPassword,
                                  @RequestParam String confirmPassword,
                                  RedirectAttributes ra) {
        String errorNueva = PasswordPolicy.validar(newPassword);
        if (errorNueva != null) {
            ra.addFlashAttribute("mensaje", errorNueva);
            ra.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/usuarios";
        }
        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("mensaje", "Las contraseñas no coinciden");
            ra.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/usuarios";
        }
        service.cambiarPassword(id, newPassword);
        ra.addFlashAttribute("mensaje", "Contraseña actualizada");
        ra.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}/rol")
    public String cambiarRol(@PathVariable Long id,
                             @RequestParam RolUsuario rol,
                             Authentication auth,
                             RedirectAttributes ra) {
        if (service.esElMismoUsuario(id, auth.getName())) {
            ra.addFlashAttribute("mensaje", "No puedes cambiar tu propio rol");
            ra.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/usuarios";
        }
        service.cambiarRol(id, rol);
        ra.addFlashAttribute("mensaje", "Rol actualizado a " + rol.getDisplayName());
        ra.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}/rol-custom")
    public String asignarRolCustom(@PathVariable Long id,
                                   @RequestParam(required = false) Long rolCustomId,
                                   Authentication auth,
                                   RedirectAttributes ra) {
        if (service.esElMismoUsuario(id, auth.getName())) {
            ra.addFlashAttribute("mensaje", "No puedes cambiar tu propio rol");
            ra.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/usuarios";
        }
        service.asignarRolCustom(id, rolCustomId);
        ra.addFlashAttribute("mensaje", rolCustomId != null ? "Rol personalizado asignado" : "Rol personalizado removido");
        ra.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/usuarios";
    }
}