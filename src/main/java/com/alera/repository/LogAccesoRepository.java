package com.alera.repository;

import com.alera.model.LogAcceso;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface LogAccesoRepository extends JpaRepository<LogAcceso, Long> {

    Page<LogAcceso> findAllByOrderByFechaDesc(Pageable pageable);

    Page<LogAcceso> findByTipoOrderByFechaDesc(String tipo, Pageable pageable);

    @Query("SELECT COUNT(l) FROM LogAcceso l WHERE l.tipo = 'LOGIN_FALLIDO' AND l.fecha >= :desde")
    long countFallidosDesde(@org.springframework.data.repository.query.Param("desde") java.time.LocalDateTime desde);
}
