package com.alera.service;

import com.alera.model.LecturaFermentacion;
import com.alera.model.LoteCerveza;
import com.alera.repository.LecturaFermentacionRepository;
import com.alera.repository.LoteCervezaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class LecturaFermentacionService {

    private final LecturaFermentacionRepository repo;
    private final LoteCervezaRepository loteRepo;

    public LecturaFermentacionService(LecturaFermentacionRepository repo,
                                       LoteCervezaRepository loteRepo) {
        this.repo = repo;
        this.loteRepo = loteRepo;
    }

    @Transactional(readOnly = true)
    public List<LecturaFermentacion> listarPorLote(Long loteId) {
        return repo.findByLoteIdOrdenadas(loteId);
    }

    public void agregar(Long loteId, LocalDate fecha, Integer densidad,
                        BigDecimal temperatura, String notas) {
        LoteCerveza lote = loteRepo.findById(loteId)
                .orElseThrow(() -> new RuntimeException("Lote no encontrado: " + loteId));
        LecturaFermentacion lectura = new LecturaFermentacion();
        lectura.setLote(lote);
        lectura.setFecha(fecha);
        lectura.setDensidad(densidad);
        lectura.setTemperatura(temperatura);
        lectura.setNotas(notas != null && !notas.isBlank() ? notas.trim() : null);
        repo.save(lectura);
    }

    public void eliminar(Long lecturaId) {
        repo.deleteById(lecturaId);
    }
}