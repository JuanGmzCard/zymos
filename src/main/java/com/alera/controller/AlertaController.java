package com.alera.controller;

import com.alera.dto.AlertaContadores;
import com.alera.repository.InsumoInventarioRepository;
import com.alera.service.AlertaScheduler;
import com.alera.service.EquipoService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/alertas")
public class AlertaController {

    private final InsumoInventarioRepository insumoRepo;
    private final EquipoService              equipoService;
    private final AlertaScheduler            scheduler;

    public AlertaController(InsumoInventarioRepository insumoRepo,
                             EquipoService equipoService,
                             AlertaScheduler scheduler) {
        this.insumoRepo    = insumoRepo;
        this.equipoService = equipoService;
        this.scheduler     = scheduler;
    }

    /** Contadores para el badge del navbar — accesible a cualquier usuario autenticado. */
    @GetMapping(value = "/contadores", produces = MediaType.APPLICATION_JSON_VALUE)
    public AlertaContadores contadores() {
        int bajoStock    = (int) insumoRepo.countBajoStock();
        int vencimientos = (int) insumoRepo.countProximosAVencer(
                LocalDate.now().plusDays(30));
        int mantenimiento = equipoService.listarMantenimientoPendiente().size();
        return new AlertaContadores(bajoStock, vencimientos, mantenimiento);
    }

    /** Fuerza la ejecución del scheduler de alertas — solo ADMIN. */
    @PostMapping(value = "/ejecutar", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> ejecutar() {
        scheduler.enviarAlertasDiarias();
        return Map.of("success", true);
    }
}
