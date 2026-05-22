package com.alera.service;

import com.alera.AbstractIntegrationTest;
import com.alera.config.TenantContext;
import com.alera.model.ElaboracionPlanificada;
import com.alera.model.enums.EstadoPlanificacion;
import com.alera.repository.ElaboracionPlanificadaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de integración para PlanificacionService contra BD real (Testcontainers).
 * Verifica CRUD de elaboraciones planificadas con contexto de tenant.
 */
@Transactional
class PlanificacionServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired private PlanificacionService service;
    @Autowired private ElaboracionPlanificadaRepository repo;

    @BeforeEach void setUp()    { TenantContext.setCurrentTenant("default"); }
    @AfterEach  void tearDown() { TenantContext.clear(); }

    // ── Guardar ───────────────────────────────────────────────────────────

    @Test
    void guardarPlanLoGuardaConEstadoPlanificadaPorDefecto() {
        var plan = buildPlan("IPA Test", LocalDate.now().plusDays(7), null);

        service.guardar(plan, null);

        var todos = repo.findAllOrdenadas();
        assertThat(todos).hasSize(1);
        assertThat(todos.get(0).getNombreElaboracion()).isEqualTo("IPA Test");
        assertThat(todos.get(0).getEstado()).isEqualTo(EstadoPlanificacion.PLANIFICADA);
    }

    @Test
    void guardarPlanConVolumenLoPersisteCorrectamente() {
        var plan = buildPlan("Stout Imperial", LocalDate.now().plusDays(14), new BigDecimal("20.5"));

        service.guardar(plan, null);

        var guardado = repo.findAllOrdenadas().get(0);
        assertThat(guardado.getVolumenEstimado()).isEqualByComparingTo("20.5");
    }

    @Test
    void guardarDosPlanesMismoNombreLosPersisteSeparados() {
        service.guardar(buildPlan("APA", LocalDate.now().plusDays(5), null), null);
        service.guardar(buildPlan("APA", LocalDate.now().plusDays(12), null), null);

        assertThat(repo.findAllOrdenadas()).hasSize(2);
    }

    // ── Cambiar estado ────────────────────────────────────────────────────

    @Test
    void cambiarEstadoActualizaCorrectamente() {
        var plan = buildPlan("Wheat", LocalDate.now().plusDays(3), null);
        var guardado = service.guardar(plan, null);

        service.cambiarEstado(guardado.getId(), EstadoPlanificacion.EN_PROCESO);

        var actualizado = service.buscarPorId(guardado.getId()).orElseThrow();
        assertThat(actualizado.getEstado()).isEqualTo(EstadoPlanificacion.EN_PROCESO);
    }

    @Test
    void flujoCompletoEstados() {
        var plan = buildPlan("Lager", LocalDate.now().plusDays(1), null);
        var guardado = service.guardar(plan, null);

        service.cambiarEstado(guardado.getId(), EstadoPlanificacion.EN_PROCESO);
        assertThat(service.buscarPorId(guardado.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoPlanificacion.EN_PROCESO);

        service.cambiarEstado(guardado.getId(), EstadoPlanificacion.COMPLETADA);
        assertThat(service.buscarPorId(guardado.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoPlanificacion.COMPLETADA);
    }

    @Test
    void cancelarPlanActualizaEstado() {
        var plan = buildPlan("Porter", LocalDate.now().plusDays(2), null);
        var guardado = service.guardar(plan, null);

        service.cambiarEstado(guardado.getId(), EstadoPlanificacion.CANCELADA);

        assertThat(service.buscarPorId(guardado.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoPlanificacion.CANCELADA);
    }

    // ── Listar ────────────────────────────────────────────────────────────

    @Test
    void listarProximasRetornaDesdeAyerEnAdelante() {
        service.guardar(buildPlan("Plan Pasado",  LocalDate.now().minusDays(5), null), null);
        service.guardar(buildPlan("Plan Próximo", LocalDate.now().plusDays(3), null), null);
        service.guardar(buildPlan("Plan Hoy",     LocalDate.now(),               null), null);

        var proximas = service.listarProximas();

        assertThat(proximas).hasSize(2); // "Hoy" y "Próximo" — "Pasado" excluido
        assertThat(proximas).extracting(ElaboracionPlanificada::getNombreElaboracion)
                .doesNotContain("Plan Pasado");
    }

    @Test
    void listarPorRangoRetornaSoloElRangoIndicado() {
        service.guardar(buildPlan("Fuera de rango",    LocalDate.now().plusMonths(2), null), null);
        service.guardar(buildPlan("Dentro del rango",  LocalDate.now().plusDays(7),  null), null);

        var enRango = service.listarPorRango(LocalDate.now(), LocalDate.now().plusMonths(1));

        assertThat(enRango).hasSize(1);
        assertThat(enRango.get(0).getNombreElaboracion()).isEqualTo("Dentro del rango");
    }

    // ── Eliminar ──────────────────────────────────────────────────────────

    @Test
    void eliminarPlanLoEliminaDeBaseDeDatos() {
        var plan = buildPlan("Para Eliminar", LocalDate.now().plusDays(1), null);
        var guardado = service.guardar(plan, null);
        assertThat(repo.findById(guardado.getId())).isPresent();

        service.eliminar(guardado.getId());

        assertThat(repo.findById(guardado.getId())).isEmpty();
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private ElaboracionPlanificada buildPlan(String nombre, LocalDate fecha, BigDecimal volumen) {
        var p = new ElaboracionPlanificada();
        p.setNombreElaboracion(nombre);
        p.setFechaPlaneada(fecha);
        p.setVolumenEstimado(volumen);
        return p;
    }
}
