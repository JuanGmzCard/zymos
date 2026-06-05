package com.alera.service;

import com.alera.model.Barril;
import com.alera.model.MovimientoBarril;
import com.alera.model.enums.EstadoBarril;
import com.alera.repository.BarrilRepository;
import com.alera.repository.MovimientoBarrilRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BarrilServiceTest {

    @Mock BarrilRepository           barrilRepo;
    @Mock MovimientoBarrilRepository movimientoRepo;

    @InjectMocks BarrilService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "pageSize", 15);
        lenient().when(movimientoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ── listarPaginado ─────────────────────────────────────────────────────

    @Test
    void listarPaginado_delegaARepo() {
        Page<Barril> pagina = new PageImpl<>(List.of());
        when(barrilRepo.findByFiltros(anyString(), isNull(), any())).thenReturn(pagina);

        Page<Barril> result = service.listarPaginado(null, null, 0);

        assertThat(result).isNotNull();
        verify(barrilRepo).findByFiltros(eq(""), isNull(), any());
    }

    @Test
    void listarPaginado_conFiltros_pasaCorrectamente() {
        when(barrilRepo.findByFiltros(anyString(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        service.listarPaginado("KEG", EstadoBarril.DISPONIBLE, 0);

        verify(barrilRepo).findByFiltros(eq("KEG"), eq(EstadoBarril.DISPONIBLE), any());
    }

    // ── buscarPorId ────────────────────────────────────────────────────────

    @Test
    void buscarPorId_encontrado_retornaBarril() {
        Barril b = barrilConId(1L, "KEG-001");
        when(barrilRepo.findById(1L)).thenReturn(Optional.of(b));

        Barril result = service.buscarPorId(1L);

        assertThat(result.getCodigo()).isEqualTo("KEG-001");
    }

    @Test
    void buscarPorId_noExiste_lanzaExcepcion() {
        when(barrilRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    // ── guardar ────────────────────────────────────────────────────────────

    @Test
    void guardar_persisteBarrilConEstadoPorDefecto() {
        when(barrilRepo.existsByCodigoIgnoreCase("KEG-001")).thenReturn(false);
        when(barrilRepo.save(any())).thenAnswer(inv -> {
            Barril b = inv.getArgument(0);
            b.setId(1L);
            return b;
        });

        Barril barril = new Barril();
        barril.setCodigo("KEG-001");
        barril.setTipo("Keg 20L");
        barril.setCapacidadLitros(new BigDecimal("20"));

        Barril guardado = service.guardar(barril);

        assertThat(guardado.getId()).isEqualTo(1L);
        assertThat(guardado.getEstado()).isEqualTo(EstadoBarril.DISPONIBLE);
    }

    @Test
    void guardar_codigoDuplicado_lanzaExcepcion() {
        when(barrilRepo.existsByCodigoIgnoreCase("KEG-001")).thenReturn(true);

        Barril barril = new Barril();
        barril.setCodigo("KEG-001");

        assertThatThrownBy(() -> service.guardar(barril))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("KEG-001");
    }

    @Test
    void guardar_creaMovimientoInicial() {
        when(barrilRepo.existsByCodigoIgnoreCase(anyString())).thenReturn(false);
        when(barrilRepo.save(any())).thenAnswer(inv -> {
            Barril b = inv.getArgument(0);
            b.setId(5L);
            return b;
        });

        Barril barril = new Barril();
        barril.setCodigo("KEG-002");

        service.guardar(barril);

        ArgumentCaptor<MovimientoBarril> captor = ArgumentCaptor.forClass(MovimientoBarril.class);
        verify(movimientoRepo).save(captor.capture());
        MovimientoBarril mov = captor.getValue();
        assertThat(mov.getEstadoAnterior()).isNull();
        assertThat(mov.getEstadoNuevo()).isEqualTo(EstadoBarril.DISPONIBLE);
    }

    @Test
    void guardar_normalizaBlancos() {
        when(barrilRepo.existsByCodigoIgnoreCase(anyString())).thenReturn(false);
        when(barrilRepo.save(any())).thenAnswer(inv -> {
            Barril b = inv.getArgument(0);
            b.setId(1L);
            return b;
        });

        Barril barril = new Barril();
        barril.setCodigo("KEG-003");
        barril.setClienteNombre("  ");
        barril.setObservaciones("   ");

        service.guardar(barril);

        ArgumentCaptor<Barril> captor = ArgumentCaptor.forClass(Barril.class);
        verify(barrilRepo).save(captor.capture());
        assertThat(captor.getValue().getClienteNombre()).isNull();
        assertThat(captor.getValue().getObservaciones()).isNull();
    }

    // ── actualizar ─────────────────────────────────────────────────────────

    @Test
    void actualizar_modificaCodigo() {
        Barril existente = barrilConId(1L, "KEG-001");
        when(barrilRepo.findById(1L)).thenReturn(Optional.of(existente));
        when(barrilRepo.existsByCodigoIgnoreCaseAndIdNot("KEG-001-MOD", 1L)).thenReturn(false);
        when(barrilRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Barril nuevo = new Barril();
        nuevo.setCodigo("KEG-001-MOD");

        Barril resultado = service.actualizar(1L, nuevo);

        assertThat(resultado.getId()).isEqualTo(1L);
        verify(barrilRepo).save(argThat(b -> b.getId().equals(1L)));
    }

    @Test
    void actualizar_mismoCodigoNoVerificaUnicidad() {
        Barril existente = barrilConId(1L, "KEG-001");
        when(barrilRepo.findById(1L)).thenReturn(Optional.of(existente));
        when(barrilRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Barril mismo = new Barril();
        mismo.setCodigo("KEG-001");

        service.actualizar(1L, mismo);

        verify(barrilRepo, never()).existsByCodigoIgnoreCaseAndIdNot(any(), any());
    }

    // ── cambiarEstado ──────────────────────────────────────────────────────

    @Test
    void cambiarEstado_actualizaEstadoYCreaMovimiento() {
        Barril existente = barrilConId(1L, "KEG-001");
        existente.setEstado(EstadoBarril.LLENO);
        when(barrilRepo.findById(1L)).thenReturn(Optional.of(existente));
        when(barrilRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.cambiarEstado(1L, EstadoBarril.DESPACHADO, "Despacho a Bar Espuma");

        ArgumentCaptor<Barril> barrilCaptor = ArgumentCaptor.forClass(Barril.class);
        verify(barrilRepo).save(barrilCaptor.capture());
        assertThat(barrilCaptor.getValue().getEstado()).isEqualTo(EstadoBarril.DESPACHADO);

        ArgumentCaptor<MovimientoBarril> movCaptor = ArgumentCaptor.forClass(MovimientoBarril.class);
        verify(movimientoRepo).save(movCaptor.capture());
        MovimientoBarril mov = movCaptor.getValue();
        assertThat(mov.getEstadoAnterior()).isEqualTo(EstadoBarril.LLENO);
        assertThat(mov.getEstadoNuevo()).isEqualTo(EstadoBarril.DESPACHADO);
        assertThat(mov.getNotas()).isEqualTo("Despacho a Bar Espuma");
    }

    @Test
    void cambiarEstado_aDisponible_limpiaLoteYCliente() {
        Barril existente = barrilConId(1L, "KEG-001");
        existente.setEstado(EstadoBarril.DESPACHADO);
        existente.setCodigoLote("IPA-003");
        existente.setClienteNombre("Bar La Espuma");
        existente.setFechaDespacho(java.time.LocalDate.now());
        when(barrilRepo.findById(1L)).thenReturn(Optional.of(existente));
        when(barrilRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.cambiarEstado(1L, EstadoBarril.DISPONIBLE, null);

        ArgumentCaptor<Barril> captor = ArgumentCaptor.forClass(Barril.class);
        verify(barrilRepo).save(captor.capture());
        Barril guardado = captor.getValue();
        assertThat(guardado.getCodigoLote()).isNull();
        assertThat(guardado.getClienteNombre()).isNull();
        assertThat(guardado.getFechaDespacho()).isNull();
    }

    @Test
    void cambiarEstado_noExiste_lanzaExcepcion() {
        when(barrilRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cambiarEstado(99L, EstadoBarril.VACIO, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    // ── eliminar ───────────────────────────────────────────────────────────

    @Test
    void eliminar_llama_deleteById() {
        Barril existente = barrilConId(1L, "KEG-001");
        when(barrilRepo.findById(1L)).thenReturn(Optional.of(existente));

        service.eliminar(1L);

        verify(barrilRepo).deleteById(1L);
    }

    @Test
    void eliminar_noExiste_lanzaExcepcion() {
        when(barrilRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(99L))
                .isInstanceOf(RuntimeException.class);
        verify(barrilRepo, never()).deleteById(any());
    }

    // ── stats ──────────────────────────────────────────────────────────────

    @Test
    void countTotal_delegaARepo() {
        when(barrilRepo.count()).thenReturn(10L);
        assertThat(service.countTotal()).isEqualTo(10L);
    }

    @Test
    void countByEstado_delegaARepo() {
        when(barrilRepo.countByEstado(EstadoBarril.DISPONIBLE)).thenReturn(4L);
        assertThat(service.countByEstado(EstadoBarril.DISPONIBLE)).isEqualTo(4L);
    }

    // ── listarMovimientos ──────────────────────────────────────────────────

    @Test
    void listarMovimientos_delegaARepo() {
        when(movimientoRepo.findByBarrilIdOrderByFechaDesc(5L)).thenReturn(List.of());
        List<MovimientoBarril> result = service.listarMovimientos(5L);
        assertThat(result).isEmpty();
        verify(movimientoRepo).findByBarrilIdOrderByFechaDesc(5L);
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private Barril barrilConId(Long id, String codigo) {
        Barril b = new Barril();
        b.setId(id);
        b.setCodigo(codigo);
        b.setEstado(EstadoBarril.DISPONIBLE);
        return b;
    }
}
