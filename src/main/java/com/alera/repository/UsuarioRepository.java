package com.alera.repository;

import com.alera.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);
    boolean existsByUsername(String username);
    List<Usuario> findAllByOrderByCreatedAtDesc();

    // ── Queries nativas para admin cross-tenant (bypasean el filtro automático @TenantId) ──

    @Query(value = "SELECT * FROM usuarios WHERE tenant_id = :tenantId ORDER BY created_at DESC", nativeQuery = true)
    List<Usuario> findAllByTenantId(@Param("tenantId") String tenantId);

    @Query(value = "SELECT COUNT(*) FROM usuarios WHERE tenant_id = :tenantId", nativeQuery = true)
    long countByTenantId(@Param("tenantId") String tenantId);

    @Query(value = "SELECT COUNT(*) FROM usuarios WHERE username = :username AND tenant_id = :tenantId", nativeQuery = true)
    int countByUsernameAndTenantId(@Param("username") String username, @Param("tenantId") String tenantId);

    @Transactional
    @Modifying
    @Query(value = "INSERT INTO usuarios (username, password, rol, activo, tenant_id, created_at) VALUES (:username, :password, :rol, true, :tenantId, NOW())", nativeQuery = true)
    void insertarConTenant(@Param("username") String username,
                           @Param("password") String password,
                           @Param("rol") String rol,
                           @Param("tenantId") String tenantId);

    @Transactional
    @Modifying
    @Query(value = "UPDATE usuarios SET activo = NOT activo WHERE id = :id AND tenant_id = :tenantId", nativeQuery = true)
    void toggleActivoByIdAndTenantId(@Param("id") Long id, @Param("tenantId") String tenantId);

    @Transactional
    @Modifying
    @Query(value = "UPDATE usuarios SET password = :password WHERE id = :id AND tenant_id = :tenantId", nativeQuery = true)
    void updatePasswordByIdAndTenantId(@Param("id") Long id, @Param("tenantId") String tenantId, @Param("password") String password);

    @Transactional
    @Modifying
    @Query(value = "UPDATE usuarios SET rol = :rol WHERE id = :id AND tenant_id = :tenantId", nativeQuery = true)
    void updateRolByIdAndTenantId(@Param("id") Long id, @Param("tenantId") String tenantId, @Param("rol") String rol);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM usuarios WHERE id = :id AND tenant_id = :tenantId", nativeQuery = true)
    void deleteByIdAndTenantId(@Param("id") Long id, @Param("tenantId") String tenantId);

    @Query(value = "SELECT COUNT(*) FROM usuarios WHERE rol_custom_id = :rolId", nativeQuery = true)
    long countByRolCustomId(@Param("rolId") Long rolId);

    @Transactional
    @Modifying
    @Query(value = "UPDATE usuarios SET rol_custom_id = NULL WHERE rol_custom_id = :rolId", nativeQuery = true)
    void limpiarRolCustom(@Param("rolId") Long rolId);

    @Transactional
    @Modifying
    @Query(value = "UPDATE usuarios SET rol_custom_id = :rolId, rol = 'ADMIN' WHERE id = :id AND tenant_id = :tenantId", nativeQuery = true)
    void asignarRolCustomByIdAndTenantId(@Param("id") Long id, @Param("tenantId") String tenantId, @Param("rolId") Long rolId);

    @Transactional
    @Modifying
    @Query(value = "UPDATE usuarios SET rol_custom_id = NULL WHERE id = :id AND tenant_id = :tenantId", nativeQuery = true)
    void quitarRolCustomByIdAndTenantId(@Param("id") Long id, @Param("tenantId") String tenantId);
}
