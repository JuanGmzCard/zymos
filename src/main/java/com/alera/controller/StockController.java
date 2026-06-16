package com.alera.controller;

import com.alera.service.StockService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/stock")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping
    public String index(Model model) {
        var stocks = stockService.listarStock();
        model.addAttribute("stocks",          stocks);
        model.addAttribute("totalLotes",       stocks.size());
        model.addAttribute("lotesConStock",    stockService.countLotesConStock());
        model.addAttribute("lotesAgotados",    stockService.countLotesAgotados());
        model.addAttribute("totalDisponible",  stockService.getTotalDisponibleLitros());
        return "stock/index";
    }

    @GetMapping("/ajustes/{loteId}")
    @ResponseBody
    public Object listarAjustes(@PathVariable Long loteId) {
        return stockService.listarAjustesPorLote(loteId);
    }

    @PostMapping("/ajustar/{loteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCCION', 'SUPERADMIN')")
    public String ajustar(@PathVariable Long loteId,
                          @RequestParam BigDecimal cantidad,
                          @RequestParam(defaultValue = "L") String unidad,
                          @RequestParam String motivo,
                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                          Authentication auth,
                          RedirectAttributes ra) {
        try {
            stockService.registrarAjuste(loteId, cantidad, unidad, motivo, fecha, auth.getName());
            ra.addFlashAttribute("mensaje", "Ajuste registrado correctamente.");
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("mensaje", "Error al registrar ajuste: " + e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/stock";
    }

    @PostMapping("/ajuste/eliminar/{ajusteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public String eliminarAjuste(@PathVariable Long ajusteId, RedirectAttributes ra) {
        stockService.eliminarAjuste(ajusteId);
        ra.addFlashAttribute("mensaje", "Ajuste eliminado.");
        ra.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/stock";
    }
}
