package com.alera.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class TenantMetricsService {

    private final JdbcTemplate jdbc;

    public TenantMetricsService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record TenantMetrics(
        // Producción
        long totalLotes,
        long lotesEnProceso,
        long lotesCompletados,
        BigDecimal litrosTotales,
        // Ventas
        long totalVentas,
        BigDecimal ingresosVentas,
        long totalClientes,
        // Compras
        long totalFacturas,
        BigDecimal totalGastado,
        // Inventario & Equipos
        long totalInsumos,
        long bajoStock,
        long totalEquipos,
        // Sistema
        long totalUsuarios,
        LocalDateTime ultimoAcceso
    ) {}

    @Transactional(readOnly = true)
    public TenantMetrics obtener(String tenantId) {
        return new TenantMetrics(
            count("SELECT COUNT(*) FROM lotes_cerveza WHERE tenant_id = ? AND deleted_at IS NULL", tenantId),
            count("SELECT COUNT(*) FROM lotes_cerveza WHERE tenant_id = ? AND deleted_at IS NULL AND carb_fecha_final IS NULL AND ferm_fecha_inicial IS NOT NULL", tenantId),
            count("SELECT COUNT(*) FROM lotes_cerveza WHERE tenant_id = ? AND deleted_at IS NULL AND carb_fecha_final IS NOT NULL", tenantId),
            sum("SELECT COALESCE(SUM(litros_finales), 0) FROM lotes_cerveza WHERE tenant_id = ? AND deleted_at IS NULL", tenantId),
            count("SELECT COUNT(*) FROM ventas WHERE tenant_id = ? AND deleted_at IS NULL", tenantId),
            sum("SELECT COALESCE(SUM(vi.cantidad * vi.precio_unitario * (1 - vi.descuento_pct / 100.0)), 0) FROM ventas v JOIN venta_items vi ON vi.venta_id = v.id WHERE v.tenant_id = ? AND v.estado = 'DESPACHADO' AND v.deleted_at IS NULL", tenantId),
            count("SELECT COUNT(*) FROM clientes WHERE tenant_id = ? AND activo = true", tenantId),
            count("SELECT COUNT(*) FROM facturas_proveedor WHERE tenant_id = ?", tenantId),
            sum("SELECT COALESCE(SUM(valor_total), 0) FROM facturas_proveedor WHERE tenant_id = ?", tenantId),
            count("SELECT COUNT(*) FROM insumos_inventario WHERE tenant_id = ?", tenantId),
            count("SELECT COUNT(*) FROM insumos_inventario WHERE tenant_id = ? AND stock_minimo IS NOT NULL AND cantidad <= stock_minimo", tenantId),
            count("SELECT COUNT(*) FROM equipos WHERE tenant_id = ?", tenantId),
            count("SELECT COUNT(*) FROM usuarios WHERE tenant_id = ? AND activo = true", tenantId),
            ultimoAcceso(tenantId)
        );
    }

    private long count(String sql, String tenantId) {
        Long result = jdbc.queryForObject(sql, Long.class, tenantId);
        return result != null ? result : 0L;
    }

    private BigDecimal sum(String sql, String tenantId) {
        BigDecimal result = jdbc.queryForObject(sql, BigDecimal.class, tenantId);
        return result != null ? result : BigDecimal.ZERO;
    }

    private LocalDateTime ultimoAcceso(String tenantId) {
        return jdbc.queryForObject(
            "SELECT MAX(fecha) FROM log_accesos WHERE tenant_id = ?",
            LocalDateTime.class, tenantId);
    }

    /** Último acceso registrado por cada tenant (todos), para el reporte de tenants inactivos. */
    @Transactional(readOnly = true)
    public Map<String, LocalDateTime> ultimoAccesoPorTenant() {
        Map<String, LocalDateTime> resultado = new HashMap<>();
        jdbc.query("SELECT tenant_id, MAX(fecha) AS ultimo FROM log_accesos GROUP BY tenant_id", rs -> {
            resultado.put(rs.getString("tenant_id"), rs.getTimestamp("ultimo").toLocalDateTime());
        });
        return resultado;
    }
}
