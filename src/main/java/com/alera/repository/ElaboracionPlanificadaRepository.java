package com.alera.repository;

import com.alera.model.ElaboracionPlanificada;
import com.alera.model.enums.EstadoPlanificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ElaboracionPlanificadaRepository
        extends JpaRepository<ElaboracionPlanificada, Long> {

    @Query("SELECT DISTINCT e FROM ElaboracionPlanificada e " +
           "LEFT JOIN FETCH e.receta r LEFT JOIN FETCH r.ingredientes " +
           "WHERE e.id = :id")
    Optional<ElaboracionPlanificada> findByIdWithRecetaEIngredientes(@Param("id") Long id);

    @Query("SELECT e FROM ElaboracionPlanificada e LEFT JOIN FETCH e.receta " +
           "WHERE e.fechaPlaneada >= :desde ORDER BY e.fechaPlaneada ASC")
    List<ElaboracionPlanificada> findProximas(@Param("desde") LocalDate desde);

    @Query("SELECT e FROM ElaboracionPlanificada e LEFT JOIN FETCH e.receta " +
           "ORDER BY e.fechaPlaneada ASC")
    List<ElaboracionPlanificada> findAllOrdenadas();

    @Query("SELECT e FROM ElaboracionPlanificada e LEFT JOIN FETCH e.receta " +
           "WHERE e.estado = :estado ORDER BY e.fechaPlaneada ASC")
    List<ElaboracionPlanificada> findByEstado(@Param("estado") EstadoPlanificacion estado);

    @Query("SELECT e FROM ElaboracionPlanificada e LEFT JOIN FETCH e.receta " +
           "WHERE e.fechaPlaneada BETWEEN :desde AND :hasta ORDER BY e.fechaPlaneada ASC")
    List<ElaboracionPlanificada> findByRangoFecha(@Param("desde") LocalDate desde,
                                                   @Param("hasta") LocalDate hasta);
}
