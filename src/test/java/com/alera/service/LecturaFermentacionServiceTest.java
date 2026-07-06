package com.alera.service;

import com.alera.model.LecturaFermentacion;
import com.alera.model.LoteCerveza;
import com.alera.repository.LecturaFermentacionRepository;
import com.alera.repository.LoteCervezaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LecturaFermentacionService")
class LecturaFermentacionServiceTest {

    @Mock LecturaFermentacionRepository repo;
    @Mock LoteCervezaRepository loteRepo;

    @InjectMocks LecturaFermentacionService service;

    // ── listarPorLote ────────────────────────────────────────────────────────

    @Test
    @DisplayName("listarPorLote delega al repositorio y retorna su resultado")
    void listarPorLote_delegaAlRepo() {
        LecturaFermentacion lectura = new LecturaFermentacion();
        when(repo.findByLoteIdOrdenadas(5L)).thenReturn(List.of(lectura));

        List<LecturaFermentacion> resultado = service.listarPorLote(5L);

        assertThat(resultado).containsExactly(lectura);
        verify(repo).findByLoteIdOrdenadas(5L);
    }

    // ── agregar ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("agregar — lote existe: guarda la lectura con los datos correctos")
    void agregar_loteExiste_guardaLecturaConDatosCorrectos() {
        LoteCerveza lote = new LoteCerveza();
        lote.setId(1L);
        when(loteRepo.findById(1L)).thenReturn(Optional.of(lote));

        service.agregar(1L, LocalDate.of(2025, 3, 5), 1048, new BigDecimal("19.5"), "Va bien");

        ArgumentCaptor<LecturaFermentacion> captor = ArgumentCaptor.forClass(LecturaFermentacion.class);
        verify(repo).save(captor.capture());
        LecturaFermentacion guardada = captor.getValue();
        assertThat(guardada.getLote()).isSameAs(lote);
        assertThat(guardada.getFecha()).isEqualTo(LocalDate.of(2025, 3, 5));
        assertThat(guardada.getDensidad()).isEqualTo(1048);
        assertThat(guardada.getTemperatura()).isEqualByComparingTo("19.5");
        assertThat(guardada.getNotas()).isEqualTo("Va bien");
    }

    @Test
    @DisplayName("agregar — lote no existe: lanza RuntimeException con el id")
    void agregar_loteNoExiste_lanzaRuntimeException() {
        when(loteRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.agregar(99L, LocalDate.now(), 1050, null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");

        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("agregar — notas en blanco: guarda null en el campo notas")
    void agregar_notasEnBlanco_guardaNull() {
        LoteCerveza lote = new LoteCerveza();
        when(loteRepo.findById(1L)).thenReturn(Optional.of(lote));

        service.agregar(1L, LocalDate.now(), 1040, null, "   ");

        ArgumentCaptor<LecturaFermentacion> captor = ArgumentCaptor.forClass(LecturaFermentacion.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getNotas()).isNull();
    }

    @Test
    @DisplayName("agregar — notas null: guarda null en el campo notas")
    void agregar_notasNull_guardaNull() {
        LoteCerveza lote = new LoteCerveza();
        when(loteRepo.findById(1L)).thenReturn(Optional.of(lote));

        service.agregar(1L, LocalDate.now(), 1040, null, null);

        ArgumentCaptor<LecturaFermentacion> captor = ArgumentCaptor.forClass(LecturaFermentacion.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getNotas()).isNull();
    }

    @Test
    @DisplayName("agregar — notas con espacios: guarda trimmed")
    void agregar_notasConEspacios_guardaTrimmed() {
        LoteCerveza lote = new LoteCerveza();
        when(loteRepo.findById(1L)).thenReturn(Optional.of(lote));

        service.agregar(1L, LocalDate.now(), 1040, null, "  OK  ");

        ArgumentCaptor<LecturaFermentacion> captor = ArgumentCaptor.forClass(LecturaFermentacion.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getNotas()).isEqualTo("OK");
    }

    // ── eliminar ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("eliminar delega a repo.deleteById con el id correcto")
    void eliminar_delegaAlRepo() {
        service.eliminar(42L);

        verify(repo).deleteById(42L);
    }
}
