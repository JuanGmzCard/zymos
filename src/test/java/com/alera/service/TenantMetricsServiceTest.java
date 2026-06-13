package com.alera.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantMetricsServiceTest {

    @Mock JdbcTemplate jdbc;
    TenantMetricsService service;

    @BeforeEach
    void setup() {
        service = new TenantMetricsService(jdbc);
    }

    @Test
    void obtener_retornaTenantMetricsCompleto() {
        String tid = "cerveceria1";
        LocalDateTime ultimoAcceso = LocalDateTime.of(2026, 6, 1, 8, 0);

        // Producción
        when(jdbc.queryForObject(contains("COUNT(*) FROM lotes_cerveza WHERE tenant_id = ?"), eq(Long.class), eq(tid)))
            .thenReturn(10L);
        when(jdbc.queryForObject(contains("ferm_fecha_inicial IS NOT NULL"), eq(Long.class), eq(tid)))
            .thenReturn(3L);
        when(jdbc.queryForObject(contains("carb_fecha_final IS NOT NULL"), eq(Long.class), eq(tid)))
            .thenReturn(5L);
        when(jdbc.queryForObject(contains("SUM(litros_finales)"), eq(BigDecimal.class), eq(tid)))
            .thenReturn(new BigDecimal("850.00"));

        // Ventas
        when(jdbc.queryForObject(contains("COUNT(*) FROM ventas"), eq(Long.class), eq(tid)))
            .thenReturn(20L);
        when(jdbc.queryForObject(contains("SUM(vi.cantidad"), eq(BigDecimal.class), eq(tid)))
            .thenReturn(new BigDecimal("5000000.00"));
        when(jdbc.queryForObject(contains("COUNT(*) FROM clientes"), eq(Long.class), eq(tid)))
            .thenReturn(8L);

        // Compras
        when(jdbc.queryForObject(contains("COUNT(*) FROM facturas_proveedor"), eq(Long.class), eq(tid)))
            .thenReturn(15L);
        when(jdbc.queryForObject(contains("SUM(valor_total)"), eq(BigDecimal.class), eq(tid)))
            .thenReturn(new BigDecimal("2000000.00"));

        // Inventario
        when(jdbc.queryForObject(contains("COUNT(*) FROM insumos_inventario WHERE tenant_id = ?"), eq(Long.class), eq(tid)))
            .thenReturn(40L);
        when(jdbc.queryForObject(contains("stock_minimo IS NOT NULL"), eq(Long.class), eq(tid)))
            .thenReturn(5L);
        when(jdbc.queryForObject(contains("COUNT(*) FROM equipos"), eq(Long.class), eq(tid)))
            .thenReturn(12L);

        // Sistema
        when(jdbc.queryForObject(contains("COUNT(*) FROM usuarios"), eq(Long.class), eq(tid)))
            .thenReturn(4L);
        when(jdbc.queryForObject(contains("MAX(fecha) FROM log_accesos"), eq(LocalDateTime.class), eq(tid)))
            .thenReturn(ultimoAcceso);

        TenantMetricsService.TenantMetrics m = service.obtener(tid);

        assertThat(m.totalLotes()).isEqualTo(10L);
        assertThat(m.lotesEnProceso()).isEqualTo(3L);
        assertThat(m.lotesCompletados()).isEqualTo(5L);
        assertThat(m.litrosTotales()).isEqualByComparingTo("850.00");
        assertThat(m.totalVentas()).isEqualTo(20L);
        assertThat(m.ingresosVentas()).isEqualByComparingTo("5000000.00");
        assertThat(m.totalClientes()).isEqualTo(8L);
        assertThat(m.totalFacturas()).isEqualTo(15L);
        assertThat(m.totalGastado()).isEqualByComparingTo("2000000.00");
        assertThat(m.totalInsumos()).isEqualTo(40L);
        assertThat(m.bajoStock()).isEqualTo(5L);
        assertThat(m.totalEquipos()).isEqualTo(12L);
        assertThat(m.totalUsuarios()).isEqualTo(4L);
        assertThat(m.ultimoAcceso()).isEqualTo(ultimoAcceso);
    }

    @Test
    void obtener_nullsEnBd_retornaCerosYNull() {
        String tid = "nuevo";

        // Todos los count retornan null → 0
        when(jdbc.queryForObject(contains("COUNT(*)"), eq(Long.class), eq(tid)))
            .thenReturn(null);
        when(jdbc.queryForObject(contains("SUM("), eq(BigDecimal.class), eq(tid)))
            .thenReturn(null);
        when(jdbc.queryForObject(contains("MAX(fecha)"), eq(LocalDateTime.class), eq(tid)))
            .thenReturn(null);

        TenantMetricsService.TenantMetrics m = service.obtener(tid);

        assertThat(m.totalLotes()).isZero();
        assertThat(m.litrosTotales()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(m.ultimoAcceso()).isNull();
    }
}
