package com.alera.service;

import com.alera.config.TenantContext;
import com.alera.dto.OrdenCompraFormDto;
import com.alera.dto.OrdenCompraItemDto;
import com.alera.model.OrdenCompra;
import com.alera.model.OrdenCompraItem;
import com.alera.model.enums.EstadoOrdenCompra;
import com.alera.repository.OrdenCompraRepository;
import com.alera.repository.ProveedorRepository;
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

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class OrdenCompraService {

    private static final Logger log = LoggerFactory.getLogger(OrdenCompraService.class);

    private static final Map<EstadoOrdenCompra, Set<EstadoOrdenCompra>> TRANSICIONES = Map.of(
            EstadoOrdenCompra.BORRADOR,         EnumSet.of(EstadoOrdenCompra.ENVIADA, EstadoOrdenCompra.CANCELADA),
            EstadoOrdenCompra.ENVIADA,          EnumSet.of(EstadoOrdenCompra.RECIBIDA_PARCIAL, EstadoOrdenCompra.RECIBIDA, EstadoOrdenCompra.CANCELADA),
            EstadoOrdenCompra.RECIBIDA_PARCIAL, EnumSet.of(EstadoOrdenCompra.RECIBIDA, EstadoOrdenCompra.CANCELADA),
            EstadoOrdenCompra.RECIBIDA,         EnumSet.noneOf(EstadoOrdenCompra.class),
            EstadoOrdenCompra.CANCELADA,        EnumSet.noneOf(EstadoOrdenCompra.class)
    );

    private final OrdenCompraRepository repo;
    private final ProveedorRepository proveedorRepo;

    @PersistenceContext
    private EntityManager em;

    @Value("${app.page-size:15}")
    private int pageSize;

    public OrdenCompraService(OrdenCompraRepository repo, ProveedorRepository proveedorRepo) {
        this.repo          = repo;
        this.proveedorRepo = proveedorRepo;
    }

    @Transactional(readOnly = true)
    public Page<OrdenCompra> listarPaginado(EstadoOrdenCompra estado, int page) {
        return repo.findAllFiltered(estado, PageRequest.of(page, pageSize));
    }

    @Transactional(readOnly = true)
    public OrdenCompra buscarPorId(Long id) {
        return repo.findByIdWithItems(id)
                .orElseThrow(() -> new RuntimeException("Orden de compra no encontrada: " + id));
    }

    public OrdenCompra guardar(OrdenCompraFormDto dto) {
        OrdenCompra oc = new OrdenCompra();
        mapearDto(oc, dto);
        em.flush();
        String tenantId = TenantContext.getCurrentTenant();
        Integer max = repo.findMaxNumeroOc(tenantId);
        oc.setNumeroOc(String.format("OC-%03d", (max == null ? 0 : max) + 1));
        OrdenCompra saved = repo.save(oc);
        log.info("OC creada: {} | proveedor={} | items={}", saved.getNumeroOc(), saved.getProveedor(), saved.getItems().size());
        return saved;
    }

    public OrdenCompra actualizar(Long id, OrdenCompraFormDto dto) {
        OrdenCompra oc = buscarPorId(id);
        if (!oc.isEditable()) {
            throw new RuntimeException("Solo se pueden editar órdenes en estado BORRADOR");
        }
        oc.getItems().clear();
        em.flush();
        mapearDto(oc, dto);
        OrdenCompra saved = repo.save(oc);
        log.info("OC actualizada: {}", saved.getNumeroOc());
        return saved;
    }

    public void eliminar(Long id) {
        OrdenCompra oc = buscarPorId(id);
        if (oc.getEstado() != EstadoOrdenCompra.BORRADOR && oc.getEstado() != EstadoOrdenCompra.CANCELADA) {
            throw new RuntimeException("Solo se pueden eliminar órdenes en estado BORRADOR o CANCELADA");
        }
        repo.deleteById(id);
        log.info("OC eliminada: {}", oc.getNumeroOc());
    }

    public OrdenCompra cambiarEstado(Long id, EstadoOrdenCompra nuevoEstado) {
        OrdenCompra oc = buscarPorId(id);
        Set<EstadoOrdenCompra> validas = TRANSICIONES.getOrDefault(oc.getEstado(), EnumSet.noneOf(EstadoOrdenCompra.class));
        if (!validas.contains(nuevoEstado)) {
            throw new RuntimeException("Transición inválida: " + oc.getEstado() + " → " + nuevoEstado);
        }
        oc.setEstado(nuevoEstado);
        return repo.save(oc);
    }

    public Long convertirAFactura(Long id, FacturaProveedorService facturaService) {
        OrdenCompra oc = buscarPorId(id);
        if (!oc.isConvertible()) {
            throw new RuntimeException("La OC no está en estado RECIBIDA o ya fue convertida");
        }
        Long facturaId = facturaService.crearDesdeOrdenCompra(oc);
        oc.setFacturaId(facturaId);
        repo.save(oc);
        log.info("OC {} convertida a factura id={}", oc.getNumeroOc(), facturaId);
        return facturaId;
    }

    @Transactional(readOnly = true)
    public long countTotal() { return repo.count(); }

    @Transactional(readOnly = true)
    public long countByEstado(EstadoOrdenCompra estado) { return repo.countByEstado(estado); }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> suggest(String q) {
        if (q == null || q.trim().length() < 2) return List.of();
        return repo.search(q.trim(), PageRequest.of(0, 6)).stream()
                .map(oc -> Map.<String, Object>of(
                        "titulo",    oc.getNumeroOc() != null ? oc.getNumeroOc() : "OC sin número",
                        "sub",       oc.getProveedor() != null ? oc.getProveedor() : "",
                        "estado",    oc.getEstado().getDisplayName(),
                        "url",       "/ordenes-compra/ver/" + oc.getId()
                ))
                .toList();
    }

    public List<EstadoOrdenCompra> transicionesValidas(EstadoOrdenCompra actual) {
        return List.copyOf(TRANSICIONES.getOrDefault(actual, EnumSet.noneOf(EstadoOrdenCompra.class)));
    }

    private void mapearDto(OrdenCompra oc, OrdenCompraFormDto dto) {
        oc.setFechaEmision(dto.getFechaEmision());
        oc.setFechaRequerida(dto.getFechaRequerida());
        oc.setNotas(blank(dto.getNotas()));

        if (dto.getProveedorId() != null) {
            proveedorRepo.findById(dto.getProveedorId()).ifPresent(p -> {
                oc.setProveedorRef(p);
                oc.setProveedor(p.getNombre());
            });
        } else {
            oc.setProveedorRef(null);
            oc.setProveedor(blank(dto.getProveedor()));
        }

        if (dto.getItems() != null) {
            for (OrdenCompraItemDto itemDto : dto.getItems()) {
                if (itemDto.getNombre() == null || itemDto.getNombre().isBlank()) continue;
                OrdenCompraItem item = new OrdenCompraItem();
                item.setOrden(oc);
                item.setTipoItem(itemDto.getTipoItem());
                item.setNombre(itemDto.getNombre().trim());
                item.setDescripcion(blank(itemDto.getDescripcion()));
                item.setCantidad(itemDto.getCantidad() != null ? itemDto.getCantidad() : java.math.BigDecimal.ONE);
                item.setUnidad(blank(itemDto.getUnidad()));
                item.setPrecioUnitarioEstimado(itemDto.getPrecioUnitarioEstimado());
                item.setPorcentajeIvaItem(itemDto.getPorcentajeIvaItem() != null ? itemDto.getPorcentajeIvaItem() : java.math.BigDecimal.ZERO);
                item.setTipoInsumo(blank(itemDto.getTipoInsumo()));
                item.setTipoEquipo(blank(itemDto.getTipoEquipo()));
                oc.getItems().add(item);
            }
        }
    }

    private String blank(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private String usuarioActual() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            return (auth != null && auth.isAuthenticated()) ? auth.getName() : "sistema";
        } catch (Exception e) {
            return "sistema";
        }
    }
}
