package com.alera.repository;

import com.alera.model.CategoriaEquipo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoriaEquipoRepository extends JpaRepository<CategoriaEquipo, Long> {
    List<CategoriaEquipo> findAllByActivoTrueOrderByNombreAsc();
    List<CategoriaEquipo> findAllByOrderByNombreAsc();
    Optional<CategoriaEquipo> findByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCase(String nombre);
}
