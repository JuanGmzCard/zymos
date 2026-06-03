package com.alera.service;

import com.alera.model.Proveedor;
import com.alera.repository.ProveedorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class ProveedorService {

    private static final Logger log = LoggerFactory.getLogger(ProveedorService.class);

    private final ProveedorRepository repo;

    public ProveedorService(ProveedorRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<Proveedor> listarActivos() {
        return repo.findAllByActivoTrueOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public List<Proveedor> listarTodos() {
        return repo.findAllByOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public Optional<Proveedor> buscarPorId(Long id) {
        return repo.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> suggest(String q) {
        if (q == null || q.isBlank() || q.trim().length() < 2) return List.of();
        return repo.search(q.trim(), PageRequest.of(0, 6)).stream()
            .map(p -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("nombre", p.getNombre());
                m.put("nit",    p.getNit() != null ? p.getNit() : "");
                m.put("activo", p.isActivo());
                m.put("url",    "/proveedores/editar/" + p.getId());
                return m;
            }).toList();
    }

    public Proveedor guardar(Proveedor proveedor) {
        Proveedor saved = repo.save(proveedor);
        log.info("Proveedor guardado: {}", saved.getNombre());
        return saved;
    }

    public void eliminar(Long id) {
        repo.deleteById(id);
        log.info("Proveedor eliminado: {}", id);
    }

    @Transactional(readOnly = true)
    public long contarFacturas(Long proveedorId) {
        return repo.countFacturas(proveedorId);
    }

    @Transactional(readOnly = true)
    public BigDecimal totalFacturas(Long proveedorId) {
        return repo.sumFacturas(proveedorId);
    }
}
