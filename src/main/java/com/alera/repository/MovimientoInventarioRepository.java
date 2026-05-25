package com.alera.repository;

import com.alera.model.MovimientoInventario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {
    Page<MovimientoInventario> findByInsumoIdOrderByFechaDesc(Long insumoId, Pageable pageable);
}
