package com.alera.service;

import com.alera.model.InsumoInventario;
import com.alera.model.MovimientoInventario;
import com.alera.model.enums.TipoMovimiento;
import com.alera.repository.InsumoInventarioRepository;
import com.alera.repository.MovimientoInventarioRepository;
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
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class InsumoInventarioService {

    private static final Logger log = LoggerFactory.getLogger(InsumoInventarioService.class);

    @Value("${app.page-size:15}")
    private int pageSize;

    private final InsumoInventarioRepository repo;
    private final MovimientoInventarioRepository movimientoRepo;

    public InsumoInventarioService(InsumoInventarioRepository repo,
                                   MovimientoInventarioRepository movimientoRepo) {
        this.repo          = repo;
        this.movimientoRepo = movimientoRepo;
    }

    public static final int PAGE_SIZE = 15;

    public List<InsumoInventario> listarTodos() {
        return repo.findAllByOrderByNombreAsc();
    }

    public Page<InsumoInventario> listarPaginado(String nombre, String tipo, int page) {
        String nombreParam = (nombre != null && !nombre.isBlank()) ? nombre.trim() : "";
        String tipoParam = (tipo != null && !tipo.isBlank()) ? tipo : null;
        return repo.findByFiltros(nombreParam, tipoParam, PageRequest.of(page, pageSize));
    }

    public Optional<InsumoInventario> buscarPorId(Long id) {
        return repo.findById(id);
    }

    public Optional<InsumoInventario> buscarPorNombreExacto(String nombre) {
        return findUnico(nombre);
    }

    private Optional<InsumoInventario> findUnico(String nombre) {
        List<InsumoInventario> lista = repo.findByNombreExacto(nombre);
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    public InsumoInventario guardar(InsumoInventario insumo) {
        return repo.save(insumo);
    }

    public void eliminar(Long id) {
        repo.deleteById(id);
    }

    public List<InsumoInventario> listarBajoStock() {
        return repo.findBajoStock();
    }

    public List<InsumoInventario> listarProximosAVencer(int dias) {
        return repo.findProximosAVencer(LocalDate.now().plusDays(dias));
    }

    public Page<MovimientoInventario> listarMovimientos(Long insumoId, int page) {
        return movimientoRepo.findByInsumoIdOrderByFechaDesc(insumoId, PageRequest.of(page, pageSize));
    }

    public void ajustar(Long id, TipoMovimiento tipo, BigDecimal cantidad, String motivo) {
        InsumoInventario insumo = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Insumo no encontrado: " + id));
        BigDecimal anterior = insumo.getCantidad();
        BigDecimal posterior;
        if (tipo == TipoMovimiento.ENTRADA) {
            posterior = anterior.add(cantidad);
        } else if (tipo == TipoMovimiento.SALIDA) {
            posterior = anterior.subtract(cantidad).max(BigDecimal.ZERO);
        } else {
            posterior = cantidad;
        }
        insumo.setCantidad(posterior);
        repo.save(insumo);
        registrarMovimiento(insumo.getId(), insumo.getNombre(), tipo,
                cantidad, anterior, posterior, motivo, null);
        log.info("Ajuste de stock '{}': {} {} → {} {}", insumo.getNombre(),
                tipo, cantidad, posterior, insumo.getUnidad());
    }

    /**
     * Descuenta del inventario. Retorna el nombre del insumo si el stock era insuficiente,
     * o null si había suficiente stock (o el insumo no existe en inventario).
     */
    public String descontarIngrediente(String nombre, String cantidadTexto) {
        return descontarIngrediente(nombre, cantidadTexto, null);
    }

    public String descontarIngrediente(String nombre, String cantidadTexto, String referencia) {
        if (nombre == null || nombre.isBlank()) return null;
        BigDecimal cantidad = parsearCantidad(cantidadTexto);
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) return null;

        Optional<InsumoInventario> opt = findUnico(nombre);
        if (opt.isEmpty()) return null;

        InsumoInventario insumo = opt.get();
        BigDecimal disponible = insumo.getCantidad();
        boolean insuficiente  = disponible.compareTo(cantidad) < 0;

        BigDecimal posterior = disponible.subtract(cantidad).max(BigDecimal.ZERO);
        insumo.setCantidad(posterior);
        repo.save(insumo);

        registrarMovimiento(insumo.getId(), insumo.getNombre(), TipoMovimiento.DESCUENTO_LOTE,
                insuficiente ? disponible : cantidad, disponible, posterior,
                insuficiente ? "Stock insuficiente — se dejó en 0" : null, referencia);

        if (insuficiente) {
            log.warn("Stock insuficiente para '{}': disponible={} requerido={} — se dejó en 0",
                    nombre, disponible, cantidad);
        } else {
            log.debug("Descuento inventario '{}': -{} | nuevo stock={}", nombre, cantidad, posterior);
        }
        return insuficiente ? nombre : null;
    }

    public void restaurarIngrediente(String nombre, String cantidadTexto) {
        restaurarIngrediente(nombre, cantidadTexto, null);
    }

    public void restaurarIngrediente(String nombre, String cantidadTexto, String referencia) {
        if (nombre == null || nombre.isBlank()) return;
        BigDecimal cantidad = parsearCantidad(cantidadTexto);
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) return;
        findUnico(nombre).ifPresent(insumo -> {
            BigDecimal anterior  = insumo.getCantidad();
            BigDecimal posterior = anterior.add(cantidad);
            insumo.setCantidad(posterior);
            repo.save(insumo);
            registrarMovimiento(insumo.getId(), insumo.getNombre(), TipoMovimiento.RESTAURACION_LOTE,
                    cantidad, anterior, posterior, null, referencia);
        });
    }

    public void ingresarDeFactura(String nombre, BigDecimal cantidad, String unidad,
                                  String tipoInsumo, String proveedor, String referencia) {
        if (nombre == null || nombre.isBlank() || cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) return;
        InsumoInventario insumo = findUnico(nombre).orElseGet(() -> {
            InsumoInventario nuevo = new InsumoInventario();
            nuevo.setNombre(nombre);
            nuevo.setTipo(tipoInsumo != null ? tipoInsumo : detectarTipo(nombre));
            nuevo.setCantidad(BigDecimal.ZERO);
            nuevo.setUnidad(unidad != null ? unidad : "und");
            nuevo.setStockMinimo(BigDecimal.ZERO);
            nuevo.setProveedor(proveedor);
            return repo.save(nuevo);
        });
        BigDecimal anterior = insumo.getCantidad();
        BigDecimal posterior = anterior.add(cantidad);
        insumo.setCantidad(posterior);
        repo.save(insumo);
        registrarMovimiento(insumo.getId(), insumo.getNombre(), TipoMovimiento.INGRESO_FACTURA,
                cantidad, anterior, posterior, null, referencia);
        log.info("Ingreso factura '{}': +{} {} | nuevo stock={}", nombre, cantidad, insumo.getUnidad(), posterior);
    }

    public void revertirEntradaFactura(String nombre, BigDecimal cantidad, String referencia) {
        if (nombre == null || nombre.isBlank() || cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) return;
        findUnico(nombre).ifPresent(insumo -> {
            BigDecimal anterior = insumo.getCantidad();
            BigDecimal posterior = anterior.subtract(cantidad).max(BigDecimal.ZERO);
            insumo.setCantidad(posterior);
            repo.save(insumo);
            registrarMovimiento(insumo.getId(), insumo.getNombre(), TipoMovimiento.REVERSION_FACTURA,
                    cantidad, anterior, posterior, null, referencia);
            log.info("Reversión factura '{}': -{} | nuevo stock={}", nombre, cantidad, posterior);
        });
    }

    public BigDecimal parsearCantidad(String texto) {
        if (texto == null || texto.isBlank()) return BigDecimal.ZERO;
        try {
            String numero = texto.trim().split("\\s+")[0].replace(",", ".");
            return new BigDecimal(numero);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    public String detectarTipo(String nombre) {
        if (nombre == null) return "Otro";
        String n = nombre.toLowerCase();
        if (n.contains("malta") || n.contains("pilsner") || n.contains("malt")) return "Malta";
        if (n.contains("lupulo") || n.contains("lúpulo") || n.contains("hop"))  return "Lúpulo";
        if (n.contains("levadura") || n.contains("yeast"))                       return "Levadura";
        if (n.contains("clarific") || n.contains("gelatin") || n.contains("irish")) return "Clarificante";
        if (n.contains("dextrosa") || n.contains("sacarosa") || n.contains("priming")
                || n.contains("carbonat") || n.contains("extracto de malta"))     return "Agente de Carbonatación";
        if (n.contains("envase") || n.contains("botell") || n.contains("lata"))  return "Envase";
        return "Otro";
    }

    private void registrarMovimiento(Long insumoId, String nombre, TipoMovimiento tipo,
                                     BigDecimal cantidad, BigDecimal anterior,
                                     BigDecimal posterior, String motivo, String referencia) {
        movimientoRepo.save(MovimientoInventario.of(
                insumoId, nombre, tipo, cantidad, anterior, posterior, motivo, referencia, currentUser()));
    }

    private String currentUser() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            return auth != null ? auth.getName() : "sistema";
        } catch (Exception e) {
            return "sistema";
        }
    }
}
