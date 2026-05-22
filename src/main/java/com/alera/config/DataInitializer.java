package com.alera.config;

import com.alera.model.Tenant;
import com.alera.model.TipoCerveza;
import com.alera.model.Usuario;
import com.alera.model.enums.RolUsuario;
import com.alera.repository.TenantRepository;
import com.alera.repository.TipoCervezaRepository;
import com.alera.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository usuarioRepo;
    private final TipoCervezaRepository tipoCervezaRepo;
    private final TenantRepository tenantRepo;
    private final BrandingProperties branding;
    private final PasswordEncoder encoder;

    @Value("${app.default-subdomain:default}")
    private String defaultSubdomain;

    @Value("${ADMIN_USERNAME:admin}")        private String adminUsername;
    @Value("${ADMIN_PASSWORD:alera2024}")    private String adminPassword;
    @Value("${INVENTARIO_USERNAME:}")        private String inventarioUsername;
    @Value("${INVENTARIO_PASSWORD:}")        private String inventarioPassword;
    @Value("${FACTURACION_USERNAME:}")       private String facturacionUsername;
    @Value("${FACTURACION_PASSWORD:}")       private String facturacionPassword;
    @Value("${EQUIPOS_USERNAME:}")           private String equiposUsername;
    @Value("${EQUIPOS_PASSWORD:}")           private String equiposPassword;

    public DataInitializer(UsuarioRepository usuarioRepo,
                            TipoCervezaRepository tipoCervezaRepo,
                            TenantRepository tenantRepo,
                            BrandingProperties branding,
                            PasswordEncoder encoder) {
        this.usuarioRepo     = usuarioRepo;
        this.tipoCervezaRepo = tipoCervezaRepo;
        this.tenantRepo      = tenantRepo;
        this.branding        = branding;
        this.encoder         = encoder;
    }

    @Override
    public void run(String... args) {
        crearTenantDefault();

        // Todas las operaciones siguientes deben correr con el tenant activo
        TenantContext.setCurrentTenant(defaultSubdomain);
        try {
            crearUsuarios();
            crearTiposCerveza();
        } finally {
            TenantContext.clear();
        }
    }

    private void crearTenantDefault() {
        if (tenantRepo.existsById(defaultSubdomain)) return;

        Tenant t = new Tenant();
        t.setSubdomain(defaultSubdomain);
        t.setName(branding.getName());
        t.setTagline(branding.getTagline());
        t.setLogoUrl(branding.getLogoUrl());
        t.setColorNavbar(branding.getColorNavbar());
        t.setColorPrimary(branding.getColorPrimary());
        t.setColorAccent(branding.getColorAccent());
        t.setColorAccentHover(branding.getColorAccentHover());
        t.setColorCream(branding.getColorCream());
        t.setColorBodyBg(branding.getColorBodyBg());
        t.setActive(true);
        tenantRepo.save(t);
        log.info("Tenant '{}' creado con branding '{}'", defaultSubdomain, branding.getName());
    }

    private void crearUsuarios() {
        crearUsuarioSiNoExiste(adminUsername,       adminPassword,       RolUsuario.ADMIN);
        crearUsuarioSiNoExiste(inventarioUsername,  inventarioPassword,  RolUsuario.INVENTARIO);
        crearUsuarioSiNoExiste(facturacionUsername, facturacionPassword, RolUsuario.FACTURACION);
        crearUsuarioSiNoExiste(equiposUsername,     equiposPassword,     RolUsuario.EQUIPOS);
    }

    private void crearUsuarioSiNoExiste(String username, String password, RolUsuario rol) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) return;
        if (!usuarioRepo.existsByUsername(username)) {
            Usuario u = new Usuario();
            u.setUsername(username);
            u.setPassword(encoder.encode(password));
            u.setRol(rol);
            usuarioRepo.save(u);
            log.info("Usuario '{}' ({}) creado en tenant '{}'", username, rol, defaultSubdomain);
        }
    }

    private void crearTiposCerveza() {
        String[] tipos = {"APA", "IPA", "Stout", "Helles", "Porter", "Wheat", "Saison", "Lager", "Otro"};
        int creados = 0;
        for (String nombre : tipos) {
            if (!tipoCervezaRepo.existsByNombreIgnoreCase(nombre)) {
                TipoCerveza t = new TipoCerveza();
                t.setNombre(nombre);
                t.setActivo(true);
                tipoCervezaRepo.save(t);
                creados++;
            }
        }
        if (creados > 0) log.info("{} tipos de cerveza inicializados en tenant '{}'", creados, defaultSubdomain);
    }
}
