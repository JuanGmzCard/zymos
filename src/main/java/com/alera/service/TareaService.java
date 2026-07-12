package com.alera.service;

import com.alera.model.Tarea;
import com.alera.model.TareaItem;
import com.alera.model.enums.EstadoTarea;
import com.alera.model.enums.PrioridadTarea;
import com.alera.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class TareaService {

    private final TareaRepository                  repo;
    private final TareaItemRepository              itemRepo;
    private final LoteCervezaRepository            loteRepo;
    private final EquipoRepository                 equipoRepo;
    private final InsumoInventarioRepository       insumoRepo;
    private final ElaboracionPlanificadaRepository elaboracionRepo;
    private final OrdenCompraRepository            ordenCompraRepo;
    private final VentaRepository                  ventaRepo;
    private final NotificacionService              notificacionService;

    public TareaService(TareaRepository repo,
                        TareaItemRepository itemRepo,
                        LoteCervezaRepository loteRepo,
                        EquipoRepository equipoRepo,
                        InsumoInventarioRepository insumoRepo,
                        ElaboracionPlanificadaRepository elaboracionRepo,
                        OrdenCompraRepository ordenCompraRepo,
                        VentaRepository ventaRepo,
                        NotificacionService notificacionService) {
        this.repo               = repo;
        this.itemRepo           = itemRepo;
        this.loteRepo           = loteRepo;
        this.equipoRepo         = equipoRepo;
        this.insumoRepo         = insumoRepo;
        this.elaboracionRepo    = elaboracionRepo;
        this.ordenCompraRepo    = ordenCompraRepo;
        this.ventaRepo          = ventaRepo;
        this.notificacionService = notificacionService;
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
                         String refTipo,
                         Long refId,
                         List<Map<String, String>> itemsData,
                         String creadoPor) {

        Tarea tarea = new Tarea();
        tarea.setTitulo(titulo);
        tarea.setDescripcion(descripcion);
        tarea.setFechaVencimiento(fechaVencimiento);
        tarea.setPrioridad(prioridad != null ? prioridad : PrioridadTarea.MEDIA);
        tarea.setAsignadoA(asignadoA != null && !asignadoA.isBlank() ? asignadoA : null);
        tarea.setCreadoPor(creadoPor);

        resolverReferencia(tarea, refTipo, refId);
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
                            String refTipo,
                            Long refId,
                            List<Map<String, String>> itemsData) {

        Tarea tarea = buscarPorId(id);
        String anteriorAsignado = tarea.getAsignadoA();

        tarea.setTitulo(titulo);
        tarea.setDescripcion(descripcion);
        tarea.setFechaVencimiento(fechaVencimiento);
        tarea.setPrioridad(prioridad != null ? prioridad : PrioridadTarea.MEDIA);
        tarea.setAsignadoA(asignadoA != null && !asignadoA.isBlank() ? asignadoA : null);

        limpiarReferencias(tarea);
        resolverReferencia(tarea, refTipo, refId);

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

    private void resolverReferencia(Tarea tarea, String refTipo, Long refId) {
        if (refTipo == null || refTipo.isBlank() || refId == null) return;
        switch (refTipo.toUpperCase()) {
            case "LOTE"         -> tarea.setLote(loteRepo.findById(refId).orElse(null));
            case "EQUIPO"       -> tarea.setEquipo(equipoRepo.findById(refId).orElse(null));
            case "INSUMO"       -> tarea.setInsumo(insumoRepo.findById(refId).orElse(null));
            case "ELABORACION"  -> tarea.setElaboracion(elaboracionRepo.findById(refId).orElse(null));
            case "ORDEN_COMPRA" -> tarea.setOrdenCompra(ordenCompraRepo.findById(refId).orElse(null));
            case "VENTA"        -> tarea.setVenta(ventaRepo.findById(refId).orElse(null));
        }
    }

    private void limpiarReferencias(Tarea tarea) {
        tarea.setLote(null);
        tarea.setEquipo(null);
        tarea.setInsumo(null);
        tarea.setElaboracion(null);
        tarea.setOrdenCompra(null);
        tarea.setVenta(null);
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

            tarea.getItems().add(item);
        }
    }
}
