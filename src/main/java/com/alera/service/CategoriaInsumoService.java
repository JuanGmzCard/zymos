package com.alera.service;

import com.alera.model.CategoriaInsumo;
import com.alera.repository.CategoriaInsumoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoriaInsumoService {

    private final CategoriaInsumoRepository repo;

    public CategoriaInsumoService(CategoriaInsumoRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<String> listarNombresActivos() {
        return repo.findAllByActivoTrueOrderByNombreAsc()
                   .stream().map(CategoriaInsumo::getNombre).toList();
    }

    @Transactional(readOnly = true)
    public List<CategoriaInsumo> listarTodos() {
        return repo.findAllByOrderByNombreAsc();
    }

    public CategoriaInsumo guardar(String nombre) {
        String nombreTrim = nombre.trim();
        if (repo.existsByNombreIgnoreCase(nombreTrim))
            throw new RuntimeException("Ya existe una categoría con ese nombre");
        CategoriaInsumo cat = new CategoriaInsumo();
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
