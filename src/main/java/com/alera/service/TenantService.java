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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
        Optional<Tenant> existente = repo.findById(tenant.getSubdomain());
        boolean esNuevo = existente.isEmpty();
        String detalles = esNuevo ? null : diffCambios(existente.get(), tenant);
        Tenant saved = repo.save(tenant);
        tenantFilter.evictCache(tenant.getSubdomain());
        historialRepo.save(HistorialTenant.of(
                tenant.getSubdomain(), esNuevo ? "CREADO" : "EDITADO", usuarioActual(), detalles));
        return saved;
    }

    private static final int MAX_DETALLES = 500;

    /** Compara los campos editables del formulario y devuelve un resumen "campo: antes → después", o null si no hubo cambios. */
    private String diffCambios(Tenant antes, Tenant despues) {
        List<String> cambios = new ArrayList<>();
        agregarCambio(cambios, "nombre", antes.getName(), despues.getName());
        agregarCambio(cambios, "tagline", antes.getTagline(), despues.getTagline());
        agregarCambio(cambios, "logoUrl", antes.getLogoUrl(), despues.getLogoUrl());
        agregarCambio(cambios, "emailAdmin", antes.getEmailAdmin(), despues.getEmailAdmin());
        agregarCambio(cambios, "colorNavbar", antes.getColorNavbar(), despues.getColorNavbar());
        agregarCambio(cambios, "colorPrimary", antes.getColorPrimary(), despues.getColorPrimary());
        agregarCambio(cambios, "colorAccent", antes.getColorAccent(), despues.getColorAccent());
        agregarCambio(cambios, "colorAccentHover", antes.getColorAccentHover(), despues.getColorAccentHover());
        agregarCambio(cambios, "colorCream", antes.getColorCream(), despues.getColorCream());
        agregarCambio(cambios, "colorBodyBg", antes.getColorBodyBg(), despues.getColorBodyBg());
        agregarCambio(cambios, "fontHeadings", antes.getFontHeadings(), despues.getFontHeadings());
        agregarCambio(cambios, "fontBody", antes.getFontBody(), despues.getFontBody());
        agregarCambio(cambios, "active", antes.isActive(), despues.isActive());
        agregarCambio(cambios, "maxLotes", antes.getMaxLotes(), despues.getMaxLotes());
        agregarCambio(cambios, "maxUsuarios", antes.getMaxUsuarios(), despues.getMaxUsuarios());
        agregarCambio(cambios, "planTipo", antes.getPlanTipo(), despues.getPlanTipo());
        agregarCambio(cambios, "planInicio", antes.getPlanInicio(), despues.getPlanInicio());
        agregarCambio(cambios, "planFin", antes.getPlanFin(), despues.getPlanFin());

        if (cambios.isEmpty()) return null;
        String resumen = String.join("; ", cambios);
        return resumen.length() > MAX_DETALLES ? resumen.substring(0, MAX_DETALLES - 3) + "..." : resumen;
    }

    private void agregarCambio(List<String> cambios, String campo, Object antes, Object despues) {
        if (!Objects.equals(antes, despues)) {
            cambios.add(campo + ": " + formatValor(antes) + " → " + formatValor(despues));
        }
    }

    private String formatValor(Object valor) {
        return valor == null ? "—" : valor.toString();
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