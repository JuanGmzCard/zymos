package com.alera.service;

import com.alera.model.EvaluacionSensorial;
import com.alera.model.LoteCerveza;
import com.alera.repository.EvaluacionSensorialRepository;
import com.alera.repository.LoteCervezaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class EvaluacionSensorialService {

    private final EvaluacionSensorialRepository repo;
    private final LoteCervezaRepository loteRepo;

    public EvaluacionSensorialService(EvaluacionSensorialRepository repo,
                                       LoteCervezaRepository loteRepo) {
        this.repo = repo;
        this.loteRepo = loteRepo;
    }

    @Transactional(readOnly = true)
    public List<EvaluacionSensorial> listarPorLote(Long loteId) {
        return repo.findByLoteIdOrdenadas(loteId);
    }

    public void agregar(Long loteId, LocalDate fecha, String catador,
                        Integer aroma, Integer apariencia, Integer sabor,
                        Integer sensacionBoca, Integer impresionGeneral, String notas) {
        LoteCerveza lote = loteRepo.findById(loteId)
                .orElseThrow(() -> new RuntimeException("Lote no encontrado: " + loteId));
        EvaluacionSensorial eval = new EvaluacionSensorial();
        eval.setLote(lote);
        eval.setFecha(fecha);
        eval.setCatador(catador != null && !catador.isBlank() ? catador.trim() : null);
        eval.setAroma(aroma);
        eval.setApariencia(apariencia);
        eval.setSabor(sabor);
        eval.setSensacionBoca(sensacionBoca);
        eval.setImpresionGeneral(impresionGeneral);
        eval.setNotas(notas != null && !notas.isBlank() ? notas.trim() : null);
        repo.save(eval);
    }

    public void eliminar(Long evalId) {
        repo.deleteById(evalId);
    }

    /** Promedio del puntaje total de una lista ya cargada (evita segunda query). */
    @Transactional(readOnly = true)
    public Double calcularPromedio(List<EvaluacionSensorial> evaluaciones) {
        return evaluaciones.stream()
                .map(EvaluacionSensorial::getPuntajeTotal)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
    }
}
