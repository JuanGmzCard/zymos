package com.alera.controller;

import com.alera.model.Tarea;
import com.alera.model.enums.EstadoTarea;
import com.alera.model.enums.PrioridadTarea;
import com.alera.service.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
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
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/tareas")
public class TareaController {

    private final TareaService              service;
    private final UsuarioService            usuarioService;
    private final TrazabilidadService       trazabilidadService;
    private final EquipoService             equipoService;
    private final InsumoInventarioService   insumoService;
    private final PlanificacionService      planificacionService;
    private final OrdenCompraService        ordenCompraService;
    private final VentaService              ventaService;
    private final ClienteService            clienteService;
    private final FacturaProveedorService   facturaService;
    private final ProveedorService          proveedorService;
    private final RecetaService             recetaService;
    private final BarrilService             barrilService;
    private final MessageSource             messageSource;

    public TareaController(TareaService service,
                           UsuarioService usuarioService,
                           TrazabilidadService trazabilidadService,
                           EquipoService equipoService,
                           InsumoInventarioService insumoService,
                           PlanificacionService planificacionService,
                           OrdenCompraService ordenCompraService,
                           VentaService ventaService,
                           ClienteService clienteService,
                           FacturaProveedorService facturaService,
                           ProveedorService proveedorService,
                           RecetaService recetaService,
                           BarrilService barrilService,
                           MessageSource messageSource) {
        this.service              = service;
        this.usuarioService       = usuarioService;
        this.trazabilidadService  = trazabilidadService;
        this.equipoService        = equipoService;
        this.insumoService        = insumoService;
        this.planificacionService = planificacionService;
        this.ordenCompraService   = ordenCompraService;
        this.ventaService         = ventaService;
        this.clienteService       = clienteService;
        this.facturaService       = facturaService;
        this.proveedorService     = proveedorService;
        this.recetaService        = recetaService;
        this.barrilService        = barrilService;
        this.messageSource        = messageSource;
    }

    private String msg(String key, Locale locale) {
        return messageSource.getMessage(key, null, key, locale);
    }

    private String msgf(String key, Locale locale, Object... args) {
        return messageSource.getMessage(key, args, key, locale);
    }

    @GetMapping
    public String index(@RequestParam(required = false) EstadoTarea estado,
                        @RequestParam(required = false) String asignadoA,
                        Model model) {
        List<Tarea> tareas = service.listar(estado, asignadoA);
        Map<String, Long> conteos = service.contarPorEstado();

        model.addAttribute("tareas",       tareas);
        model.addAttribute("estadoFiltro", estado);
        model.addAttribute("asignadoFiltro", asignadoA);
        model.addAttribute("conteos",      conteos);
        model.addAttribute("usuarios",     usuarioService.listarTodos());
        model.addAttribute("estados",      EstadoTarea.values());
        return "tareas/index";
    }

