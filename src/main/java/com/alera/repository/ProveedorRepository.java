package com.alera.repository;

import com.alera.model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
    List<Proveedor> findAllByActivoTrueOrderByNombreAsc();
    List<Proveedor> findAllByOrderByNombreAsc();

    @Query("SELECT COUNT(f) FROM FacturaProveedor f WHERE f.proveedorRef.id = :id")
    long countFacturas(@Param("id") Long id);

    @Query("SELECT COALESCE(SUM(f.valorTotal), 0) FROM FacturaProveedor f WHERE f.proveedorRef.id = :id")
    java.math.BigDecimal sumFacturas(@Param("id") Long id);
}
