package com.alera.service;

import com.alera.config.TenantContext;
import com.alera.dto.VentaFormDto;
import com.alera.dto.VentaItemFormDto;
import com.alera.model.Venta;
import com.alera.model.VentaHistorialEstado;
import com.alera.model.VentaItem;
import com.alera.model.enums.EstadoVenta;
import com.alera.model.enums.TipoNotificacion;
import com.alera.repository.LoteCervezaRepository;
import com.alera.repository.VentaHistorialEstadoRepository;
import com.alera.repository.VentaItemRepository;
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
    private final VentaItemRepository ventaItemRepo;
    private final LoteCervezaRepository loteRepo;
    private final VentaHistorialEstadoRepository historialRepo;
    private final NotificacionService notificacionService;

    @Value("${app.page-size:15}")
    private int pageSize;

    public VentaService(VentaRepository ventaRepo,
                        VentaItemRepository ventaItemRepo,
                        LoteCervezaRepository loteRepo,
                        VentaHistorialEstadoRepository historialRepo,
                        NotificacionService notificacionService) {
        this.ventaRepo           = ventaRepo;
        this.ventaItemRepo       = ventaItemRepo;
        this.loteRepo            = loteRepo;
        this.historialRepo       = historialRepo;
        this.notificacionService = notificacionService;
    }

    @Transactional(readOnly = true)
    public Page<Venta> listarPaginado(EstadoVenta estado, LocalDate desde, LocalDate hasta, int page) {
        LocalDate d = desde != null ? desde : LocalDate.of(1900, 1, 1);
        LocalDate h = hasta != null ? hasta : LocalDate.of(2100, 1, 1);
        return ventaRepo.findAllFiltered(estado, d, h, PageRequest.of(page, pageSize));
    }

    @Transactional(readOnly = true)
    public Optional<Venta> buscarPorId(Long id) {
        return ventaRepo.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Venta> listarPorLote(Long loteId) {
        return ventaItemRepo.findVentasByLoteId(loteId);
    }

    @Transactional(readOnly = true)
    public List<VentaHistorialEstado> listarHistorial(Long ventaId) {
        return historialRepo.findByVentaIdOrderByFechaDesc(ventaId);
    }

    /**
     * Valida la cantidad disponible por lote para todos los ítems del DTO.
     * Retorna un mensaje de advertencia o null si todo está bien.
     */
    @Transactional(readOnly = true)
    public String validarCantidadDisponible(List<VentaItemFormDto> items, Long excludeVentaId) {
        if (items == null || items.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (VentaItemFormDto item : items) {
            if (item.getLoteId() == null || item.getCantidad() == null) continue;
            String warn = validarItemCantidad(item.getLoteId(), item.getCantidad(), excludeVentaId);
            if (warn != null) sb.append(warn).append(" ");
        }
        return sb.length() > 0 ? sb.toString().trim() : null;
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
    public List<Map<String, Object>> suggestLotesParaVenta(String q) {
        if (q == null || q.trim().length() < 2) return List.of();
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (var l : loteRepo.searchCompletados(q.trim(), PageRequest.of(0, 20))) {
            BigDecimal disponible = null;
            if (l.getLitrosFinales() != null) {
                BigDecimal vendido = ventaItemRepo.sumCantidadActivaByLote(l.getId(), null);
                disponible = l.getLitrosFinales().subtract(vendido);
                if (disponible.compareTo(BigDecimal.ZERO) <= 0) continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",                l.getId());
            m.put("codigoLote",        l.getCodigoLote());
            m.put("estilo",            l.getEstilo());
            m.put("carbDestino",       l.getCarbDestino()   != null ? l.getCarbDestino()   : "");
            m.put("litrosFinales",     l.getLitrosFinales() != null ? l.getLitrosFinales() : "");
            m.put("litrosDisponibles", disponible != null ? disponible.stripTrailingZeros().toPlainString() : "");
            result.add(m);
            if (result.size() >= 6) break;
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> suggest(String q) {
        if (q == null || q.trim().length() < 2) return List.of();
        return ventaRepo.search(q.trim(), PageRequest.of(0, 6)).stream()
                .map(v -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("titulo", v.getCliente());
                    m.put("sub",    v.getPrimerCodigoLote() != null ? v.getPrimerCodigoLote() : "Sin lote");
                    m.put("fecha",  v.getFechaDespacho() != null ? v.getFechaDespacho().toString() : "");
                    m.put("url",    "/ventas/ver/" + v.getId());
                    return m;
                }).toList();
    }

    // ── Export / Reporte ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Venta> listarParaExport(EstadoVenta estado, LocalDate desde, LocalDate hasta) {
        LocalDate d = desde != null ? desde : LocalDate.of(1900, 1, 1);
        LocalDate h = hasta != null ? hasta : LocalDate.of(2100, 1, 1);
        var lista = ventaRepo.findByPeriodo(d, h);
        if (estado != null) {
            return lista.stream().filter(v -> v.getEstado() == estado).toList();
        }
        return lista;
    }

    @Transactional(readOnly = true)
    public List<Venta> listarPorPeriodo(LocalDate desde, LocalDate hasta) {
        LocalDate d = desde != null ? desde : LocalDate.of(1900, 1, 1);
        LocalDate h = hasta != null ? hasta : LocalDate.of(2100, 1, 1);
        return ventaRepo.findByPeriodo(d, h);
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
        BigDecimal v = ventaItemRepo.sumIngresosDespachados();
        return v != null ? v : BigDecimal.ZERO;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void mapearDto(VentaFormDto dto, Venta v) {
        v.setCliente(dto.getCliente());
        v.setFechaDespacho(dto.getFechaDespacho());
        v.setNotas(dto.getNotas() != null && dto.getNotas().isBlank() ? null : dto.getNotas());
        v.setEstado(dto.getEstado() != null ? dto.getEstado() : EstadoVenta.PENDIENTE);

        v.getItems().clear();
        if (dto.getItems() != null) {
            for (VentaItemFormDto d : dto.getItems()) {
                if (d.getCantidad() == null || d.getPrecioUnitario() == null) continue;
                VentaItem item = new VentaItem();
                item.setVenta(v);
                if (d.getLoteId() != null) {
                    loteRepo.findById(d.getLoteId()).ifPresent(lote -> {
                        item.setLote(lote);
                        item.setCodigoLote(lote.getCodigoLote());
                    });
                }
                item.setDescripcion(d.getDescripcion() != null && !d.getDescripcion().isBlank()
                        ? d.getDescripcion() : null);
                item.setCantidad(d.getCantidad());
                item.setUnidad(d.getUnidad() != null && !d.getUnidad().isBlank() ? d.getUnidad() : null);
                item.setPrecioUnitario(d.getPrecioUnitario());
                item.setDescuentoPct(d.getDescuentoPct());
                v.getItems().add(item);
            }
        }
    }

    private String validarItemCantidad(Long loteId, BigDecimal nuevaCantidad, Long excludeVentaId) {
        return loteRepo.findById(loteId).map(lote -> {
            if (lote.getLitrosFinales() == null) return null;
            BigDecimal disponible = lote.getLitrosFinales();
            BigDecimal yaVendido = ventaItemRepo.sumCantidadActivaByLote(loteId, excludeVentaId);
            BigDecimal total = yaVendido.add(nuevaCantidad);
            if (total.compareTo(disponible) > 0) {
                return String.format(
                    "Advertencia: la cantidad total vendida del lote %s sería %.2f, " +
                    "superando los %.0f L producidos.",
                    lote.getCodigoLote(), total, disponible);
            }
            return null;
        }).orElse(null);
    }

    private void crearNotificacionDespacho(Venta v) {
        String primer = v.getPrimerCodigoLote();
        String loteRef = primer != null ? " (lote " + primer + ")" : "";
        int numItems = v.getItems() != null ? v.getItems().size() : 0;
        String detalle = v.getCliente() + loteRef + " despachado"
                + (numItems > 1 ? " (" + numItems + " ítems)" : ".");
        notificacionService.crear(
                TipoNotificacion.SISTEMA,
                "Venta despachada — " + v.getCliente(),
                detalle,
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
