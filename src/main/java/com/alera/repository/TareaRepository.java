package com.alera.repository;

import com.alera.model.Tarea;
import com.alera.model.enums.EstadoTarea;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface TareaRepository extends JpaRepository<Tarea, Long> {

    @EntityGraph(attributePaths = "items")
    List<Tarea> findAllByOrderByFechaVencimientoAscCreatedAtDesc();

    @EntityGraph(attributePaths = "items")
    List<Tarea> findAllByEstadoOrderByFechaVencimientoAscCreatedAtDesc(EstadoTarea estado);

    @EntityGraph(attributePaths = "items")
    List<Tarea> findAllByAsignadoAOrderByFechaVencimientoAscCreatedAtDesc(String asignadoA);

    @EntityGraph(attributePaths = "items")
    List<Tarea> findAllByEstadoAndAsignadoAOrderByFechaVencimientoAscCreatedAtDesc(EstadoTarea estado, String asignadoA);

    @Query("SELECT t.estado, COUNT(t) FROM Tarea t GROUP BY t.estado")
    List<Object[]> countGroupByEstado();

    List<Tarea> findByFechaVencimientoLessThanEqualAndEstadoNot(LocalDate fecha, EstadoTarea estado);
}
