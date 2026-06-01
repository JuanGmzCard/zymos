package com.alera.repository;

import com.alera.model.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    List<Cliente> findAllByActivoTrueOrderByNombreAsc();

    List<Cliente> findAllByOrderByNombreAsc();

    Optional<Cliente> findByNit(String nit);

    @Query("SELECT c FROM Cliente c WHERE " +
           "(:nombre IS NULL OR LOWER(c.nombre) LIKE LOWER(CONCAT('%',:nombre,'%'))) AND " +
           "(:activo IS NULL OR c.activo = :activo) " +
           "ORDER BY c.nombre ASC")
    Page<Cliente> findAllFiltered(@Param("nombre") String nombre,
                                   @Param("activo") Boolean activo,
                                   Pageable pageable);

    @Query("SELECT c FROM Cliente c WHERE c.activo = true AND (" +
           "LOWER(c.nombre) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(COALESCE(c.nit,'')) LIKE LOWER(CONCAT('%',:q,'%'))) " +
           "ORDER BY c.nombre ASC")
    List<Cliente> searchActivos(@Param("q") String q, Pageable pageable);
}
