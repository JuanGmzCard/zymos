package com.alera.service;

import com.alera.config.TenantFilter;
import com.alera.model.HistorialTenant;
import com.alera.model.Tenant;
import com.alera.repository.HistorialTenantRepository;
import com.alera.repository.TenantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantService")
class TenantServiceTest {

    @Mock TenantRepository           repo;
    @Mock TenantFilter               tenantFilter;
    @Mock HistorialTenantRepository  historialRepo;

    @InjectMocks
    TenantService service;

    @AfterEach
    void limpiarSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private Tenant tenant(String subdomain, boolean active) {
        Tenant t = new Tenant();
        t.setSubdomain(subdomain);
        t.setName("Tenant " + subdomain);
        t.setActive(active);
        t.setColorNavbar("#242E0D");
        t.setColorPrimary("#364318");
        t.setColorAccent("#C9A028");
        t.setColorAccentHover("#E0B840");
        t.setColorCream("#F5EDD0");
        t.setColorBodyBg("#F0EDE2");
        return t;
    }

    private void autenticarComo(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, List.of()));
    }

    // ── listarTodos ───────────────────────────────────────────────────

    @Test
    @DisplayName("listarTodos delega al repositorio ordenado por subdomain")
    void listarTodos_delegaConOrden() {
        Tenant a = tenant("alfa", true);
        Tenant b = tenant("beta", true);
        when(repo.findAll(Sort.by("subdomain"))).thenReturn(List.of(a, b));

        List<Tenant> resultado = service.listarTodos();

        assertThat(resultado).hasSize(2);
        verify(repo).findAll(Sort.by("subdomain"));
    }

    // ── buscarPorSubdomain ────────────────────────────────────────────

    @Test
    @DisplayName("buscarPorSubdomain retorna el tenant cuando existe")
    void buscarPorSubdomain_existe_retornaTenant() {
        when(repo.findById("mosto")).thenReturn(Optional.of(tenant("mosto", true)));

        Optional<Tenant> resultado = service.buscarPorSubdomain("mosto");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getSubdomain()).isEqualTo("mosto");
    }

    @Test
    @DisplayName("buscarPorSubdomain retorna vacío cuando no existe")
    void buscarPorSubdomain_noExiste_retornaVacio() {
        when(repo.findById("fantasma")).thenReturn(Optional.empty());

        assertThat(service.buscarPorSubdomain("fantasma")).isEmpty();
    }

    // ── guardar ───────────────────────────────────────────────────────

    @Test
    @DisplayName("guardar registra CREADO en historial para un tenant nuevo")
    void guardar_tenantNuevo_registraCREADO() {
        autenticarComo("admin");
        Tenant t = tenant("nuevo", true);
        when(repo.existsById("nuevo")).thenReturn(false);
        when(repo.save(t)).thenReturn(t);

        service.guardar(t);

        ArgumentCaptor<HistorialTenant> captor = ArgumentCaptor.forClass(HistorialTenant.class);
        verify(historialRepo).save(captor.capture());
        assertThat(captor.getValue().getAccion()).isEqualTo("CREADO");
        assertThat(captor.getValue().getSubdomain()).isEqualTo("nuevo");
        assertThat(captor.getValue().getUsuario()).isEqualTo("admin");
    }

    @Test
    @DisplayName("guardar registra EDITADO en historial para un tenant existente")
    void guardar_tenantExistente_registraEDITADO() {
        autenticarComo("admin");
        Tenant t = tenant("mosto", true);
        when(repo.existsById("mosto")).thenReturn(true);
        when(repo.save(t)).thenReturn(t);

        service.guardar(t);

        ArgumentCaptor<HistorialTenant> captor = ArgumentCaptor.forClass(HistorialTenant.class);
        verify(historialRepo).save(captor.capture());
        assertThat(captor.getValue().getAccion()).isEqualTo("EDITADO");
    }

    @Test
    @DisplayName("guardar evicta el cache del tenant guardado")
    void guardar_evictaCacheDelTenant() {
        Tenant t = tenant("mosto", true);
        when(repo.existsById("mosto")).thenReturn(true);
        when(repo.save(t)).thenReturn(t);

        service.guardar(t);

        verify(tenantFilter).evictCache("mosto");
    }

    @Test
    @DisplayName("guardar retorna el tenant guardado")
    void guardar_retornaTenantGuardado() {
        Tenant t = tenant("alfa", true);
        when(repo.existsById("alfa")).thenReturn(false);
        when(repo.save(t)).thenReturn(t);

        Tenant resultado = service.guardar(t);

        assertThat(resultado.getSubdomain()).isEqualTo("alfa");
    }

    // ── evictAllCache ─────────────────────────────────────────────────

    @Test
    @DisplayName("evictAllCache delega a tenantFilter.evictAll()")
    void evictAllCache_delegaEvictAll() {
        service.evictAllCache();

        verify(tenantFilter).evictAll();
    }

    // ── toggleActivo ──────────────────────────────────────────────────

    @Test
    @DisplayName("toggleActivo desactiva un tenant activo y registra DESACTIVADO")
    void toggleActivo_activoQuedaDesactivado() {
        autenticarComo("admin");
        Tenant t = tenant("mosto", true);
        when(repo.findById("mosto")).thenReturn(Optional.of(t));

        service.toggleActivo("mosto");

        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(repo).save(tenantCaptor.capture());
        assertThat(tenantCaptor.getValue().isActive()).isFalse();

        ArgumentCaptor<HistorialTenant> histCaptor = ArgumentCaptor.forClass(HistorialTenant.class);
        verify(historialRepo).save(histCaptor.capture());
        assertThat(histCaptor.getValue().getAccion()).isEqualTo("DESACTIVADO");
    }

    @Test
    @DisplayName("toggleActivo activa un tenant inactivo y registra ACTIVADO")
    void toggleActivo_inactivoQuedaActivado() {
        autenticarComo("admin");
        Tenant t = tenant("mosto", false);
        when(repo.findById("mosto")).thenReturn(Optional.of(t));

        service.toggleActivo("mosto");

        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(repo).save(tenantCaptor.capture());
        assertThat(tenantCaptor.getValue().isActive()).isTrue();

        ArgumentCaptor<HistorialTenant> histCaptor = ArgumentCaptor.forClass(HistorialTenant.class);
        verify(historialRepo).save(histCaptor.capture());
        assertThat(histCaptor.getValue().getAccion()).isEqualTo("ACTIVADO");
    }

    @Test
    @DisplayName("toggleActivo evicta el cache del tenant")
    void toggleActivo_evictaCache() {
        Tenant t = tenant("mosto", true);
        when(repo.findById("mosto")).thenReturn(Optional.of(t));

        service.toggleActivo("mosto");

        verify(tenantFilter).evictCache("mosto");
    }

    @Test
    @DisplayName("toggleActivo no hace nada si el tenant no existe")
    void toggleActivo_noExiste_noHaceNada() {
        when(repo.findById("fantasma")).thenReturn(Optional.empty());

        service.toggleActivo("fantasma");

        verify(repo, never()).save(any());
        verify(tenantFilter, never()).evictCache(any());
        verify(historialRepo, never()).save(any());
    }

    // ── listarHistorial ───────────────────────────────────────────────

    @Test
    @DisplayName("listarHistorial delega al repositorio ordenado por fecha DESC")
    void listarHistorial_delegaAlRepositorio() {
        HistorialTenant h = HistorialTenant.of("mosto", "CREADO", "admin", null);
        when(historialRepo.findBySubdomainOrderByFechaDesc("mosto")).thenReturn(List.of(h));

        List<HistorialTenant> resultado = service.listarHistorial("mosto");

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getAccion()).isEqualTo("CREADO");
        verify(historialRepo).findBySubdomainOrderByFechaDesc("mosto");
    }

    // ── registrarAccion ───────────────────────────────────────────────

    @Test
    @DisplayName("registrarAccion guarda el historial con subdomain, accion y detalles correctos")
    void registrarAccion_guardaHistorial() {
        autenticarComo("admin");

        service.registrarAccion("mosto", "USUARIO_CREADO", "juan (ADMIN)");

        ArgumentCaptor<HistorialTenant> captor = ArgumentCaptor.forClass(HistorialTenant.class);
        verify(historialRepo).save(captor.capture());
        HistorialTenant h = captor.getValue();
        assertThat(h.getSubdomain()).isEqualTo("mosto");
        assertThat(h.getAccion()).isEqualTo("USUARIO_CREADO");
        assertThat(h.getDetalles()).isEqualTo("juan (ADMIN)");
        assertThat(h.getUsuario()).isEqualTo("admin");
        assertThat(h.getFecha()).isNotNull();
    }

    @Test
    @DisplayName("registrarAccion usa 'sistema' como usuario cuando no hay autenticación")
    void registrarAccion_sinAutenticacion_usaSistema() {
        // SecurityContext limpio — sin autenticación
        service.registrarAccion("default", "CREADO", null);

        ArgumentCaptor<HistorialTenant> captor = ArgumentCaptor.forClass(HistorialTenant.class);
        verify(historialRepo).save(captor.capture());
        assertThat(captor.getValue().getUsuario()).isEqualTo("sistema");
    }

    // ── registrarEnvioExitoso ─────────────────────────────────────────

    @Test
    @DisplayName("registrarEnvioExitoso resetea el contador y registra timestamps")
    void registrarEnvioExitoso_resetaContador() {
        Tenant t = tenant("mosto", true);
        t.setAlertasIntentosFallidos(3);
        when(repo.findById("mosto")).thenReturn(Optional.of(t));

        service.registrarEnvioExitoso("mosto");

        ArgumentCaptor<Tenant> captor = ArgumentCaptor.forClass(Tenant.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getAlertasIntentosFallidos()).isZero();
        assertThat(captor.getValue().getAlertasUltimoIntento()).isNotNull();
        assertThat(captor.getValue().getAlertasUltimoExito()).isNotNull();
    }

    @Test
    @DisplayName("registrarEnvioExitoso no hace nada si el tenant no existe")
    void registrarEnvioExitoso_noExiste_noHaceNada() {
        when(repo.findById("fantasma")).thenReturn(Optional.empty());

        service.registrarEnvioExitoso("fantasma");

        verify(repo, never()).save(any());
    }

    // ── registrarEnvioFallido ─────────────────────────────────────────

    @Test
    @DisplayName("registrarEnvioFallido incrementa el contador de fallos")
    void registrarEnvioFallido_incrementaContador() {
        Tenant t = tenant("mosto", true);
        t.setAlertasIntentosFallidos(2);
        when(repo.findById("mosto")).thenReturn(Optional.of(t));

        service.registrarEnvioFallido("mosto");

        ArgumentCaptor<Tenant> captor = ArgumentCaptor.forClass(Tenant.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getAlertasIntentosFallidos()).isEqualTo(3);
        assertThat(captor.getValue().getAlertasUltimoIntento()).isNotNull();
    }

    @Test
    @DisplayName("registrarEnvioFallido no hace nada si el tenant no existe")
    void registrarEnvioFallido_noExiste_noHaceNada() {
        when(repo.findById("fantasma")).thenReturn(Optional.empty());

        service.registrarEnvioFallido("fantasma");

        verify(repo, never()).save(any());
    }
}
