package com.alera.service;

import com.alera.AbstractIntegrationTest;
import com.alera.config.TenantContext;
import com.alera.dto.LoteFormDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de integración para LecturaFermentacionService contra BD real.
 * Verifica registro, ordenamiento y eliminación de lecturas de fermentación.
 */
@Transactional
class LecturaFermentacionServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired private LecturaFermentacionService lecturaService;
    @Autowired private TrazabilidadService trazService;

    @BeforeEach void setUp()    { TenantContext.setCurrentTenant("default"); }
    @AfterEach  void tearDown() { TenantContext.clear(); }

    // ── Agregar lecturas ──────────────────────────────────────────────────

    @Test
    void agregarLecturaLaPersisteCorrectamente() {
        var lote = crearLote("Porter");

        lecturaService.agregar(lote.getId(), LocalDate.now(), 1040, new BigDecimal("18.5"), "Primera medición");

        var lecturas = lecturaService.listarPorLote(lote.getId());
        assertThat(lecturas).hasSize(1);
        assertThat(lecturas.get(0).getDensidad()).isEqualTo(1040);
        assertThat(lecturas.get(0).getTemperatura()).isEqualByComparingTo("18.5");
        assertThat(lecturas.get(0).getNotas()).isEqualTo("Primera medición");
    }

    @Test
    void agregarLecturaSinTemperaturaPermiteNull() {
        var lote = crearLote("IPA Sin Temp");

        lecturaService.agregar(lote.getId(), LocalDate.now(), 1055, null, null);

        var lectura = lecturaService.listarPorLote(lote.getId()).get(0);
        assertThat(lectura.getTemperatura()).isNull();
        assertThat(lectura.getDensidad()).isEqualTo(1055);
    }

    @Test
    void agregarLecturaSinDensidadPermiteNull() {
        var lote = crearLote("Stout Solo Temp");

        lecturaService.agregar(lote.getId(), LocalDate.now(), null, new BigDecimal("20.0"), null);

        var lectura = lecturaService.listarPorLote(lote.getId()).get(0);
        assertThat(lectura.getDensidad()).isNull();
        assertThat(lectura.getTemperatura()).isEqualByComparingTo("20.0");
    }

    @Test
    void notasBlancoSeSanitizanANull() {
        var lote = crearLote("Sanitize Test");

        lecturaService.agregar(lote.getId(), LocalDate.now(), 1048, null, "   ");

        assertThat(lecturaService.listarPorLote(lote.getId()).get(0).getNotas()).isNull();
    }

    // ── Ordenamiento ──────────────────────────────────────────────────────

    @Test
    void lecturasSeOrdenanPorFechaAscendente() {
        var lote = crearLote("Orden Test");

        lecturaService.agregar(lote.getId(), LocalDate.of(2025, 3, 10), 1020, null, null);
        lecturaService.agregar(lote.getId(), LocalDate.of(2025, 3, 1),  1058, null, null);
        lecturaService.agregar(lote.getId(), LocalDate.of(2025, 3, 5),  1040, null, null);

        var lecturas = lecturaService.listarPorLote(lote.getId());

        assertThat(lecturas).hasSize(3);
        assertThat(lecturas.get(0).getFecha()).isEqualTo(LocalDate.of(2025, 3, 1));
        assertThat(lecturas.get(1).getFecha()).isEqualTo(LocalDate.of(2025, 3, 5));
        assertThat(lecturas.get(2).getFecha()).isEqualTo(LocalDate.of(2025, 3, 10));
    }

    // ── ABV parcial ───────────────────────────────────────────────────────

    @Test
    void abvParcialCalculaCorrectamenteConOgDelLote() {
        var dto = LoteFormDto.empty();
        dto.setEstilo("ABV Test");
        dto.setDensidadInicial(1060);
        var lote = trazService.guardar(dto).getLote();

        lecturaService.agregar(lote.getId(), LocalDate.now(), 1020, null, null);

        var lectura = lecturaService.listarPorLote(lote.getId()).get(0);
        // ABV = (1060 - 1020) * 131.25 / 1000 = 5.25
        assertThat(lectura.getAbvParcial(1060))
                .isEqualByComparingTo(new BigDecimal("5.25"));
    }

    @Test
    void abvParcialRetornaNullSiDensidadLecturaEsNull() {
        var lote = crearLote("ABV Null Test");
        lecturaService.agregar(lote.getId(), LocalDate.now(), null, new BigDecimal("19.0"), null);

        var lectura = lecturaService.listarPorLote(lote.getId()).get(0);
        assertThat(lectura.getAbvParcial(1060)).isNull();
    }

    @Test
    void abvParcialRetornaNullSiLecturaIgualOG() {
        var lote = crearLote("Sin Atenuacion");
        lecturaService.agregar(lote.getId(), LocalDate.now(), 1060, null, null);

        var lectura = lecturaService.listarPorLote(lote.getId()).get(0);
        assertThat(lectura.getAbvParcial(1060)).isNull(); // densidad >= OG → null
    }

    // ── Eliminar ──────────────────────────────────────────────────────────

    @Test
    void eliminarLecturaLaEliminaDeBaseDeDatos() {
        var lote = crearLote("Eliminar Test");
        lecturaService.agregar(lote.getId(), LocalDate.now(), 1050, null, null);
        var id = lecturaService.listarPorLote(lote.getId()).get(0).getId();

        lecturaService.eliminar(id);

        assertThat(lecturaService.listarPorLote(lote.getId())).isEmpty();
    }

    @Test
    void eliminarUnaLecturaNoAfectaLasOtras() {
        var lote = crearLote("Multi Lectura");
        lecturaService.agregar(lote.getId(), LocalDate.of(2025, 4, 1), 1058, null, null);
        lecturaService.agregar(lote.getId(), LocalDate.of(2025, 4, 5), 1040, null, null);

        var idPrimera = lecturaService.listarPorLote(lote.getId()).get(0).getId();
        lecturaService.eliminar(idPrimera);

        var restantes = lecturaService.listarPorLote(lote.getId());
        assertThat(restantes).hasSize(1);
        assertThat(restantes.get(0).getDensidad()).isEqualTo(1040);
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private com.alera.model.LoteCerveza crearLote(String estilo) {
        var dto = LoteFormDto.empty();
        dto.setEstilo(estilo);
        return trazService.guardar(dto).getLote();
    }
}
