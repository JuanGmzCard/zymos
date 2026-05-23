package com.alera;

import com.alera.config.TenantContext;
import com.alera.model.TipoCerveza;
import com.alera.repository.TipoCervezaRepository;
import com.alera.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que el aislamiento de datos entre tenants funciona correctamente.
 *
 * Cubre dos niveles:
 *  - @TenantId automático de Hibernate: cada repo call sin @Transactional externo
 *    crea su propio EntityManager que captura el TenantContext en ese momento.
 *  - Queries nativas cross-tenant (admin): findAllByTenantId, insertarConTenant
 *    bypassean el filtro automático y operan por tenant_id explícito.
 *
 * Sin @Transactional en el test: cada repo call tiene su propia transacción y
 * EntityManager → captura el TenantContext correcto en cada momento.
 * JdbcTemplate para cleanup: evita dependencia del filtro @TenantId en teardown.
 */
class TenantIsolationIntegrationTest extends AbstractIntegrationTest {

    static final String TENANT_A = "test-iso-a";
    static final String TENANT_B = "test-iso-b";

    @Autowired TipoCervezaRepository tipoCervezaRepo;
    @Autowired UsuarioRepository     usuarioRepo;
    @Autowired JdbcTemplate          jdbc;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        jdbc.update("DELETE FROM tipos_cerveza WHERE tenant_id IN (?, ?)", TENANT_A, TENANT_B);
        jdbc.update("DELETE FROM usuarios WHERE tenant_id IN (?, ?)", TENANT_A, TENANT_B);
    }

    // ── @TenantId — aislamiento automático en SELECT ─────────────────────

    @Test
    @DisplayName("TipoCerveza creado en tenant A no es visible desde tenant B")
    void tipoCerveza_noEsVisibleDesdeOtroTenant() {
        TenantContext.setCurrentTenant(TENANT_A);
        TipoCerveza tipo = new TipoCerveza();
        tipo.setNombre("IsolTestIPA");
        tipo.setActivo(true);
        tipoCervezaRepo.save(tipo);
        TenantContext.clear();

        TenantContext.setCurrentTenant(TENANT_B);
        assertThat(tipoCervezaRepo.findByNombreIgnoreCase("IsolTestIPA"))
                .as("Tenant B no debe ver el tipo creado en tenant A")
                .isEmpty();
        TenantContext.clear();

        TenantContext.setCurrentTenant(TENANT_A);
        assertThat(tipoCervezaRepo.findByNombreIgnoreCase("IsolTestIPA"))
                .as("Tenant A sí debe ver su propio tipo")
                .isPresent();
        TenantContext.clear();
    }

    @Test
    @DisplayName("existsByNombreIgnoreCase solo busca dentro del tenant activo")
    void existsByNombre_soloDelTenantActivo() {
        TenantContext.setCurrentTenant(TENANT_A);
        TipoCerveza tipo = new TipoCerveza();
        tipo.setNombre("IsolExistsStout");
        tipo.setActivo(true);
        tipoCervezaRepo.save(tipo);
        TenantContext.clear();

        TenantContext.setCurrentTenant(TENANT_B);
        assertThat(tipoCervezaRepo.existsByNombreIgnoreCase("IsolExistsStout"))
                .as("Tenant B no debe ver el nombre de A")
                .isFalse();
        TenantContext.clear();

        TenantContext.setCurrentTenant(TENANT_A);
        assertThat(tipoCervezaRepo.existsByNombreIgnoreCase("IsolExistsStout"))
                .as("Tenant A sí encuentra su propio nombre")
                .isTrue();
        TenantContext.clear();
    }

    @Test
    @DisplayName("count() solo cuenta registros del tenant activo")
    void count_soloDelTenantActivo() {
        TenantContext.setCurrentTenant(TENANT_A);
        for (int i = 1; i <= 3; i++) {
            TipoCerveza t = new TipoCerveza();
            t.setNombre("IsolA-" + i);
            t.setActivo(true);
            tipoCervezaRepo.save(t);
        }
        TenantContext.clear();

        TenantContext.setCurrentTenant(TENANT_B);
        for (int i = 1; i <= 2; i++) {
            TipoCerveza t = new TipoCerveza();
            t.setNombre("IsolB-" + i);
            t.setActivo(true);
            tipoCervezaRepo.save(t);
        }
        long countB = tipoCervezaRepo.count();
        TenantContext.clear();

        TenantContext.setCurrentTenant(TENANT_A);
        long countA = tipoCervezaRepo.count();
        TenantContext.clear();

        assertThat(countA).as("Tenant A ve exactamente sus 3 registros").isEqualTo(3);
        assertThat(countB).as("Tenant B ve exactamente sus 2 registros").isEqualTo(2);
    }

    // ── Usuario — aislamiento automático ─────────────────────────────────

    @Test
    @DisplayName("findByUsername solo encuentra usuarios del tenant activo")
    void usuario_findByUsername_soloDelTenantActivo() {
        usuarioRepo.insertarConTenant("isol-user-a", "$2a$10$xxxxx.xxxxx", "ADMIN", TENANT_A);

        TenantContext.setCurrentTenant(TENANT_B);
        assertThat(usuarioRepo.findByUsername("isol-user-a"))
                .as("Tenant B no debe encontrar al usuario de tenant A")
                .isEmpty();
        TenantContext.clear();

        TenantContext.setCurrentTenant(TENANT_A);
        assertThat(usuarioRepo.findByUsername("isol-user-a"))
                .as("Tenant A sí encuentra su usuario")
                .isPresent();
        TenantContext.clear();
    }

    // ── Queries nativas cross-tenant (admin) ─────────────────────────────

    @Test
    @DisplayName("findAllByTenantId retorna solo usuarios del tenant especificado")
    void findAllByTenantId_retornaSoloElTenantCorrecto() {
        usuarioRepo.insertarConTenant("isol-admin-a", "$2a$10$xxxxx.xxxxx", "ADMIN",      TENANT_A);
        usuarioRepo.insertarConTenant("isol-inv-b",   "$2a$10$xxxxx.xxxxx", "INVENTARIO", TENANT_B);

        var usuariosA = usuarioRepo.findAllByTenantId(TENANT_A);
        var usuariosB = usuarioRepo.findAllByTenantId(TENANT_B);

        assertThat(usuariosA).as("Tenant A tiene exactamente 1 usuario").hasSize(1);
        assertThat(usuariosA.get(0).getUsername()).isEqualTo("isol-admin-a");

        assertThat(usuariosB).as("Tenant B tiene exactamente 1 usuario").hasSize(1);
        assertThat(usuariosB.get(0).getUsername()).isEqualTo("isol-inv-b");

        assertThat(usuariosA.stream().anyMatch(u -> "isol-inv-b".equals(u.getUsername())))
                .as("Lista de A no debe contener usuarios de B").isFalse();
        assertThat(usuariosB.stream().anyMatch(u -> "isol-admin-a".equals(u.getUsername())))
                .as("Lista de B no debe contener usuarios de A").isFalse();
    }

    @Test
    @DisplayName("countByUsernameAndTenantId respeta el tenant especificado")
    void countByUsername_respetaTenantExplicito() {
        usuarioRepo.insertarConTenant("isol-count-user", "$2a$10$xxxxx.xxxxx", "ADMIN", TENANT_A);

        assertThat(usuarioRepo.countByUsernameAndTenantId("isol-count-user", TENANT_A))
                .as("Debe encontrar el usuario en su tenant").isEqualTo(1);

        assertThat(usuarioRepo.countByUsernameAndTenantId("isol-count-user", TENANT_B))
                .as("No debe encontrar el usuario en otro tenant").isZero();
    }
}
