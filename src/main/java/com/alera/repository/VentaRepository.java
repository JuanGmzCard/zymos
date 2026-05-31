package com.alera.repository;

import com.alera.model.Venta;
import com.alera.model.enums.EstadoVenta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    @Query("SELECT v FROM Venta v WHERE " +
           "(:estado IS NULL OR v.estado = :estado) AND " +
           "(:desde IS NULL OR v.fechaDespacho >= :desde) AND " +
           "(:hasta IS NULL OR v.fechaDespacho <= :hasta) " +
           "ORDER BY v.fechaDespacho DESC NULLS LAST, v.id DESC")
    Page<Venta> findAllFiltered(@Param("estado") EstadoVenta estado,
                                @Param("desde")  LocalDate desde,
                                @Param("hasta")  LocalDate hasta,
                                Pageable pageable);

    List<Venta> findByLoteIdOrderByFechaDespachoDesc(Long loteId);

    long countByEstado(EstadoVenta estado);

    @Query("SELECT COUNT(DISTINCT v.cliente) FROM Venta v")
    long countClientesUnicos();

    @Query("SELECT COALESCE(SUM(v.cantidad * v.precioUnitario * (1 - v.descuentoPct / 100.0)), 0.0) " +
           "FROM Venta v WHERE v.estado = com.alera.model.enums.EstadoVenta.DESPACHADO")
    BigDecimal sumIngresosDespachados();

    @Query("SELECT v FROM Venta v WHERE " +
           "LOWER(v.cliente) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(COALESCE(v.codigoLote,'')) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "ORDER BY v.fechaDespacho DESC NULLS LAST")
    List<Venta> search(@Param("q") String q, Pageable pageable);

    @Query("SELECT v FROM Venta v WHERE " +
           "(:desde IS NULL OR v.fechaDespacho >= :desde) AND " +
           "(:hasta IS NULL OR v.fechaDespacho <= :hasta) " +
           "ORDER BY v.fechaDespacho DESC NULLS LAST, v.id DESC")
    List<Venta> findByPeriodo(@Param("desde") LocalDate desde,
                              @Param("hasta") LocalDate hasta);

    // Suma de cantidad vendida no cancelada para un lote (excluye la venta en edición)
    @Query("SELECT COALESCE(SUM(v.cantidad), 0) FROM Venta v WHERE v.lote.id = :loteId " +
           "AND v.estado != com.alera.model.enums.EstadoVenta.CANCELADO " +
           "AND (:excludeId IS NULL OR v.id != :excludeId)")
    BigDecimal sumCantidadActivaByLote(@Param("loteId") Long loteId,
                                       @Param("excludeId") Long excludeId);

    // Top 5 clientes por ingresos despachados (native — necesita tenantId explícito)
    @Query(nativeQuery = true, value =
           "SELECT v.cliente, COUNT(*) AS total_ventas, " +
           "SUM(v.cantidad * v.precio_unitario * (1 - v.descuento_pct / 100.0)) AS total_ingresos " +
           "FROM ventas v " +
           "WHERE v.tenant_id = :tenantId AND v.deleted_at IS NULL AND v.estado = 'DESPACHADO' " +
           "GROUP BY v.cliente ORDER BY total_ingresos DESC LIMIT 5")
    List<Map<String, Object>> findTopClientes(@Param("tenantId") String tenantId);
}
