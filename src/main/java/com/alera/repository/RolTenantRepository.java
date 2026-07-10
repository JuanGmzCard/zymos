package com.alera.repository;

import com.alera.model.RolTenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

public interface RolTenantRepository extends JpaRepository<RolTenant, Long> {

    List<RolTenant> findAllByOrderByNombreAsc();

    List<RolTenant> findAllByActivoTrueOrderByNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    // ── Queries nativas para DataInitializer (cross-tenant, bypass @TenantId filter) ──

    @Query(value = "SELECT COUNT(*) FROM roles_tenant WHERE tenant_id = :tenantId", nativeQuery = true)
    long countByTenantId(@Param("tenantId") String tenantId);

    @Query(value = "SELECT id FROM roles_tenant WHERE tenant_id = :tenantId AND nombre = :nombre", nativeQuery = true)
    Optional<Long> findIdByTenantIdAndNombre(@Param("tenantId") String tenantId, @Param("nombre") String nombre);

    @Query(value = "SELECT es_admin FROM roles_tenant WHERE id = :id", nativeQuery = true)
    Boolean findEsAdminById(@Param("id") Long id);

    @Query(value = "SELECT nombre FROM roles_tenant WHERE id = :id", nativeQuery = true)
    String findNombreById(@Param("id") Long id);

    @Transactional
    @Modifying
    @Query(value = """
            INSERT INTO roles_tenant (tenant_id, nombre, descripcion, activo, es_sistema, es_admin, created_at)
            VALUES (:tenantId, :nombre, :descripcion, true, :esSistema, :esAdmin, NOW())
            ON CONFLICT (tenant_id, nombre) DO NOTHING
            """, nativeQuery = true)
    void insertarRolNativo(@Param("tenantId") String tenantId,
                           @Param("nombre") String nombre,
                           @Param("descripcion") String descripcion,
                           @Param("esSistema") boolean esSistema,
                           @Param("esAdmin") boolean esAdmin);

    @Transactional
    @Modifying
    @Query(value = """
            INSERT INTO roles_modulos_permisos (rol_id, modulo, puede_ver, puede_crear, puede_editar, puede_eliminar)
            VALUES (:rolId, :modulo, :ver, :crear, :editar, :eliminar)
            ON CONFLICT (rol_id, modulo) DO NOTHING
            """, nativeQuery = true)
    void insertarPermisoNativo(@Param("rolId") Long rolId,
                               @Param("modulo") String modulo,
                               @Param("ver") boolean ver,
                               @Param("crear") boolean crear,
                               @Param("editar") boolean editar,
                               @Param("eliminar") boolean eliminar);
}
