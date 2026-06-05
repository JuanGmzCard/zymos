package com.alera.repository;

import com.alera.model.EvaluacionSensorial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface EvaluacionSensorialRepository extends JpaRepository<EvaluacionSensorial, Long> {

    @Query("SELECT e FROM EvaluacionSensorial e WHERE e.lote.id = :loteId ORDER BY e.fecha DESC, e.id DESC")
    List<EvaluacionSensorial> findByLoteIdOrdenadas(@Param("loteId") Long loteId);
}
