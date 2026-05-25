package com.alera.service;

import com.alera.dto.MantenimientoDto;
import com.alera.mapper.MantenimientoMapper;
import com.alera.model.Equipo;
import com.alera.model.MantenimientoEquipo;
import com.alera.repository.EquipoRepository;
import com.alera.repository.MantenimientoEquipoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class MantenimientoEquipoService {

    private final MantenimientoEquipoRepository repo;
    private final EquipoRepository equipoRepo;
    private final MantenimientoMapper mapper;

    public MantenimientoEquipoService(MantenimientoEquipoRepository repo,
                                       EquipoRepository equipoRepo,
                                       MantenimientoMapper mapper) {
        this.repo = repo;
        this.equipoRepo = equipoRepo;
        this.mapper = mapper;
    }

    public List<MantenimientoEquipo> listarPorEquipo(Long equipoId) {
        return repo.findByEquipoIdOrderByFechaDesc(equipoId);
    }

    public MantenimientoEquipo registrar(Long equipoId, MantenimientoDto dto) {
        Equipo equipo = equipoRepo.findById(equipoId)
                .orElseThrow(() -> new RuntimeException("Equipo no encontrado: " + equipoId));

        MantenimientoEquipo m = mapper.toEntity(dto);
        m.setEquipo(equipo);

        MantenimientoEquipo saved = repo.save(m);

        equipo.setFechaUltimoMantenimiento(dto.getFecha());
        if (dto.getProximoMantenimiento() != null) {
            equipo.setProximoMantenimiento(dto.getProximoMantenimiento());
        }
        equipoRepo.save(equipo);

        return saved;
    }

    public void eliminar(Long id) {
        repo.deleteById(id);
    }

    @Transactional(readOnly = true)
    public java.math.BigDecimal sumCostoPorEquipo(Long equipoId) {
        return repo.sumCostoByEquipoId(equipoId);
    }

    @Transactional(readOnly = true)
    public long countPorEquipo(Long equipoId) {
        return repo.countByEquipoId(equipoId);
    }
}
