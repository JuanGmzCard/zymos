package com.alera.repository;

import com.alera.model.StockAjuste;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockAjusteRepository extends JpaRepository<StockAjuste, Long> {

    List<StockAjuste> findByLoteIdOrderByFechaDesc(Long loteId);

    @Query("SELECT COALESCE(SUM(a.cantidad), 0) FROM StockAjuste a WHERE a.lote.id = :loteId")
    java.math.BigDecimal sumCantidadByLoteId(@Param("loteId") Long loteId);
}
