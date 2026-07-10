package com.alera.controller;

import com.alera.model.Notificacion;
import com.alera.service.NotificacionService;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/notificaciones")
public class NotificacionController {

    private final NotificacionService service;

    public NotificacionController(NotificacionService service) {
        this.service = service;
    }

    @GetMapping
    public String index(@RequestParam(defaultValue = "0") int page, Model model,
                        Authentication auth) {
        Collection<String> authorities = authorities(auth);
        Page<Notificacion> pagina = service.listarTodas(page, authorities);
        model.addAttribute("notificaciones", pagina);
        model.addAttribute("totalNoLeidas",  service.contarNoLeidas(authorities));
        model.addAttribute("paginaActual",   page);
        model.addAttribute("totalPaginas",   pagina.getTotalPages());
        return "notificaciones/index";
    }

    /** JSON para el dropdown del navbar: total no leídas + últimas 5, filtradas por rol. */
    @GetMapping(value = "/recientes", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> recientes(Authentication auth) {
        Collection<String> authorities = authorities(auth);
        long total         = service.contarNoLeidas(authorities);
        List<Notificacion> items = service.listarRecientes(authorities);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("total", total);
        resp.put("items", items.stream().map(this::toMap).toList());
        return resp;
    }

    /** Marca una notificación como leída. Devuelve el nuevo conteo de no leídas. */
    @PostMapping(value = "/{id}/leer", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> marcarLeida(@PathVariable Long id, Authentication auth) {
        service.marcarLeida(id);
        return Map.of("success", true, "noLeidas", service.contarNoLeidas(authorities(auth)));
    }

    /** Marca todas como leídas y redirige a la página de notificaciones. */
    @PostMapping("/leer-todas")
    public String marcarTodasLeidas() {
        service.marcarTodasLeidas();
        return "redirect:/notificaciones";
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static Collection<String> authorities(Authentication auth) {
        if (auth == null) return List.of();
        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toSet());
    }

    private Map<String, Object> toMap(Notificacion n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",             n.getId());
        m.put("tipo",           n.getTipo().name());
        m.put("icono",          n.getTipo().getIcono());
        m.put("colorClase",     n.getTipo().getColorClase());
        m.put("titulo",         n.getTitulo());
        m.put("mensaje",        n.getMensaje());
        m.put("urlAccion",      n.getUrlAccion());
        m.put("leida",          n.isLeida());
        m.put("tiempoRelativo", tiempoRelativo(n.getCreatedAt()));
        return m;
    }

    private static String tiempoRelativo(LocalDateTime dt) {
        if (dt == null) return "";
        Duration d = Duration.between(dt, LocalDateTime.now());
        long min = d.toMinutes();
        if (min < 1)  return "Hace un momento";
        if (min < 60) return "Hace " + min + " min";
        long h = d.toHours();
        if (h < 24)   return "Hace " + h + " h";
        long dias = d.toDays();
        return "Hace " + dias + " día" + (dias > 1 ? "s" : "");
    }
}
