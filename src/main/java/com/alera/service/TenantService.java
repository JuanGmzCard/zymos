package com.alera.service;

import com.alera.config.TenantFilter;
import com.alera.model.Tenant;
import com.alera.repository.TenantRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TenantService {

    private final TenantRepository repo;
    private final TenantFilter tenantFilter;

    public TenantService(TenantRepository repo, TenantFilter tenantFilter) {
        this.repo = repo;
        this.tenantFilter = tenantFilter;
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
        Tenant saved = repo.save(tenant);
        tenantFilter.evictCache(tenant.getSubdomain());
        return saved;
    }

    public void toggleActivo(String subdomain) {
        repo.findById(subdomain).ifPresent(t -> {
            t.setActive(!t.isActive());
            repo.save(t);
            tenantFilter.evictCache(subdomain);
        });
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