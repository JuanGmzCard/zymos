package com.alera.service;

import com.alera.config.TenantContext;
import com.alera.dto.VentaFormDto;
import com.alera.dto.VentaItemFormDto;
import com.alera.model.Cliente;
import com.alera.model.Venta;
import com.alera.model.VentaHistorialEstado;
import com.alera.model.VentaItem;
import com.alera.model.enums.EstadoVenta;
import com.alera.model.enums.TipoNotificacion;
import com.alera.repository.ClienteRepository;
import com.alera.repository.LoteCervezaRepository;
import com.alera.repository.VentaHistorialEstadoRepository;
import com.alera.repository.VentaItemRepository;
import com.alera.repository.VentaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class VentaService {

    private static final Logger log = LoggerFactory.getLogger(VentaService.class);

    private static final Map<EstadoVenta, Set<EstadoVenta>> TRANSICIONES_VALIDAS = Map.of(
        EstadoVenta.COTIZACION, Set.of(EstadoVenta.PENDIENTE, EstadoVenta.CANCELADO),
        EstadoVenta.PENDIENTE,  Set.of(EstadoVenta.DESPACHADO, EstadoVenta.CANCELADO),
        EstadoVenta.DESPACHADO, Set.of(),
        EstadoVenta.CANCELADO,  Set.of(),
        EstadoVenta.EXPIRADO,   Set.of()
    );

    private final VentaRepository             ventaRepo;
    private final VentaItemRepository         ventaItemRepo;
    private final LoteCervezaRepository       loteRepo;
    private final VentaHistorialEstadoRepository historialRepo;
    private final NotificacionService         notificacionService;
    private final ClienteRepository           clienteRepo;
    private final InsumoInventarioService     insumoService;

    @PersistenceContext
    private EntityManager em;

    @Value("${app.page-size:15}")
    private int pageSize;

    @Value("${app.cotizacion.expiracion-dias:15}")
    private int expiracionDias;

    public VentaService(VentaRepository ventaRepo,
                        VentaItemRepository ventaItemRepo,
                        LoteCervezaRepository loteRepo,
                        VentaHistorialEstadoRepository historialRepo,
                        NotificacionService notificacionService,
                        ClienteRepository clienteRepo,
                        InsumoInventarioService insumoService) {
        this.ventaRepo           = ventaRepo;
        this.ventaItemRepo       = ventaItemRepo;
        this.loteRepo            = loteRepo;
        this.historialRepo       = historialRepo;
        this.notificacionService = notificacionService;
        this.clienteRepo         = clienteRepo;
        this.insumoService       = insumoService;
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

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
        return ventaItemRepo.findVentasByLoteId(loteId);
    }

    @Transactional(readOnly = true)
    public List<VentaHistorialEstado> listarHistorial(Long ventaId) {
        return historialRepo.findByVentaIdOrderByFechaDesc(ventaId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> topClientes() {
        String tenantId = TenantContext.getCurrentTenant();
        return ventaRepo.findTopClientes(tenantId);
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

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
                generarRemisionNumero(saved);
                descontarEnvases(saved);
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
            Set<EstadoVenta> permitidos = TRANSICIONES_VALIDAS.getOrDefault(anterior, Set.of());
            if (!permitidos.contains(nuevoEstado)) {
                throw new RuntimeException("Transición de estado no válida: " + anterior + " → " + nuevoEstado);
            }
            v.setEstado(nuevoEstado);
            v.setLastModifiedBy(usuarioActual());
            ventaRepo.save(v);
            historialRepo.save(VentaHistorialEstado.of(id, anterior, nuevoEstado, usuarioActual()));
            if (nuevoEstado == EstadoVenta.DESPACHADO) {
                generarRemisionNumero(v);
                descontarEnvases(v);
                crearNotificacionDespacho(v);
            }
        });
    }

    // ── Expiración de cotizaciones (llamado por AlertaScheduler) ─────────────

    public int expirarCotizaciones() {
        List<Venta> vencidas = ventaRepo.findCotizacionesVencidas(LocalDate.now());
        for (Venta v : vencidas) {
            EstadoVenta anterior = v.getEstado();
            v.setEstado(EstadoVenta.EXPIRADO);
            v.setLastModifiedBy("sistema");
            ventaRepo.save(v);
            historialRepo.save(VentaHistorialEstado.of(v.getId(), anterior, EstadoVenta.EXPIRADO, "sistema"));
            notificacionService.crear(
                    TipoNotificacion.SISTEMA,
                    "Cotización expirada — " + v.getCliente(),
                    "La cotización venció el " + v.getCotizacionExpiraEn(),
                    "/ventas/ver/" + v.getId());
        }
        return vencidas.size();
    }

    // ── Validación de disponibilidad ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public String validarCantidadDisponible(List<VentaItemFormDto> items, Long excludeVentaId) {
        if (items == null || items.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (VentaItemFormDto item : items) {
            if (item.getLoteId() == null || item.getCantidad() == null) continue;
            String warn = validarItemCantidad(item.getLoteId(), item.getCantidad(), item.getUnidad(), excludeVentaId);
            if (warn != null) sb.append(warn).append(" ");
        }
        return sb.length() > 0 ? sb.toString().trim() : null;
    }

    // ── Suggest / Export ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Map<String, Object>> suggestLotesParaVenta(String q) {
        String trimmed = q != null ? q.trim() : "";
        boolean sinFiltro = trimmed.isEmpty();
        int preload = sinFiltro ? 50 : 20;
        int limit   = sinFiltro ? 20 : 6;
        var candidatos = sinFiltro
                ? loteRepo.findAllCompletados(PageRequest.of(0, preload))
                : loteRepo.searchCompletados(trimmed, PageRequest.of(0, preload));
        List<Map<String, Object>> result = new ArrayList<>();
        for (var l : candidatos) {
            BigDecimal vendido = ventaItemRepo.sumCantidadActivaByLote(l.getId(), null);
            BigDecimal disponible = null;
            String unidadDisponible = "L";

            String carbDestino = l.getCarbDestino();
            List<DestinoEntry> entradas = parseDestino(carbDestino);
            if (!entradas.isEmpty()) {
                BigDecimal totalCapacidad = entradas.stream()
                    .map(DestinoEntry::cantidad).reduce(BigDecimal.ZERO, BigDecimal::add);
                disponible = totalCapacidad.subtract(vendido);
                if (disponible.compareTo(BigDecimal.ZERO) <= 0) continue;
                unidadDisponible = entradas.size() == 1 ? entradas.get(0).formato() : "uds";
            }

            if (disponible == null && l.getLitrosFinales() != null) {
                disponible = l.getLitrosFinales().subtract(vendido);
                if (disponible.compareTo(BigDecimal.ZERO) <= 0) continue;
            }

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",                l.getId());
            m.put("codigoLote",        l.getCodigoLote());
            m.put("estilo",            l.getEstilo());
            m.put("carbDestino",       carbDestino != null ? carbDestino : "");
            m.put("litrosFinales",     l.getLitrosFinales() != null ? l.getLitrosFinales() : "");
            m.put("litrosDisponibles", disponible != null
                    ? disponible.stripTrailingZeros().toPlainString() + " " + unidadDisponible
                    : "");
            result.add(m);
            if (result.size() >= limit) break;
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<String> suggestClientes(String q) {
        if (q == null || q.trim().length() < 1) return List.of();
        return clienteRepo.searchActivos(q.trim(), PageRequest.of(0, 8))
                .stream().map(Cliente::getNombre).toList();
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
        BigDecimal v = ventaItemRepo.sumIngresosDespachados();
        return v != null ? v : BigDecimal.ZERO;
    }

    // ── Helpers privados ──────────────────────────────────────────────────────

    private void mapearDto(VentaFormDto dto, Venta v) {
        // Cliente registrado (obligatorio en nuevas ventas)
        if (dto.getClienteId() != null) {
            clienteRepo.findById(dto.getClienteId()).ifPresent(c -> {
                v.setClienteRef(c);
                v.setCliente(c.getNombre());
            });
        } else if (dto.getCliente() != null && !dto.getCliente().isBlank()) {
            // Retrocompatibilidad: ventas sin clienteId (datos pre-migración)
            v.setCliente(dto.getCliente());
        }

        v.setFechaDespacho(dto.getFechaDespacho());
        v.setNotas(dto.getNotas() != null && dto.getNotas().isBlank() ? null : dto.getNotas());

        EstadoVenta estado = dto.getEstado() != null ? dto.getEstado() : EstadoVenta.PENDIENTE;
        v.setEstado(estado);

        // Fecha de expiración para cotizaciones
        if (estado == EstadoVenta.COTIZACION) {
            if (dto.getCotizacionExpiraEn() != null) {
                v.setCotizacionExpiraEn(dto.getCotizacionExpiraEn());
            } else if (v.getCotizacionExpiraEn() == null) {
                v.setCotizacionExpiraEn(LocalDate.now().plusDays(expiracionDias));
            }
        } else {
            // Limpiar fecha de expiración si cambia a otro estado
            if (v.getCotizacionExpiraEn() != null && v.getId() == null) {
                v.setCotizacionExpiraEn(null);
            }
        }

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

    private void generarRemisionNumero(Venta v) {
        if (v.getRemisionNumero() != null) return;
        em.flush();
        String tenantId = TenantContext.getCurrentTenant();
        Object maxObj = em.createNativeQuery(
                "SELECT COALESCE(MAX(CAST(SUBSTRING(remision_numero FROM 5) AS INTEGER)), 0) " +
                "FROM ventas WHERE tenant_id = :tid AND remision_numero IS NOT NULL AND deleted_at IS NULL")
                .setParameter("tid", tenantId)
                .getSingleResult();
        int siguiente = ((Number) maxObj).intValue() + 1;
        v.setRemisionNumero(String.format("REM-%03d", siguiente));
    }

    private void descontarEnvases(Venta v) {
        if (v.getId() == null) return;
        List<VentaItem> itemsEnvase = ventaItemRepo.findItemsConEnvase(v.getId());
        for (VentaItem item : itemsEnvase) {
            String insuficiente = insumoService.descontarIngrediente(
                    item.getUnidad(),
                    item.getCantidad().toPlainString(),
                    "DESPACHO-" + v.getId());
            if (insuficiente != null) {
                log.warn("Stock insuficiente al descontar envase en despacho {}: {}", v.getId(), insuficiente);
            }
        }
    }

    private static final java.util.regex.Pattern DESTINO_PATTERN =
        java.util.regex.Pattern.compile("^(\\d+(?:[.,]\\d+)?)\\s*[×x]\\s*(.+)$",
            java.util.regex.Pattern.CASE_INSENSITIVE);

    private record DestinoEntry(BigDecimal cantidad, String formato) {}

    private List<DestinoEntry> parseDestino(String carbDestino) {
        if (carbDestino == null || carbDestino.isBlank()) return List.of();
        var result = new ArrayList<DestinoEntry>();
        for (var parte : carbDestino.split("\\s*\\|\\s*")) {
            parte = parte.trim();
            if (parte.isEmpty()) continue;
            var m = DESTINO_PATTERN.matcher(parte);
            if (m.matches()) result.add(new DestinoEntry(
                new BigDecimal(m.group(1).replace(',', '.')), m.group(2).trim()));
        }
        return result;
    }

    private String validarItemCantidad(Long loteId, BigDecimal nuevaCantidad, String unidad, Long excludeVentaId) {
        return loteRepo.findById(loteId).map(lote -> {
            if (unidad != null && !unidad.isBlank()) {
                var previas = ventaItemRepo.findUnidadesActivasByLote(loteId, excludeVentaId);
                if (!previas.isEmpty() && previas.stream().anyMatch(u -> !u.equals(unidad))) {
                    return String.format(
                        "Advertencia: el lote %s ya tiene ventas registradas en %s. " +
                        "Verifica que mezclar unidades sea intencional.",
                        lote.getCodigoLote(), String.join(", ", previas));
                }
            }

            BigDecimal yaVendido = ventaItemRepo.sumCantidadActivaByLote(loteId, excludeVentaId);
            BigDecimal total = yaVendido.add(nuevaCantidad);

            String carbDestino = lote.getCarbDestino();
            List<DestinoEntry> entradas = parseDestino(carbDestino);
            if (!entradas.isEmpty()) {
                var matchingEntrada = entradas.stream()
                    .filter(e -> unidad == null || unidad.isBlank() || unidad.equalsIgnoreCase(e.formato()))
                    .findFirst();
                if (matchingEntrada.isPresent()) {
                    var entrada = matchingEntrada.get();
                    BigDecimal vendidoFormato = ventaItemRepo.sumCantidadActivaByLoteAndUnidad(
                        loteId, entrada.formato(), excludeVentaId);
                    BigDecimal totalFormato = vendidoFormato.add(nuevaCantidad);
                    if (totalFormato.compareTo(entrada.cantidad()) > 0) {
                        return String.format(
                            "Advertencia: la cantidad vendida del lote %s en %s sería %.0f, " +
                            "superando las %.0f envasadas.",
                            lote.getCodigoLote(), entrada.formato(), totalFormato, entrada.cantidad());
                    }
                    return null;
                }
            }

            if (unidad != null && !unidad.isBlank() && !unidad.equals("L") && !unidad.equals("A granel")) {
                return null;
            }
            if (lote.getLitrosFinales() == null) return null;
            if (total.compareTo(lote.getLitrosFinales()) > 0) {
                return String.format(
                    "Advertencia: la cantidad total vendida del lote %s sería %.2f L, " +
                    "superando los %.0f L producidos.",
                    lote.getCodigoLote(), total, lote.getLitrosFinales());
            }
            return null;
        }).orElse(null);
    }

    private void crearNotificacionDespacho(Venta v) {
        String primer = v.getPrimerCodigoLote();
        String loteRef = primer != null ? " (lote " + primer + ")" : "";
        String remision = v.getRemisionNumero() != null ? " [" + v.getRemisionNumero() + "]" : "";
        notificacionService.crear(
                TipoNotificacion.SISTEMA,
                "Venta despachada — " + v.getCliente() + remision,
                v.getCliente() + loteRef + " despachado.",
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
