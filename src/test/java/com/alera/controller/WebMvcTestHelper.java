package com.alera.controller;

import com.alera.config.ZymosAccessDeniedHandler;
import com.alera.config.ZymosAuthFailureHandler;
import com.alera.config.ZymosAuthSuccessHandler;
import com.alera.model.Tenant;
import com.alera.repository.TenantRepository;
import org.mockito.Mockito;

import java.util.Optional;

/**
 * Utilidades comunes para tests @WebMvcTest.
 *
 * SecurityConfig.filterChain() necesita 6 dependencias que NO se crean automáticamente
 * en @WebMvcTest. Hay que @MockBean todas:
 *   - TenantRepository             → TenantFilter lo inyecta
 *   - ZymosAuthSuccessHandler      → filterChain lo recibe como parámetro
 *   - ZymosAuthFailureHandler      → ídem
 *   - ZymosAccessDeniedHandler     → ídem
 *   - UsuarioService               → DaoAuthenticationProvider + auth handlers
 *   - BrandingProperties           → GlobalControllerAdvice lo inyecta
 *   - LogAccesoService             → auth handlers lo inyectan
 *
 * Sin estos mocks, Spring usa la configuración de seguridad por defecto
 * (sin URL-based restrictions, sin form-login redirect configurado → 401 en lugar de 302).
 */
class WebMvcTestHelper {

    /**
     * Configura el mock de TenantRepository para que TenantFilter resuelva el tenant "default"
     * con todos los colores del branding válidos (necesarios para el template del navbar).
     */
    static void configureTenantMock(TenantRepository tenantRepo) {
        Tenant t = new Tenant();
        t.setSubdomain("default");
        t.setName("Alera Test");
        t.setTagline("Test");
        t.setActive(true);
        t.setColorNavbar("#242E0D");
        t.setColorPrimary("#364318");
        t.setColorAccent("#C9A028");
        t.setColorAccentHover("#E0B840");
        t.setColorCream("#F5EDD0");
        t.setColorBodyBg("#F0EDE2");
        Mockito.when(tenantRepo.findBySubdomainAndActiveTrue(Mockito.anyString()))
               .thenReturn(Optional.of(t));
    }
}
