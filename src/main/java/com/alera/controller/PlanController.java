package com.alera.controller;

import com.alera.config.TenantContext;
import com.alera.repository.LoteCervezaRepository;
import com.alera.repository.TenantRepository;
import com.alera.repository.UsuarioRepository;
import com.alera.repository.VentaItemRepository;
import com.alera.service.StockService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Controller
public class PlanController {

    private final TenantRepository   tenantRepo;
    private final LoteCervezaRepository loteRepo;
    private final UsuarioRepository  usuarioRepo;
    private final StockService       stockService;

    public PlanController(TenantRepository tenantRepo,
                          LoteCervezaRepository loteRepo,
                          UsuarioRepository usuarioRepo,
                          StockService stockService) {
        this.tenantRepo  = tenantRepo;
        this.loteRepo    = loteRepo;
        this.usuarioRepo = usuarioRepo;
        this.stockService = stockService;
    }

    @GetMapping("/plan-vencido")
    public String planVencido() {
        return "plan/vencido";
    }

    @GetMapping("/mi-plan")
    public String miPlan(Model model) {
        String tenantId = TenantContext.getCurrentTenant();
        var tenant = tenantRepo.findById(tenantId).orElse(null);
        if (tenant == null) return "redirect:/";

        long totalLotes    = loteRepo.count();
        long totalUsuarios = usuarioRepo.countByTenantId(tenantId);

        Integer maxLotes    = tenant.getMaxLotes();
        Integer maxUsuarios = tenant.getMaxUsuarios();
        Integer pctLotes    = (maxLotes    != null && maxLotes    > 0)
                ? (int) Math.min(totalLotes    * 100L / maxLotes,    100) : null;
        Integer pctUsuarios = (maxUsuarios != null && maxUsuarios > 0)
                ? (int) Math.min(totalUsuarios * 100L / maxUsuarios, 100) : null;

        Long diasRestantes = null;
        if (tenant.getPlanFin() != null) {
            diasRestantes = ChronoUnit.DAYS.between(LocalDate.now(), tenant.getPlanFin());
        }

        // Stock total disponible (sumatoria de litros en lotes completados)
        var totalStockLitros = stockService.getTotalDisponibleLitros();

        model.addAttribute("tenant",          tenant);
        model.addAttribute("totalLotes",      totalLotes);
        model.addAttribute("totalUsuarios",   totalUsuarios);
        model.addAttribute("pctLotes",        pctLotes);
        model.addAttribute("pctUsuarios",     pctUsuarios);
        model.addAttribute("diasRestantes",   diasRestantes);
        model.addAttribute("totalStock",      totalStockLitros);
        return "plan/mi-plan";
    }
}
