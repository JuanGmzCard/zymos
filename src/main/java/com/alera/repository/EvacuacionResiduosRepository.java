package com.alera.repository;

import com.alera.model.EvacuacionResiduos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface EvacuacionResiduosRepository extends JpaRepository<EvacuacionResiduos, Long> {
    List<EvacuacionResiduos> findAllByOrderByFechaDescIdDesc();
    List<EvacuacionResiduos> findByFechaBetweenOrderByFechaAscHoraSalidaAsc(LocalDate desde, LocalDate hasta);
    long countByFechaBetween(LocalDate desde, LocalDate hasta);
}
