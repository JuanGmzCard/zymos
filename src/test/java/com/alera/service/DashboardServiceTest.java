package com.alera.service;

import com.alera.dto.DashboardStats;
import com.alera.model.enums.EstadoEquipo;
import com.alera.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService")
class DashboardServiceTest {

    @Mock private LoteCervezaRepository loteRepo;
    @Mock private InsumoInventarioRepository insumoRepo;
    @Mock private EquipoRepository equipoRepo;
    @Mock private FacturaProveedorRepository facturaRepo;
    @Mock private MantenimientoEquipoRepository mantenimientoRepo;

    @InjectMocks
    private DashboardService service;

    @Test
    @DisplayName("obtenerEstadisticas usa COUNT queries — no carga listas completas")
    void obtenerEstadisticas_usaCountQueries() {
        // Stubbing de todos los COUNT a nivel de BD
        when(loteRepo.count()).thenReturn(10L);
        when(loteRepo.countEnProceso()).thenReturn(6L);
        when(loteRepo.countCompletados()).thenReturn(4L);
        when(loteRepo.countDistinctEstilos()).thenReturn(3L);
        when(insumoRepo.count()).thenReturn(20L);
        when(insumoRepo.countBajoStock()).thenReturn(2L);
        when(insumoRepo.countProximosAVencer(any(LocalDate.class))).thenReturn(1L);
        when(equipoRepo.count()).thenReturn(5L);
        when(equipoRepo.countByEstado(EstadoEquipo.MANTENIMIENTO)).thenReturn(1L);
        when(equipoRepo.countMantenimientoPendiente(any(LocalDate.class))).thenReturn(2L);
        when(facturaRepo.countTotal()).thenReturn(8L);
        when(facturaRepo.sumTotalFacturas()).thenReturn(new BigDecimal("1500000"));
        when(mantenimientoRepo.sumTotalCostos()).thenReturn(new BigDecimal("250000"));
        when(loteRepo.findTop5(any(Pageable.class))).thenReturn(List.of());

        DashboardStats stats = service.obtenerEstadisticas();

        assertThat(stats.getTotalLotes()).isEqualTo(10);
        assertThat(stats.getEnProceso()).isEqualTo(6);
        assertThat(stats.getCompletados()).isEqualTo(4);
        assertThat(stats.getEstilosDistintos()).isEqualTo(3);
        assertThat(stats.getBajoStock()).isEqualTo(2);
        assertThat(stats.getProximosAVencer()).isEqualTo(1);
        assertThat(stats.getMantenimientoPendiente()).isEqualTo(2);
        assertThat(stats.getTotalGastado()).isEqualByComparingTo(new BigDecimal("1500000"));
    }

    @Test
    @DisplayName("obtenerEstadisticas maneja sumas null como cero")
    void obtenerEstadisticas_sumasNullSonCero() {
        when(loteRepo.count()).thenReturn(0L);
        when(loteRepo.countEnProceso()).thenReturn(0L);
        when(loteRepo.countCompletados()).thenReturn(0L);
        when(loteRepo.countDistinctEstilos()).thenReturn(0L);
        when(insumoRepo.count()).thenReturn(0L);
        when(insumoRepo.countBajoStock()).thenReturn(0L);
        when(insumoRepo.countProximosAVencer(any())).thenReturn(0L);
        when(equipoRepo.count()).thenReturn(0L);
        when(equipoRepo.countByEstado(any())).thenReturn(0L);
        when(equipoRepo.countMantenimientoPendiente(any())).thenReturn(0L);
        when(facturaRepo.countTotal()).thenReturn(0L);
        when(facturaRepo.sumTotalFacturas()).thenReturn(null);      // BD vacía → null
        when(mantenimientoRepo.sumTotalCostos()).thenReturn(null);  // BD vacía → null
        when(loteRepo.findTop5(any())).thenReturn(List.of());

        DashboardStats stats = service.obtenerEstadisticas();

        assertThat(stats.getTotalGastado()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(stats.getTotalMantenimientos()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
