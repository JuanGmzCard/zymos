package com.alera.service;

import com.alera.model.ElaboracionPlanificada;
import com.alera.model.enums.EstadoPlanificacion;
import com.alera.repository.ElaboracionPlanificadaRepository;
import com.alera.repository.RecetaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class PlanificacionService {

    private final ElaboracionPlanificadaRepository repo;
    private final RecetaRepository recetaRepo;

    public PlanificacionService(ElaboracionPlanificadaRepository repo,
                                 RecetaRepository recetaRepo) {
        this.repo      = repo;
        this.recetaRepo = recetaRepo;
    }

    @Transactional(readOnly = true)
    public List<ElaboracionPlanificada> listarProximas() {
        return repo.findProximas(LocalDate.now().minusDays(1));
    }

    @Transactional(readOnly = true)
    public List<ElaboracionPlanificada> listarTodas() {
        return repo.findAllOrdenadas();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> suggest(String q) {
        if (q == null || q.isBlank()) return List.of();
        String ql = q.toLowerCase();
        return listarTodas().stream()
                .filter(e -> e.getNombreElaboracion().toLowerCase().contains(ql))
                .limit(6)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",    e.getId());
                    m.put("label", e.getNombreElaboracion());
                    m.put("sub",   e.getFechaPlaneada() != null ? e.getFechaPlaneada().toString() : "");
                    return m;
                }).toList();
    }

    @Transactional(readOnly = true)
    public Optional<ElaboracionPlanificada> buscarPorId(Long id) {
        return repo.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<ElaboracionPlanificada> buscarConRecetaEIngredientes(Long id) {
        return repo.findByIdWithRecetaEIngredientes(id);
    }

    @Transactional(readOnly = true)
    public List<ElaboracionPlanificada> listarPorRango(LocalDate desde, LocalDate hasta) {
        return repo.findByRangoFecha(desde, hasta);
    }

    public ElaboracionPlanificada guardar(ElaboracionPlanificada plan, Long recetaId) {
        if (recetaId != null) {
            recetaRepo.findById(recetaId).ifPresent(plan::setReceta);
        } else {
            plan.setReceta(null);
        }
        // Si tiene receta y no tiene nombre propio, usar el nombre de la receta
        if ((plan.getNombreElaboracion() == null || plan.getNombreElaboracion().isBlank())
                && plan.getReceta() != null) {
            plan.setNombreElaboracion(plan.getReceta().getNombre());
        }
        return repo.save(plan);
    }

    public void cambiarEstado(Long id, EstadoPlanificacion nuevoEstado) {
        repo.findById(id).ifPresent(p -> {
            p.setEstado(nuevoEstado);
            repo.save(p);
        });
    }

    public void eliminar(Long id) {
        repo.deleteById(id);
    }
}
