package com.alera.repository;

import com.alera.model.LecturaFermentacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface LecturaFermentacionRepository extends JpaRepository<LecturaFermentacion, Long> {

    @Query("SELECT l FROM LecturaFermentacion l WHERE l.lote.id = :loteId ORDER BY l.fecha ASC, l.id ASC")
    List<LecturaFermentacion> findByLoteIdOrdenadas(@Param("loteId") Long loteId);
}