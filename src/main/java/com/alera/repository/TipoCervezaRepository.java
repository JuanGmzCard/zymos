package com.alera.repository;

import com.alera.model.TipoCerveza;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TipoCervezaRepository extends JpaRepository<TipoCerveza, Long> {
    List<TipoCerveza> findByActivoTrueOrderByNombreAsc();
    Optional<TipoCerveza> findByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCase(String nombre);
}
