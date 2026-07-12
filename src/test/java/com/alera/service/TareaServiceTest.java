package com.alera.service;

import com.alera.model.Equipo;
import com.alera.model.LoteCerveza;
import com.alera.model.Tarea;
import com.alera.model.TareaItem;
import com.alera.model.enums.EstadoTarea;
import com.alera.model.enums.PrioridadTarea;
import com.alera.repository.*;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TareaService")
class TareaServiceTest {

    @Mock TareaRepository                  repo;
    @Mock TareaItemRepository              itemRepo;
    @Mock LoteCervezaRepository            loteRepo;
    @Mock EquipoRepository                 equipoRepo;
    @Mock InsumoInventarioRepository       insumoRepo;
    @Mock ElaboracionPlanificadaRepository elaboracionRepo;
    @Mock OrdenCompraRepository            ordenCompraRepo;
    @Mock VentaRepository                  ventaRepo;
    @Mock ClienteRepository                clienteRepo;
    @Mock FacturaProveedorRepository       facturaRepo;
    @Mock ProveedorRepository              proveedorRepo;
    @Mock NotificacionService              notificacionService;

    @InjectMocks TareaService service;

    private Tarea tarea(Long id, EstadoTarea estado, String asignadoA) {
        Tarea t = new Tarea();
        t.setId(id);
        t.setTitulo("Limpiar tanque");
        t.setEstado(estado);
        t.setAsignadoA(asignadoA);
        t.setPrioridad(PrioridadTarea.MEDIA);
        t.setItems(new ArrayList<>());
        return t;
    }

    private TareaItem item(Long id, Long tareaId, boolean completado) {
        TareaItem i = new TareaItem();
        i.setId(id);
        Tarea t = tarea(tareaId, EstadoTarea.PENDIENTE, null);
        i.setTarea(t);
        i.setDescripcion("Ítem " + id);
        i.setCompletado(completado);
        i.setOrdenItem(0);
        return i;
    }

    // ── listar ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("listar")
    class Listar {

        @Test
        @DisplayName("sin filtros llama findAllByOrder")
        void sinFiltros_llamaFindAll() {
            when(repo.findAllByOrderByFechaVencimientoAscCreatedAtDesc()).thenReturn(List.of());
            service.listar(null, null);
            verify(repo).findAllByOrderByFechaVencimientoAscCreatedAtDesc();
        }

        @Test
        @DisplayName("solo estado llama findAllByEstado")
        void soloEstado_llamaPorEstado() {
            when(repo.findAllByEstadoOrderByFechaVencimientoAscCreatedAtDesc(EstadoTarea.PENDIENTE))
                    .thenReturn(List.of());
            service.listar(EstadoTarea.PENDIENTE, null);
            verify(repo).findAllByEstadoOrderByFechaVencimientoAscCreatedAtDesc(EstadoTarea.PENDIENTE);
        }

        @Test
        @DisplayName("solo asignadoA llama findAllByAsignadoA")
        void soloUsuario_llamaPorUsuario() {
            when(repo.findAllByAsignadoAOrderByFechaVencimientoAscCreatedAtDesc("juan"))
                    .thenReturn(List.of());
            service.listar(null, "juan");
            verify(repo).findAllByAsignadoAOrderByFechaVencimientoAscCreatedAtDesc("juan");
        }

        @Test
        @DisplayName("estado y asignadoA llama findAllByEstadoAndAsignadoA")
        void estadoYUsuario_llamaCombinado() {
            when(repo.findAllByEstadoAndAsignadoAOrderByFechaVencimientoAscCreatedAtDesc(
                    EstadoTarea.EN_PROGRESO, "karen")).thenReturn(List.of());
            service.listar(EstadoTarea.EN_PROGRESO, "karen");
            verify(repo).findAllByEstadoAndAsignadoAOrderByFechaVencimientoAscCreatedAtDesc(
                    EstadoTarea.EN_PROGRESO, "karen");
        }

