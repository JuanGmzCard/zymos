package com.alera.repository;

import com.alera.model.FacturaProveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface FacturaProveedorRepository extends JpaRepository<FacturaProveedor, Long> {

    @Query("SELECT DISTINCT f FROM FacturaProveedor f LEFT JOIN FETCH f.items ORDER BY f.fechaFactura DESC")
    List<FacturaProveedor> findAllWithItems();

    @Query("SELECT f FROM FacturaProveedor f ORDER BY f.id DESC")
    Page<FacturaProveedor> findAllPaged(Pageable pageable);

    @Query("SELECT f FROM FacturaProveedor f LEFT JOIN FETCH f.items WHERE f.id = :id")
    Optional<FacturaProveedor> findByIdWithItems(@Param("id") Long id);

    @Query("SELECT f FROM FacturaProveedor f WHERE " +
           "LOWER(COALESCE(f.numeroFactura,'')) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(COALESCE(f.proveedor,''))    LIKE LOWER(CONCAT('%',:q,'%')) " +
           "ORDER BY f.fechaFactura DESC NULLS LAST")
    List<FacturaProveedor> search(@Param("q") String q, Pageable pageable);

    @Query("SELECT SUM(f.valorTotal) FROM FacturaProveedor f")
    BigDecimal sumTotalFacturas();

    @Query("SELECT COUNT(f) FROM FacturaProveedor f")
    long countTotal();
}
