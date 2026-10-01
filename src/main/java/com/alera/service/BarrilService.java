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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    public List<Map<String, Object>> suggest(String q) {
        if (q == null || q.isBlank() || q.trim().length() < 2) return List.of();
        return barrilRepo.findByFiltros(q.trim(), null, PageRequest.of(0, 6)).getContent().stream()
            .map(b -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id",    b.getId());
                m.put("label", b.getCodigo() + (b.getTipo() != null ? " — " + b.getTipo() : ""));
                m.put("sub",   b.getEstado().getDisplayName()
                               + (b.getClienteNombre() != null ? " · " + b.getClienteNombre() : ""));
                return m;
            }).toList();
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

    /**
     * Crea barriles/kegs automáticamente desde el campo carbDestino del lote.
     * Solo procesa entradas que contengan "barril" o "keg" (ignorando mayúsculas).
     * Omite silenciosamente los códigos ya existentes (idempotente).
     * @return cantidad de barriles creados en esta llamada
     */
    public int crearDesdeDestino(Long loteId, String codigoLote, String carbDestino) {
        if (carbDestino == null || carbDestino.isBlank()) return 0;
        var entradas = parseDestinoBarril(carbDestino);
        int seq = 1;
        int creados = 0;
        for (var entrada : entradas) {
            int cantidad = Math.max(1, entrada.cantidad().intValue());
            for (int i = 0; i < cantidad; i++) {
                String codigo = codigoLote + "-B-" + seq++;
                if (barrilRepo.existsByCodigoIgnoreCase(codigo)) continue;
                Barril b = new Barril();
                b.setCodigo(codigo);
                b.setTipo(entrada.formato());
                b.setCapacidadLitros(parseLitros(entrada.formato()));
                b.setEstado(EstadoBarril.LLENO);
                b.setLoteId(loteId);
                b.setCodigoLote(codigoLote);
                barrilRepo.save(b);
                movimientoRepo.save(MovimientoBarril.of(
                        b.getId(), null, EstadoBarril.LLENO, usuarioActual(), "Auto-creado desde lote"));
                creados++;
            }
        }
        return creados;
    }

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

    private static final java.util.regex.Pattern DESTINO_PATTERN =
        java.util.regex.Pattern.compile("^(\\d+(?:[.,]\\d+)?)\\s*[×x]\\s*(.+)$",
            java.util.regex.Pattern.CASE_INSENSITIVE);

    private static final java.util.regex.Pattern LITROS_PATTERN =
        java.util.regex.Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*[Ll]\\b");

    private record DestinoEntry(java.math.BigDecimal cantidad, String formato) {}

    private List<DestinoEntry> parseDestinoBarril(String carbDestino) {
        var result = new java.util.ArrayList<DestinoEntry>();
        for (var parte : carbDestino.split("\\s*\\|\\s*")) {
            parte = parte.trim();
            if (parte.isEmpty()) continue;
            var m = DESTINO_PATTERN.matcher(parte);
            if (!m.matches()) continue;
            String fmt = m.group(2).trim();
            String fmtLower = fmt.toLowerCase();
            if (fmtLower.contains("barril") || fmtLower.contains("keg")) {
                result.add(new DestinoEntry(
                        new java.math.BigDecimal(m.group(1).replace(',', '.')), fmt));
            }
        }
        return result;
    }

    private static java.math.BigDecimal parseLitros(String formato) {
        if (formato == null) return null;
        var m = LITROS_PATTERN.matcher(formato);
        if (m.find()) {
            try { return new java.math.BigDecimal(m.group(1).replace(',', '.')); }
            catch (NumberFormatException ignored) {}
        }
        return null;
    }
}
