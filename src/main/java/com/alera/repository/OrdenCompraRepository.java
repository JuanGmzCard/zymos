package com.alera.repository;

import com.alera.model.OrdenCompra;
import com.alera.model.enums.EstadoOrdenCompra;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Long> {

    @Query("""
            SELECT o FROM OrdenCompra o
            WHERE (:estado IS NULL OR o.estado = :estado)
            ORDER BY o.fechaEmision DESC NULLS LAST, o.id DESC
            """)
    Page<OrdenCompra> findAllFiltered(@Param("estado") EstadoOrdenCompra estado, Pageable pageable);

    @Query("SELECT o FROM OrdenCompra o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<OrdenCompra> findByIdWithItems(@Param("id") Long id);

    @Query(value = """
            SELECT COALESCE(MAX(CAST(SUBSTRING(numero_oc FROM 4) AS INTEGER)), 0)
            FROM ordenes_compra
            WHERE tenant_id = :tenantId AND numero_oc ~ '^OC-[0-9]+$'
            """, nativeQuery = true)
    Integer findMaxNumeroOc(@Param("tenantId") String tenantId);

    long countByEstado(EstadoOrdenCompra estado);

    @Query("""
            SELECT o FROM OrdenCompra o
            WHERE LOWER(COALESCE(o.proveedor,'')) LIKE LOWER(CONCAT('%',:q,'%'))
               OR LOWER(COALESCE(o.numeroOc,''))  LIKE LOWER(CONCAT('%',:q,'%'))
            ORDER BY o.fechaEmision DESC NULLS LAST
            """)
    Page<OrdenCompra> search(@Param("q") String q, Pageable pageable);
}
