package com.alera.service;

import com.alera.model.CategoriaEquipo;
import com.alera.repository.CategoriaEquipoRepository;
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
class CategoriaEquipoServiceTest {

    @Mock CategoriaEquipoRepository repo;
    @InjectMocks CategoriaEquipoService service;

    private CategoriaEquipo cat(String nombre, boolean activo) {
        CategoriaEquipo c = new CategoriaEquipo();
        c.setNombre(nombre);
        c.setActivo(activo);
        return c;
    }

    // ── listarNombresActivos ───────────────────────────────────────────────────

    @Test
    void listarNombresActivos_retornaNombres() {
        when(repo.findAllByActivoTrueOrderByNombreAsc())
            .thenReturn(List.of(cat("Fermentador", true), cat("Olla de Macerado", true)));

        List<String> result = service.listarNombresActivos();

        assertThat(result).containsExactly("Fermentador", "Olla de Macerado");
    }

    @Test
    void listarNombresActivos_vacio_retornaListaVacia() {
        when(repo.findAllByActivoTrueOrderByNombreAsc()).thenReturn(List.of());
        assertThat(service.listarNombresActivos()).isEmpty();
    }

    // ── listarTodos ────────────────────────────────────────────────────────────

    @Test
    void listarTodos_incluyeInactivos() {
        when(repo.findAllByOrderByNombreAsc())
            .thenReturn(List.of(cat("Fermentador", true), cat("Otro", false)));

        assertThat(service.listarTodos()).hasSize(2);
    }

    // ── guardar ────────────────────────────────────────────────────────────────

    @Test
    void guardar_nombreNuevo_persisteCategoria() {
        when(repo.existsByNombreIgnoreCase("Bomba")).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CategoriaEquipo result = service.guardar("Bomba");

        assertThat(result.getNombre()).isEqualTo("Bomba");
        verify(repo).save(any(CategoriaEquipo.class));
    }

    @Test
    void guardar_trimeaNombre() {
        when(repo.existsByNombreIgnoreCase("Filtro")).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.guardar("  Filtro  ");

        ArgumentCaptor<CategoriaEquipo> cap = ArgumentCaptor.forClass(CategoriaEquipo.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getNombre()).isEqualTo("Filtro");
    }

    @Test
    void guardar_nombreDuplicado_lanzaExcepcion() {
        when(repo.existsByNombreIgnoreCase("Fermentador")).thenReturn(true);

        assertThatThrownBy(() -> service.guardar("Fermentador"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Ya existe");

        verify(repo, never()).save(any());
    }

    // ── toggleActivo ───────────────────────────────────────────────────────────

    @Test
    void toggleActivo_activa_desactiva() {
        CategoriaEquipo cat = cat("Fermentador", true);
        when(repo.findById(1L)).thenReturn(Optional.of(cat));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.toggleActivo(1L);

        ArgumentCaptor<CategoriaEquipo> cap = ArgumentCaptor.forClass(CategoriaEquipo.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().isActivo()).isFalse();
    }

    @Test
    void toggleActivo_inactiva_activa() {
        CategoriaEquipo cat = cat("Olla", false);
        when(repo.findById(2L)).thenReturn(Optional.of(cat));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.toggleActivo(2L);

        ArgumentCaptor<CategoriaEquipo> cap = ArgumentCaptor.forClass(CategoriaEquipo.class);
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
        service.eliminar(5L);
        verify(repo).deleteById(5L);
    }
}
