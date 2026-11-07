package com.alera.repository;

import com.alera.model.TipoCerveza;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface TipoCervezaRepository extends JpaRepository<TipoCerveza, Long> {
    List<TipoCerveza> findByActivoTrueOrderByNombreAsc();
    Optional<TipoCerveza> findByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCase(String nombre);

    @Query(value = "SELECT COUNT(*) > 0 FROM tipos_cerveza WHERE LOWER(nombre) = LOWER(:nombre) AND tenant_id = :tenantId",
           nativeQuery = true)
    boolean existsByNombreIgnoreCaseAndTenantId(@Param("nombre") String nombre, @Param("tenantId") String tenantId);
}
