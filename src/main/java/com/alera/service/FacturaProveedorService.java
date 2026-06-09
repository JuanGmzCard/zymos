package com.alera.service;

import com.alera.config.UnidadUtils;
import com.alera.dto.FacturaFormDto;
import com.alera.dto.FacturaItemDto;
import com.alera.model.*;
import com.alera.model.enums.EstadoEquipo;
import com.alera.model.enums.EstadoFactura;
import com.alera.model.enums.TipoItemFactura;
import com.alera.repository.*;
import com.alera.model.Proveedor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class FacturaProveedorService {

    private static final Logger log = LoggerFactory.getLogger(FacturaProveedorService.class);

    private final FacturaProveedorRepository repo;
    private final FacturaHistorialEstadoRepository historialRepo;
    private final InsumoInventarioRepository insumoRepo;
    private final EquipoRepository equipoRepo;
    private final InsumoInventarioService insumoService;
    private final ProveedorRepository proveedorRepo;

    public FacturaProveedorService(FacturaProveedorRepository repo,
                                    FacturaHistorialEstadoRepository historialRepo,
                                    InsumoInventarioRepository insumoRepo,
                                    EquipoRepository equipoRepo,
                                    InsumoInventarioService insumoService,
                                    ProveedorRepository proveedorRepo) {
        this.repo = repo;
        this.historialRepo = historialRepo;
        this.insumoRepo = insumoRepo;
        this.equipoRepo = equipoRepo;
        this.proveedorRepo = proveedorRepo;
        this.insumoService = insumoService;
    }

    @Value("${app.page-size:15}")
    private int pageSize;

    public List<FacturaProveedor> listarTodas() {
        return repo.findAllWithItems();
    }

    public Page<FacturaProveedor> listarPaginado(EstadoFactura estado, java.time.LocalDate desde, java.time.LocalDate hasta, int page) {
        return repo.findAllFiltered(estado, desde, hasta, PageRequest.of(page, pageSize));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> suggest(String q) {
        if (q == null || q.isBlank() || q.trim().length() < 2) return List.of();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return repo.search(q.trim(), PageRequest.of(0, 6)).stream()
            .map(f -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("titulo",    f.getNumeroFactura() != null && !f.getNumeroFactura().isBlank()
                                   ? f.getNumeroFactura() : "#" + f.getId());
                m.put("proveedor", f.getProveedor() != null ? f.getProveedor() : "");
                m.put("fecha",     f.getFechaFactura() != null ? f.getFechaFactura().format(fmt) : "");
                m.put("total",     f.getValorTotal() != null ? f.getValorTotal() : BigDecimal.ZERO);
                m.put("url",       "/facturas/ver/" + f.getId());
                return m;
            }).toList();
    }

    public Optional<FacturaProveedor> buscarPorId(Long id) {
        return repo.findByIdWithItems(id);
    }

    @Transactional(readOnly = true)
    public List<FacturaProveedor> listarParaExport(EstadoFactura estado, java.time.LocalDate desde, java.time.LocalDate hasta) {
        return repo.findAllWithItems().stream()
                .filter(f -> estado == null || f.getEstado() == estado)
                .filter(f -> desde == null || (f.getFechaFactura() != null && !f.getFechaFactura().isBefore(desde)))
                .filter(f -> hasta == null || (f.getFechaFactura() != null && !f.getFechaFactura().isAfter(hasta)))
                .collect(java.util.stream.Collectors.toList());
    }

    public void cambiarEstado(Long id, EstadoFactura nuevoEstado) {
        repo.findById(id).ifPresent(f -> {
            EstadoFactura anterior = f.getEstado();
            f.setEstado(nuevoEstado);
            repo.save(f);
            historialRepo.save(FacturaHistorialEstado.of(id, anterior, nuevoEstado, usuarioActual()));
            log.info("Factura {} → estado {} (antes: {})", id, nuevoEstado, anterior);
        });
    }

    @Transactional(readOnly = true)
    public List<FacturaHistorialEstado> listarHistorial(Long facturaId) {
        return historialRepo.findByFacturaIdOrderByFechaDesc(facturaId);
    }

    @Transactional(readOnly = true)
    public BigDecimal sumTotal(EstadoFactura estado, LocalDate desde, LocalDate hasta) {
        return repo.sumTotalFiltered(estado, desde, hasta);
    }

    @Transactional(readOnly = true)
    public BigDecimal sumPendiente(LocalDate desde, LocalDate hasta) {
        return repo.sumPorEstados(List.of(EstadoFactura.RECIBIDA, EstadoFactura.VERIFICADA), desde, hasta);
    }

    @Transactional(readOnly = true)
    public long countPendiente(LocalDate desde, LocalDate hasta) {
        return repo.countPorEstados(List.of(EstadoFactura.RECIBIDA, EstadoFactura.VERIFICADA), desde, hasta);
    }

    @Transactional(readOnly = true)
    public List<FacturaProveedor> listarSinProcesar(int dias) {
        LocalDate umbral = LocalDate.now().minusDays(dias);
        return repo.findSinProcesar(List.of(EstadoFactura.RECIBIDA, EstadoFactura.VERIFICADA), umbral);
    }

    public FacturaFormDto duplicarComoFormDto(Long id) {
        FacturaProveedor original = buscarPorId(id).orElseThrow();
        FacturaFormDto dto = toFormDto(original);
        dto.setNumeroFactura(null);
        dto.setFechaFactura(null);
        dto.setEstado(EstadoFactura.RECIBIDA);
        return dto;
    }

    @CacheEvict(value = "dashboard-stats", allEntries = true)
    public FacturaProveedor guardar(FacturaFormDto dto) {
        FacturaProveedor factura = new FacturaProveedor();
        mapearDto(factura, dto);
        calcularTotales(factura);
        FacturaProveedor saved = repo.save(factura);
        historialRepo.save(FacturaHistorialEstado.of(saved.getId(), null, saved.getEstado(), usuarioActual()));
        procesarInventario(saved.getItems(), saved.getProveedor(), saved.getFechaFactura());
        log.info("Factura creada: id={} | proveedor={} | items={} | total={}",
                saved.getId(), saved.getProveedor(), saved.getItems().size(), saved.getValorTotal());
        return saved;
    }

    @CacheEvict(value = "dashboard-stats", allEntries = true)
    public FacturaProveedor actualizar(Long id, FacturaFormDto dto) {
        FacturaProveedor factura = repo.findByIdWithItems(id)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada"));
        log.info("Actualizando factura id={} | proveedor={}", id, factura.getProveedor());
        revertirInventario(factura.getItems());
        factura.getItems().clear();
        mapearDto(factura, dto);
        calcularTotales(factura);
        FacturaProveedor saved = repo.save(factura);
        procesarInventario(saved.getItems(), saved.getProveedor(), saved.getFechaFactura());
        log.info("Factura actualizada: id={} | total={}", saved.getId(), saved.getValorTotal());
        return saved;
    }

    @CacheEvict(value = "dashboard-stats", allEntries = true)
    public void eliminar(Long id) {
        repo.findByIdWithItems(id).ifPresent(f -> {
            revertirInventario(f.getItems());
            repo.delete(f);
        });
    }

    private void mapearDto(FacturaProveedor factura, FacturaFormDto dto) {
        factura.setNumeroFactura(dto.getNumeroFactura());
        factura.setEstado(dto.getEstado() != null ? dto.getEstado() : EstadoFactura.RECIBIDA);
        factura.setIvaIncluido(dto.isIvaIncluido());
        if (dto.getProveedorId() != null) {
            proveedorRepo.findById(dto.getProveedorId()).ifPresent(p -> {
                factura.setProveedorRef(p);
                if (factura.getProveedor() == null || factura.getProveedor().isBlank()) {
                    factura.setProveedor(p.getNombre());
                }
            });
        } else {
            factura.setProveedorRef(null);
        }
        factura.setProveedor(dto.getProveedor());
        factura.setFechaFactura(dto.getFechaFactura());
        factura.setDescripcion(dto.getDescripcion());
        factura.setPorcentajeIva(BigDecimal.ZERO);
        factura.setCostoEnvio(dto.getCostoEnvio() != null ? dto.getCostoEnvio() : BigDecimal.ZERO);

        if (dto.getItems() != null) {
            for (FacturaItemDto itemDto : dto.getItems()) {
                if (itemDto.getNombre() == null || itemDto.getNombre().isBlank()) continue;
                FacturaItem item = new FacturaItem();
                item.setTipoItem(itemDto.getTipoItem());
                item.setNombre(itemDto.getNombre());
                item.setTipoInsumo(itemDto.getTipoInsumo());
                item.setTipoEquipo(itemDto.getTipoEquipo());
                item.setCantidad(itemDto.getCantidad() != null ? itemDto.getCantidad() : BigDecimal.ONE);
                item.setUnidad(itemDto.getUnidad());
                item.setValorUnitario(itemDto.getValorUnitario() != null ? itemDto.getValorUnitario() : BigDecimal.ZERO);
                item.setPorcentajeDescuento(itemDto.getPorcentajeDescuento() != null ? itemDto.getPorcentajeDescuento() : BigDecimal.ZERO);
                item.setPorcentajeIvaItem(itemDto.getPorcentajeIvaItem() != null ? itemDto.getPorcentajeIvaItem() : BigDecimal.ZERO);
                item.setImpuestoConsumo(itemDto.getImpuestoConsumo() != null ? itemDto.getImpuestoConsumo() : BigDecimal.ZERO);
                item.setFactura(factura);
                factura.getItems().add(item);
            }
        }
    }

    private void calcularTotales(FacturaProveedor f) {
        BigDecimal subtotal         = BigDecimal.ZERO;
        BigDecimal totalIva         = BigDecimal.ZERO;
        BigDecimal totalImpConsumo  = BigDecimal.ZERO;

        for (FacturaItem item : f.getItems()) {
            BigDecimal base    = item.getValorBase();
            BigDecimal ivaItem = item.getValorIvaItem();
            BigDecimal ic      = item.getImpuestoConsumo() != null ? item.getImpuestoConsumo() : BigDecimal.ZERO;
            subtotal        = subtotal.add(base);
            totalIva        = totalIva.add(ivaItem);
            totalImpConsumo = totalImpConsumo.add(ic);
            item.setValorLinea(base.add(ivaItem).add(ic));
        }

        f.setSubtotal(subtotal);
        f.setPorcentajeIva(BigDecimal.ZERO);
        f.setValorIva(totalIva);
        BigDecimal envio = f.getCostoEnvio() != null ? f.getCostoEnvio() : BigDecimal.ZERO;
        f.setValorTotal(subtotal.add(totalIva).add(totalImpConsumo).add(envio));
    }

    private void procesarInventario(List<FacturaItem> items, String proveedor, java.time.LocalDate fecha) {
        for (FacturaItem item : items) {
            if (item.getTipoItem() == TipoItemFactura.INSUMO) {
                BigDecimal cantNorm = normalizarCantidad(item.getCantidad(), item.getUnidad());
                String unidadNorm = normalizeUnit(item.getUnidad());
                Optional<InsumoInventario> existing = insumoRepo.findByNombreExacto(item.getNombre());
                if (existing.isPresent()) {
                    InsumoInventario ins = existing.get();
                    ins.setCantidad(ins.getCantidad().add(cantNorm));
                    insumoRepo.save(ins);
                } else {
                    InsumoInventario nuevo = new InsumoInventario();
                    nuevo.setNombre(item.getNombre());
                    nuevo.setTipo(item.getTipoInsumo() != null ? item.getTipoInsumo() : insumoService.detectarTipo(item.getNombre()));
                    nuevo.setCantidad(cantNorm);
                    nuevo.setUnidad(unidadNorm);
                    nuevo.setStockMinimo(BigDecimal.ZERO);
                    nuevo.setProveedor(proveedor);
                    insumoRepo.save(nuevo);
                }
            } else if (item.getTipoItem() == TipoItemFactura.EQUIPO) {
                Optional<Equipo> existing = equipoRepo.findAll().stream()
                        .filter(e -> e.getNombre().equalsIgnoreCase(item.getNombre()))
                        .findFirst();
                if (existing.isPresent()) {
                    Equipo eq = existing.get();
                    eq.setObservaciones("Recomprado " + fecha + " - " + proveedor);
                    equipoRepo.save(eq);
                } else {
                    Equipo nuevo = new Equipo();
                    nuevo.setNombre(item.getNombre());
                    nuevo.setTipo(item.getTipoEquipo() != null ? item.getTipoEquipo() : "Otro");
                    nuevo.setEstado(EstadoEquipo.OPERATIVO);
                    nuevo.setFechaAdquisicion(fecha);
                    nuevo.setObservaciones("Comprado el " + fecha + " a " + proveedor);
                    equipoRepo.save(nuevo);
                }
            }
        }
    }

    private void revertirInventario(List<FacturaItem> items) {
        for (FacturaItem item : items) {
            if (item.getTipoItem() == TipoItemFactura.INSUMO) {
                BigDecimal cantNorm = normalizarCantidad(item.getCantidad(), item.getUnidad());
                insumoRepo.findByNombreExacto(item.getNombre()).ifPresent(ins -> {
                    BigDecimal nueva = ins.getCantidad().subtract(cantNorm).max(BigDecimal.ZERO);
                    ins.setCantidad(nueva);
                    insumoRepo.save(ins);
                });
            }
        }
    }

    // DRY: delegado a UnidadUtils centralizado
    private BigDecimal normalizarCantidad(BigDecimal cantidad, String unidad) {
        return UnidadUtils.convertirAUnidadBase(cantidad, unidad);
    }

    private String normalizeUnit(String unidad) {
        return UnidadUtils.unidadBase(unidad);
    }

    private String usuarioActual() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()))
               ? auth.getName() : "sistema";
    }

    public FacturaFormDto toFormDto(FacturaProveedor f) {
        FacturaFormDto dto = new FacturaFormDto();
        dto.setNumeroFactura(f.getNumeroFactura());
        dto.setEstado(f.getEstado());
        dto.setProveedor(f.getProveedor());
        if (f.getProveedorRef() != null) dto.setProveedorId(f.getProveedorRef().getId());
        dto.setFechaFactura(f.getFechaFactura());
        dto.setDescripcion(f.getDescripcion());
        dto.setPorcentajeIva(f.getPorcentajeIva());
        dto.setCostoEnvio(f.getCostoEnvio());
        dto.setIvaIncluido(f.isIvaIncluido());
        List<FacturaItemDto> itemDtos = f.getItems().stream().map(item -> {
            FacturaItemDto d = new FacturaItemDto();
            d.setTipoItem(item.getTipoItem());
            d.setNombre(item.getNombre());
            d.setTipoInsumo(item.getTipoInsumo());
            d.setTipoEquipo(item.getTipoEquipo());
            d.setCantidad(item.getCantidad());
            d.setUnidad(item.getUnidad());
            d.setValorUnitario(item.getValorUnitario());
            d.setPorcentajeDescuento(item.getPorcentajeDescuento());
            d.setPorcentajeIvaItem(item.getPorcentajeIvaItem());
            d.setImpuestoConsumo(item.getImpuestoConsumo());
            return d;
        }).collect(java.util.stream.Collectors.toList());
        if (itemDtos.isEmpty()) itemDtos.add(new FacturaItemDto());
        dto.setItems(itemDtos);
        return dto;
    }

    @CacheEvict(value = "dashboard-stats", allEntries = true)
    public Long crearDesdeOrdenCompra(com.alera.model.OrdenCompra oc) {
        FacturaProveedor factura = new FacturaProveedor();
        factura.setProveedor(oc.getProveedor());
        factura.setProveedorRef(oc.getProveedorRef());
        factura.setFechaFactura(java.time.LocalDate.now());
        factura.setEstado(EstadoFactura.RECIBIDA);
        factura.setDescripcion("Generada desde OC " + oc.getNumeroOc());

        for (com.alera.model.OrdenCompraItem ocItem : oc.getItems()) {
            FacturaItem item = new FacturaItem();
            item.setTipoItem(ocItem.getTipoItem());
            item.setNombre(ocItem.getNombre());
            item.setTipoInsumo(ocItem.getTipoInsumo());
            item.setTipoEquipo(ocItem.getTipoEquipo());
            item.setCantidad(ocItem.getCantidad() != null ? ocItem.getCantidad() : BigDecimal.ONE);
            item.setUnidad(ocItem.getUnidad());
            item.setValorUnitario(ocItem.getPrecioUnitarioEstimado() != null ? ocItem.getPrecioUnitarioEstimado() : BigDecimal.ZERO);
            item.setPorcentajeDescuento(BigDecimal.ZERO);
            item.setPorcentajeIvaItem(ocItem.getPorcentajeIvaItem() != null ? ocItem.getPorcentajeIvaItem() : BigDecimal.ZERO);
            item.setFactura(factura);
            factura.getItems().add(item);
        }

        calcularTotales(factura);
        FacturaProveedor saved = repo.save(factura);
        historialRepo.save(FacturaHistorialEstado.of(saved.getId(), null, saved.getEstado(), usuarioActual()));
        log.info("Factura creada desde OC {}: id={}", oc.getNumeroOc(), saved.getId());
        return saved.getId();
    }
}
