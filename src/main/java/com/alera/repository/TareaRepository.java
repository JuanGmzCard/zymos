package com.alera.repository;

import com.alera.model.Tarea;
import com.alera.model.enums.EstadoTarea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TareaRepository extends JpaRepository<Tarea, Long> {

    List<Tarea> findAllByOrderByFechaVencimientoAscCreatedAtDesc();

    List<Tarea> findAllByEstadoOrderByFechaVencimientoAscCreatedAtDesc(EstadoTarea estado);

    List<Tarea> findAllByAsignadoAOrderByFechaVencimientoAscCreatedAtDesc(String asignadoA);

    List<Tarea> findAllByEstadoAndAsignadoAOrderByFechaVencimientoAscCreatedAtDesc(EstadoTarea estado, String asignadoA);

    long countByEstado(EstadoTarea estado);

    List<Tarea> findByFechaVencimientoAndEstadoNot(LocalDate fecha, EstadoTarea estado);
}
