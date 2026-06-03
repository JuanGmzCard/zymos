package com.alera.controller;

import com.alera.config.PasswordPolicy;
import com.alera.model.Tenant;
import com.alera.model.enums.RolUsuario;
import com.alera.repository.UsuarioRepository;
import com.alera.service.EmailService;
import com.alera.service.TenantService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/tenants")
public class TenantAdminController {


    private final TenantService tenantService;
    private final UsuarioRepository usuarioRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    public TenantAdminController(TenantService tenantService,
                                  UsuarioRepository usuarioRepo, PasswordEncoder passwordEncoder,
                                  EmailService emailService, ObjectMapper objectMapper) {
        this.tenantService   = tenantService;
        this.usuarioRepo     = usuarioRepo;
        this.passwordEncoder = passwordEncoder;
        this.emailService    = emailService;
        this.objectMapper    = objectMapper;
    }

    @GetMapping
    public String lista(Model model) {
        model.addAttribute("tenants", tenantService.listarTodos());
        return "admin/tenants";
    }

    @GetMapping("/nuevo")
    public String formularioNuevo(Model model) {
        model.addAttribute("tenant", new Tenant());
        model.addAttribute("esNuevo", true);
        return "admin/tenant-formulario";
    }

    @GetMapping("/editar/{subdomain}")
    public String formularioEditar(@PathVariable String subdomain, Model model, RedirectAttributes ra) {
        Tenant tenant = tenantService.buscarPorSubdomain(subdomain).orElse(null);
        if (tenant == null) {
            ra.addFlashAttribute("mensaje", "Tenant no encontrado: " + subdomain);
            ra.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/admin/tenants";
        }
        model.addAttribute("tenant", tenant);
        model.addAttribute("esNuevo", false);
        model.addAttribute("otrosTenants", tenantService.listarTodos().stream()
                .filter(t -> !t.getSubdomain().equals(subdomain)).toList());
        return "admin/tenant-formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Tenant tenant,
                          @RequestParam(defaultValue = "false") boolean esNuevo,
                          RedirectAttributes ra) {
        tenantService.guardar(tenant);
        String accion = esNuevo ? "creado" : "actualizado";
        ra.addFlashAttribute("mensaje", "Tenant '" + tenant.getSubdomain() + "' " + accion + " correctamente");
        ra.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/admin/tenants";
    }

    @PostMapping("/{subdomain}/toggle")
    public String toggleActivo(@PathVariable String subdomain, RedirectAttributes ra) {
        tenantService.toggleActivo(subdomain);
        ra.addFlashAttribute("mensaje", "Estado del tenant actualizado");
        ra.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/admin/tenants";
    }

    @GetMapping("/{subdomain}/historial")
    public String historial(@PathVariable String subdomain,
                             @RequestParam(defaultValue = "0") int page,
                             Model model, RedirectAttributes ra) {
        Tenant tenant = tenantService.buscarPorSubdomain(subdomain).orElse(null);
        if (tenant == null) {
            ra.addFlashAttribute("mensaje", "Tenant no encontrado: " + subdomain);
            ra.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/admin/tenants";
        }
        var pagina = tenantService.listarHistorialPaginado(subdomain, page);
        model.addAttribute("tenant", tenant);
        model.addAttribute("historial", pagina.getContent());
        model.addAttribute("paginaActual", page);
        model.addAttribute("totalPaginas", pagina.getTotalPages());
        model.addAttribute("baseUrl", "/admin/tenants/" + subdomain + "/historial");
        model.addAttribute("extraParams", "");
        return "admin/tenant-historial";
    }

    @PostMapping("/cache/evict")
    public String evictCache(RedirectAttributes ra) {
        tenantService.evictAllCache();
        ra.addFlashAttribute("mensaje", "Cache de tenants limpiado correctamente");
        ra.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/admin/tenants";
    }

    // ── Importar / Exportar configuración ────────────────────────────

