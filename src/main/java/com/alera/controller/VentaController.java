package com.alera.controller;

import com.alera.dto.VentaFormDto;
import com.alera.model.enums.EstadoVenta;
import com.alera.service.TrazabilidadService;
import com.alera.service.VentaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
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

    public VentaController(VentaService service, TrazabilidadService trazabilidadService) {
        this.service = service;
        this.trazabilidadService = trazabilidadService;
    }

    @GetMapping
    public String lista(@RequestParam(defaultValue = "0") int page,
                        @RequestParam(required = false) EstadoVenta estado,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                        Model model) {
        var pagina = service.listarPaginado(estado, desde, hasta, page);
        model.addAttribute("ventas",        pagina.getContent());
        model.addAttribute("paginaActual",  page);
        model.addAttribute("totalPaginas",  pagina.getTotalPages());
        model.addAttribute("totalVentas",   pagina.getTotalElements());
        model.addAttribute("estadoFiltro",  estado);
        model.addAttribute("desde",         desde);
        model.addAttribute("hasta",         hasta);
        model.addAttribute("estados",       EstadoVenta.values());
        model.addAttribute("statsTotal",    service.countTotal());
        model.addAttribute("statsPendientes", service.countByEstado(EstadoVenta.PENDIENTE));
        model.addAttribute("statsClientes", service.countClientesUnicos());
        model.addAttribute("statsIngresos", service.sumIngresosDespachados());
        return "ventas/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(@RequestParam(required = false) Long loteId, Model model) {
        VentaFormDto dto = new VentaFormDto();
        if (loteId != null) {
            dto.setLoteId(loteId);
            trazabilidadService.buscarPorIdOpcional(loteId).ifPresent(lote -> {
                dto.setCodigoLoteBuscador(lote.getCodigoLote());
            });
        }
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
            var venta = service.guardar(dto);
            ra.addFlashAttribute("mensaje", "Venta registrada correctamente");
            ra.addFlashAttribute("tipoMensaje", "success");
            return "redirect:/ventas/ver/" + venta.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("mensaje", "Error al registrar la venta: " + e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/ventas";
        }
    }

    @GetMapping("/ver/{id}")
    public String ver(@PathVariable Long id, Model model) {
        var venta = service.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada: " + id));
        model.addAttribute("venta", venta);
        model.addAttribute("estados", EstadoVenta.values());
        return "ventas/detalle";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        var venta = service.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada: " + id));
        VentaFormDto dto = new VentaFormDto();
        dto.setId(venta.getId());
        dto.setLoteId(venta.getLote() != null ? venta.getLote().getId() : null);
        dto.setCodigoLoteBuscador(venta.getCodigoLote());
        dto.setCliente(venta.getCliente());
        dto.setFechaDespacho(venta.getFechaDespacho());
        dto.setCantidad(venta.getCantidad());
        dto.setUnidad(venta.getUnidad());
        dto.setPrecioUnitario(venta.getPrecioUnitario());
        dto.setDescuentoPct(venta.getDescuentoPct());
        dto.setNotas(venta.getNotas());
        dto.setEstado(venta.getEstado());
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
            service.actualizar(id, dto);
            ra.addFlashAttribute("mensaje", "Venta actualizada correctamente");
            ra.addFlashAttribute("tipoMensaje", "success");
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
        service.cambiarEstado(id, estado);
        ra.addFlashAttribute("mensaje", "Estado actualizado a " + estado.getDisplayName());
        ra.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/ventas/ver/" + id;
    }

    @GetMapping(value = "/suggest", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Map<String, Object>> suggest(@RequestParam(defaultValue = "") String q) {
        return service.suggest(q);
    }
}
