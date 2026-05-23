package com.alera.controller;

import com.alera.model.Notificacion;
import com.alera.service.NotificacionService;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/notificaciones")
public class NotificacionController {

    private final NotificacionService service;

    public NotificacionController(NotificacionService service) {
        this.service = service;
    }

    @GetMapping
    public String index(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<Notificacion> pagina = service.listarTodas(page);
        model.addAttribute("notificaciones", pagina);
        model.addAttribute("totalNoLeidas",  service.contarNoLeidas());
        model.addAttribute("paginaActual",   page);
        model.addAttribute("totalPaginas",   pagina.getTotalPages());
        return "notificaciones/index";
    }

    /** JSON para el dropdown del navbar: total no leídas + últimas 5. */
    @GetMapping(value = "/recientes", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> recientes() {
        long total         = service.contarNoLeidas();
        List<Notificacion> items = service.listarRecientes();

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("total", total);
        resp.put("items", items.stream().map(this::toMap).toList());
        return resp;
    }

    /** Marca una notificación como leída. Devuelve el nuevo conteo de no leídas. */
    @PostMapping(value = "/{id}/leer", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> marcarLeida(@PathVariable Long id) {
        service.marcarLeida(id);
        return Map.of("success", true, "noLeidas", service.contarNoLeidas());
    }

    /** Marca todas como leídas y redirige a la página de notificaciones. */
    @PostMapping("/leer-todas")
    public String marcarTodasLeidas() {
        service.marcarTodasLeidas();
        return "redirect:/notificaciones";
    }

    // ── helpers ──────────────────────────────────────────────────────────────

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
