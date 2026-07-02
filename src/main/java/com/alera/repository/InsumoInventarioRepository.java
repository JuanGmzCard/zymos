package com.alera.repository;

import com.alera.model.InsumoInventario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InsumoInventarioRepository extends JpaRepository<InsumoInventario, Long> {

    @Query("SELECT i FROM InsumoInventario i WHERE i.cantidad <= i.stockMinimo ORDER BY i.nombre")
    List<InsumoInventario> findBajoStock();

    @Query("SELECT COUNT(i) FROM InsumoInventario i WHERE i.cantidad <= i.stockMinimo")
    long countBajoStock();

    @Query("SELECT i FROM InsumoInventario i WHERE LOWER(TRIM(i.nombre)) = LOWER(TRIM(:nombre)) ORDER BY i.id ASC")
    List<InsumoInventario> findByNombreExacto(@Param("nombre") String nombre);

    @Query("SELECT i FROM InsumoInventario i WHERE i.fechaVencimiento <= :fecha ORDER BY i.fechaVencimiento ASC")
    List<InsumoInventario> findProximosAVencer(@Param("fecha") LocalDate fecha);

    @Query("SELECT COUNT(i) FROM InsumoInventario i WHERE i.fechaVencimiento IS NOT NULL AND i.fechaVencimiento <= :fecha")
    long countProximosAVencer(@Param("fecha") LocalDate fecha);

    List<InsumoInventario> findAllByOrderByNombreAsc();

    @Query("SELECT i FROM InsumoInventario i WHERE " +
           "(:nombre = '' OR LOWER(i.nombre) LIKE LOWER(CONCAT('%', :nombre, '%'))) " +
           "AND (:tipo IS NULL OR i.tipo = :tipo) " +
           "ORDER BY i.nombre ASC")
    Page<InsumoInventario> findByFiltros(@Param("nombre") String nombre,
                                          @Param("tipo") String tipo,
                                          Pageable pageable);
}
