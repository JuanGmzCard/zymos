package com.alera.repository;

import com.alera.model.MantenimientoEquipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface MantenimientoEquipoRepository extends JpaRepository<MantenimientoEquipo, Long> {

    List<MantenimientoEquipo> findByEquipoIdOrderByFechaDesc(Long equipoId);

    @Query("SELECT MAX(m.fecha) FROM MantenimientoEquipo m WHERE m.equipo.id = :equipoId")
    Optional<java.time.LocalDate> findUltimaFechaByEquipoId(@Param("equipoId") Long equipoId);

    @Query("SELECT SUM(m.costo) FROM MantenimientoEquipo m")
    java.math.BigDecimal sumTotalCostos();

    @Query("SELECT COALESCE(SUM(m.costo), 0) FROM MantenimientoEquipo m WHERE m.equipo.id = :equipoId")
    java.math.BigDecimal sumCostoByEquipoId(@Param("equipoId") Long equipoId);

    @Query("SELECT COUNT(m) FROM MantenimientoEquipo m WHERE m.equipo.id = :equipoId")
    long countByEquipoId(@Param("equipoId") Long equipoId);
}
