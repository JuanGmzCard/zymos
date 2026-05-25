package com.alera.service;

import com.alera.exception.EquipoEnUsoException;
import com.alera.model.Equipo;
import com.alera.model.enums.EstadoEquipo;
import com.alera.model.enums.TipoEquipo;
import com.alera.repository.EquipoRepository;
import com.alera.repository.LoteCervezaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class EquipoService {

    private final EquipoRepository repo;
    private final LoteCervezaRepository loteRepo;

    public EquipoService(EquipoRepository repo, LoteCervezaRepository loteRepo) {
        this.repo = repo;
        this.loteRepo = loteRepo;
    }

    public static final int PAGE_SIZE = 15;

    public List<Equipo> listarTodos() {
        return repo.findAll();
    }

    public List<Equipo> listarPorEstado(EstadoEquipo estado) {
        return repo.findByEstadoOrderByNombreAsc(estado);
    }

    public Page<Equipo> listarPaginado(EstadoEquipo estado, int page) {
        PageRequest pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("nombre").ascending());
        return estado != null
                ? repo.findByEstadoOrderByNombreAsc(estado, pageable)
                : repo.findAllByOrderByNombreAsc(pageable);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> suggest(String q, EstadoEquipo estado) {
        if (q == null || q.isBlank() || q.trim().length() < 2) return List.of();
        String lower = q.trim().toLowerCase();
        List<Equipo> base = estado != null ? listarPorEstado(estado) : listarTodos();
        return base.stream()
            .filter(e -> e.getNombre().toLowerCase().contains(lower))
            .limit(6)
            .map(e -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("nombre",     e.getNombre());
                m.put("tipo",       e.getTipo() != null ? e.getTipo().getDisplayName() : "");
                m.put("estado",     e.getEstado() != null ? e.getEstado().getDisplayName() : "");
                m.put("colorEstado",e.getColorEstado());
                m.put("pendiente",  e.isMantenimientoPendiente());
                m.put("url",        "/equipos/editar/" + e.getId());
                return m;
            }).toList();
    }

    public Optional<Equipo> buscarPorId(Long id) {
        return repo.findById(id);
    }

    public Equipo guardar(Equipo equipo) {
        return repo.save(equipo);
    }

    /**
     * Elimina el equipo solo si no tiene lotes activos asignados.
     * Lanza EquipoEnUsoException si hay lotes en proceso usando este equipo.
     */
    public void eliminar(Long id) {
        Equipo equipo = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Equipo no encontrado: " + id));

        long lotesActivos = loteRepo.countLotesActivosByEquipo(id);
        if (lotesActivos > 0) {
            throw new EquipoEnUsoException(equipo.getNombre(), lotesActivos);
        }
        repo.deleteById(id);
    }

    public List<Equipo> listarFermentadoresDisponibles() {
        return repo.findFermentadoresDisponibles(TipoEquipo.FERMENTADOR, EstadoEquipo.OPERATIVO);
    }

    public List<Equipo> listarMantenimientoPendiente() {
        return repo.findMantenimientoPendiente(java.time.LocalDate.now().plusDays(7));
    }

    public Equipo cambiarEstado(Long id, EstadoEquipo estado) {
        Equipo equipo = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Equipo no encontrado: " + id));
        equipo.setEstado(estado);
        return repo.save(equipo);
    }

    @Transactional(readOnly = true)
    public long countByEstado(EstadoEquipo estado) {
        return repo.countByEstado(estado);
    }

    @Transactional(readOnly = true)
    public long countMantenimientoPendiente() {
        return repo.countMantenimientoPendiente(java.time.LocalDate.now().plusDays(7));
    }

    @Transactional(readOnly = true)
    public long countTotal() {
        return repo.count();
    }
}
