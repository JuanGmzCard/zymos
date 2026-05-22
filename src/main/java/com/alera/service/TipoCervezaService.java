package com.alera.service;

import com.alera.model.TipoCerveza;
import com.alera.repository.TipoCervezaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TipoCervezaService {

    private final TipoCervezaRepository repo;

    public TipoCervezaService(TipoCervezaRepository repo) {
        this.repo = repo;
    }

    public List<TipoCerveza> listarActivos() {
        return repo.findByActivoTrueOrderByNombreAsc();
    }

    public List<TipoCerveza> listarTodos() {
        return repo.findAll();
    }

    public Optional<TipoCerveza> buscarPorId(Long id) {
        return repo.findById(id);
    }

    public boolean existePorNombre(String nombre) {
        return repo.existsByNombreIgnoreCase(nombre);
    }

    public TipoCerveza guardar(TipoCerveza tipo) {
        return repo.save(tipo);
    }

    public void eliminar(Long id) {
        repo.deleteById(id);
    }

    public void toggleActivo(Long id) {
        repo.findById(id).ifPresent(t -> {
            t.setActivo(!t.isActivo());
            repo.save(t);
        });
    }
}
