package com.alera.service;

import com.alera.dto.VentaFormDto;
import com.alera.model.Venta;
import com.alera.model.enums.EstadoVenta;
import com.alera.repository.LoteCervezaRepository;
import com.alera.repository.VentaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class VentaService {

    private final VentaRepository ventaRepo;
    private final LoteCervezaRepository loteRepo;

    @Value("${app.page-size:15}")
    private int pageSize;

    public VentaService(VentaRepository ventaRepo, LoteCervezaRepository loteRepo) {
        this.ventaRepo = ventaRepo;
        this.loteRepo  = loteRepo;
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

    public Venta guardar(VentaFormDto dto) {
        Venta v = new Venta();
        mapearDto(dto, v);
        v.setCreatedBy(usuarioActual());
        v.setLastModifiedBy(usuarioActual());
        return ventaRepo.save(v);
    }

    public Venta actualizar(Long id, VentaFormDto dto) {
        Venta v = ventaRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada: " + id));
        mapearDto(dto, v);
        v.setLastModifiedBy(usuarioActual());
        return ventaRepo.save(v);
    }

    public void eliminar(Long id) {
        ventaRepo.deleteById(id);
    }

    public void cambiarEstado(Long id, EstadoVenta nuevoEstado) {
        ventaRepo.findById(id).ifPresent(v -> {
            v.setEstado(nuevoEstado);
            v.setLastModifiedBy(usuarioActual());
            ventaRepo.save(v);
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

    private String usuarioActual() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            return (auth != null) ? auth.getName() : "sistema";
        } catch (Exception e) {
            return "sistema";
        }
    }
}
