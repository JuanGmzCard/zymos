package com.alera.repository;

import com.alera.model.SolucionDesinfectante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface SolucionDesinfectanteRepository extends JpaRepository<SolucionDesinfectante, Long> {
    List<SolucionDesinfectante> findAllByOrderByFechaDescIdDesc();
    List<SolucionDesinfectante> findByFechaBetweenOrderByFechaAscHoraAsc(LocalDate desde, LocalDate hasta);
    long countByFechaBetween(LocalDate desde, LocalDate hasta);
}
