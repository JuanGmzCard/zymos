package com.alera.controller;

import com.alera.model.ElaboracionPlanificada;
import com.alera.model.enums.EstadoPlanificacion;
import com.alera.service.PlanificacionService;
import com.alera.service.RecetaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/planificacion")
public class PlanificacionController {

    private final PlanificacionService planService;
    private final RecetaService recetaService;

    public PlanificacionController(PlanificacionService planService,
                                    RecetaService recetaService) {
        this.planService   = planService;
        this.recetaService = recetaService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("proximas", planService.listarProximas());
        model.addAttribute("todas",    planService.listarTodas());
        model.addAttribute("recetas",  recetaService.listarActivas());
        model.addAttribute("estados",  EstadoPlanificacion.values());
        return "planificacion/index";
    }

    /** Eventos para FullCalendar — rango enviado por el calendario como parámetros ISO. */
    @GetMapping(value = "/eventos", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Map<String, Object>> eventos(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end) {

        LocalDate desde = start != null ? LocalDate.parse(start.substring(0, 10))
                                        : LocalDate.now().minusMonths(1);
        LocalDate hasta = end   != null ? LocalDate.parse(end.substring(0, 10))
                                        : LocalDate.now().plusMonths(3);

        List<Map<String, Object>> eventos = new ArrayList<>();
        for (ElaboracionPlanificada p : planService.listarPorRango(desde, hasta)) {
            Map<String, Object> ev = new LinkedHashMap<>();
            ev.put("id",               String.valueOf(p.getId()));
            ev.put("title",            buildTitle(p));
            ev.put("start",            p.getFechaPlaneada().toString());
            ev.put("backgroundColor",  p.getEstado().getColor());
            ev.put("borderColor",      p.getEstado().getColor());
            ev.put("textColor",        p.getEstado().getColorTexto());

            Map<String, Object> props = new LinkedHashMap<>();
            props.put("planId",           p.getId());
            props.put("nombreElaboracion",p.getNombreElaboracion());
            props.put("recetaId",   p.getReceta() != null ? p.getReceta().getId() : null);
            props.put("recetaNombre",p.getReceta() != null ? p.getReceta().getNombre() : "");
            props.put("volumenEstimado",  p.getVolumenEstimado());
            props.put("estado",           p.getEstado().name());
            props.put("notas",            p.getNotas());
            ev.put("extendedProps", props);
            eventos.add(ev);
        }
        return eventos;
    }

    @PostMapping("/guardar")
    @PreAuthorize("hasRole('ADMIN')")
    public String guardar(@RequestParam(required = false) Long id,
                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaPlaneada,
                          @RequestParam String nombreElaboracion,
                          @RequestParam(required = false) Long recetaId,
                          @RequestParam(required = false) BigDecimal volumenEstimado,
                          @RequestParam(required = false) String notas,
                          RedirectAttributes ra) {
        ElaboracionPlanificada plan = id != null
                ? planService.buscarPorId(id).orElse(new ElaboracionPlanificada())
                : new ElaboracionPlanificada();

        plan.setFechaPlaneada(fechaPlaneada);
        plan.setNombreElaboracion(nombreElaboracion.trim());
        plan.setVolumenEstimado(volumenEstimado);
        plan.setNotas(notas != null && !notas.isBlank() ? notas.trim() : null);

        planService.guardar(plan, recetaId);
        ra.addFlashAttribute("mensaje", id != null ? "Planificación actualizada" : "Planificación creada");
        ra.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/planificacion";
    }

    @PostMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public String cambiarEstado(@PathVariable Long id,
                                 @RequestParam EstadoPlanificacion estado,
                                 RedirectAttributes ra) {
        planService.cambiarEstado(id, estado);
        ra.addFlashAttribute("mensaje", "Estado actualizado a: " + estado.getDisplayName());
        ra.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/planificacion";
    }

    @PostMapping("/{id}/eliminar")
    @PreAuthorize("hasRole('ADMIN')")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        planService.eliminar(id);
        ra.addFlashAttribute("mensaje", "Planificación eliminada");
        ra.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/planificacion";
    }

    private String buildTitle(ElaboracionPlanificada p) {
        String nombre = p.getNombreElaboracion();
        return p.getVolumenEstimado() != null
                ? nombre + " · " + p.getVolumenEstimado().stripTrailingZeros().toPlainString() + "L"
                : nombre;
    }
}
