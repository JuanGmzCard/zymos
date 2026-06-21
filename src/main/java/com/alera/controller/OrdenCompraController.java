package com.alera.controller;

import com.alera.dto.OrdenCompraFormDto;
import com.alera.model.Equipo;
import com.alera.model.InsumoInventario;
import com.alera.model.OrdenCompra;
import com.alera.model.enums.EstadoOrdenCompra;
import com.alera.model.enums.TipoItemFactura;
import com.alera.repository.EquipoRepository;
import com.alera.repository.InsumoInventarioRepository;
import com.alera.service.*;
import com.alera.config.ExportBranding;
import com.alera.model.Tenant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/ordenes-compra")
public class OrdenCompraController {

    private static final Logger log = LoggerFactory.getLogger(OrdenCompraController.class);

    private final OrdenCompraService service;
    private final FacturaProveedorService facturaService;
    private final ProveedorService proveedorService;
    private final CategoriaInsumoService categoriaInsumoService;
    private final CategoriaEquipoService categoriaEquipoService;
    private final PdfExportService pdfExportService;
    private final InsumoInventarioRepository insumoRepo;
    private final EquipoRepository equipoRepo;

    public OrdenCompraController(OrdenCompraService service,
                                 FacturaProveedorService facturaService,
                                 ProveedorService proveedorService,
                                 CategoriaInsumoService categoriaInsumoService,
                                 CategoriaEquipoService categoriaEquipoService,
                                 PdfExportService pdfExportService,
                                 InsumoInventarioRepository insumoRepo,
                                 EquipoRepository equipoRepo) {
        this.service                = service;
        this.facturaService         = facturaService;
        this.proveedorService       = proveedorService;
        this.categoriaInsumoService = categoriaInsumoService;
        this.categoriaEquipoService = categoriaEquipoService;
        this.pdfExportService       = pdfExportService;
        this.insumoRepo             = insumoRepo;
        this.equipoRepo             = equipoRepo;
    }

    // ── Lista ──────────────────────────────────────────────────────────────

    @GetMapping
    public String lista(@RequestParam(required = false) EstadoOrdenCompra estado,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        model.addAttribute("ordenes",        service.listarPaginado(estado, page));
        model.addAttribute("estadoFiltro",   estado);
        model.addAttribute("estados",        EstadoOrdenCompra.values());
        model.addAttribute("statsTotal",     service.countTotal());
        model.addAttribute("statsBorrador",  service.countByEstado(EstadoOrdenCompra.BORRADOR));
        model.addAttribute("statsEnviadas",  service.countByEstado(EstadoOrdenCompra.ENVIADA));
        model.addAttribute("statsRecibidas", service.countByEstado(EstadoOrdenCompra.RECIBIDA)
                                           + service.countByEstado(EstadoOrdenCompra.RECIBIDA_PARCIAL));
        model.addAttribute("paginaActual",   page);
        model.addAttribute("baseUrl",        "/ordenes-compra");
        return "ordenes-compra/lista";
    }

    // ── Suggest ───────────────────────────────────────────────────────────

    @GetMapping("/suggest")
    @ResponseBody
    public List<Map<String, Object>> suggest(@RequestParam(defaultValue = "") String q) {
        return service.suggest(q);
    }

    // ── Formulario nuevo ──────────────────────────────────────────────────

    @GetMapping("/nueva")
    public String nueva(Model model) {
        model.addAttribute("oc", new OrdenCompraFormDto());
        agregarDatosFormulario(model);
        return "ordenes-compra/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("oc") OrdenCompraFormDto dto,
                          BindingResult result,
                          Model model,
                          RedirectAttributes flash) {
        if (result.hasErrors()) {
            agregarDatosFormulario(model);
            return "ordenes-compra/formulario";
        }
        try {
            OrdenCompra saved = service.guardar(dto);
            flash.addFlashAttribute("mensaje",     "Orden de compra " + saved.getNumeroOc() + " creada correctamente.");
            flash.addFlashAttribute("tipoMensaje", "success");
            return "redirect:/ordenes-compra/ver/" + saved.getId();
        } catch (Exception e) {
            flash.addFlashAttribute("mensaje",     "Error al crear la orden: " + e.getMessage());
            flash.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/ordenes-compra";
        }
    }

