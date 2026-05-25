package com.alera.service;

import com.alera.config.TenantContext;
import com.alera.dto.DashboardStats;
import com.alera.model.enums.EstadoEquipo;
import com.alera.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final LoteCervezaRepository loteRepo;
    private final InsumoInventarioRepository insumoRepo;
    private final EquipoRepository equipoRepo;
    private final FacturaProveedorRepository facturaRepo;
    private final MantenimientoEquipoRepository mantenimientoRepo;

    public DashboardService(LoteCervezaRepository loteRepo,
                             InsumoInventarioRepository insumoRepo,
                             EquipoRepository equipoRepo,
                             FacturaProveedorRepository facturaRepo,
                             MantenimientoEquipoRepository mantenimientoRepo) {
        this.loteRepo = loteRepo;
        this.insumoRepo = insumoRepo;
        this.equipoRepo = equipoRepo;
        this.facturaRepo = facturaRepo;
        this.mantenimientoRepo = mantenimientoRepo;
    }

    @Cacheable(value = "dashboard-litros-mes", key = "T(com.alera.config.TenantContext).getCurrentTenant()")
    public Map<String, Number> getLitrosPorMes() {
        String tenantId = TenantContext.getCurrentTenant();
        LocalDate desde = LocalDate.now().minusMonths(5).withDayOfMonth(1);
        List<Object[]> raw = loteRepo.findLitrosPorMes(desde, tenantId);

        Map<String, Number> result = new LinkedHashMap<>();
        LocalDate now = LocalDate.now();
        for (int i = 5; i >= 0; i--) {
            LocalDate mes = now.minusMonths(i).withDayOfMonth(1);
            result.put(labelMes(mes), 0);
        }
        for (Object[] row : raw) {
            int year  = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            Number litros = (Number) row[2];
            String label = labelMes(LocalDate.of(year, month, 1));
            if (result.containsKey(label)) result.put(label, litros);
        }
        return result;
    }

    @Cacheable(value = "dashboard-estilos", key = "T(com.alera.config.TenantContext).getCurrentTenant()")
    public Map<String, Long> getLotesPorEstilo() {
        String tenantId = TenantContext.getCurrentTenant();
        List<Object[]> raw = loteRepo.findLotesPorEstilo(tenantId);
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : raw) {
            result.put((String) row[0], ((Number) row[1]).longValue());
        }
        return result;
    }

    private String labelMes(LocalDate date) {
        return date.getMonth().getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("es"))
               + " " + date.getYear();
    }

    @Cacheable(value = "dashboard-stats", key = "T(com.alera.config.TenantContext).getCurrentTenant()")
    public DashboardStats obtenerEstadisticas() {
        log.debug("Calculando estadísticas del dashboard");

        LocalDate hoy = LocalDate.now();

        BigDecimal totalGastado = facturaRepo.sumTotalFacturas();
        BigDecimal totalMant    = mantenimientoRepo.sumTotalCostos();

        return new DashboardStats()
                // Lotes — todo a nivel de BD
                .totalLotes(loteRepo.count())
                .enProceso(loteRepo.countEnProceso())
                .completados(loteRepo.countCompletados())
                .estilosDistintos(loteRepo.countDistinctEstilos())
                // Inventario — COUNT queries en lugar de findAll().size()
                .totalInsumos(insumoRepo.count())
                .bajoStock(insumoRepo.countBajoStock())
                .proximosAVencer(insumoRepo.countProximosAVencer(hoy.plusDays(30)))
                // Equipos — COUNT queries en lugar de findList().size()
                .totalEquipos(equipoRepo.count())
                .equiposMantenimiento(equipoRepo.countByEstado(EstadoEquipo.MANTENIMIENTO))
                .mantenimientoPendiente(equipoRepo.countMantenimientoPendiente(hoy.plusDays(7)))
                // Financiero
                .totalFacturas(facturaRepo.countTotal())
                .totalGastado(totalGastado  != null ? totalGastado : BigDecimal.ZERO)
                .totalMantenimientos(totalMant != null ? totalMant : BigDecimal.ZERO)
                // Últimos 5 lotes — LIMIT a nivel de BD, no stream().limit()
                .ultimosLotes(loteRepo.findTop5(PageRequest.of(0, 5)));
    }
}
