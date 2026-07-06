package com.alera.repository;

import com.alera.model.AvistamientoPlagas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface AvistamientoPlagasRepository extends JpaRepository<AvistamientoPlagas, Long> {
    List<AvistamientoPlagas> findAllByOrderByFechaDescIdDesc();
    List<AvistamientoPlagas> findByFechaBetweenOrderByFechaAsc(LocalDate desde, LocalDate hasta);
    long countByFechaBetween(LocalDate desde, LocalDate hasta);
}
