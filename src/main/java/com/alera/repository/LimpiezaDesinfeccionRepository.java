package com.alera.repository;

import com.alera.model.LimpiezaDesinfeccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface LimpiezaDesinfeccionRepository extends JpaRepository<LimpiezaDesinfeccion, Long> {
    List<LimpiezaDesinfeccion> findAllByOrderByFechaDescIdDesc();
    List<LimpiezaDesinfeccion> findByFechaBetweenOrderByFechaAscAreaUtensilioAsc(LocalDate desde, LocalDate hasta);
    long countByFechaBetween(LocalDate desde, LocalDate hasta);
}
