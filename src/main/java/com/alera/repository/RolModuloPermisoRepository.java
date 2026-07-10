package com.alera.repository;

import com.alera.model.RolModuloPermiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface RolModuloPermisoRepository extends JpaRepository<RolModuloPermiso, Long> {

    List<RolModuloPermiso> findByRolId(Long rolId);

    // Nativo — bypasea el filtro @TenantId, seguro para llamar desde loadUserByUsername
    @Query(value = "SELECT * FROM roles_modulos_permisos WHERE rol_id = :rolId", nativeQuery = true)
    List<RolModuloPermiso> findByRolIdNative(@Param("rolId") Long rolId);
}
