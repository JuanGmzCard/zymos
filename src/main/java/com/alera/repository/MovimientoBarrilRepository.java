package com.alera.repository;

import com.alera.model.MovimientoBarril;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoBarrilRepository extends JpaRepository<MovimientoBarril, Long> {

    List<MovimientoBarril> findByBarrilIdOrderByFechaDesc(Long barrilId);
}
