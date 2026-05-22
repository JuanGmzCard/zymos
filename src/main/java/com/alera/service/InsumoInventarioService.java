package com.alera.service;

import com.alera.model.InsumoInventario;
import com.alera.model.enums.TipoInsumo;
import com.alera.repository.InsumoInventarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    private final InsumoInventarioRepository repo;

    public InsumoInventarioService(InsumoInventarioRepository repo) {
        this.repo = repo;
    }

    public static final int PAGE_SIZE = 15;

    public List<InsumoInventario> listarTodos() {
        return repo.findAllByOrderByNombreAsc();
    }

    // Fix 5+6: paginación + filtros
    public Page<InsumoInventario> listarPaginado(String nombre, TipoInsumo tipo, int page) {
        // Pasar "" en vez de null para evitar inferencia de tipo bytea en PostgreSQL
        String nombreParam = (nombre != null && !nombre.isBlank()) ? nombre.trim() : "";
        return repo.findByFiltros(nombreParam, tipo, PageRequest.of(page, PAGE_SIZE));
    }

    public Optional<InsumoInventario> buscarPorId(Long id) {
        return repo.findById(id);
    }

    public Optional<InsumoInventario> buscarPorNombreExacto(String nombre) {
        return repo.findByNombreExacto(nombre);
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

    /**
     * Descuenta del inventario. Retorna el nombre del insumo si el stock era insuficiente,
     * o null si había suficiente stock (o el insumo no existe en inventario).
     */
    public String descontarIngrediente(String nombre, String cantidadTexto) {
        if (nombre == null || nombre.isBlank()) return null;
        BigDecimal cantidad = parsearCantidad(cantidadTexto);
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) return null;

        Optional<InsumoInventario> opt = repo.findByNombreExacto(nombre);
        if (opt.isEmpty()) return null; // no registrado en inventario → no hay que descontar

        InsumoInventario insumo = opt.get();
        BigDecimal disponible = insumo.getCantidad();
        boolean insuficiente = disponible.compareTo(cantidad) < 0;

        insumo.setCantidad(disponible.subtract(cantidad).max(BigDecimal.ZERO));
        repo.save(insumo);

        if (insuficiente) {
            log.warn("Stock insuficiente para '{}': disponible={} requerido={} — se dejó en 0",
                    nombre, disponible, cantidad);
        } else {
            log.debug("Descuento inventario '{}': -{} | nuevo stock={}", nombre, cantidad, insumo.getCantidad());
        }
        return insuficiente ? nombre : null;
    }

    public void restaurarIngrediente(String nombre, String cantidadTexto) {
        if (nombre == null || nombre.isBlank()) return;
        BigDecimal cantidad = parsearCantidad(cantidadTexto);
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) return;
        repo.findByNombreExacto(nombre).ifPresent(insumo -> {
            insumo.setCantidad(insumo.getCantidad().add(cantidad));
            repo.save(insumo);
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

    public TipoInsumo detectarTipo(String nombre) {
        if (nombre == null) return TipoInsumo.OTRO;
        String n = nombre.toLowerCase();
        if (n.contains("malta") || n.contains("pilsner") || n.contains("malt")) return TipoInsumo.MALTA;
        if (n.contains("lupulo") || n.contains("lúpulo") || n.contains("hop"))  return TipoInsumo.LUPULO;
        if (n.contains("levadura") || n.contains("yeast"))                       return TipoInsumo.LEVADURA;
        if (n.contains("clarific") || n.contains("gelatin") || n.contains("irish")) return TipoInsumo.CLARIFICANTE;
        if (n.contains("envase") || n.contains("botell") || n.contains("lata"))  return TipoInsumo.ENVASE;
        return TipoInsumo.OTRO;
    }
}
