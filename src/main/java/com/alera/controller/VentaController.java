package com.alera.controller;

import com.alera.config.ExportBranding;
import com.alera.dto.VentaFormDto;
import com.alera.dto.VentaItemFormDto;
import com.alera.model.Tenant;
import com.alera.model.VentaItem;
import com.alera.model.enums.EstadoVenta;
import com.alera.service.ClienteService;
import com.alera.service.ExcelExportService;
import com.alera.service.PdfExportService;
import com.alera.service.TrazabilidadService;
import com.alera.service.VentaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/ventas")
public class VentaController {

    private final VentaService service;
    private final TrazabilidadService trazabilidadService;
    private final ExcelExportService excelExportService;
    private final PdfExportService pdfExportService;
    private final ClienteService clienteService;

    public VentaController(VentaService service,
                           TrazabilidadService trazabilidadService,
                           ExcelExportService excelExportService,
                           PdfExportService pdfExportService,
                           ClienteService clienteService) {
        this.service              = service;
        this.trazabilidadService  = trazabilidadService;
        this.excelExportService   = excelExportService;
        this.pdfExportService     = pdfExportService;
        this.clienteService       = clienteService;
    }

    @GetMapping
    public String lista(@RequestParam(defaultValue = "0") int page,
                        @RequestParam(required = false) EstadoVenta estado,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                        Model model) {
        var pagina = service.listarPaginado(estado, desde, hasta, page);
        model.addAttribute("ventas",          pagina.getContent());
        model.addAttribute("paginaActual",    page);
        model.addAttribute("totalPaginas",    pagina.getTotalPages());
        model.addAttribute("totalVentas",     pagina.getTotalElements());
        model.addAttribute("estadoFiltro",    estado);
        model.addAttribute("desde",           desde);
        model.addAttribute("hasta",           hasta);
        model.addAttribute("estados",         EstadoVenta.values());
        model.addAttribute("statsTotal",      service.countTotal());
        model.addAttribute("statsPendientes", service.countByEstado(EstadoVenta.PENDIENTE));
        model.addAttribute("statsClientes",   service.countClientesUnicos());
        model.addAttribute("statsIngresos",   service.sumIngresosDespachados());
        model.addAttribute("topClientes",     service.topClientes());
        return "ventas/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(@RequestParam(required = false) Long loteId, Model model) {
        VentaFormDto dto = new VentaFormDto();
        VentaItemFormDto primerItem = new VentaItemFormDto();
        if (loteId != null) {
            primerItem.setLoteId(loteId);
            trazabilidadService.buscarPorIdOpcional(loteId).ifPresent(lote ->
                primerItem.setCodigoLoteBuscador(lote.getCodigoLote()));
        }
        dto.getItems().add(primerItem);
        model.addAttribute("ventaForm", dto);
        model.addAttribute("estados", EstadoVenta.values());
        return "ventas/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("ventaForm") VentaFormDto dto,
                          BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("estados", EstadoVenta.values());
            return "ventas/formulario";
        }
        try {
            String advertencia = service.validarCantidadDisponible(dto.getItems(), null);
            var venta = service.guardar(dto);
            if (advertencia != null) {
                ra.addFlashAttribute("mensaje", advertencia);
                ra.addFlashAttribute("tipoMensaje", "warning");
            } else {
                ra.addFlashAttribute("mensaje", "Venta registrada correctamente");
                ra.addFlashAttribute("tipoMensaje", "success");
            }
            return "redirect:/ventas/ver/" + venta.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("mensaje", "Error al registrar la venta: " + e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/ventas";
        }
    }

    @GetMapping("/ver/{id}")
    public String ver(@PathVariable Long id, Model model, RedirectAttributes ra) {
        var venta = service.buscarPorId(id).orElse(null);
        if (venta == null) {
            ra.addFlashAttribute("mensaje", "La venta no existe o fue eliminada");
            ra.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/ventas";
        }
        model.addAttribute("venta",    venta);
        model.addAttribute("estados",  EstadoVenta.values());
        model.addAttribute("historial", service.listarHistorial(id));
        return "ventas/detalle";
    }

