package com.alera.repository;

import com.alera.model.LecturaFermentacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface LecturaFermentacionRepository extends JpaRepository<LecturaFermentacion, Long> {

    @Query("""
        SELECT l FROM LecturaFermentacion l WHERE l.lote.id = :loteId
        ORDER BY
            CASE WHEN l.notas = 'OG inicial' THEN 0 WHEN l.notas = 'FG final' THEN 2 ELSE 1 END ASC,
            l.fecha ASC, l.id ASC
        """)
    List<LecturaFermentacion> findByLoteIdOrdenadas(@Param("loteId") Long loteId);

    Optional<LecturaFermentacion> findFirstByLoteIdAndNotas(Long loteId, String notas);
}