    @GetMapping("/nueva")
    public String nueva(Model model) {
        model.addAttribute("tarea",       new Tarea());
        model.addAttribute("usuarios",    usuarioService.listarTodos());
        model.addAttribute("prioridades", PrioridadTarea.values());
        model.addAttribute("tiposRef",    tiposReferencia());
        model.addAttribute("modoEdicion", false);
        return "tareas/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@RequestParam String titulo,
                          @RequestParam(required = false) String descripcion,
                          @RequestParam(required = false) String fechaVencimiento,
                          @RequestParam(required = false) PrioridadTarea prioridad,
                          @RequestParam(required = false) String asignadoA,
                          @RequestParam(value = "refTipos", required = false) List<String> refTipos,
                          @RequestParam(value = "refIds",   required = false) List<Long>   refIds,
                          @RequestParam(value = "itemDesc", required = false) List<String> itemDescs,
                          Authentication auth,
                          RedirectAttributes ra,
                          Locale locale) {
        try {
            LocalDate fv = (fechaVencimiento != null && !fechaVencimiento.isBlank())
                    ? LocalDate.parse(fechaVencimiento) : null;
            List<Map<String, String>> itemsData = buildItemsData(itemDescs);
            service.guardar(titulo, descripcion, fv, prioridad, asignadoA, refTipos, refIds, itemsData, auth.getName());
            ra.addFlashAttribute("mensaje", msg("tarea.guardada", locale));
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("mensaje", msgf("tarea.error.crear", locale, e.getMessage()));
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
        model.addAttribute("tarea",       tarea);
        model.addAttribute("usuarios",    usuarioService.listarTodos());
        model.addAttribute("prioridades", PrioridadTarea.values());
        model.addAttribute("tiposRef",    tiposReferencia());
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
                             @RequestParam(value = "refTipos", required = false) List<String> refTipos,
                             @RequestParam(value = "refIds",   required = false) List<Long>   refIds,
                             @RequestParam(value = "itemDesc", required = false) List<String> itemDescs,
                             RedirectAttributes ra,
                             Locale locale) {
        try {
            LocalDate fv = (fechaVencimiento != null && !fechaVencimiento.isBlank())
                    ? LocalDate.parse(fechaVencimiento) : null;
            List<Map<String, String>> itemsData = buildItemsData(itemDescs);
            service.actualizar(id, titulo, descripcion, fv, prioridad, asignadoA, refTipos, refIds, itemsData);
            ra.addFlashAttribute("mensaje", msg("tarea.actualizada", locale));
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("mensaje", msgf("tarea.error.actualizar", locale, e.getMessage()));
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/tareas/" + id;
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra, Locale locale) {
        try {
            service.eliminar(id);
            ra.addFlashAttribute("mensaje", msg("tarea.eliminada", locale));
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("mensaje", msgf("tarea.error.eliminar", locale, e.getMessage()));
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/tareas";
    }

    @PostMapping(value = "/{tareaId}/items/{itemId}/toggle", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleItem(@PathVariable Long tareaId,
                                                          @PathVariable Long itemId) {
        try {
            Map<String, Object> result = service.toggleItem(tareaId, itemId);
            return ResponseEntity.ok(result);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping(value = "/suggest-ref", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Map<String, Object>> suggestRef(@RequestParam String tipo,
                                                @RequestParam(defaultValue = "") String q) {
        if (q.isBlank() || q.trim().length() < 2) return List.of();
        return switch (tipo.toUpperCase()) {
            case "LOTE" -> trazabilidadService.suggest(q).stream()
                    .map(m -> Map.<String, Object>of(
                            "id",    m.get("id"),
                            "label", m.get("codigoLote") + (m.get("estilo") != null && !m.get("estilo").toString().isBlank() ? " — " + m.get("estilo") : ""),
                            "sub",   String.valueOf(m.getOrDefault("fase", ""))))
                    .toList();
            case "EQUIPO" -> equipoService.suggest(q, null).stream()
                    .map(m -> Map.<String, Object>of(
                            "id",    m.get("id"),
                            "label", m.get("nombre"),
                            "sub",   String.valueOf(m.getOrDefault("tipo", ""))))
                    .toList();
            case "INSUMO" -> insumoService.suggest(q);
            case "ELABORACION" -> planificacionService.suggest(q);
            case "ORDEN_COMPRA" -> ordenCompraService.suggest(q).stream()
                    .map(m -> Map.<String, Object>of(
                            "id",    m.get("id"),
                            "label", m.get("titulo"),
                            "sub",   String.valueOf(m.getOrDefault("sub", ""))))
                    .toList();
            case "VENTA" -> ventaService.suggest(q).stream()
                    .map(m -> Map.<String, Object>of(
                            "id",    m.get("id"),
                            "label", m.get("titulo"),
                            "sub",   String.valueOf(m.getOrDefault("sub", ""))))
                    .toList();
            case "CLIENTE" -> clienteService.suggest(q).stream()
                    .map(m -> Map.<String, Object>of(
                            "id",    m.get("id"),
                            "label", m.get("nombre"),
                            "sub",   String.valueOf(m.getOrDefault("nit", ""))))
                    .toList();
            case "FACTURA" -> facturaService.suggest(q).stream()
                    .map(m -> Map.<String, Object>of(
                            "id",    m.get("id"),
                            "label", m.get("titulo"),
                            "sub",   String.valueOf(m.getOrDefault("proveedor", ""))))
                    .toList();
            case "PROVEEDOR" -> proveedorService.suggest(q).stream()
                    .map(m -> Map.<String, Object>of(
                            "id",    m.get("id"),
                            "label", m.get("nombre"),
                            "sub",   String.valueOf(m.getOrDefault("nit", ""))))
                    .toList();
            case "RECETA" -> recetaService.suggest(q, null).stream()
                    .map(m -> Map.<String, Object>of(
                            "id",    m.get("id"),
                            "label", m.get("nombre"),
                            "sub",   String.valueOf(m.getOrDefault("estilo", ""))))
                    .toList();
            case "BARRIL" -> barrilService.suggest(q);
            default -> List.of();
        };
    }

    private List<Map<String, String>> tiposReferencia() {
        return List.of(
            Map.of("valor", "LOTE",         "etiqueta", "Lote de producción"),
            Map.of("valor", "EQUIPO",        "etiqueta", "Equipo"),
            Map.of("valor", "INSUMO",        "etiqueta", "Insumo / Inventario"),
            Map.of("valor", "ELABORACION",   "etiqueta", "Elaboración planificada"),
            Map.of("valor", "ORDEN_COMPRA",  "etiqueta", "Orden de compra"),
            Map.of("valor", "VENTA",         "etiqueta", "Venta"),
            Map.of("valor", "CLIENTE",       "etiqueta", "Cliente"),
            Map.of("valor", "FACTURA",       "etiqueta", "Factura de proveedor"),
            Map.of("valor", "PROVEEDOR",     "etiqueta", "Proveedor"),
            Map.of("valor", "RECETA",        "etiqueta", "Receta"),
            Map.of("valor", "BARRIL",        "etiqueta", "Barril")
        );
    }

    private List<Map<String, String>> buildItemsData(List<String> descs) {
        List<Map<String, String>> result = new ArrayList<>();
        if (descs == null) return result;
        for (String desc : descs) {
            Map<String, String> m = new HashMap<>();
            m.put("descripcion", desc);
            result.add(m);
        }
        return result;
    }
}