        @Test
        @DisplayName("asignadoA en blanco equivale a sin filtro")
        void asignadoEnBlanco_sinFiltroUsuario() {
            when(repo.findAllByOrderByFechaVencimientoAscCreatedAtDesc()).thenReturn(List.of());
            service.listar(null, "  ");
            verify(repo).findAllByOrderByFechaVencimientoAscCreatedAtDesc();
        }
    }

    // ── buscarPorId ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("buscarPorId")
    class BuscarPorId {

        @Test
        @DisplayName("id existente retorna la tarea")
        void idExistente_retornaTarea() {
            Tarea t = tarea(1L, EstadoTarea.PENDIENTE, null);
            when(repo.findById(1L)).thenReturn(Optional.of(t));
            assertThat(service.buscarPorId(1L)).isSameAs(t);
        }

        @Test
        @DisplayName("id inexistente lanza EntityNotFoundException")
        void idInexistente_lanzaExcepcion() {
            when(repo.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.buscarPorId(99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    // ── guardar ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("guardar")
    class Guardar {

        @BeforeEach
        void setup() {
            when(repo.save(any(Tarea.class))).thenAnswer(inv -> {
                Tarea t = inv.getArgument(0);
                t.setId(1L);
                return t;
            });
        }

        @Test
        @DisplayName("sin asignadoA no dispara notificación")
        void sinAsignado_noDispararNotificacion() {
            service.guardar("Título", null, null, PrioridadTarea.MEDIA, null, null, null, List.of(), "admin");
            verify(notificacionService, never()).crearAlertaTareaAsignada(any());
        }

        @Test
        @DisplayName("con asignadoA dispara notificación")
        void conAsignado_disparaNotificacion() {
            service.guardar("Título", null, null, PrioridadTarea.ALTA, "juan", null, null, List.of(), "admin");
            verify(notificacionService).crearAlertaTareaAsignada(any(Tarea.class));
        }

        @Test
        @DisplayName("prioridad null default MEDIA")
        void prioridadNull_defaultMedia() {
            service.guardar("Título", null, null, null, null, null, null, List.of(), "admin");
            ArgumentCaptor<Tarea> cap = ArgumentCaptor.forClass(Tarea.class);
            verify(repo).save(cap.capture());
            assertThat(cap.getValue().getPrioridad()).isEqualTo(PrioridadTarea.MEDIA);
        }

        @Test
        @DisplayName("creadoPor se setea correctamente")
        void creadoPor_seteaCorrectamente() {
            service.guardar("T", null, null, PrioridadTarea.BAJA, null, null, null, List.of(), "operador");
            ArgumentCaptor<Tarea> cap = ArgumentCaptor.forClass(Tarea.class);
            verify(repo).save(cap.capture());
            assertThat(cap.getValue().getCreadoPor()).isEqualTo("operador");
        }

        @Test
        @DisplayName("ítems se agregan a la tarea")
        void conItems_seAgregaALaTarea() {
            List<Map<String, String>> items = List.of(
                    Map.of("descripcion", "Lavar tanque"),
                    Map.of("descripcion", "Enjuagar")
            );
            service.guardar("T", null, null, PrioridadTarea.MEDIA, null, null, null, items, "admin");
            ArgumentCaptor<Tarea> cap = ArgumentCaptor.forClass(Tarea.class);
            verify(repo).save(cap.capture());
            assertThat(cap.getValue().getItems()).hasSize(2);
        }

        @Test
        @DisplayName("ítems con descripción vacía se omiten")
        void itemsConDescripcionVacia_seOmiten() {
            List<Map<String, String>> items = List.of(
                    Map.of("descripcion", ""),
                    Map.of("descripcion", "Enjuagar")
            );
            service.guardar("T", null, null, PrioridadTarea.MEDIA, null, null, null, items, "admin");
            ArgumentCaptor<Tarea> cap = ArgumentCaptor.forClass(Tarea.class);
            verify(repo).save(cap.capture());
            assertThat(cap.getValue().getItems()).hasSize(1);
        }
    }

    // ── actualizar ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("actualizar")
    class Actualizar {

        @Test
        @DisplayName("si asignadoA cambia dispara notificación")
        void cambiaAsignado_disparaNotificacion() {
            Tarea existente = tarea(1L, EstadoTarea.PENDIENTE, "juan");
            when(repo.findById(1L)).thenReturn(Optional.of(existente));
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.actualizar(1L, "T", null, null, PrioridadTarea.MEDIA, "karen",
                    null, null, List.of());

            verify(notificacionService).crearAlertaTareaAsignada(any());
        }

        @Test
        @DisplayName("si asignadoA no cambia no dispara notificación")
        void mismoAsignado_noDispararNotificacion() {
            Tarea existente = tarea(1L, EstadoTarea.PENDIENTE, "juan");
            when(repo.findById(1L)).thenReturn(Optional.of(existente));
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.actualizar(1L, "T", null, null, PrioridadTarea.MEDIA, "juan",
                    null, null, List.of());

            verify(notificacionService, never()).crearAlertaTareaAsignada(any());
        }

        @Test
        @DisplayName("los ítems anteriores se reemplazan")
        void actualizarItems_reemplazaLosAnteriores() {
            Tarea existente = tarea(1L, EstadoTarea.PENDIENTE, null);
            existente.getItems().add(item(10L, 1L, false));
            when(repo.findById(1L)).thenReturn(Optional.of(existente));
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            List<Map<String, String>> nuevosItems = List.of(Map.of("descripcion", "Nuevo ítem"));
            service.actualizar(1L, "T", null, null, PrioridadTarea.MEDIA, null, null, null, nuevosItems);

            ArgumentCaptor<Tarea> cap = ArgumentCaptor.forClass(Tarea.class);
            verify(repo).save(cap.capture());
            assertThat(cap.getValue().getItems()).hasSize(1);
            assertThat(cap.getValue().getItems().get(0).getDescripcion()).isEqualTo("Nuevo ítem");
        }
    }

    // ── eliminar ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("eliminar llama repo.delete con la tarea encontrada")
    void eliminar_llamaDelete() {
        Tarea t = tarea(1L, EstadoTarea.PENDIENTE, null);
        when(repo.findById(1L)).thenReturn(Optional.of(t));

        service.eliminar(1L);

        verify(repo).delete(t);
    }

    // ── toggleItem + recalcularEstado ─────────────────────────────────────────

    @Nested
    @DisplayName("toggleItem / recalcularEstado")
    class Toggle {

        @Test
        @DisplayName("marcar completado invierte el flag")
        void toggle_invierteCompletado() {
            TareaItem it = item(5L, 1L, false);
            when(itemRepo.findById(5L)).thenReturn(Optional.of(it));
            when(itemRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(itemRepo.findByTareaIdOrderByOrdenItemAscIdAsc(1L)).thenReturn(List.of(it));
            Tarea t = tarea(1L, EstadoTarea.PENDIENTE, null);
            when(repo.findById(1L)).thenReturn(Optional.of(t));
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Map<String, Object> result = service.toggleItem(1L, 5L);

            assertThat(result.get("completado")).isEqualTo(Boolean.TRUE);
        }

        @Test
        @DisplayName("todos completados → estado COMPLETADA")
        void todosCompletados_estadoCompletada() {
            TareaItem it1 = item(1L, 10L, false); // se va a marcar como done
            TareaItem it2 = item(2L, 10L, true);
            when(itemRepo.findById(1L)).thenReturn(Optional.of(it1));
            when(itemRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(itemRepo.findByTareaIdOrderByOrdenItemAscIdAsc(10L)).thenReturn(List.of(it1, it2));
            Tarea t = tarea(10L, EstadoTarea.EN_PROGRESO, null);
            when(repo.findById(10L)).thenReturn(Optional.of(t));
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.toggleItem(10L, 1L);

            ArgumentCaptor<Tarea> cap = ArgumentCaptor.forClass(Tarea.class);
            verify(repo).save(cap.capture());
            assertThat(cap.getValue().getEstado()).isEqualTo(EstadoTarea.COMPLETADA);
        }

        @Test
        @DisplayName("algunos completados → estado EN_PROGRESO")
        void algunosCompletados_estadoEnProgreso() {
            TareaItem it1 = item(1L, 10L, false); // se marca done
            TareaItem it2 = item(2L, 10L, false); // queda pendiente
            when(itemRepo.findById(1L)).thenReturn(Optional.of(it1));
            when(itemRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(itemRepo.findByTareaIdOrderByOrdenItemAscIdAsc(10L)).thenReturn(List.of(it1, it2));
            Tarea t = tarea(10L, EstadoTarea.PENDIENTE, null);
            when(repo.findById(10L)).thenReturn(Optional.of(t));
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.toggleItem(10L, 1L);

            ArgumentCaptor<Tarea> cap = ArgumentCaptor.forClass(Tarea.class);
            verify(repo).save(cap.capture());
            assertThat(cap.getValue().getEstado()).isEqualTo(EstadoTarea.EN_PROGRESO);
        }

        @Test
        @DisplayName("desmarcar único completado → estado PENDIENTE")
        void desmarcarUnico_estadoPendiente() {
            TareaItem it1 = item(1L, 10L, true); // se desmarca
            TareaItem it2 = item(2L, 10L, false);
            when(itemRepo.findById(1L)).thenReturn(Optional.of(it1));
            when(itemRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(itemRepo.findByTareaIdOrderByOrdenItemAscIdAsc(10L)).thenReturn(List.of(it1, it2));
            Tarea t = tarea(10L, EstadoTarea.EN_PROGRESO, null);
            when(repo.findById(10L)).thenReturn(Optional.of(t));
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.toggleItem(10L, 1L);

            ArgumentCaptor<Tarea> cap = ArgumentCaptor.forClass(Tarea.class);
            verify(repo).save(cap.capture());
            assertThat(cap.getValue().getEstado()).isEqualTo(EstadoTarea.PENDIENTE);
        }

        @Test
        @DisplayName("item inexistente lanza EntityNotFoundException")
        void itemInexistente_lanzaExcepcion() {
            when(itemRepo.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.toggleItem(1L, 99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("resultado incluye pct y estado")
        void resultado_incluyePctYEstado() {
            TareaItem it = item(5L, 1L, false);
            when(itemRepo.findById(5L)).thenReturn(Optional.of(it));
            when(itemRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(itemRepo.findByTareaIdOrderByOrdenItemAscIdAsc(1L)).thenReturn(List.of(it));
            Tarea t = tarea(1L, EstadoTarea.PENDIENTE, null);
            when(repo.findById(1L)).thenReturn(Optional.of(t));
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Map<String, Object> result = service.toggleItem(1L, 5L);

            assertThat(result).containsKeys("completado", "estado", "pct");
        }
    }

    // ── contarPorEstado ────────────────────────────────────────────────────────

    @Test
    @DisplayName("contarPorEstado retorna mapa con 4 claves")
    void contarPorEstado_retornaMapaCompleto() {
        when(repo.count()).thenReturn(10L);
        when(repo.countByEstado(EstadoTarea.PENDIENTE)).thenReturn(5L);
        when(repo.countByEstado(EstadoTarea.EN_PROGRESO)).thenReturn(3L);
        when(repo.countByEstado(EstadoTarea.COMPLETADA)).thenReturn(2L);

        Map<String, Long> mapa = service.contarPorEstado();

        assertThat(mapa).containsEntry("total", 10L)
                        .containsEntry("pendiente", 5L)
                        .containsEntry("en_progreso", 3L)
                        .containsEntry("completada", 2L);
    }

    // ── listarProximasAVencer ──────────────────────────────────────────────────

    @Test
    @DisplayName("listarProximasAVencer delega al repositorio con la fecha y estado NOT COMPLETADA")
    void listarProximasAVencer_delegaARepo() {
        LocalDate manana = LocalDate.now().plusDays(1);
        when(repo.findByFechaVencimientoAndEstadoNot(manana, EstadoTarea.COMPLETADA))
                .thenReturn(List.of());

        service.listarProximasAVencer(manana);

        verify(repo).findByFechaVencimientoAndEstadoNot(manana, EstadoTarea.COMPLETADA);
    }
}