    @GetMapping("/duplicar/{id}")
    public String duplicar(@PathVariable Long id, Model model, RedirectAttributes ra) {
        var venta = service.buscarPorId(id).orElse(null);
        if (venta == null) {
            ra.addFlashAttribute("mensaje", "Venta no encontrada");
            ra.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/ventas";
        }
        VentaFormDto dto = new VentaFormDto();
        dto.setCliente(venta.getCliente());
        dto.setClienteId(venta.getClienteRef() != null ? venta.getClienteRef().getId() : null);
        dto.setNotas(venta.getNotas());
        dto.setEstado(EstadoVenta.PENDIENTE);
        for (VentaItem item : venta.getItems()) {
            VentaItemFormDto itemDto = new VentaItemFormDto();
            itemDto.setLoteId(item.getLote() != null ? item.getLote().getId() : null);
            itemDto.setCodigoLoteBuscador(item.getCodigoLote());
            itemDto.setDescripcion(item.getDescripcion());
            itemDto.setCantidad(item.getCantidad());
            itemDto.setUnidad(item.getUnidad());
            itemDto.setPrecioUnitario(item.getPrecioUnitario());
            itemDto.setDescuentoPct(item.getDescuentoPct());
            dto.getItems().add(itemDto);
        }
        if (dto.getItems().isEmpty()) dto.getItems().add(new VentaItemFormDto());
        model.addAttribute("ventaForm", dto);
        model.addAttribute("estados",   EstadoVenta.values());
        model.addAttribute("duplicadoDe", id);
        return "ventas/formulario";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes ra) {
        var venta = service.buscarPorId(id).orElse(null);
        if (venta == null) {
            ra.addFlashAttribute("mensaje", "La venta no existe o fue eliminada");
            ra.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/ventas";
        }
        VentaFormDto dto = new VentaFormDto();
        dto.setId(venta.getId());
        dto.setCliente(venta.getCliente());
        dto.setClienteId(venta.getClienteRef() != null ? venta.getClienteRef().getId() : null);
        dto.setFechaDespacho(venta.getFechaDespacho());
        dto.setCotizacionExpiraEn(venta.getCotizacionExpiraEn());
        dto.setNotas(venta.getNotas());
        dto.setEstado(venta.getEstado());
        for (VentaItem item : venta.getItems()) {
            VentaItemFormDto itemDto = new VentaItemFormDto();
            itemDto.setLoteId(item.getLote() != null ? item.getLote().getId() : null);
            itemDto.setCodigoLoteBuscador(item.getCodigoLote());
            itemDto.setDescripcion(item.getDescripcion());
            itemDto.setCantidad(item.getCantidad());
            itemDto.setUnidad(item.getUnidad());
            itemDto.setPrecioUnitario(item.getPrecioUnitario());
            itemDto.setDescuentoPct(item.getDescuentoPct());
            dto.getItems().add(itemDto);
        }
        if (dto.getItems().isEmpty()) {
            dto.getItems().add(new VentaItemFormDto());
        }
        model.addAttribute("ventaForm", dto);
        model.addAttribute("ventaId", id);
        model.addAttribute("estados", EstadoVenta.values());
        return "ventas/formulario";
    }

    @PostMapping("/actualizar/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("ventaForm") VentaFormDto dto,
                             BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("estados", EstadoVenta.values());
            model.addAttribute("ventaId", id);
            return "ventas/formulario";
        }
        try {
            String advertencia = service.validarCantidadDisponible(dto.getItems(), id);
            service.actualizar(id, dto);
            if (advertencia != null) {
                ra.addFlashAttribute("mensaje", advertencia);
                ra.addFlashAttribute("tipoMensaje", "warning");
            } else {
                ra.addFlashAttribute("mensaje", "Venta actualizada correctamente");
                ra.addFlashAttribute("tipoMensaje", "success");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("mensaje", "Error al actualizar: " + e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/ventas/ver/" + id;
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            service.eliminar(id);
            ra.addFlashAttribute("mensaje", "Venta eliminada");
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("mensaje", "Error al eliminar: " + e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/ventas";
    }

    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable Long id,
                                @RequestParam EstadoVenta estado,
                                RedirectAttributes ra) {
        try {
            service.cambiarEstado(id, estado);
            ra.addFlashAttribute("mensaje", "Estado actualizado a " + estado.getDisplayName());
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("mensaje", e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/ventas/ver/" + id;
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id, HttpServletRequest request) {
        var venta = service.buscarPorId(id).orElse(null);
        if (venta == null) return ResponseEntity.notFound().build();
        Tenant tenant = (Tenant) request.getAttribute("currentTenant");
        ExportBranding branding = ExportBranding.from(tenant);
        byte[] bytes = pdfExportService.generarPdfVenta(venta, branding);
        String filename = "remision-venta-" + id + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(bytes);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) EstadoVenta estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            HttpServletRequest request) {

        var ventas = service.listarParaExport(estado, desde, hasta);
        Tenant tenant = (Tenant) request.getAttribute("currentTenant");
        ExportBranding branding = ExportBranding.from(tenant);

        byte[] excel = excelExportService.generarExcelVentas(ventas, estado, desde, hasta, branding);
        String filename = "ventas-" + LocalDate.now() + ".xlsx";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(excel);
    }

    @GetMapping(value = "/suggest", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Map<String, Object>> suggest(@RequestParam(defaultValue = "") String q) {
        return service.suggest(q);
    }

    @GetMapping(value = "/suggest-lotes", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Map<String, Object>> suggestLotes(@RequestParam(defaultValue = "") String q) {
        return service.suggestLotesParaVenta(q);
    }

    @GetMapping(value = "/suggest-clientes", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<String> suggestClientes(@RequestParam(defaultValue = "") String q) {
        return service.suggestClientes(q);
    }

    @PostMapping(value = "/{id}/despachar", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> despacharRapido(@PathVariable Long id) {
        try {
            service.cambiarEstado(id, EstadoVenta.DESPACHADO);
            return Map.of(
                "success",     true,
                "displayName", EstadoVenta.DESPACHADO.getDisplayName(),
                "badgeClass",  EstadoVenta.DESPACHADO.getBadgeClass()
            );
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }
}
