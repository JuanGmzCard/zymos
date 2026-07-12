package com.alera.controller;

import com.alera.model.Tarea;
import com.alera.model.enums.EstadoTarea;
import com.alera.model.enums.PrioridadTarea;
import com.alera.service.EquipoService;
import com.alera.service.TareaService;
import com.alera.service.TrazabilidadService;
import com.alera.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/tareas")
public class TareaController {

    private final TareaService       service;
    private final UsuarioService     usuarioService;
    private final TrazabilidadService trazabilidadService;
    private final EquipoService      equipoService;

    public TareaController(TareaService service,
                           UsuarioService usuarioService,
                           TrazabilidadService trazabilidadService,
                           EquipoService equipoService) {
        this.service             = service;
        this.usuarioService      = usuarioService;
        this.trazabilidadService = trazabilidadService;
        this.equipoService       = equipoService;
    }

    @GetMapping
    public String index(@RequestParam(required = false) EstadoTarea estado,
                        @RequestParam(required = false) String asignadoA,
                        Model model) {
        List<Tarea> tareas = service.listar(estado, asignadoA);
        Map<String, Long> conteos = service.contarPorEstado();

        model.addAttribute("tareas",      tareas);
        model.addAttribute("estadoFiltro", estado);
        model.addAttribute("asignadoFiltro", asignadoA);
        model.addAttribute("conteos",     conteos);
        model.addAttribute("usuarios",    usuarioService.listarTodos());
        model.addAttribute("estados",     EstadoTarea.values());
        return "tareas/index";
    }

    @GetMapping("/nueva")
    public String nueva(Model model) {
        model.addAttribute("usuarios",  usuarioService.listarTodos());
        model.addAttribute("lotes",     trazabilidadService.listarTodos());
        model.addAttribute("equipos",   equipoService.listarTodos());
        model.addAttribute("prioridades", PrioridadTarea.values());
        model.addAttribute("modoEdicion", false);
        return "tareas/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@RequestParam String titulo,
                          @RequestParam(required = false) String descripcion,
                          @RequestParam(required = false) String fechaVencimiento,
                          @RequestParam(required = false) PrioridadTarea prioridad,
                          @RequestParam(required = false) String asignadoA,
                          @RequestParam(required = false) Long loteId,
                          @RequestParam(required = false) Long equipoId,
                          @RequestParam(value = "itemDesc",      required = false) List<String> itemDescs,
                          @RequestParam(value = "itemLoteId",    required = false) List<String> itemLoteIds,
                          @RequestParam(value = "itemEquipoId",  required = false) List<String> itemEquipoIds,
                          Authentication auth,
                          RedirectAttributes ra) {
        try {
            LocalDate fv = (fechaVencimiento != null && !fechaVencimiento.isBlank())
                    ? LocalDate.parse(fechaVencimiento) : null;
            List<Map<String, String>> itemsData = buildItemsData(itemDescs, itemLoteIds, itemEquipoIds);
            service.guardar(titulo, descripcion, fv, prioridad, asignadoA, loteId, equipoId, itemsData, auth.getName());
            ra.addFlashAttribute("mensaje", "Tarea creada exitosamente.");
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("mensaje", "Error al crear la tarea: " + e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/tareas";
    }

    @GetMapping("/{id}")
    public String ver(@PathVariable Long id, Model model) {
        model.addAttribute("tarea", service.buscarPorId(id));
        return "tareas/detalle";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        Tarea tarea = service.buscarPorId(id);
        model.addAttribute("tarea",      tarea);
        model.addAttribute("usuarios",   usuarioService.listarTodos());
        model.addAttribute("lotes",      trazabilidadService.listarTodos());
        model.addAttribute("equipos",    equipoService.listarTodos());
        model.addAttribute("prioridades", PrioridadTarea.values());
        model.addAttribute("modoEdicion", true);
        return "tareas/formulario";
    }

    @PostMapping("/actualizar/{id}")
    public String actualizar(@PathVariable Long id,
                             @RequestParam String titulo,
                             @RequestParam(required = false) String descripcion,
                             @RequestParam(required = false) String fechaVencimiento,
                             @RequestParam(required = false) PrioridadTarea prioridad,
                             @RequestParam(required = false) String asignadoA,
                             @RequestParam(required = false) Long loteId,
                             @RequestParam(required = false) Long equipoId,
                             @RequestParam(value = "itemDesc",      required = false) List<String> itemDescs,
                             @RequestParam(value = "itemLoteId",    required = false) List<String> itemLoteIds,
                             @RequestParam(value = "itemEquipoId",  required = false) List<String> itemEquipoIds,
                             RedirectAttributes ra) {
        try {
            LocalDate fv = (fechaVencimiento != null && !fechaVencimiento.isBlank())
                    ? LocalDate.parse(fechaVencimiento) : null;
            List<Map<String, String>> itemsData = buildItemsData(itemDescs, itemLoteIds, itemEquipoIds);
            service.actualizar(id, titulo, descripcion, fv, prioridad, asignadoA, loteId, equipoId, itemsData);
            ra.addFlashAttribute("mensaje", "Tarea actualizada.");
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("mensaje", "Error al actualizar la tarea: " + e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/tareas/" + id;
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            service.eliminar(id);
            ra.addFlashAttribute("mensaje", "Tarea eliminada.");
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("mensaje", "Error al eliminar la tarea: " + e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/tareas";
    }

    @PostMapping(value = "/{tareaId}/items/{itemId}/toggle", produces = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleItem(@PathVariable Long tareaId,
                                                          @PathVariable Long itemId) {
        try {
            Map<String, Object> result = service.toggleItem(tareaId, itemId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private List<Map<String, String>> buildItemsData(List<String> descs,
                                                     List<String> loteIds,
                                                     List<String> equipoIds) {
        List<Map<String, String>> result = new ArrayList<>();
        if (descs == null) return result;
        for (int i = 0; i < descs.size(); i++) {
            Map<String, String> m = new HashMap<>();
            m.put("descripcion", descs.get(i));
            m.put("loteId",   safeGet(loteIds, i));
            m.put("equipoId", safeGet(equipoIds, i));
            result.add(m);
        }
        return result;
    }

    private String safeGet(List<String> list, int index) {
        return (list != null && index < list.size()) ? list.get(index) : null;
    }
}