    // ── Formulario edición ────────────────────────────────────────────────

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes flash) {
        try {
            OrdenCompra oc = service.buscarPorId(id);
            if (!oc.isEditable()) {
                flash.addFlashAttribute("mensaje",     "Solo se pueden editar órdenes en estado BORRADOR.");
                flash.addFlashAttribute("tipoMensaje", "warning");
                return "redirect:/ordenes-compra/ver/" + id;
            }
            model.addAttribute("oc",    toFormDto(oc));
            model.addAttribute("ocId",  id);
            agregarDatosFormulario(model);
            return "ordenes-compra/formulario";
        } catch (Exception e) {
            flash.addFlashAttribute("mensaje",     "Orden no encontrada.");
            flash.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/ordenes-compra";
        }
    }

    @PostMapping("/actualizar/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("oc") OrdenCompraFormDto dto,
                             BindingResult result,
                             Model model,
                             RedirectAttributes flash) {
        if (result.hasErrors()) {
            model.addAttribute("ocId", id);
            agregarDatosFormulario(model);
            return "ordenes-compra/formulario";
        }
        try {
            service.actualizar(id, dto);
            flash.addFlashAttribute("mensaje",     "Orden actualizada correctamente.");
            flash.addFlashAttribute("tipoMensaje", "success");
        } catch (Exception e) {
            flash.addFlashAttribute("mensaje",     "Error: " + e.getMessage());
            flash.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/ordenes-compra/ver/" + id;
    }

    // ── Detalle ───────────────────────────────────────────────────────────

    @GetMapping("/ver/{id}")
    public String ver(@PathVariable Long id, Model model, RedirectAttributes flash) {
        try {
            OrdenCompra oc = service.buscarPorId(id);
            model.addAttribute("oc",                 oc);
            model.addAttribute("estados",            EstadoOrdenCompra.values());
            model.addAttribute("tiposItem",          TipoItemFactura.values());
            model.addAttribute("transicionesValidas", service.transicionesValidas(oc.getEstado()));
            return "ordenes-compra/detalle";
        } catch (Exception e) {
            flash.addFlashAttribute("mensaje",     "Orden no encontrada.");
            flash.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/ordenes-compra";
        }
    }

    // ── Cambiar estado ────────────────────────────────────────────────────

    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable Long id,
                                @RequestParam EstadoOrdenCompra nuevoEstado,
                                RedirectAttributes flash) {
        try {
            OrdenCompra oc = service.cambiarEstado(id, nuevoEstado);
            flash.addFlashAttribute("mensaje",     "Estado actualizado a: " + oc.getEstado().getDisplayName());
            flash.addFlashAttribute("tipoMensaje", "success");
        } catch (Exception e) {
            flash.addFlashAttribute("mensaje",     "Error: " + e.getMessage());
            flash.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/ordenes-compra/ver/" + id;
    }

    // ── Convertir a factura ───────────────────────────────────────────────

    @PostMapping("/{id}/convertir")
    public String convertir(@PathVariable Long id, RedirectAttributes flash) {
        try {
            Long facturaId = service.convertirAFactura(id, facturaService);
            flash.addFlashAttribute("mensaje",     "Factura creada. Completa el número y fecha de la factura real.");
            flash.addFlashAttribute("tipoMensaje", "success");
            return "redirect:/facturas/editar/" + facturaId;
        } catch (Exception e) {
            flash.addFlashAttribute("mensaje",     "Error al convertir: " + e.getMessage());
            flash.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/ordenes-compra/ver/" + id;
        }
    }

    // ── Eliminar ──────────────────────────────────────────────────────────

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes flash) {
        try {
            service.eliminar(id);
            flash.addFlashAttribute("mensaje",     "Orden eliminada.");
            flash.addFlashAttribute("tipoMensaje", "success");
        } catch (Exception e) {
            flash.addFlashAttribute("mensaje",     "Error: " + e.getMessage());
            flash.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/ordenes-compra";
    }

    // ── PDF ───────────────────────────────────────────────────────────────

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id, HttpServletRequest request, Locale locale) {
        OrdenCompra oc = service.buscarPorId(id);
        Tenant tenant = (Tenant) request.getAttribute("currentTenant");
        ExportBranding branding = (tenant != null) ? ExportBranding.from(tenant) : ExportBranding.defaults("Zymos");
        byte[] bytes = pdfExportService.generarPdfOrdenCompra(oc, branding, locale);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"oc-" + (oc.getNumeroOc() != null ? oc.getNumeroOc() : id) + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    // ── Helpers privados ──────────────────────────────────────────────────

    private void agregarDatosFormulario(Model model) {
        List<String> tiposInsumo = categoriaInsumoService.listarNombresActivos();
        List<String> tiposEquipo = categoriaEquipoService.listarNombresActivos();
        model.addAttribute("proveedores",  proveedorService.listarActivos());
        model.addAttribute("tiposInsumo",  tiposInsumo);
        model.addAttribute("tiposEquipo",  tiposEquipo);
        model.addAttribute("tiposItem",    TipoItemFactura.values());

        List<InsumoInventario> todosInsumos = insumoRepo.findAllByOrderByNombreAsc();
        Map<String, List<String>> insumosPorTipo = new LinkedHashMap<>();
        for (String tipo : tiposInsumo) {
            insumosPorTipo.put(tipo, todosInsumos.stream()
                    .filter(i -> tipo.equals(i.getTipo()))
                    .map(InsumoInventario::getNombre)
                    .toList());
        }

        List<Equipo> todosEquipos = equipoRepo.findAll();
        Map<String, List<String>> equiposPorTipo = new LinkedHashMap<>();
        for (String tipo : tiposEquipo) {
            equiposPorTipo.put(tipo, todosEquipos.stream()
                    .filter(e -> tipo.equals(e.getTipo()))
                    .map(Equipo::getNombre)
                    .toList());
        }
        model.addAttribute("insumosPorTipo", insumosPorTipo);
        model.addAttribute("equiposPorTipo", equiposPorTipo);
    }

    private OrdenCompraFormDto toFormDto(OrdenCompra oc) {
        OrdenCompraFormDto dto = new OrdenCompraFormDto();
        dto.setProveedor(oc.getProveedor());
        if (oc.getProveedorRef() != null) dto.setProveedorId(oc.getProveedorRef().getId());
        dto.setFechaEmision(oc.getFechaEmision());
        dto.setFechaRequerida(oc.getFechaRequerida());
        dto.setNotas(oc.getNotas());
        oc.getItems().forEach(item -> {
            var d = new com.alera.dto.OrdenCompraItemDto();
            d.setTipoItem(item.getTipoItem());
            d.setNombre(item.getNombre());
            d.setDescripcion(item.getDescripcion());
            d.setCantidad(item.getCantidad());
            d.setUnidad(item.getUnidad());
            d.setPrecioUnitarioEstimado(item.getPrecioUnitarioEstimado());
            d.setPorcentajeIvaItem(item.getPorcentajeIvaItem());
            d.setTipoInsumo(item.getTipoInsumo());
            d.setTipoEquipo(item.getTipoEquipo());
            dto.getItems().add(d);
        });
        return dto;
    }
}
