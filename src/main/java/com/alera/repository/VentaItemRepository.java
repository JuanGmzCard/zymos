package com.alera.repository;

import com.alera.model.Venta;
import com.alera.model.VentaItem;
import com.alera.model.enums.EstadoVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public interface VentaItemRepository extends JpaRepository<VentaItem, Long> {

    List<VentaItem> findByVentaId(Long ventaId);

    // Ventas que contienen al menos un ítem del lote dado (para detalle de lote)
    @Query("SELECT DISTINCT i.venta FROM VentaItem i WHERE i.lote.id = :loteId " +
           "ORDER BY i.venta.fechaDespacho DESC")
    List<Venta> findVentasByLoteId(@Param("loteId") Long loteId);

    // Suma de cantidad vendida no cancelada para un lote (excluye la venta en edición)
    @Query("SELECT COALESCE(SUM(i.cantidad), 0) FROM VentaItem i " +
           "WHERE i.lote.id = :loteId " +
           "AND i.venta.estado != com.alera.model.enums.EstadoVenta.CANCELADO " +
           "AND (:excludeVentaId IS NULL OR i.venta.id != :excludeVentaId)")
    BigDecimal sumCantidadActivaByLote(@Param("loteId") Long loteId,
                                       @Param("excludeVentaId") Long excludeVentaId);

    // Unidades distintas usadas en ventas activas de un lote (excluye la venta en edición)
    @Query("SELECT DISTINCT i.unidad FROM VentaItem i " +
           "WHERE i.lote.id = :loteId " +
           "AND i.venta.estado != com.alera.model.enums.EstadoVenta.CANCELADO " +
           "AND (:excludeVentaId IS NULL OR i.venta.id != :excludeVentaId) " +
           "AND i.unidad IS NOT NULL AND i.unidad <> ''")
    Set<String> findUnidadesActivasByLote(@Param("loteId") Long loteId,
                                          @Param("excludeVentaId") Long excludeVentaId);

    // Suma de ingresos de ventas despachadas (para stat-card)
    @Query("SELECT COALESCE(SUM(i.cantidad * i.precioUnitario * (1 - i.descuentoPct / 100.0)), 0.0) " +
           "FROM VentaItem i WHERE i.venta.estado = com.alera.model.enums.EstadoVenta.DESPACHADO")
    BigDecimal sumIngresosDespachados();
}
