package com.alera.repository;

import com.alera.model.FacturaHistorialEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FacturaHistorialEstadoRepository extends JpaRepository<FacturaHistorialEstado, Long> {
    List<FacturaHistorialEstado> findByFacturaIdOrderByFechaDesc(Long facturaId);
}
