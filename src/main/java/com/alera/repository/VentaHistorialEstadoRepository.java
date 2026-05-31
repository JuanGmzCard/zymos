package com.alera.repository;

import com.alera.model.VentaHistorialEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VentaHistorialEstadoRepository extends JpaRepository<VentaHistorialEstado, Long> {
    List<VentaHistorialEstado> findByVentaIdOrderByFechaDesc(Long ventaId);
}
