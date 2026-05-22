package com.alera.service;

import com.alera.dto.MantenimientoDto;
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

    public MantenimientoEquipoService(MantenimientoEquipoRepository repo, EquipoRepository equipoRepo) {
        this.repo = repo;
        this.equipoRepo = equipoRepo;
    }

    public List<MantenimientoEquipo> listarPorEquipo(Long equipoId) {
        return repo.findByEquipoIdOrderByFechaDesc(equipoId);
    }

    public MantenimientoEquipo registrar(Long equipoId, MantenimientoDto dto) {
        Equipo equipo = equipoRepo.findById(equipoId)
                .orElseThrow(() -> new RuntimeException("Equipo no encontrado: " + equipoId));

        MantenimientoEquipo m = new MantenimientoEquipo();
        m.setEquipo(equipo);
        m.setFecha(dto.getFecha());
        m.setTipo(dto.getTipo());
        m.setDescripcion(dto.getDescripcion());
        m.setTecnico(dto.getTecnico());
        m.setCosto(dto.getCosto());
        m.setProximoMantenimiento(dto.getProximoMantenimiento());

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
}
