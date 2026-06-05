package com.alera.service;

import com.alera.model.Barril;
import com.alera.model.MovimientoBarril;
import com.alera.model.enums.EstadoBarril;
import com.alera.repository.BarrilRepository;
import com.alera.repository.MovimientoBarrilRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class BarrilService {

    private final BarrilRepository             barrilRepo;
    private final MovimientoBarrilRepository   movimientoRepo;

    @Value("${app.page-size:15}")
    private int pageSize;

    public BarrilService(BarrilRepository barrilRepo,
                         MovimientoBarrilRepository movimientoRepo) {
        this.barrilRepo    = barrilRepo;
        this.movimientoRepo = movimientoRepo;
    }

    @Transactional(readOnly = true)
    public Page<Barril> listarPaginado(String codigo, EstadoBarril estado, int page) {
        String codigoFiltro = (codigo == null) ? "" : codigo.trim();
        return barrilRepo.findByFiltros(codigoFiltro, estado, PageRequest.of(page, pageSize));
    }

    @Transactional(readOnly = true)
    public Barril buscarPorId(Long id) {
        return barrilRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Barril no encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public List<MovimientoBarril> listarMovimientos(Long barrilId) {
        return movimientoRepo.findByBarrilIdOrderByFechaDesc(barrilId);
    }

    public Barril guardar(Barril barril) {
        validarCodigoUnico(barril.getCodigo(), null);
        normalizar(barril);
        Barril guardado = barrilRepo.save(barril);
        movimientoRepo.save(MovimientoBarril.of(
                guardado.getId(), null, guardado.getEstado(), usuarioActual(), null));
        return guardado;
    }

    public Barril actualizar(Long id, Barril barril) {
        Barril existente = buscarPorId(id);
        if (!existente.getCodigo().equalsIgnoreCase(barril.getCodigo())) {
            validarCodigoUnico(barril.getCodigo(), id);
        }
        normalizar(barril);
        barril.setId(id);
        return barrilRepo.save(barril);
    }

    public void cambiarEstado(Long id, EstadoBarril nuevoEstado, String notas) {
        Barril barril = buscarPorId(id);
        EstadoBarril estadoAnterior = barril.getEstado();
        barril.setEstado(nuevoEstado);

        if (nuevoEstado == EstadoBarril.DISPONIBLE || nuevoEstado == EstadoBarril.VACIO
                || nuevoEstado == EstadoBarril.LIMPIEZA || nuevoEstado == EstadoBarril.BAJA) {
            barril.setLoteId(null);
            barril.setCodigoLote(null);
            barril.setClienteNombre(null);
            barril.setFechaDespacho(null);
        }

        barrilRepo.save(barril);
        movimientoRepo.save(MovimientoBarril.of(
                id, estadoAnterior, nuevoEstado, usuarioActual(), notas));
    }

    public void eliminar(Long id) {
        buscarPorId(id);
        barrilRepo.deleteById(id);
    }

    @Transactional(readOnly = true)
    public long countTotal()                         { return barrilRepo.count(); }

    @Transactional(readOnly = true)
    public long countByEstado(EstadoBarril estado)   { return barrilRepo.countByEstado(estado); }

    // ── helpers ────────────────────────────────────────────────────────────

    private void normalizar(Barril b) {
        if (b.getClienteNombre() != null && b.getClienteNombre().isBlank()) b.setClienteNombre(null);
        if (b.getCodigoLote()    != null && b.getCodigoLote().isBlank())    b.setCodigoLote(null);
        if (b.getObservaciones() != null && b.getObservaciones().isBlank()) b.setObservaciones(null);
    }

    private void validarCodigoUnico(String codigo, Long excludeId) {
        boolean existe = excludeId == null
                ? barrilRepo.existsByCodigoIgnoreCase(codigo)
                : barrilRepo.existsByCodigoIgnoreCaseAndIdNot(codigo, excludeId);
        if (existe) {
            throw new RuntimeException("Ya existe un barril con el código: " + codigo);
        }
    }

    private String usuarioActual() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            return (auth != null) ? auth.getName() : "sistema";
        } catch (Exception e) {
            return "sistema";
        }
    }
}
