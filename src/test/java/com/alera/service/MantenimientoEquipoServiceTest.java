package com.alera.service;

import com.alera.dto.MantenimientoDto;
import com.alera.mapper.MantenimientoMapper;
import com.alera.model.Equipo;
import com.alera.model.MantenimientoEquipo;
import com.alera.model.enums.TipoMantenimiento;
import com.alera.repository.EquipoRepository;
import com.alera.repository.MantenimientoEquipoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MantenimientoEquipoService")
class MantenimientoEquipoServiceTest {

    @Mock MantenimientoEquipoRepository repo;
    @Mock EquipoRepository              equipoRepo;
    @Spy  MantenimientoMapper           mapper = Mappers.getMapper(MantenimientoMapper.class);

    @InjectMocks
    MantenimientoEquipoService service;

    private Equipo equipo(Long id, String nombre) {
        Equipo e = new Equipo();
        e.setId(id);
        e.setNombre(nombre);
        return e;
    }

    private MantenimientoDto dto(LocalDate fecha, LocalDate proximo) {
        MantenimientoDto d = new MantenimientoDto();
        d.setFecha(fecha);
        d.setTipo(TipoMantenimiento.PREVENTIVO);
        d.setDescripcion("Limpieza general");
        d.setTecnico("Juan");
        d.setCosto(new BigDecimal("150000"));
        d.setProximoMantenimiento(proximo);
        return d;
    }

    // ── listarPorEquipo ───────────────────────────────────────────────

    @Test
    @DisplayName("listarPorEquipo delega al repositorio ordenado por fecha DESC")
    void listarPorEquipo_delegaAlRepositorio() {
        MantenimientoEquipo m = new MantenimientoEquipo();
        when(repo.findByEquipoIdOrderByFechaDesc(1L)).thenReturn(List.of(m));

        List<MantenimientoEquipo> resultado = service.listarPorEquipo(1L);

        assertThat(resultado).hasSize(1);
        verify(repo).findByEquipoIdOrderByFechaDesc(1L);
    }

    @Test
    @DisplayName("listarPorEquipo retorna lista vacía si el equipo no tiene mantenimientos")
    void listarPorEquipo_sinMantenimientos_retornaVacio() {
        when(repo.findByEquipoIdOrderByFechaDesc(99L)).thenReturn(List.of());

        assertThat(service.listarPorEquipo(99L)).isEmpty();
    }

    // ── registrar ─────────────────────────────────────────────────────

    @Test
    @DisplayName("registrar crea el mantenimiento con todos los campos del DTO")
    void registrar_creaMantenimientoConCamposCorrectos() {
        Equipo equipo = equipo(1L, "Fermentador 1");
        LocalDate fecha = LocalDate.of(2025, 3, 15);
        LocalDate proximo = LocalDate.of(2025, 9, 15);
        MantenimientoDto dto = dto(fecha, proximo);

        when(equipoRepo.findById(1L)).thenReturn(Optional.of(equipo));
        MantenimientoEquipo guardado = new MantenimientoEquipo();
        when(repo.save(any())).thenReturn(guardado);

        service.registrar(1L, dto);

        ArgumentCaptor<MantenimientoEquipo> captor = ArgumentCaptor.forClass(MantenimientoEquipo.class);
        verify(repo).save(captor.capture());
        MantenimientoEquipo m = captor.getValue();
        assertThat(m.getEquipo()).isEqualTo(equipo);
        assertThat(m.getFecha()).isEqualTo(fecha);
        assertThat(m.getTipo()).isEqualTo(TipoMantenimiento.PREVENTIVO);
        assertThat(m.getDescripcion()).isEqualTo("Limpieza general");
        assertThat(m.getTecnico()).isEqualTo("Juan");
        assertThat(m.getCosto()).isEqualByComparingTo("150000");
        assertThat(m.getProximoMantenimiento()).isEqualTo(proximo);
    }

    @Test
    @DisplayName("registrar actualiza fechaUltimoMantenimiento en el equipo")
    void registrar_actualizaFechaUltimoMantenimientoEnEquipo() {
        Equipo equipo = equipo(1L, "Fermentador 1");
        LocalDate fecha = LocalDate.of(2025, 3, 15);
        MantenimientoDto dto = dto(fecha, LocalDate.of(2025, 9, 15));

        when(equipoRepo.findById(1L)).thenReturn(Optional.of(equipo));
        when(repo.save(any())).thenReturn(new MantenimientoEquipo());

        service.registrar(1L, dto);

        ArgumentCaptor<Equipo> equipoCaptor = ArgumentCaptor.forClass(Equipo.class);
        verify(equipoRepo).save(equipoCaptor.capture());
        assertThat(equipoCaptor.getValue().getFechaUltimoMantenimiento()).isEqualTo(fecha);
    }

    @Test
    @DisplayName("registrar actualiza proximoMantenimiento en el equipo cuando está definido")
    void registrar_actualizaProximoMantenimiento() {
        Equipo equipo = equipo(1L, "Fermentador 1");
        LocalDate proximo = LocalDate.of(2025, 9, 15);
        MantenimientoDto dto = dto(LocalDate.now(), proximo);

        when(equipoRepo.findById(1L)).thenReturn(Optional.of(equipo));
        when(repo.save(any())).thenReturn(new MantenimientoEquipo());

        service.registrar(1L, dto);

        ArgumentCaptor<Equipo> equipoCaptor = ArgumentCaptor.forClass(Equipo.class);
        verify(equipoRepo).save(equipoCaptor.capture());
        assertThat(equipoCaptor.getValue().getProximoMantenimiento()).isEqualTo(proximo);
    }

    @Test
    @DisplayName("registrar no actualiza proximoMantenimiento si el DTO lo tiene null")
    void registrar_proximoMantenimientoNull_noActualiza() {
        Equipo equipo = equipo(1L, "Fermentador 1");
        LocalDate fechaAnterior = LocalDate.of(2024, 1, 1);
        equipo.setProximoMantenimiento(fechaAnterior);

        MantenimientoDto dto = dto(LocalDate.now(), null); // proximo = null

        when(equipoRepo.findById(1L)).thenReturn(Optional.of(equipo));
        when(repo.save(any())).thenReturn(new MantenimientoEquipo());

        service.registrar(1L, dto);

        ArgumentCaptor<Equipo> equipoCaptor = ArgumentCaptor.forClass(Equipo.class);
        verify(equipoRepo).save(equipoCaptor.capture());
        assertThat(equipoCaptor.getValue().getProximoMantenimiento()).isEqualTo(fechaAnterior);
    }

    @Test
    @DisplayName("registrar lanza RuntimeException cuando el equipo no existe")
    void registrar_equipoNoExiste_lanzaExcepcion() {
        when(equipoRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(99L, dto(LocalDate.now(), null)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");

        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("registrar retorna el mantenimiento guardado")
    void registrar_retornaMantenimientoGuardado() {
        Equipo equipo = equipo(1L, "Fermentador 1");
        MantenimientoEquipo esperado = new MantenimientoEquipo();
        when(equipoRepo.findById(1L)).thenReturn(Optional.of(equipo));
        when(repo.save(any())).thenReturn(esperado);

        MantenimientoEquipo resultado = service.registrar(1L, dto(LocalDate.now(), null));

        assertThat(resultado).isSameAs(esperado);
    }

    // ── eliminar ──────────────────────────────────────────────────────

    @Test
    @DisplayName("eliminar delega a deleteById")
    void eliminar_delegaADeleteById() {
        service.eliminar(5L);

        verify(repo).deleteById(5L);
    }
}
