package com.alera.service;

import com.alera.model.CategoriaEquipo;
import com.alera.repository.CategoriaEquipoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoriaEquipoService {

    private final CategoriaEquipoRepository repo;

    public CategoriaEquipoService(CategoriaEquipoRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<String> listarNombresActivos() {
        return repo.findAllByActivoTrueOrderByNombreAsc()
                   .stream().map(CategoriaEquipo::getNombre).toList();
    }

    @Transactional(readOnly = true)
    public List<CategoriaEquipo> listarTodos() {
        return repo.findAllByOrderByNombreAsc();
    }

    public CategoriaEquipo guardar(String nombre) {
        String nombreTrim = nombre.trim();
        if (repo.existsByNombreIgnoreCase(nombreTrim))
            throw new RuntimeException("Ya existe una categoría con ese nombre");
        CategoriaEquipo cat = new CategoriaEquipo();
        cat.setNombre(nombreTrim);
        return repo.save(cat);
    }

    public void toggleActivo(Long id) {
        repo.findById(id).ifPresent(c -> {
            c.setActivo(!c.isActivo());
            repo.save(c);
        });
    }

    public void eliminar(Long id) {
        repo.deleteById(id);
    }
}
