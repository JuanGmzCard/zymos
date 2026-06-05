package com.alera.service;

import com.alera.model.EvaluacionSensorial;
import com.alera.model.LoteCerveza;
import com.alera.repository.EvaluacionSensorialRepository;
import com.alera.repository.LoteCervezaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluacionSensorialServiceTest {

    @Mock EvaluacionSensorialRepository repo;
    @Mock LoteCervezaRepository loteRepo;

    @InjectMocks EvaluacionSensorialService service;

    // ── listarPorLote ──────────────────────────────────────────────────────

    @Test
    void listarPorLote_delegaARepo() {
        EvaluacionSensorial ev = new EvaluacionSensorial();
        when(repo.findByLoteIdOrdenadas(1L)).thenReturn(List.of(ev));

        List<EvaluacionSensorial> resultado = service.listarPorLote(1L);

        assertThat(resultado).hasSize(1);
        verify(repo).findByLoteIdOrdenadas(1L);
    }

    @Test
    void listarPorLote_sinEvaluaciones_retornaVacio() {
        when(repo.findByLoteIdOrdenadas(99L)).thenReturn(List.of());
        assertThat(service.listarPorLote(99L)).isEmpty();
    }

    // ── agregar ────────────────────────────────────────────────────────────

    @Test
    void agregar_persisteEvaluacionConTodosLosCampos() {
        LoteCerveza lote = new LoteCerveza();
        when(loteRepo.findById(1L)).thenReturn(Optional.of(lote));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.agregar(1L, LocalDate.of(2026, 6, 5), "Juan Catador",
                10, 3, 18, 4, 9, "Notas de cata");

        ArgumentCaptor<EvaluacionSensorial> captor = ArgumentCaptor.forClass(EvaluacionSensorial.class);
        verify(repo).save(captor.capture());
        EvaluacionSensorial guardada = captor.getValue();

        assertThat(guardada.getLote()).isSameAs(lote);
        assertThat(guardada.getFecha()).isEqualTo(LocalDate.of(2026, 6, 5));
        assertThat(guardada.getCatador()).isEqualTo("Juan Catador");
        assertThat(guardada.getAroma()).isEqualTo(10);
        assertThat(guardada.getApariencia()).isEqualTo(3);
        assertThat(guardada.getSabor()).isEqualTo(18);
        assertThat(guardada.getSensacionBoca()).isEqualTo(4);
        assertThat(guardada.getImpresionGeneral()).isEqualTo(9);
        assertThat(guardada.getNotas()).isEqualTo("Notas de cata");
    }

    @Test
    void agregar_catadorBlanco_sanitizaANull() {
        LoteCerveza lote = new LoteCerveza();
        when(loteRepo.findById(1L)).thenReturn(Optional.of(lote));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.agregar(1L, LocalDate.now(), "   ", null, null, null, null, null, null);

        ArgumentCaptor<EvaluacionSensorial> captor = ArgumentCaptor.forClass(EvaluacionSensorial.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getCatador()).isNull();
    }

    @Test
    void agregar_notasBlanco_sanitizaANull() {
        LoteCerveza lote = new LoteCerveza();
        when(loteRepo.findById(1L)).thenReturn(Optional.of(lote));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.agregar(1L, LocalDate.now(), null, null, null, null, null, null, "  ");

        ArgumentCaptor<EvaluacionSensorial> captor = ArgumentCaptor.forClass(EvaluacionSensorial.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getNotas()).isNull();
    }

    @Test
    void agregar_loteNoExiste_lanzaExcepcion() {
        when(loteRepo.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.agregar(99L, LocalDate.now(), null,
                null, null, null, null, null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    // ── eliminar ───────────────────────────────────────────────────────────

    @Test
    void eliminar_llamaDeleteById() {
        service.eliminar(7L);
        verify(repo).deleteById(7L);
    }

    // ── calcularPromedio ───────────────────────────────────────────────────

    @Test
    void calcularPromedio_retornaPromedioCorrectamente() {
        EvaluacionSensorial ev1 = evalConPuntajes(10, 3, 18, 4, 9); // total 44
        EvaluacionSensorial ev2 = evalConPuntajes(8, 2, 15, 3, 7);  // total 35

        Double promedio = service.calcularPromedio(List.of(ev1, ev2));

        assertThat(promedio).isEqualTo(39.5);
    }

    @Test
    void calcularPromedio_listaVacia_retornaCero() {
        assertThat(service.calcularPromedio(List.of())).isEqualTo(0.0);
    }

    @Test
    void calcularPromedio_evaluacionSinPuntaje_excluida() {
        EvaluacionSensorial conPuntaje = evalConPuntajes(10, 3, 18, 4, 9); // total 44
        EvaluacionSensorial sinPuntaje = new EvaluacionSensorial(); // todos null

        Double promedio = service.calcularPromedio(List.of(conPuntaje, sinPuntaje));

        assertThat(promedio).isEqualTo(44.0);
    }

    // ── Métodos computados de la entidad ───────────────────────────────────

    @Test
    void puntajeTotal_calculaCorrectamente() {
        EvaluacionSensorial ev = evalConPuntajes(10, 3, 18, 4, 9);
        assertThat(ev.getPuntajeTotal()).isEqualTo(44);
    }

    @Test
    void puntajeTotal_todosNull_retornaNull() {
        assertThat(new EvaluacionSensorial().getPuntajeTotal()).isNull();
    }

    @Test
    void puntajeTotal_algunosNull_sumaLosPresentes() {
        EvaluacionSensorial ev = new EvaluacionSensorial();
        ev.setAroma(10);
        ev.setSabor(18);
        assertThat(ev.getPuntajeTotal()).isEqualTo(28);
    }

    @Test
    void clasificacion_correctaPorRango() {
        assertThat(evalConTotal(50).getClasificacion()).isEqualTo("Excepcional");
        assertThat(evalConTotal(47).getClasificacion()).isEqualTo("Excepcional");
        assertThat(evalConTotal(38).getClasificacion()).isEqualTo("Excelente");
        assertThat(evalConTotal(30).getClasificacion()).isEqualTo("Muy buena");
        assertThat(evalConTotal(21).getClasificacion()).isEqualTo("Buena");
        assertThat(evalConTotal(14).getClasificacion()).isEqualTo("Aceptable");
        assertThat(evalConTotal(7).getClasificacion()).isEqualTo("Deficiente");
        assertThat(evalConTotal(0).getClasificacion()).isEqualTo("Inaceptable");
    }

    @Test
    void badgeClass_correctaPorRango() {
        assertThat(evalConTotal(47).getBadgeClass()).isEqualTo("bg-warning text-dark");
        assertThat(evalConTotal(38).getBadgeClass()).isEqualTo("bg-success");
        assertThat(evalConTotal(30).getBadgeClass()).isEqualTo("bg-info text-dark");
        assertThat(evalConTotal(21).getBadgeClass()).isEqualTo("bg-primary");
        assertThat(evalConTotal(14).getBadgeClass()).isEqualTo("bg-secondary");
        assertThat(evalConTotal(6).getBadgeClass()).isEqualTo("bg-danger");
        assertThat(new EvaluacionSensorial().getBadgeClass()).isEqualTo("bg-secondary");
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private EvaluacionSensorial evalConPuntajes(int aroma, int apariencia, int sabor,
                                                 int boca, int general) {
        EvaluacionSensorial ev = new EvaluacionSensorial();
        ev.setAroma(aroma);
        ev.setApariencia(apariencia);
        ev.setSabor(sabor);
        ev.setSensacionBoca(boca);
        ev.setImpresionGeneral(general);
        return ev;
    }

    private EvaluacionSensorial evalConTotal(int total) {
        // Distribuye el total en sabor (max 20) + aroma (max 12) + resto
        EvaluacionSensorial ev = new EvaluacionSensorial();
        int sabor = Math.min(total, 20);
        int aroma = Math.min(total - sabor, 12);
        int resto = total - sabor - aroma;
        int boca = Math.min(resto, 5);
        int general = Math.min(resto - boca, 10);
        int apariencia = Math.min(resto - boca - general, 3);
        ev.setSabor(sabor);
        ev.setAroma(aroma);
        ev.setSensacionBoca(boca);
        ev.setImpresionGeneral(general);
        ev.setApariencia(apariencia);
        return ev;
    }
}
