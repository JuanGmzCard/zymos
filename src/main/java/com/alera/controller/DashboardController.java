package com.alera.controller;

import com.alera.dto.DashboardStats;
import com.alera.service.DashboardService;
import com.alera.service.InsumoInventarioService;
import com.alera.service.PlanificacionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final InsumoInventarioService insumoService;
    private final PlanificacionService planificacionService;

    public DashboardController(DashboardService dashboardService,
                                InsumoInventarioService insumoService,
                                PlanificacionService planificacionService) {
        this.dashboardService = dashboardService;
        this.insumoService = insumoService;
        this.planificacionService = planificacionService;
    }

    @GetMapping
    public String dashboard(Model model) {
        DashboardStats stats = dashboardService.obtenerEstadisticas();
        model.addAttribute("stats", stats);
        model.addAttribute("totalLotes",            stats.getTotalLotes());
        model.addAttribute("enProceso",             stats.getEnProceso());
        model.addAttribute("completados",           stats.getCompletados());
        model.addAttribute("estilosDistintos",      stats.getEstilosDistintos());
        model.addAttribute("totalInsumos",          stats.getTotalInsumos());
        model.addAttribute("bajoStock",             stats.getBajoStock());
        model.addAttribute("proximosAVencer",       stats.getProximosAVencer());
        model.addAttribute("totalEquipos",          stats.getTotalEquipos());
        model.addAttribute("equiposMantenimiento",  stats.getEquiposMantenimiento());
        model.addAttribute("mantenimientoPendiente",stats.getMantenimientoPendiente());
        model.addAttribute("totalFacturas",         stats.getTotalFacturas());
        model.addAttribute("totalGastado",          stats.getTotalGastado());
        model.addAttribute("totalMantenimientos",   stats.getTotalMantenimientos());
        model.addAttribute("ultimosLotes",          stats.getUltimosLotes());
        model.addAttribute("chartLitrosMes",  dashboardService.getLitrosPorMes());
        model.addAttribute("chartEstilos",    dashboardService.getLotesPorEstilo());
        model.addAttribute("alertasBajoStock",    insumoService.listarBajoStock());
        model.addAttribute("alertasProxVencer",   insumoService.listarProximosAVencer(30));
        var proximas = planificacionService.listarProximas();
        model.addAttribute("proximasElaboraciones",
            proximas.size() > 5 ? proximas.subList(0, 5) : proximas);
        return "dashboard";
    }
}
