package com.alera.controller;

import com.alera.dto.AlertaContadores;
import com.alera.repository.InsumoInventarioRepository;
import com.alera.service.EquipoService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/alertas")
public class AlertaController {

    private final InsumoInventarioRepository insumoRepo;
    private final EquipoService equipoService;

    public AlertaController(InsumoInventarioRepository insumoRepo,
                             EquipoService equipoService) {
        this.insumoRepo    = insumoRepo;
        this.equipoService = equipoService;
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
}
