package com.alera.service;

import com.alera.model.Equipo;
import com.alera.model.LoteCerveza;
import com.alera.model.Tarea;
import com.alera.model.TareaItem;
import com.alera.model.enums.EstadoTarea;
import com.alera.model.enums.PrioridadTarea;
import com.alera.repository.EquipoRepository;
import com.alera.repository.LoteCervezaRepository;
import com.alera.repository.TareaItemRepository;
import com.alera.repository.TareaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class TareaService {

    private final TareaRepository          repo;
    private final TareaItemRepository      itemRepo;
    private final LoteCervezaRepository    loteRepo;
    private final EquipoRepository         equipoRepo;
    private final NotificacionService      notificacionService;

    public TareaService(TareaRepository repo,
                        TareaItemRepository itemRepo,
                        LoteCervezaRepository loteRepo,
                        EquipoRepository equipoRepo,
                        NotificacionService notificacionService) {
        this.repo                 = repo;
        this.itemRepo             = itemRepo;
        this.loteRepo             = loteRepo;
        this.equipoRepo           = equipoRepo;
        this.notificacionService  = notificacionService;
    }

    @Transactional(readOnly = true)
    public List<Tarea> listar(EstadoTarea estado, String asignadoA) {
        boolean tieneEstado = estado != null;
        boolean tieneUser   = asignadoA != null && !asignadoA.isBlank();

        if (tieneEstado && tieneUser)
            return repo.findAllByEstadoAndAsignadoAOrderByFechaVencimientoAscCreatedAtDesc(estado, asignadoA);
        if (tieneEstado)
            return repo.findAllByEstadoOrderByFechaVencimientoAscCreatedAtDesc(estado);
        if (tieneUser)
            return repo.findAllByAsignadoAOrderByFechaVencimientoAscCreatedAtDesc(asignadoA);
        return repo.findAllByOrderByFechaVencimientoAscCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Tarea buscarPorId(Long id) {
        return repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Tarea no encontrada: " + id));
    }

    public Tarea guardar(String titulo,
                         String descripcion,
                         LocalDate fechaVencimiento,
                         PrioridadTarea prioridad,
                         String asignadoA,
                         Long loteId,
                         Long equipoId,
                         List<Map<String, String>> itemsData,
                         String creadoPor) {

        Tarea tarea = new Tarea();
        tarea.setTitulo(titulo);
        tarea.setDescripcion(descripcion);
        tarea.setFechaVencimiento(fechaVencimiento);
        tarea.setPrioridad(prioridad != null ? prioridad : PrioridadTarea.MEDIA);
        tarea.setAsignadoA(asignadoA != null && !asignadoA.isBlank() ? asignadoA : null);
        tarea.setCreadoPor(creadoPor);

        if (loteId != null)   tarea.setLote(loteRepo.findById(loteId).orElse(null));
        if (equipoId != null) tarea.setEquipo(equipoRepo.findById(equipoId).orElse(null));

        poblarItems(tarea, itemsData);

        Tarea saved = repo.save(tarea);

        if (saved.getAsignadoA() != null) {
            notificacionService.crearAlertaTareaAsignada(saved);
        }

        return saved;
    }

    public Tarea actualizar(Long id,
                            String titulo,
                            String descripcion,
                            LocalDate fechaVencimiento,
                            PrioridadTarea prioridad,
                            String asignadoA,
                            Long loteId,
                            Long equipoId,
                            List<Map<String, String>> itemsData) {

        Tarea tarea = buscarPorId(id);
        String anteriorAsignado = tarea.getAsignadoA();

        tarea.setTitulo(titulo);
        tarea.setDescripcion(descripcion);
        tarea.setFechaVencimiento(fechaVencimiento);
        tarea.setPrioridad(prioridad != null ? prioridad : PrioridadTarea.MEDIA);
        tarea.setAsignadoA(asignadoA != null && !asignadoA.isBlank() ? asignadoA : null);

        tarea.setLote(loteId   != null ? loteRepo.findById(loteId).orElse(null)     : null);
        tarea.setEquipo(equipoId != null ? equipoRepo.findById(equipoId).orElse(null) : null);

        tarea.getItems().clear();
        poblarItems(tarea, itemsData);

        Tarea saved = repo.save(tarea);

        String nuevoAsignado = saved.getAsignadoA();
        if (nuevoAsignado != null && !nuevoAsignado.equals(anteriorAsignado)) {
            notificacionService.crearAlertaTareaAsignada(saved);
        }

        return saved;
    }

    public void eliminar(Long id) {
        repo.delete(buscarPorId(id));
    }

    public Map<String, Object> toggleItem(Long tareaId, Long itemId) {
        TareaItem item = itemRepo.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Item no encontrado: " + itemId));

        item.setCompletado(!Boolean.TRUE.equals(item.getCompletado()));
        itemRepo.save(item);

        recalcularEstado(tareaId);

        return Map.of(
                "completado", item.getCompletado(),
                "estado", repo.findById(tareaId).map(t -> t.getEstado().name()).orElse(""),
                "pct", repo.findById(tareaId).map(Tarea::getPorcentajeCompletado).orElse(0)
        );
    }

    public Map<String, Long> contarPorEstado() {
        return Map.of(
                "total",       repo.count(),
                "pendiente",   repo.countByEstado(EstadoTarea.PENDIENTE),
                "en_progreso", repo.countByEstado(EstadoTarea.EN_PROGRESO),
                "completada",  repo.countByEstado(EstadoTarea.COMPLETADA)
        );
    }

    public List<Tarea> listarProximasAVencer(LocalDate fecha) {
        return repo.findByFechaVencimientoAndEstadoNot(fecha, EstadoTarea.COMPLETADA);
    }

    private void recalcularEstado(Long tareaId) {
        repo.findById(tareaId).ifPresent(tarea -> {
            List<TareaItem> items = itemRepo.findByTareaIdOrderByOrdenItemAscIdAsc(tareaId);
            if (items.isEmpty()) return;
            long completados = items.stream().filter(i -> Boolean.TRUE.equals(i.getCompletado())).count();
            EstadoTarea nuevoEstado;
            if (completados == items.size()) {
                nuevoEstado = EstadoTarea.COMPLETADA;
            } else if (completados > 0) {
                nuevoEstado = EstadoTarea.EN_PROGRESO;
            } else {
                nuevoEstado = EstadoTarea.PENDIENTE;
            }
            tarea.setEstado(nuevoEstado);
            repo.save(tarea);
        });
    }

    private void poblarItems(Tarea tarea, List<Map<String, String>> itemsData) {
        if (itemsData == null) return;
        for (int i = 0; i < itemsData.size(); i++) {
            Map<String, String> d = itemsData.get(i);
            String desc = d.get("descripcion");
            if (desc == null || desc.isBlank()) continue;

            TareaItem item = new TareaItem();
            item.setTarea(tarea);
            item.setDescripcion(desc.trim());
            item.setOrdenItem(i);
            item.setCompletado(Boolean.FALSE);

            String loteIdStr   = d.get("loteId");
            String equipoIdStr = d.get("equipoId");
            if (loteIdStr   != null && !loteIdStr.isBlank())
                item.setLote(loteRepo.findById(Long.parseLong(loteIdStr)).orElse(null));
            if (equipoIdStr != null && !equipoIdStr.isBlank())
                item.setEquipo(equipoRepo.findById(Long.parseLong(equipoIdStr)).orElse(null));

            tarea.getItems().add(item);
        }
    }
}
