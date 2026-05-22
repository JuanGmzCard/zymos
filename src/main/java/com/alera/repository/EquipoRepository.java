package com.alera.repository;

import com.alera.model.Equipo;
import com.alera.model.enums.EstadoEquipo;
import com.alera.model.enums.TipoEquipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.List;

public interface EquipoRepository extends JpaRepository<Equipo, Long> {

    @Query("SELECT e FROM Equipo e WHERE e.tipo = :tipo AND e.estado = :estado " +
           "AND NOT EXISTS (SELECT l FROM LoteCerveza l WHERE l.equipoFermentador = e AND l.carbFechaInicial IS NULL)")
    List<Equipo> findFermentadoresDisponibles(@Param("tipo") TipoEquipo tipo,
                                               @Param("estado") EstadoEquipo estado);

    @Query("SELECT e FROM Equipo e WHERE e.proximoMantenimiento <= :fecha ORDER BY e.proximoMantenimiento ASC")
    List<Equipo> findMantenimientoPendiente(@Param("fecha") LocalDate fecha);

    // COUNT — no carga registros completos
    @Query("SELECT COUNT(e) FROM Equipo e WHERE e.proximoMantenimiento IS NOT NULL AND e.proximoMantenimiento <= :fecha")
    long countMantenimientoPendiente(@Param("fecha") LocalDate fecha);

    List<Equipo> findByTipoAndEstadoOrderByNombreAsc(TipoEquipo tipo, EstadoEquipo estado);

    List<Equipo> findByEstadoOrderByNombreAsc(EstadoEquipo estado);
    Page<Equipo> findByEstadoOrderByNombreAsc(EstadoEquipo estado, Pageable pageable);
    Page<Equipo> findAllByOrderByNombreAsc(Pageable pageable);

    @Query("SELECT COUNT(e) FROM Equipo e WHERE e.estado = :estado")
    long countByEstado(@Param("estado") EstadoEquipo estado);
}
