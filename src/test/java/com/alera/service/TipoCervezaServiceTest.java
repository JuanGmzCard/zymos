package com.alera.service;

import com.alera.model.TipoCerveza;
import com.alera.repository.TipoCervezaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TipoCervezaService")
class TipoCervezaServiceTest {

    @Mock TipoCervezaRepository repo;

    @InjectMocks
    TipoCervezaService service;

    private TipoCerveza tipo(Long id, String nombre, boolean activo) {
        TipoCerveza t = new TipoCerveza();
        t.setId(id);
        t.setNombre(nombre);
        t.setActivo(activo);
        return t;
    }

    // ── listarActivos ─────────────────────────────────────────────────

    @Test
    @DisplayName("listarActivos delega al repositorio y retorna solo activos")
    void listarActivos_delegaAlRepositorio() {
        when(repo.findByActivoTrueOrderByNombreAsc())
                .thenReturn(List.of(tipo(1L, "IPA", true), tipo(2L, "Stout", true)));

        List<TipoCerveza> resultado = service.listarActivos();

        assertThat(resultado).hasSize(2);
        verify(repo).findByActivoTrueOrderByNombreAsc();
    }

    // ── listarTodos ───────────────────────────────────────────────────

    @Test
    @DisplayName("listarTodos retorna todos incluyendo inactivos")
    void listarTodos_retornaTodos() {
        when(repo.findAll()).thenReturn(List.of(
                tipo(1L, "IPA", true), tipo(2L, "Lager", false)));

        List<TipoCerveza> resultado = service.listarTodos();

        assertThat(resultado).hasSize(2);
        verify(repo).findAll();
    }

    // ── buscarPorId ───────────────────────────────────────────────────

    @Test
    @DisplayName("buscarPorId retorna el tipo cuando existe")
    void buscarPorId_existe_retornaTipo() {
        when(repo.findById(1L)).thenReturn(Optional.of(tipo(1L, "IPA", true)));

        Optional<TipoCerveza> resultado = service.buscarPorId(1L);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getNombre()).isEqualTo("IPA");
    }

    @Test
    @DisplayName("buscarPorId retorna vacío cuando no existe")
    void buscarPorId_noExiste_retornaVacio() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.buscarPorId(99L)).isEmpty();
    }

    // ── existePorNombre ───────────────────────────────────────────────

    @Test
    @DisplayName("existePorNombre retorna true cuando el nombre existe")
    void existePorNombre_existe_retornaTrue() {
        when(repo.existsByNombreIgnoreCase("IPA")).thenReturn(true);

        assertThat(service.existePorNombre("IPA")).isTrue();
    }

    @Test
    @DisplayName("existePorNombre retorna false cuando el nombre no existe")
    void existePorNombre_noExiste_retornaFalse() {
        when(repo.existsByNombreIgnoreCase("Nuevo")).thenReturn(false);

        assertThat(service.existePorNombre("Nuevo")).isFalse();
    }

    // ── guardar ───────────────────────────────────────────────────────

    @Test
    @DisplayName("guardar delega al repositorio y retorna el tipo guardado")
    void guardar_persisteYRetorna() {
        TipoCerveza t = tipo(null, "Porter", true);
        TipoCerveza guardado = tipo(5L, "Porter", true);
        when(repo.save(t)).thenReturn(guardado);

        TipoCerveza resultado = service.guardar(t);

        assertThat(resultado.getId()).isEqualTo(5L);
        verify(repo).save(t);
    }

    // ── eliminar ──────────────────────────────────────────────────────

    @Test
    @DisplayName("eliminar delega a deleteById")
    void eliminar_delegaADeleteById() {
        service.eliminar(3L);

        verify(repo).deleteById(3L);
    }

    // ── toggleActivo ──────────────────────────────────────────────────

    @Test
    @DisplayName("toggleActivo desactiva un tipo activo")
    void toggleActivo_activoQuedaInactivo() {
        when(repo.findById(1L)).thenReturn(Optional.of(tipo(1L, "IPA", true)));

        service.toggleActivo(1L);

        ArgumentCaptor<TipoCerveza> captor = ArgumentCaptor.forClass(TipoCerveza.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().isActivo()).isFalse();
    }

    @Test
    @DisplayName("toggleActivo reactiva un tipo inactivo")
    void toggleActivo_inactivoQuedaActivo() {
        when(repo.findById(2L)).thenReturn(Optional.of(tipo(2L, "Lager", false)));

        service.toggleActivo(2L);

        ArgumentCaptor<TipoCerveza> captor = ArgumentCaptor.forClass(TipoCerveza.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().isActivo()).isTrue();
    }

    @Test
    @DisplayName("toggleActivo no hace nada si el tipo no existe")
    void toggleActivo_noExiste_noHaceNada() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        service.toggleActivo(99L);

        verify(repo, never()).save(any());
    }
}
