package com.alera.repository;

import com.alera.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de integración para las queries clave del repositorio.
 * Cada test corre contra PostgreSQL real; @Transactional hace rollback automático.
 */
@Transactional
class LoteCervezaRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private LoteCervezaRepository repo;

    @Test
    void countQueriesRetornanCeroEnBdVacia() {
        assertThat(repo.countEnProceso()).isZero();
        assertThat(repo.countCompletados()).isZero();
        assertThat(repo.countDistinctEstilos()).isZero();
    }

    @Test
    void findByFiltrosConParametrosVaciosRetornaPaginaVacia() {
        var pagina = repo.findByFiltros("", "", null, null, PageRequest.of(0, 15));
        assertThat(pagina).isNotNull();
        assertThat(pagina.getTotalElements()).isZero();
        assertThat(pagina.getContent()).isEmpty();
    }

    @Test
    void findTop5SinDatosRetornaListaVacia() {
        var top5 = repo.findTop5(PageRequest.of(0, 5));
        assertThat(top5).isEmpty();
    }

    @Test
    void findLitrosPorMesNoLanzaExcepcion() {
        var resultado = repo.findLitrosPorMes(LocalDate.now().minusMonths(6), "default");
        assertThat(resultado).isNotNull();
    }

    @Test
    void findLotesPorEstiloNoLanzaExcepcion() {
        var resultado = repo.findLotesPorEstilo("default");
        assertThat(resultado).isNotNull().isEmpty();
    }

    @Test
    void findParaKanbanNoLanzaExcepcion() {
        var resultado = repo.findParaKanban(LocalDate.now().minusDays(7));
        assertThat(resultado).isNotNull().isEmpty();
    }
}
