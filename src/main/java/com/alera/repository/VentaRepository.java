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
}
