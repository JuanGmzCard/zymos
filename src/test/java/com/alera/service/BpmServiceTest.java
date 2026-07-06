package com.alera.service;

import com.alera.model.*;
import com.alera.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BpmService")
class BpmServiceTest {

    @Mock RegistroSintomasRepository     sintomasRepo;
    @Mock SolucionDesinfectanteRepository solucionesRepo;
    @Mock AvistamientoPlagasRepository   plagasRepo;
    @Mock EvacuacionResiduosRepository   residuosRepo;
    @Mock LimpiezaDesinfeccionRepository limpiezaRepo;

    @InjectMocks BpmService service;

    private static final LocalDate INICIO = LocalDate.of(2025, 1, 1);
    private static final LocalDate FIN    = LocalDate.of(2025, 1, 31);

    // ── Síntomas ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("buscarSintoma — id existe: retorna el registro")
    void buscarSintoma_existe_retornaRegistro() {
        RegistroSintomas r = new RegistroSintomas();
        when(sintomasRepo.findById(1L)).thenReturn(Optional.of(r));

        assertThat(service.buscarSintoma(1L)).isSameAs(r);
    }

    @Test
    @DisplayName("buscarSintoma — id no existe: lanza EntityNotFoundException")
    void buscarSintoma_noExiste_lanzaEntityNotFoundException() {
        when(sintomasRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarSintoma(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("guardarSintoma delega al repositorio")
    void guardarSintoma_delegaAlRepo() {
        RegistroSintomas r = new RegistroSintomas();
        service.guardarSintoma(r);
        verify(sintomasRepo).save(r);
    }

    @Test
    @DisplayName("eliminarSintoma delega al repositorio con el id correcto")
    void eliminarSintoma_delegaAlRepo() {
        service.eliminarSintoma(5L);
        verify(sintomasRepo).deleteById(5L);
    }

    @Test
    @DisplayName("contarSintomasMes delega al repositorio con el rango de fechas")
    void contarSintomasMes_delegaAlRepo() {
        when(sintomasRepo.countByFechaBetween(INICIO, FIN)).thenReturn(3L);

        assertThat(service.contarSintomasMes(INICIO, FIN)).isEqualTo(3L);
        verify(sintomasRepo).countByFechaBetween(INICIO, FIN);
    }

    // ── Soluciones Desinfectantes ─────────────────────────────────────────────

    @Test
    @DisplayName("buscarSolucion — id no existe: lanza EntityNotFoundException")
    void buscarSolucion_noExiste_lanzaEntityNotFoundException() {
        when(solucionesRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarSolucion(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("guardarSolucion delega al repositorio")
    void guardarSolucion_delegaAlRepo() {
        SolucionDesinfectante r = new SolucionDesinfectante();
        service.guardarSolucion(r);
        verify(solucionesRepo).save(r);
    }

    @Test
    @DisplayName("eliminarSolucion delega al repositorio con el id correcto")
    void eliminarSolucion_delegaAlRepo() {
        service.eliminarSolucion(7L);
        verify(solucionesRepo).deleteById(7L);
    }

    @Test
    @DisplayName("contarSolucionesMes delega al repositorio con el rango de fechas")
    void contarSolucionesMes_delegaAlRepo() {
        when(solucionesRepo.countByFechaBetween(INICIO, FIN)).thenReturn(2L);

        assertThat(service.contarSolucionesMes(INICIO, FIN)).isEqualTo(2L);
    }

    // ── Plagas ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("buscarPlaga — id no existe: lanza EntityNotFoundException")
    void buscarPlaga_noExiste_lanzaEntityNotFoundException() {
        when(plagasRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPlaga(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("guardarPlaga delega al repositorio")
    void guardarPlaga_delegaAlRepo() {
        AvistamientoPlagas r = new AvistamientoPlagas();
        service.guardarPlaga(r);
        verify(plagasRepo).save(r);
    }

    @Test
    @DisplayName("eliminarPlaga delega al repositorio con el id correcto")
    void eliminarPlaga_delegaAlRepo() {
        service.eliminarPlaga(3L);
        verify(plagasRepo).deleteById(3L);
    }

    @Test
    @DisplayName("contarPlagasMes delega al repositorio con el rango de fechas")
    void contarPlagasMes_delegaAlRepo() {
        when(plagasRepo.countByFechaBetween(INICIO, FIN)).thenReturn(1L);

        assertThat(service.contarPlagasMes(INICIO, FIN)).isEqualTo(1L);
    }

    // ── Residuos ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("buscarResiduo — id no existe: lanza EntityNotFoundException")
    void buscarResiduo_noExiste_lanzaEntityNotFoundException() {
        when(residuosRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarResiduo(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("guardarResiduo delega al repositorio")
    void guardarResiduo_delegaAlRepo() {
        EvacuacionResiduos r = new EvacuacionResiduos();
        service.guardarResiduo(r);
        verify(residuosRepo).save(r);
    }

    @Test
    @DisplayName("eliminarResiduo delega al repositorio con el id correcto")
    void eliminarResiduo_delegaAlRepo() {
        service.eliminarResiduo(8L);
        verify(residuosRepo).deleteById(8L);
    }

    @Test
    @DisplayName("contarResiduosMes delega al repositorio con el rango de fechas")
    void contarResiduosMes_delegaAlRepo() {
        when(residuosRepo.countByFechaBetween(INICIO, FIN)).thenReturn(4L);

        assertThat(service.contarResiduosMes(INICIO, FIN)).isEqualTo(4L);
    }

    // ── Limpieza ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("buscarLimpieza — id no existe: lanza EntityNotFoundException")
    void buscarLimpieza_noExiste_lanzaEntityNotFoundException() {
        when(limpiezaRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarLimpieza(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("guardarLimpieza delega al repositorio")
    void guardarLimpieza_delegaAlRepo() {
        LimpiezaDesinfeccion r = new LimpiezaDesinfeccion();
        service.guardarLimpieza(r);
        verify(limpiezaRepo).save(r);
    }

    @Test
    @DisplayName("eliminarLimpieza delega al repositorio con el id correcto")
    void eliminarLimpieza_delegaAlRepo() {
        service.eliminarLimpieza(12L);
        verify(limpiezaRepo).deleteById(12L);
    }

    @Test
    @DisplayName("contarLimpiezaMes delega al repositorio con el rango de fechas")
    void contarLimpiezaMes_delegaAlRepo() {
        when(limpiezaRepo.countByFechaBetween(INICIO, FIN)).thenReturn(6L);

        assertThat(service.contarLimpiezaMes(INICIO, FIN)).isEqualTo(6L);
    }
}