    @GetMapping("/{subdomain}/config")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getConfig(@PathVariable String subdomain) {
        Tenant t = tenantService.buscarPorSubdomain(subdomain).orElse(null);
        if (t == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(buildConfigMap(t));
    }

    @GetMapping("/{subdomain}/export")
    public ResponseEntity<byte[]> exportConfig(@PathVariable String subdomain) throws Exception {
        Tenant t = tenantService.buscarPorSubdomain(subdomain).orElse(null);
        if (t == null) return ResponseEntity.notFound().build();
        byte[] json = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsBytes(buildConfigMap(t));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(subdomain + "-branding.json")
                                .build().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .contentLength(json.length)
                .body(json);
    }

    @PostMapping("/{subdomain}/import")
    @ResponseBody
    public Map<String, Object> importConfig(@PathVariable String subdomain,
                                             @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) return Map.of("ok", false, "message", "El archivo está vacío.");
            @SuppressWarnings("unchecked")
            Map<String, String> config = objectMapper.readValue(file.getInputStream(), Map.class);
            Tenant t = tenantService.buscarPorSubdomain(subdomain).orElse(null);
            if (t == null) return Map.of("ok", false, "message", "Tenant no encontrado: " + subdomain);
            applyConfig(t, config);
            tenantService.guardar(t);
            tenantService.registrarAccion(subdomain, "CONFIG_IMPORTADA", file.getOriginalFilename());
            return Map.of("ok", true, "message", "Configuración importada correctamente.");
        } catch (Exception e) {
            return Map.of("ok", false, "message", "Error al importar: " + e.getMessage());
        }
    }

    private Map<String, Object> buildConfigMap(Tenant t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name",            t.getName());
        m.put("tagline",         t.getTagline());
        m.put("logoUrl",         t.getLogoUrl());
        m.put("colorNavbar",     t.getColorNavbar());
        m.put("colorPrimary",    t.getColorPrimary());
        m.put("colorAccent",     t.getColorAccent());
        m.put("colorAccentHover",t.getColorAccentHover());
        m.put("colorCream",      t.getColorCream());
        m.put("colorBodyBg",     t.getColorBodyBg());
        m.put("fontHeadings",    t.getFontHeadings());
        m.put("fontBody",        t.getFontBody());
        return m;
    }

    private void applyConfig(Tenant t, Map<String, String> c) {
        if (c.containsKey("name"))             t.setName(c.get("name"));
        if (c.containsKey("tagline"))          t.setTagline(c.get("tagline"));
        if (c.containsKey("logoUrl"))          t.setLogoUrl(c.get("logoUrl"));
        applyColor(c, "colorNavbar",      t::setColorNavbar);
        applyColor(c, "colorPrimary",     t::setColorPrimary);
        applyColor(c, "colorAccent",      t::setColorAccent);
        applyColor(c, "colorAccentHover", t::setColorAccentHover);
        applyColor(c, "colorCream",       t::setColorCream);
        applyColor(c, "colorBodyBg",      t::setColorBodyBg);
        if (c.containsKey("fontHeadings"))     t.setFontHeadings(c.get("fontHeadings"));
        if (c.containsKey("fontBody"))         t.setFontBody(c.get("fontBody"));
    }

    private void applyColor(Map<String, String> c, String key,
                             java.util.function.Consumer<String> setter) {
        String val = c.get(key);
        if (val != null && val.matches("^#[0-9a-fA-F]{6}$")) {
            setter.accept(val);
        }
    }

    // ── Email de prueba ──────────────────────────────────────────────

    @PostMapping("/{subdomain}/test-email")
    @ResponseBody
    public Map<String, Object> testEmail(@PathVariable String subdomain,
                                          @RequestParam String email) {
        Tenant tenant = tenantService.buscarPorSubdomain(subdomain).orElse(null);
        if (tenant == null) return Map.of("ok", false, "mensaje", "Tenant no encontrado: " + subdomain);
        String error = emailService.enviarEmailPrueba(email, tenant.getName());
        if (error == null) {
            return Map.of("ok", true, "mensaje", "Email enviado correctamente a " + email);
        }
        return Map.of("ok", false, "mensaje", error);
    }

    // ── Gestión de usuarios por tenant ───────────────────────────────

    @GetMapping("/{subdomain}/usuarios")
    public String usuarios(@PathVariable String subdomain, Model model, RedirectAttributes ra) {
        Tenant tenant = tenantService.buscarPorSubdomain(subdomain).orElse(null);
        if (tenant == null) {
            ra.addFlashAttribute("mensaje", "Tenant no encontrado: " + subdomain);
            ra.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/admin/tenants";
        }
        model.addAttribute("usuarios", usuarioRepo.findAllByTenantId(subdomain));
        model.addAttribute("tenant", tenant);
        model.addAttribute("roles", RolUsuario.values());
        return "admin/tenant-usuarios";
    }

    @Transactional
    @PostMapping("/{subdomain}/usuarios/guardar")
    public String guardarUsuario(@PathVariable String subdomain,
                                  @RequestParam String username,
                                  @RequestParam String password,
                                  @RequestParam String confirmarPassword,
                                  @RequestParam RolUsuario rol,
                                  RedirectAttributes ra) {
        String redirect = "redirect:/admin/tenants/" + subdomain + "/usuarios";
        String errorPassword = PasswordPolicy.validar(password);
        if (errorPassword != null) {
            ra.addFlashAttribute("mensaje", errorPassword);
            ra.addFlashAttribute("tipoMensaje", "danger");
            return redirect;
        }
        if (!password.equals(confirmarPassword)) {
            ra.addFlashAttribute("mensaje", "Las contraseñas no coinciden");
            ra.addFlashAttribute("tipoMensaje", "danger");
            return redirect;
        }
        if (usuarioRepo.countByUsernameAndTenantId(username, subdomain) > 0) {
            ra.addFlashAttribute("mensaje", "El usuario '" + username + "' ya existe en este tenant");
            ra.addFlashAttribute("tipoMensaje", "danger");
            return redirect;
        }
        boolean esPrimerUsuario = usuarioRepo.findAllByTenantId(subdomain).isEmpty();
        usuarioRepo.insertarConTenant(username, passwordEncoder.encode(password), rol.name(), subdomain);
        tenantService.registrarAccion(subdomain, "USUARIO_CREADO", username + " (" + rol.getDisplayName() + ")");
        if (esPrimerUsuario) {
            tenantService.buscarPorSubdomain(subdomain).ifPresent(t ->
                emailService.enviarBienvenida(t, username, password));
        }
        ra.addFlashAttribute("mensaje", "Usuario '" + username + "' creado correctamente");
        ra.addFlashAttribute("tipoMensaje", "success");
        return redirect;
    }

    @Transactional
    @PostMapping("/{subdomain}/usuarios/{id}/toggle")
    public String toggleUsuario(@PathVariable String subdomain, @PathVariable Long id, RedirectAttributes ra) {
        usuarioRepo.toggleActivoByIdAndTenantId(id, subdomain);
        ra.addFlashAttribute("mensaje", "Estado del usuario actualizado");
        ra.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/admin/tenants/" + subdomain + "/usuarios";
    }

    @Transactional
    @PostMapping("/{subdomain}/usuarios/{id}/password")
    public String cambiarPassword(@PathVariable String subdomain, @PathVariable Long id,
                                   @RequestParam String nuevaPassword,
                                   @RequestParam String confirmarPassword,
                                   RedirectAttributes ra) {
        String redirect = "redirect:/admin/tenants/" + subdomain + "/usuarios";
        String errorNueva = PasswordPolicy.validar(nuevaPassword);
        if (errorNueva != null) {
            ra.addFlashAttribute("mensaje", errorNueva);
            ra.addFlashAttribute("tipoMensaje", "danger");
            return redirect;
        }
        if (!nuevaPassword.equals(confirmarPassword)) {
            ra.addFlashAttribute("mensaje", "Las contraseñas no coinciden");
            ra.addFlashAttribute("tipoMensaje", "danger");
            return redirect;
        }
        usuarioRepo.updatePasswordByIdAndTenantId(id, subdomain, passwordEncoder.encode(nuevaPassword));
        ra.addFlashAttribute("mensaje", "Contraseña actualizada correctamente");
        ra.addFlashAttribute("tipoMensaje", "success");
        return redirect;
    }

    @Transactional
    @PostMapping("/{subdomain}/usuarios/{id}/rol")
    public String cambiarRol(@PathVariable String subdomain, @PathVariable Long id,
                              @RequestParam RolUsuario rol, RedirectAttributes ra) {
        usuarioRepo.updateRolByIdAndTenantId(id, subdomain, rol.name());
        ra.addFlashAttribute("mensaje", "Rol actualizado correctamente");
        ra.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/admin/tenants/" + subdomain + "/usuarios";
    }

    @Transactional
    @PostMapping("/{subdomain}/usuarios/{id}/eliminar")
    public String eliminarUsuario(@PathVariable String subdomain, @PathVariable Long id, RedirectAttributes ra) {
        usuarioRepo.deleteByIdAndTenantId(id, subdomain);
        tenantService.registrarAccion(subdomain, "USUARIO_ELIMINADO", "id=" + id);
        ra.addFlashAttribute("mensaje", "Usuario eliminado");
        ra.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/admin/tenants/" + subdomain + "/usuarios";
    }
}