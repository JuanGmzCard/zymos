package com.alera.controller;

import com.alera.dto.FacturaFormDto;
import com.alera.model.Equipo;
import com.alera.model.InsumoInventario;
import com.alera.model.enums.EstadoEquipo;
import com.alera.model.enums.TipoEquipo;
import com.alera.model.enums.TipoInsumo;
import com.alera.model.enums.TipoItemFactura;
import com.alera.repository.EquipoRepository;
import com.alera.repository.InsumoInventarioRepository;
import com.alera.service.EquipoService;
import com.alera.service.FacturaProveedorService;
import com.alera.service.InsumoInventarioService;
import com.alera.service.ProveedorService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/facturas")
public class FacturaProveedorController {

    private final FacturaProveedorService service;
    private final ProveedorService proveedorService;
    private final InsumoInventarioRepository insumoRepo;
    private final EquipoRepository equipoRepo;
    private final InsumoInventarioService insumoService;
    private final EquipoService equipoService;

    public FacturaProveedorController(FacturaProveedorService service,
                                       ProveedorService proveedorService,
                                       InsumoInventarioRepository insumoRepo,
                                       EquipoRepository equipoRepo,
                                       InsumoInventarioService insumoService,
                                       EquipoService equipoService) {
        this.service = service;
        this.proveedorService = proveedorService;
        this.insumoRepo = insumoRepo;
        this.equipoRepo = equipoRepo;
        this.insumoService = insumoService;
        this.equipoService = equipoService;
    }

    @GetMapping
    public String lista(@RequestParam(defaultValue = "0") int page, Model model) {
        var pagina = service.listarPaginado(page);
        model.addAttribute("facturas",     pagina.getContent());
        model.addAttribute("paginaActual", page);
        model.addAttribute("totalPaginas", pagina.getTotalPages());
        model.addAttribute("totalFacturas", pagina.getTotalElements());
        model.addAttribute("baseUrl",      "/facturas");
        model.addAttribute("extraParams",  "");
        return "facturas/lista";
    }

    @GetMapping(value = "/suggest", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Map<String, Object>> suggest(@RequestParam(defaultValue = "") String q) {
        return service.suggest(q);
    }

    @GetMapping("/nueva")
    public String nueva(Model model) {
        model.addAttribute("facturaForm", FacturaFormDto.empty());
        agregarDatosFormulario(model);
        return "facturas/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("facturaForm") FacturaFormDto dto,
                          BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            agregarDatosFormulario(model);
            return "facturas/formulario";
        }
        try {
            service.guardar(dto);
            ra.addFlashAttribute("mensaje", "Factura guardada y inventario actualizado");
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("mensaje", "Error: " + e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/facturas";
    }

    @GetMapping("/ver/{id}")
    public String ver(@PathVariable Long id, Model model) {
        model.addAttribute("factura", service.buscarPorId(id).orElseThrow());
        return "facturas/detalle";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        var factura = service.buscarPorId(id).orElseThrow();
        model.addAttribute("facturaForm", service.toFormDto(factura));
        model.addAttribute("facturaId", id);
        agregarDatosFormulario(model);
        return "facturas/formulario";
    }

    @PostMapping("/actualizar/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("facturaForm") FacturaFormDto dto,
                             BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            agregarDatosFormulario(model);
            model.addAttribute("facturaId", id);
            return "facturas/formulario";
        }
        try {
            service.actualizar(id, dto);
            ra.addFlashAttribute("mensaje", "Factura actualizada");
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("mensaje", "Error: " + e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/facturas";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            service.eliminar(id);
            ra.addFlashAttribute("mensaje", "Factura eliminada");
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("mensaje", "Error: " + e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/facturas";
    }

    // ── Quick-create endpoints (accesibles para ADMIN y FACTURACION) ────────

    @PostMapping("/guardar-insumo-rapido")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> guardarInsumoRapido(
            @RequestParam String nombre,
            @RequestParam TipoInsumo tipo,
            @RequestParam(defaultValue = "gr") String unidad) {
        Map<String, Object> resp = new LinkedHashMap<>();
        try {
            String nombreTrim = nombre.trim();
            if (insumoService.buscarPorNombreExacto(nombreTrim).isPresent()) {
                resp.put("success", false);
                resp.put("error", "Ya existe un insumo con ese nombre");
                return ResponseEntity.badRequest().body(resp);
            }
            InsumoInventario ins = new InsumoInventario();
            ins.setNombre(nombreTrim);
            ins.setTipo(tipo);
            ins.setUnidad(unidad);
            ins.setCantidad(BigDecimal.ZERO);
            ins.setStockMinimo(BigDecimal.ZERO);
            InsumoInventario saved = insumoService.guardar(ins);
            resp.put("success", true);
            resp.put("id", saved.getId());
            resp.put("nombre", saved.getNombre());
            resp.put("tipo", saved.getTipo().name());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            resp.put("success", false);
            resp.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        }
    }

    @PostMapping("/guardar-equipo-rapido")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> guardarEquipoRapido(
            @RequestParam String nombre,
            @RequestParam TipoEquipo tipo) {
        Map<String, Object> resp = new LinkedHashMap<>();
        try {
            String nombreTrim = nombre.trim();
            Equipo eq = new Equipo();
            eq.setNombre(nombreTrim);
            eq.setTipo(tipo);
            eq.setEstado(EstadoEquipo.OPERATIVO);
            Equipo saved = equipoService.guardar(eq);
            resp.put("success", true);
            resp.put("id", saved.getId());
            resp.put("nombre", saved.getNombre());
            resp.put("tipo", saved.getTipo().name());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            resp.put("success", false);
            resp.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        }
    }

    // ────────────────────────────────────────────────────────────────────────

    private void agregarDatosFormulario(Model model) {
        model.addAttribute("tiposInsumo", TipoInsumo.values());
        model.addAttribute("tiposEquipo", TipoEquipo.values());
        model.addAttribute("tiposItem",   TipoItemFactura.values());
        model.addAttribute("proveedoresActivos", proveedorService.listarActivos());

        // Agrupados por tipo para el datalist dinámico en el JS del formulario
        Map<String, List<String>> insumosPorTipo = insumoRepo.findAllByOrderByNombreAsc()
                .stream()
                .collect(Collectors.groupingBy(
                        i -> i.getTipo().name(),
                        Collectors.mapping(InsumoInventario::getNombre, Collectors.toList())));
        Map<String, List<String>> equiposPorTipo = equipoRepo.findAll()
                .stream()
                .collect(Collectors.groupingBy(
                        e -> e.getTipo().name(),
                        Collectors.mapping(Equipo::getNombre, Collectors.toList())));
        model.addAttribute("insumosPorTipo", insumosPorTipo);
        model.addAttribute("equiposPorTipo", equiposPorTipo);
    }
}