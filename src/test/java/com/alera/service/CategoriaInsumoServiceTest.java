package com.alera.service;

import com.alera.model.CategoriaInsumo;
import com.alera.repository.CategoriaInsumoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoriaInsumoServiceTest {

    @Mock CategoriaInsumoRepository repo;
    @InjectMocks CategoriaInsumoService service;

    private CategoriaInsumo cat(Long id, String nombre, boolean activo) {
        CategoriaInsumo c = new CategoriaInsumo();
        c.setNombre(nombre);
        c.setActivo(activo);
        return c;
    }

    // ── listarNombresActivos ───────────────────────────────────────────────────

    @Test
    void listarNombresActivos_retornaNombresOrdenados() {
        CategoriaInsumo a = cat(1L, "Malta", true);
        CategoriaInsumo b = cat(2L, "Lúpulo", true);
        when(repo.findAllByActivoTrueOrderByNombreAsc()).thenReturn(List.of(b, a));

        List<String> result = service.listarNombresActivos();

        assertThat(result).containsExactly("Lúpulo", "Malta");
    }

    @Test
    void listarNombresActivos_listaVacia_retornaVacia() {
        when(repo.findAllByActivoTrueOrderByNombreAsc()).thenReturn(List.of());
        assertThat(service.listarNombresActivos()).isEmpty();
    }

    // ── listarTodos ────────────────────────────────────────────────────────────

    @Test
    void listarTodos_retornaTodasLasCategorias() {
        CategoriaInsumo activa = cat(1L, "Malta", true);
        CategoriaInsumo inactiva = cat(2L, "Otro", false);
        when(repo.findAllByOrderByNombreAsc()).thenReturn(List.of(activa, inactiva));

        List<CategoriaInsumo> result = service.listarTodos();

        assertThat(result).hasSize(2);
    }

    // ── guardar ────────────────────────────────────────────────────────────────

    @Test
    void guardar_nombreNuevo_persisteCategoria() {
        when(repo.existsByNombreIgnoreCase("Levadura")).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CategoriaInsumo result = service.guardar("Levadura");

        assertThat(result.getNombre()).isEqualTo("Levadura");
        verify(repo).save(any(CategoriaInsumo.class));
    }

    @Test
    void guardar_trimeaNombre() {
        when(repo.existsByNombreIgnoreCase("Malta")).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.guardar("  Malta  ");

        ArgumentCaptor<CategoriaInsumo> cap = ArgumentCaptor.forClass(CategoriaInsumo.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getNombre()).isEqualTo("Malta");
    }

    @Test
    void guardar_nombreDuplicado_lanzaExcepcion() {
        when(repo.existsByNombreIgnoreCase("Malta")).thenReturn(true);

        assertThatThrownBy(() -> service.guardar("Malta"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Ya existe");

        verify(repo, never()).save(any());
    }

    // ── toggleActivo ───────────────────────────────────────────────────────────

    @Test
    void toggleActivo_activa_desactiva() {
        CategoriaInsumo cat = cat(1L, "Malta", true);
        when(repo.findById(1L)).thenReturn(Optional.of(cat));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.toggleActivo(1L);

        ArgumentCaptor<CategoriaInsumo> cap = ArgumentCaptor.forClass(CategoriaInsumo.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().isActivo()).isFalse();
    }

    @Test
    void toggleActivo_inactiva_activa() {
        CategoriaInsumo cat = cat(1L, "Lúpulo", false);
        when(repo.findById(1L)).thenReturn(Optional.of(cat));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.toggleActivo(1L);

        ArgumentCaptor<CategoriaInsumo> cap = ArgumentCaptor.forClass(CategoriaInsumo.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().isActivo()).isTrue();
    }

    @Test
    void toggleActivo_noExiste_noOp() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        service.toggleActivo(99L);
        verify(repo, never()).save(any());
    }

    // ── eliminar ───────────────────────────────────────────────────────────────

    @Test
    void eliminar_llamaDeleteById() {
        service.eliminar(1L);
        verify(repo).deleteById(1L);
    }
}
