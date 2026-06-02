package com.alera.repository;

import com.alera.model.CategoriaInsumo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoriaInsumoRepository extends JpaRepository<CategoriaInsumo, Long> {
    List<CategoriaInsumo> findAllByActivoTrueOrderByNombreAsc();
    List<CategoriaInsumo> findAllByOrderByNombreAsc();
    Optional<CategoriaInsumo> findByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCase(String nombre);
}
