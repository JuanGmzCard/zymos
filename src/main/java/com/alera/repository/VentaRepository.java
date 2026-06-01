package com.alera.repository;

import com.alera.model.Venta;
import com.alera.model.enums.EstadoVenta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    @Query("SELECT v FROM Venta v WHERE " +
           "(:estado IS NULL OR v.estado = :estado) AND " +
           "(CAST(:desde AS LocalDate) IS NULL OR v.fechaDespacho >= :desde) AND " +
           "(CAST(:hasta AS LocalDate) IS NULL OR v.fechaDespacho <= :hasta) " +
           "ORDER BY v.fechaDespacho DESC NULLS LAST, v.id DESC")
    Page<Venta> findAllFiltered(@Param("estado") EstadoVenta estado,
                                @Param("desde")  LocalDate desde,
                                @Param("hasta")  LocalDate hasta,
                                Pageable pageable);

    long countByEstado(EstadoVenta estado);

    @Query("SELECT COUNT(DISTINCT v.cliente) FROM Venta v")
    long countClientesUnicos();

    @Query("SELECT v FROM Venta v WHERE " +
           "LOWER(v.cliente) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "EXISTS (SELECT 1 FROM VentaItem i WHERE i.venta = v AND " +
           "LOWER(COALESCE(i.codigoLote,'')) LIKE LOWER(CONCAT('%',:q,'%'))) " +
           "ORDER BY v.fechaDespacho DESC NULLS LAST")
    List<Venta> search(@Param("q") String q, Pageable pageable);

    @Query("SELECT v FROM Venta v WHERE " +
           "(CAST(:desde AS LocalDate) IS NULL OR v.fechaDespacho >= :desde) AND " +
           "(CAST(:hasta AS LocalDate) IS NULL OR v.fechaDespacho <= :hasta) " +
           "ORDER BY v.fechaDespacho DESC NULLS LAST, v.id DESC")
    List<Venta> findByPeriodo(@Param("desde") LocalDate desde,
                              @Param("hasta") LocalDate hasta);

    @Query("SELECT DISTINCT v.cliente FROM Venta v " +
           "WHERE LOWER(v.cliente) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "ORDER BY v.cliente ASC")
    List<String> findClientesSuggestions(@Param("q") String q, Pageable pageable);

    // Top 5 clientes por ingresos despachados (native — necesita tenantId explícito)
    @Query(nativeQuery = true, value =
           "SELECT v.cliente, COUNT(DISTINCT v.id) AS total_ventas, " +
           "SUM(vi.cantidad * vi.precio_unitario * (1 - vi.descuento_pct / 100.0)) AS total_ingresos " +
           "FROM ventas v " +
           "JOIN venta_items vi ON vi.venta_id = v.id " +
           "WHERE v.tenant_id = :tenantId AND v.deleted_at IS NULL AND v.estado = 'DESPACHADO' " +
           "GROUP BY v.cliente ORDER BY total_ingresos DESC LIMIT 5")
    List<Map<String, Object>> findTopClientes(@Param("tenantId") String tenantId);
}
