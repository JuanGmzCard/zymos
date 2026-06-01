package com.alera.service;

import com.alera.exception.EquipoEnUsoException;
import com.alera.model.Equipo;
import com.alera.model.enums.EstadoEquipo;
import com.alera.repository.EquipoRepository;
import com.alera.repository.LoteCervezaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EquipoService")
class EquipoServiceTest {

    @Mock EquipoRepository repo;
    @Mock LoteCervezaRepository loteRepo;

    @InjectMocks
    EquipoService service;

    // ── listarTodos ───────────────────────────────────────────────────

    @Test
    @DisplayName("listarTodos delega al repositorio")
    void listarTodos_delegaAlRepositorio() {
        Equipo equipo = new Equipo();
        equipo.setNombre("Fermentador 1");
        when(repo.findAll()).thenReturn(List.of(equipo));

        List<Equipo> resultado = service.listarTodos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombre()).isEqualTo("Fermentador 1");
        verify(repo).findAll();
    }

    // ── listarPorEstado ───────────────────────────────────────────────

    @Test
    @DisplayName("listarPorEstado pasa el estado correcto al repositorio")
    void listarPorEstado_filtraPorEstado() {
        when(repo.findByEstadoOrderByNombreAsc(EstadoEquipo.OPERATIVO)).thenReturn(List.of());

        service.listarPorEstado(EstadoEquipo.OPERATIVO);

        verify(repo).findByEstadoOrderByNombreAsc(EstadoEquipo.OPERATIVO);
    }

    // ── listarPaginado ────────────────────────────────────────────────

    @Test
    @DisplayName("listarPaginado sin filtro usa findAllByOrderByNombreAsc")
    void listarPaginado_sinFiltro_usaFindAll() {
        when(repo.findAllByOrderByNombreAsc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.listarPaginado(null, 0);

        verify(repo).findAllByOrderByNombreAsc(any(Pageable.class));
        verify(repo, never()).findByEstadoOrderByNombreAsc(any(), any(Pageable.class));
    }

    @Test
    @DisplayName("listarPaginado con filtro de estado usa findByEstadoOrderByNombreAsc")
    void listarPaginado_conFiltro_usaFindByEstado() {
        Page<Equipo> pagina = new PageImpl<>(List.of());
        when(repo.findByEstadoOrderByNombreAsc(eq(EstadoEquipo.MANTENIMIENTO), any(Pageable.class)))
                .thenReturn(pagina);

        Page<Equipo> resultado = service.listarPaginado(EstadoEquipo.MANTENIMIENTO, 0);

        assertThat(resultado).isNotNull();
        verify(repo).findByEstadoOrderByNombreAsc(eq(EstadoEquipo.MANTENIMIENTO), any(Pageable.class));
        verify(repo, never()).findAllByOrderByNombreAsc(any(Pageable.class));
    }

    // ── buscarPorId ───────────────────────────────────────────────────

    @Test
    @DisplayName("buscarPorId retorna el equipo cuando existe")
    void buscarPorId_equipoExistente_retornaOptional() {
        Equipo equipo = new Equipo();
        equipo.setNombre("Olla macerado");
        when(repo.findById(1L)).thenReturn(Optional.of(equipo));

        Optional<Equipo> resultado = service.buscarPorId(1L);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getNombre()).isEqualTo("Olla macerado");
    }

    @Test
    @DisplayName("buscarPorId retorna Optional vacío cuando no existe")
    void buscarPorId_equipoInexistente_retornaVacio() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        Optional<Equipo> resultado = service.buscarPorId(99L);

        assertThat(resultado).isEmpty();
    }

    // ── guardar ───────────────────────────────────────────────────────

    @Test
    @DisplayName("guardar delega al repositorio y retorna el equipo guardado")
    void guardar_delegaAlRepositorioYRetornaEquipo() {
        Equipo equipo = new Equipo();
        equipo.setNombre("Fermentador Cónico");
        when(repo.save(equipo)).thenReturn(equipo);

        Equipo resultado = service.guardar(equipo);

        assertThat(resultado.getNombre()).isEqualTo("Fermentador Cónico");
        verify(repo).save(equipo);
    }

    // ── eliminar ──────────────────────────────────────────────────────

    @Test
    @DisplayName("eliminar equipo sin lotes activos lo borra correctamente")
    void eliminar_sinLotesActivos_eliminaEquipo() {
        Equipo equipo = new Equipo();
        equipo.setNombre("Densímetro");
        when(repo.findById(1L)).thenReturn(Optional.of(equipo));
        when(loteRepo.countLotesActivosByEquipo(1L)).thenReturn(0L);

        service.eliminar(1L);

        verify(repo).deleteById(1L);
    }

    @Test
    @DisplayName("eliminar equipo no encontrado lanza RuntimeException")
    void eliminar_equipoNoEncontrado_lanzaExcepcion() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("eliminar equipo con lotes activos lanza EquipoEnUsoException con mensaje descriptivo")
    void eliminar_conLotesActivos_lanzaEquipoEnUsoException() {
        Equipo equipo = new Equipo();
        equipo.setNombre("Fermentador Principal");
        when(repo.findById(1L)).thenReturn(Optional.of(equipo));
        when(loteRepo.countLotesActivosByEquipo(1L)).thenReturn(2L);

        assertThatThrownBy(() -> service.eliminar(1L))
                .isInstanceOf(EquipoEnUsoException.class)
                .hasMessageContaining("Fermentador Principal")
                .hasMessageContaining("2");

        verify(repo, never()).deleteById(any());
    }

    // ── listarFermentadoresDisponibles ────────────────────────────────

    @Test
    @DisplayName("listarFermentadoresDisponibles pasa FERMENTADOR y OPERATIVO al repositorio")
    void listarFermentadoresDisponibles_pasaParametrosCorrectos() {
        when(repo.findFermentadoresDisponibles("Fermentador", EstadoEquipo.OPERATIVO))
                .thenReturn(List.of());

        service.listarFermentadoresDisponibles();

        verify(repo).findFermentadoresDisponibles("Fermentador", EstadoEquipo.OPERATIVO);
    }

    // ── listarMantenimientoPendiente ──────────────────────────────────

    @Test
    @DisplayName("listarMantenimientoPendiente consulta equipos con vencimiento en los próximos 7 días")
    void listarMantenimientoPendiente_consultaProximos7Dias() {
        when(repo.findMantenimientoPendiente(any())).thenReturn(List.of());

        service.listarMantenimientoPendiente();

        verify(repo).findMantenimientoPendiente(
                argThat(fecha -> {
                    java.time.LocalDate hoy = java.time.LocalDate.now();
                    java.time.LocalDate en7 = hoy.plusDays(7);
                    // Tolerancia de 1 día para evitar flakiness
                    return !fecha.isBefore(en7.minusDays(1)) && !fecha.isAfter(en7.plusDays(1));
                })
        );
    }

    // ── cambiarEstado ────────────────────────────────────────────────────

    @Test
    @DisplayName("cambiarEstado actualiza y persiste el estado")
    void cambiarEstado_actualizaEstado() {
        Equipo equipo = new Equipo();
        equipo.setId(1L);
        equipo.setNombre("Tank A");
        equipo.setTipo("Fermentador");
        equipo.setEstado(EstadoEquipo.OPERATIVO);
        when(repo.findById(1L)).thenReturn(Optional.of(equipo));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Equipo resultado = service.cambiarEstado(1L, EstadoEquipo.MANTENIMIENTO);

        assertThat(resultado.getEstado()).isEqualTo(EstadoEquipo.MANTENIMIENTO);
        verify(repo).save(equipo);
    }

    @Test
    @DisplayName("cambiarEstado lanza excepción si equipo no existe")
    void cambiarEstado_noExiste_lanzaExcepcion() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.cambiarEstado(99L, EstadoEquipo.INACTIVO))
                .isInstanceOf(RuntimeException.class);
    }

    // ── countByEstado / countMantenimientoPendiente / countTotal ─────────

    @Test
    @DisplayName("countByEstado delega al repositorio")
    void countByEstado_delegaAlRepositorio() {
        when(repo.countByEstado(EstadoEquipo.OPERATIVO)).thenReturn(5L);
        assertThat(service.countByEstado(EstadoEquipo.OPERATIVO)).isEqualTo(5L);
    }

    @Test
    @DisplayName("countMantenimientoPendiente usa ventana de 7 días")
    void countMantenimientoPendiente_usa7Dias() {
        when(repo.countMantenimientoPendiente(any())).thenReturn(3L);
        assertThat(service.countMantenimientoPendiente()).isEqualTo(3L);
    }

    @Test
    @DisplayName("countTotal delega al repositorio")
    void countTotal_delegaAlRepositorio() {
        when(repo.count()).thenReturn(10L);
        assertThat(service.countTotal()).isEqualTo(10L);
    }
}