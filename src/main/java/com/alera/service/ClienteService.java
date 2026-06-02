package com.alera.service;

import com.alera.dto.ClienteFormDto;
import com.alera.model.Cliente;
import com.alera.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class ClienteService {

    private final ClienteRepository repo;

    @Value("${app.page-size:15}")
    private int pageSize;

    public ClienteService(ClienteRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<Cliente> listarActivos() {
        return repo.findAllByActivoTrueOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public List<Cliente> listarTodos() {
        return repo.findAllByOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public Page<Cliente> listarPaginado(String nombre, Boolean activo, int page) {
        String q = (nombre != null && !nombre.isBlank()) ? nombre.trim() : "";
        return repo.findAllFiltered(q, activo, PageRequest.of(page, pageSize));
    }

    @Transactional(readOnly = true)
    public Optional<Cliente> buscarPorId(Long id) {
        return repo.findById(id);
    }

    public Cliente guardar(ClienteFormDto dto) {
        if (dto.getNit() != null && !dto.getNit().isBlank()) {
            repo.findByNit(dto.getNit().trim()).ifPresent(existente -> {
                if (!existente.getId().equals(dto.getId())) {
                    throw new RuntimeException("Ya existe un cliente con el NIT " + dto.getNit());
                }
            });
        }
        Cliente c = new Cliente();
        mapearDto(dto, c);
        return repo.save(c);
    }

    public Cliente actualizar(Long id, ClienteFormDto dto) {
        Cliente c = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado: " + id));
        if (dto.getNit() != null && !dto.getNit().isBlank()) {
            repo.findByNit(dto.getNit().trim()).ifPresent(existente -> {
                if (!existente.getId().equals(id)) {
                    throw new RuntimeException("Ya existe un cliente con el NIT " + dto.getNit());
                }
            });
        }
        mapearDto(dto, c);
        return repo.save(c);
    }

    public void toggleActivo(Long id) {
        repo.findById(id).ifPresent(c -> {
            c.setActivo(!c.isActivo());
            repo.save(c);
        });
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> suggest(String q) {
        if (q == null || q.trim().length() < 1) return List.of();
        return repo.searchActivos(q.trim(), PageRequest.of(0, 8)).stream()
                .limit(6)
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",          c.getId());
                    m.put("nombre",      c.getNombre());
                    m.put("nit",         c.getNit() != null ? c.getNit() : "");
                    m.put("listaPrecio", c.getListaPrecio() != null ? c.getListaPrecio().getDisplayName() : "");
                    m.put("ciudad",      c.getCiudad() != null ? c.getCiudad() : "");
                    return m;
                }).toList();
    }

    private void mapearDto(ClienteFormDto dto, Cliente c) {
        c.setNombre(dto.getNombre().trim());
        c.setRazonSocial(blank(dto.getRazonSocial()));
        c.setNit(blank(dto.getNit()));
        c.setRegimenTributario(dto.getRegimenTributario());
        c.setEmail(blank(dto.getEmail()));
        c.setTelefono(blank(dto.getTelefono()));
        c.setDireccionDespacho(blank(dto.getDireccionDespacho()));
        c.setCiudad(blank(dto.getCiudad()));
        c.setDepartamento(blank(dto.getDepartamento()));
        c.setListaPrecio(dto.getListaPrecio());
        c.setActivo(dto.isActivo());
        c.setNotas(blank(dto.getNotas()));
    }

    private String blank(String s) {
        return (s != null && !s.isBlank()) ? s.trim() : null;
    }
}
