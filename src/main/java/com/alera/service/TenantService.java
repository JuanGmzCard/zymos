package com.alera.service;

import com.alera.config.TenantFilter;
import com.alera.model.HistorialTenant;
import com.alera.model.Tenant;
import com.alera.repository.HistorialTenantRepository;
import com.alera.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TenantService {

    @Value("${app.page-size:15}")
    private int pageSize;

    private final TenantRepository repo;
    private final TenantFilter tenantFilter;
    private final HistorialTenantRepository historialRepo;

    public TenantService(TenantRepository repo, TenantFilter tenantFilter,
                          HistorialTenantRepository historialRepo) {
        this.repo         = repo;
        this.tenantFilter = tenantFilter;
        this.historialRepo = historialRepo;
    }

    @Transactional(readOnly = true)
    public List<Tenant> listarTodos() {
        return repo.findAll(Sort.by("subdomain"));
    }

    @Transactional(readOnly = true)
    public Optional<Tenant> buscarPorSubdomain(String subdomain) {
        return repo.findById(subdomain);
    }

    public Tenant guardar(Tenant tenant) {
        boolean esNuevo = !repo.existsById(tenant.getSubdomain());
        Tenant saved = repo.save(tenant);
        tenantFilter.evictCache(tenant.getSubdomain());
        historialRepo.save(HistorialTenant.of(
                tenant.getSubdomain(), esNuevo ? "CREADO" : "EDITADO", usuarioActual(), null));
        return saved;
    }

    public void evictAllCache() {
        tenantFilter.evictAll();
    }

    public void toggleActivo(String subdomain) {
        repo.findById(subdomain).ifPresent(t -> {
            t.setActive(!t.isActive());
            repo.save(t);
            tenantFilter.evictCache(subdomain);
            historialRepo.save(HistorialTenant.of(
                    subdomain, t.isActive() ? "ACTIVADO" : "DESACTIVADO", usuarioActual(), null));
        });
    }

    @Transactional(readOnly = true)
    public List<HistorialTenant> listarHistorial(String subdomain) {
        return historialRepo.findBySubdomainOrderByFechaDesc(subdomain);
    }

    @Transactional(readOnly = true)
    public Page<HistorialTenant> listarHistorialPaginado(String subdomain, int page) {
        return historialRepo.findBySubdomainOrderByFechaDesc(
                subdomain, PageRequest.of(page, pageSize));
    }

    public void registrarAccion(String subdomain, String accion, String detalles) {
        historialRepo.save(HistorialTenant.of(subdomain, accion, usuarioActual(), detalles));
    }

    private String usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "sistema";
    }

    public void registrarEnvioExitoso(String subdomain) {
        repo.findById(subdomain).ifPresent(t -> {
            t.setAlertasIntentosFallidos(0);
            t.setAlertasUltimoIntento(LocalDateTime.now());
            t.setAlertasUltimoExito(LocalDateTime.now());
            repo.save(t);
        });
    }

    public void registrarEnvioFallido(String subdomain) {
        repo.findById(subdomain).ifPresent(t -> {
            t.setAlertasIntentosFallidos(t.getAlertasIntentosFallidos() + 1);
            t.setAlertasUltimoIntento(LocalDateTime.now());
            repo.save(t);
        });
    }
}