package com.alera.repository;

import com.alera.model.Barril;
import com.alera.model.enums.EstadoBarril;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BarrilRepository extends JpaRepository<Barril, Long> {

    @Query("""
           SELECT b FROM Barril b
           WHERE (:codigo = '' OR LOWER(b.codigo) LIKE LOWER(CONCAT('%',:codigo,'%')))
           AND   (:estado IS NULL OR b.estado = :estado)
           ORDER BY b.codigo ASC
           """)
    Page<Barril> findByFiltros(@Param("codigo") String codigo,
                                @Param("estado") EstadoBarril estado,
                                Pageable pageable);

    long countByEstado(EstadoBarril estado);

    boolean existsByCodigoIgnoreCase(String codigo);

    @Query("SELECT COUNT(b) > 0 FROM Barril b WHERE LOWER(b.codigo) = LOWER(:codigo) AND b.id <> :excludeId")
    boolean existsByCodigoIgnoreCaseAndIdNot(@Param("codigo") String codigo,
                                              @Param("excludeId") Long excludeId);
}
