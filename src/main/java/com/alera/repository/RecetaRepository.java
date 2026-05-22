package com.alera.repository;

import com.alera.model.Receta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RecetaRepository extends JpaRepository<Receta, Long> {
    List<Receta> findAllByActivaTrueOrderByNombreAsc();
    List<Receta> findAllByOrderByActivaDescNombreAsc();
    Page<Receta> findAllByOrderByActivaDescNombreAsc(Pageable pageable);
    Page<Receta> findByActivaOrderByNombreAsc(boolean activa, Pageable pageable);
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
    boolean existsByNombreIgnoreCase(String nombre);

    @Query("SELECT r FROM Receta r WHERE r.activa = true AND (" +
           "LOWER(r.nombre) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(r.estilo) LIKE LOWER(CONCAT('%', :q, '%'))) ORDER BY r.nombre")
    List<Receta> search(@Param("q") String q, Pageable pageable);
}
