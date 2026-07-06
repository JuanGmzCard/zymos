package com.alera.service;

import com.alera.model.*;
import com.alera.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class BpmService {

    private static final Logger log = LoggerFactory.getLogger(BpmService.class);

    private final RegistroSintomasRepository sintomasRepo;
    private final SolucionDesinfectanteRepository solucionesRepo;
    private final AvistamientoPlagasRepository plagasRepo;
    private final EvacuacionResiduosRepository residuosRepo;
    private final LimpiezaDesinfeccionRepository limpiezaRepo;

    public BpmService(RegistroSintomasRepository sintomasRepo,
                      SolucionDesinfectanteRepository solucionesRepo,
                      AvistamientoPlagasRepository plagasRepo,
                      EvacuacionResiduosRepository residuosRepo,
                      LimpiezaDesinfeccionRepository limpiezaRepo) {
        this.sintomasRepo = sintomasRepo;
        this.solucionesRepo = solucionesRepo;
        this.plagasRepo = plagasRepo;
        this.residuosRepo = residuosRepo;
        this.limpiezaRepo = limpiezaRepo;
    }

    // ── Síntomas ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RegistroSintomas> listarSintomas() {
        return sintomasRepo.findAllByOrderByFechaDescIdDesc();
    }

    @Transactional(readOnly = true)
    public List<RegistroSintomas> listarSintomasEntre(LocalDate desde, LocalDate hasta) {
        return sintomasRepo.findByFechaBetweenOrderByFechaAscNombreManipuladorAsc(desde, hasta);
    }

    @Transactional(readOnly = true)
    public RegistroSintomas buscarSintoma(Long id) {
        return sintomasRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Registro de síntomas no encontrado: " + id));
    }

    public void guardarSintoma(RegistroSintomas r) {
        sintomasRepo.save(r);
        log.info("BPM síntomas guardado: {} - {}", r.getFecha(), r.getNombreManipulador());
    }

    @Transactional(readOnly = true)
    public Optional<RegistroSintomas> buscarHoyPorUsuario(String username) {
        return sintomasRepo.findByNombreManipuladorAndFecha(username, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<RegistroSintomas> listarConSintomasHoy() {
        return sintomasRepo.findByFechaOrderByNombreManipuladorAsc(LocalDate.now())
                .stream()
                .filter(RegistroSintomas::tieneSintomas)
                .collect(Collectors.toList());
    }

    public void autorizarAcceso(Long id, String adminUsername, String firmaResponsable) {
        RegistroSintomas r = sintomasRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Registro no encontrado: " + id));
        r.setAutorizadoPorAdmin(true);
        r.setAutorizadoPor(adminUsername);
        if (firmaResponsable != null && !firmaResponsable.isBlank()) {
            r.setFirmaResponsable(firmaResponsable);
        }
        sintomasRepo.save(r);
        log.info("BPM acceso autorizado por {} para registro {}", adminUsername, id);
    }

    public void eliminarSintoma(Long id) {
        sintomasRepo.deleteById(id);
        log.info("BPM síntomas eliminado id={}", id);
    }

    @Transactional(readOnly = true)
    public long contarSintomasMes(LocalDate inicio, LocalDate fin) {
        return sintomasRepo.countByFechaBetween(inicio, fin);
    }

    // ── Soluciones Desinfectantes ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SolucionDesinfectante> listarSoluciones() {
        return solucionesRepo.findAllByOrderByFechaDescIdDesc();
    }

    @Transactional(readOnly = true)
    public List<SolucionDesinfectante> listarSolucionesEntre(LocalDate desde, LocalDate hasta) {
        return solucionesRepo.findByFechaBetweenOrderByFechaAscHoraAsc(desde, hasta);
    }

    @Transactional(readOnly = true)
    public SolucionDesinfectante buscarSolucion(Long id) {
        return solucionesRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Solución desinfectante no encontrada: " + id));
    }

    public void guardarSolucion(SolucionDesinfectante r) {
        solucionesRepo.save(r);
        log.info("BPM solución guardada: {} - {}", r.getFecha(), r.getProducto());
    }

    public void eliminarSolucion(Long id) {
        solucionesRepo.deleteById(id);
        log.info("BPM solución eliminada id={}", id);
    }

    @Transactional(readOnly = true)
    public long contarSolucionesMes(LocalDate inicio, LocalDate fin) {
        return solucionesRepo.countByFechaBetween(inicio, fin);
    }

    // ── Plagas ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AvistamientoPlagas> listarPlagas() {
        return plagasRepo.findAllByOrderByFechaDescIdDesc();
    }

    @Transactional(readOnly = true)
    public List<AvistamientoPlagas> listarPlagasEntre(LocalDate desde, LocalDate hasta) {
        return plagasRepo.findByFechaBetweenOrderByFechaAsc(desde, hasta);
    }

    @Transactional(readOnly = true)
    public AvistamientoPlagas buscarPlaga(Long id) {
        return plagasRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Registro de plagas no encontrado: " + id));
    }

    public void guardarPlaga(AvistamientoPlagas r) {
        plagasRepo.save(r);
        log.info("BPM plagas guardado: {}", r.getFecha());
    }

    public void eliminarPlaga(Long id) {
        plagasRepo.deleteById(id);
        log.info("BPM plagas eliminado id={}", id);
    }

    @Transactional(readOnly = true)
    public long contarPlagasMes(LocalDate inicio, LocalDate fin) {
        return plagasRepo.countByFechaBetween(inicio, fin);
    }

    // ── Residuos ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<EvacuacionResiduos> listarResiduos() {
        return residuosRepo.findAllByOrderByFechaDescIdDesc();
    }

    @Transactional(readOnly = true)
    public List<EvacuacionResiduos> listarResiduosEntre(LocalDate desde, LocalDate hasta) {
        return residuosRepo.findByFechaBetweenOrderByFechaAscHoraSalidaAsc(desde, hasta);
    }

    @Transactional(readOnly = true)
    public EvacuacionResiduos buscarResiduo(Long id) {
        return residuosRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Registro de residuos no encontrado: " + id));
    }

    public void guardarResiduo(EvacuacionResiduos r) {
        residuosRepo.save(r);
        log.info("BPM residuos guardado: {}", r.getFecha());
    }

    public void eliminarResiduo(Long id) {
        residuosRepo.deleteById(id);
        log.info("BPM residuos eliminado id={}", id);
    }

    @Transactional(readOnly = true)
    public long contarResiduosMes(LocalDate inicio, LocalDate fin) {
        return residuosRepo.countByFechaBetween(inicio, fin);
    }

    // ── Limpieza ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<LimpiezaDesinfeccion> listarLimpieza() {
        return limpiezaRepo.findAllByOrderByFechaDescIdDesc();
    }

    @Transactional(readOnly = true)
    public List<LimpiezaDesinfeccion> listarLimpiezaEntre(LocalDate desde, LocalDate hasta) {
        return limpiezaRepo.findByFechaBetweenOrderByFechaAscAreaUtensilioAsc(desde, hasta);
    }

    @Transactional(readOnly = true)
    public LimpiezaDesinfeccion buscarLimpieza(Long id) {
        return limpiezaRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Registro de limpieza no encontrado: " + id));
    }

    public void guardarLimpieza(LimpiezaDesinfeccion r) {
        limpiezaRepo.save(r);
        log.info("BPM limpieza guardada: {} - {}", r.getFecha(), r.getAreaUtensilio());
    }

    public void eliminarLimpieza(Long id) {
        limpiezaRepo.deleteById(id);
        log.info("BPM limpieza eliminada id={}", id);
    }

    @Transactional(readOnly = true)
    public long contarLimpiezaMes(LocalDate inicio, LocalDate fin) {
        return limpiezaRepo.countByFechaBetween(inicio, fin);
    }
}
