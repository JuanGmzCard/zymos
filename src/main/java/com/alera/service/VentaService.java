package com.alera.service;

import com.alera.config.TenantContext;
import com.alera.dto.VentaFormDto;
import com.alera.model.Venta;
import com.alera.model.VentaHistorialEstado;
import com.alera.model.enums.EstadoVenta;
import com.alera.model.enums.TipoNotificacion;
import com.alera.repository.LoteCervezaRepository;
import com.alera.repository.VentaHistorialEstadoRepository;
import com.alera.repository.VentaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class VentaService {

    private final VentaRepository ventaRepo;
    private final LoteCervezaRepository loteRepo;
    private final VentaHistorialEstadoRepository historialRepo;
    private final NotificacionService notificacionService;

    @Value("${app.page-size:15}")
    private int pageSize;

    public VentaService(VentaRepository ventaRepo,
                        LoteCervezaRepository loteRepo,
                        VentaHistorialEstadoRepository historialRepo,
                        NotificacionService notificacionService) {
        this.ventaRepo         = ventaRepo;
        this.loteRepo          = loteRepo;
        this.historialRepo     = historialRepo;
        this.notificacionService = notificacionService;
    }

    @Transactional(readOnly = true)
    public Page<Venta> listarPaginado(EstadoVenta estado, LocalDate desde, LocalDate hasta, int page) {
        return ventaRepo.findAllFiltered(estado, desde, hasta, PageRequest.of(page, pageSize));
    }

    @Transactional(readOnly = true)
    public Optional<Venta> buscarPorId(Long id) {
        return ventaRepo.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Venta> listarPorLote(Long loteId) {
        return ventaRepo.findByLoteIdOrderByFechaDespachoDesc(loteId);
    }

    @Transactional(readOnly = true)
    public List<VentaHistorialEstado> listarHistorial(Long ventaId) {
        return historialRepo.findByVentaIdOrderByFechaDesc(ventaId);
    }

    /**
     * Verifica si la cantidad de la nueva venta supera el disponible en el lote.
     * Retorna un mensaje de advertencia o null si todo está bien.
     */
    @Transactional(readOnly = true)
    public String validarCantidadDisponible(Long loteId, BigDecimal nuevaCantidad, Long excludeVentaId) {
        if (loteId == null || nuevaCantidad == null) return null;
        return loteRepo.findById(loteId).map(lote -> {
            if (lote.getLitrosFinales() == null) return null;
            BigDecimal disponible = lote.getLitrosFinales();
            BigDecimal yaVendido = ventaRepo.sumCantidadActivaByLote(loteId, excludeVentaId);
            BigDecimal total = yaVendido.add(nuevaCantidad);
            if (total.compareTo(disponible) > 0) {
                return String.format(
                    "Advertencia: la cantidad total vendida del lote %s sería %.2f L, " +
                    "superando los %.0f L producidos.",
                    lote.getCodigoLote(), total, disponible);
            }
            return null;
        }).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> topClientes() {
        String tenantId = TenantContext.getCurrentTenant();
        return ventaRepo.findTopClientes(tenantId);
    }

    public Venta guardar(VentaFormDto dto) {
        Venta v = new Venta();
        mapearDto(dto, v);
        v.setCreatedBy(usuarioActual());
        v.setLastModifiedBy(usuarioActual());
        Venta saved = ventaRepo.save(v);
        historialRepo.save(VentaHistorialEstado.of(
                saved.getId(), null, saved.getEstado(), usuarioActual()));
        return saved;
    }

    public Venta actualizar(Long id, VentaFormDto dto) {
        Venta v = ventaRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada: " + id));
        EstadoVenta estadoAnterior = v.getEstado();
        mapearDto(dto, v);
        v.setLastModifiedBy(usuarioActual());
        Venta saved = ventaRepo.save(v);
        if (estadoAnterior != saved.getEstado()) {
            historialRepo.save(VentaHistorialEstado.of(
                    saved.getId(), estadoAnterior, saved.getEstado(), usuarioActual()));
            if (saved.getEstado() == EstadoVenta.DESPACHADO) {
                crearNotificacionDespacho(saved);
            }
        }
        return saved;
    }

    public void eliminar(Long id) {
        ventaRepo.findById(id).ifPresent(v -> {
            v.setDeletedAt(LocalDateTime.now());
            v.setLastModifiedBy(usuarioActual());
            ventaRepo.save(v);
        });
    }

    public void cambiarEstado(Long id, EstadoVenta nuevoEstado) {
        ventaRepo.findById(id).ifPresent(v -> {
            EstadoVenta anterior = v.getEstado();
            v.setEstado(nuevoEstado);
            v.setLastModifiedBy(usuarioActual());
            ventaRepo.save(v);
            historialRepo.save(VentaHistorialEstado.of(id, anterior, nuevoEstado, usuarioActual()));
            if (nuevoEstado == EstadoVenta.DESPACHADO) {
                crearNotificacionDespacho(v);
            }
        });
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> suggest(String q) {
        if (q == null || q.trim().length() < 2) return List.of();
        return ventaRepo.search(q.trim(), PageRequest.of(0, 6)).stream()
                .map(v -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("titulo", v.getCliente());
                    m.put("sub",    v.getCodigoLote() != null ? v.getCodigoLote() : "Sin lote");
                    m.put("fecha",  v.getFechaDespacho() != null ? v.getFechaDespacho().toString() : "");
                    m.put("url",    "/ventas/ver/" + v.getId());
                    return m;
                }).toList();
    }

    // ── Export / Reporte ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Venta> listarParaExport(EstadoVenta estado, LocalDate desde, LocalDate hasta) {
        var lista = ventaRepo.findByPeriodo(desde, hasta);
        if (estado != null) {
            return lista.stream().filter(v -> v.getEstado() == estado).toList();
        }
        return lista;
    }

    @Transactional(readOnly = true)
    public List<Venta> listarPorPeriodo(LocalDate desde, LocalDate hasta) {
        return ventaRepo.findByPeriodo(desde, hasta);
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public long countTotal() { return ventaRepo.count(); }

    @Transactional(readOnly = true)
    public long countByEstado(EstadoVenta estado) { return ventaRepo.countByEstado(estado); }

    @Transactional(readOnly = true)
    public long countClientesUnicos() { return ventaRepo.countClientesUnicos(); }

    @Transactional(readOnly = true)
    public BigDecimal sumIngresosDespachados() {
        BigDecimal v = ventaRepo.sumIngresosDespachados();
        return v != null ? v : BigDecimal.ZERO;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void mapearDto(VentaFormDto dto, Venta v) {
        if (dto.getLoteId() != null) {
            loteRepo.findById(dto.getLoteId()).ifPresent(lote -> {
                v.setLote(lote);
                v.setCodigoLote(lote.getCodigoLote());
            });
        } else {
            v.setLote(null);
            v.setCodigoLote(null);
        }
        v.setCliente(dto.getCliente());
        v.setFechaDespacho(dto.getFechaDespacho());
        v.setCantidad(dto.getCantidad());
        v.setUnidad(dto.getUnidad());
        v.setPrecioUnitario(dto.getPrecioUnitario());
        v.setDescuentoPct(dto.getDescuentoPct());
        v.setNotas(dto.getNotas() != null && dto.getNotas().isBlank() ? null : dto.getNotas());
        v.setEstado(dto.getEstado() != null ? dto.getEstado() : EstadoVenta.PENDIENTE);
    }

    private void crearNotificacionDespacho(Venta v) {
        String loteRef = v.getCodigoLote() != null ? " (lote " + v.getCodigoLote() + ")" : "";
        notificacionService.crear(
                TipoNotificacion.SISTEMA,
                "Venta despachada — " + v.getCliente(),
                v.getCliente() + loteRef + " despachado" + (v.getUnidad() != null
                        ? ": " + v.getCantidad() + " " + v.getUnidad() : "."),
                "/ventas/ver/" + v.getId());
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
