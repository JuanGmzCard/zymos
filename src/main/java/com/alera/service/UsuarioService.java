package com.alera.service;

import com.alera.config.TenantContext;
import com.alera.model.RolModuloPermiso;
import com.alera.model.SuperAdmin;
import com.alera.model.Usuario;
import com.alera.model.enums.RolUsuario;
import com.alera.repository.RolModuloPermisoRepository;
import com.alera.repository.RolTenantRepository;
import com.alera.repository.SuperAdminRepository;
import com.alera.repository.UsuarioRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class UsuarioService implements UserDetailsService {

    // Mapeo de enum RolUsuario → nombre del rol de sistema en roles_tenant.
    // Usado para auto-resolver rolCustomId en loadUserByUsername y para sincronizar en cambiarRol/guardar.
    private static final Map<RolUsuario, String> ENUM_TO_SISTEMA = Map.of(
        RolUsuario.ADMIN,       "Administrador",
        RolUsuario.PRODUCCION,  "Producción",
        RolUsuario.INVENTARIO,  "Inventario",
        RolUsuario.FACTURACION, "Facturación",
        RolUsuario.EQUIPOS,     "Equipos"
    );

    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;
    private final SuperAdminRepository superAdminRepo;
    private final RolModuloPermisoRepository permisoRepo;
    private final RolTenantRepository rolTenantRepo;

    public UsuarioService(UsuarioRepository repo, PasswordEncoder encoder,
                          SuperAdminRepository superAdminRepo,
                          RolModuloPermisoRepository permisoRepo,
                          RolTenantRepository rolTenantRepo) {
        this.repo = repo;
        this.encoder = encoder;
        this.superAdminRepo = superAdminRepo;
        this.permisoRepo = permisoRepo;
        this.rolTenantRepo = rolTenantRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Super-admins no tienen tenant — se buscan antes del filtro de tenant de Hibernate
        Optional<SuperAdmin> superAdmin = superAdminRepo.findByUsernameAndActivoTrue(username);
        if (superAdmin.isPresent()) {
            SuperAdmin sa = superAdmin.get();
            return new org.springframework.security.core.userdetails.User(
                    sa.getUsername(), sa.getPassword(),
                    List.of(new SimpleGrantedAuthority("ROLE_SUPERADMIN")));
        }
        Usuario u = repo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
        if (!u.isActivo()) throw new UsernameNotFoundException("Usuario inactivo: " + username);

        if (u.getRolCustomId() != null) {
            return new org.springframework.security.core.userdetails.User(
                    u.getUsername(), u.getPassword(),
                    buildCustomAuthorities(u.getRolCustomId()));
        }
        // Auto-resolver: buscar el rol de sistema correspondiente al enum (post-V73 para usuarios nuevos)
        String tenantId = TenantContext.getCurrentTenant();
        String sistemaNombre = ENUM_TO_SISTEMA.get(u.getRol());
        if (tenantId != null && sistemaNombre != null) {
            Optional<Long> sid = rolTenantRepo.findIdByTenantIdAndNombre(tenantId, sistemaNombre);
            if (sid.isPresent()) {
                return new org.springframework.security.core.userdetails.User(
                        u.getUsername(), u.getPassword(),
                        buildCustomAuthorities(sid.get()));
            }
        }
        // Fallback legacy: enum directamente (backward compat / sin roles en BD)
        return new org.springframework.security.core.userdetails.User(
                u.getUsername(), u.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + u.getRol().name())));
    }

    private List<GrantedAuthority> buildCustomAuthorities(Long rolCustomId) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_CUSTOM"));

        // Otorgar ROLE_ADMIN para roles de sistema con acceso total (es_admin=true)
        if (Boolean.TRUE.equals(rolTenantRepo.findEsAdminById(rolCustomId))) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        // Nombre del rol para mostrar en el badge del navbar
        String nombre = rolTenantRepo.findNombreById(rolCustomId);
        if (nombre != null) authorities.add(new SimpleGrantedAuthority("NOMBRE_ROL_" + nombre));

        // Native query — no filtra por tenant, seguro desde loadUserByUsername
        List<RolModuloPermiso> permisos = permisoRepo.findByRolIdNative(rolCustomId);
        for (RolModuloPermiso p : permisos) {
            String m = p.getModulo();
            if (p.isPuedeVer())      authorities.add(new SimpleGrantedAuthority("MODULO_" + m + "_VER"));
            if (p.isPuedeCrear())    authorities.add(new SimpleGrantedAuthority("MODULO_" + m + "_CREAR"));
            if (p.isPuedeEditar())   authorities.add(new SimpleGrantedAuthority("MODULO_" + m + "_EDITAR"));
            if (p.isPuedeEliminar()) authorities.add(new SimpleGrantedAuthority("MODULO_" + m + "_ELIMINAR"));
        }
        return authorities;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarTodos() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorId(Long id) {
        return repo.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> suggest(String q) {
        if (q == null || q.isBlank() || q.trim().length() < 2) return List.of();
        String lower = q.trim().toLowerCase();
        return repo.findAllByOrderByCreatedAtDesc().stream()
            .filter(u -> u.getUsername().toLowerCase().contains(lower))
            .limit(6)
            .map(u -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("username", u.getUsername());
                m.put("rol",      u.getRol() != null ? u.getRol().getDisplayName() : "");
                m.put("activo",   u.isActivo());
                m.put("anchor",   "usuario-" + u.getId());
                return m;
            }).toList();
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorUsername(String username) {
        return repo.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public boolean existeUsername(String username) {
        return repo.existsByUsername(username);
    }

    // Devuelve true si el usuario con ese id tiene el mismo username que el parámetro.
    // Usado para evitar que un admin se elimine o cambie su propio rol.
    @Transactional(readOnly = true)
    public boolean esElMismoUsuario(Long id, String username) {
        return repo.findById(id)
                .map(u -> u.getUsername().equals(username))
                .orElse(false);
    }

    public void guardar(String username, String password, RolUsuario rol) {
        Usuario u = new Usuario();
        u.setUsername(username);
        u.setPassword(encoder.encode(password));
        u.setRol(rol != null ? rol : RolUsuario.ADMIN);
        // Sincronizar al rol de sistema correspondiente
        sincronizarRolCustom(u);
        repo.save(u);
    }

    // TenantContext debe estar seteado al tenant destino ANTES de llamar este método.
    // REQUIRES_NEW abre un EntityManager nuevo que captura el tenant en ese momento.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void guardarEnTenant(String username, String password, RolUsuario rol) {
        guardar(username, password, rol);
    }

    public void toggleActivo(Long id) {
        repo.findById(id).ifPresent(u -> {
            u.setActivo(!u.isActivo());
            repo.save(u);
        });
    }

    public void cambiarPassword(Long id, String newPassword) {
        repo.findById(id).ifPresent(u -> {
            u.setPassword(encoder.encode(newPassword));
            repo.save(u);
        });
    }

    public void cambiarRol(Long id, RolUsuario nuevoRol) {
        repo.findById(id).ifPresent(u -> {
            u.setRol(nuevoRol);
            sincronizarRolCustom(u);
            repo.save(u);
        });
    }

    // Busca el rol de sistema para el enum rol del usuario y lo asigna como rolCustomId.
    // Si no existe (BD sin V73 o tests), deja rolCustomId como null.
    private void sincronizarRolCustom(Usuario u) {
        String tenantId = TenantContext.getCurrentTenant();
        String sistemaNombre = ENUM_TO_SISTEMA.get(u.getRol());
        if (tenantId != null && sistemaNombre != null) {
            rolTenantRepo.findIdByTenantIdAndNombre(tenantId, sistemaNombre)
                .ifPresentOrElse(u::setRolCustomId, () -> u.setRolCustomId(null));
        } else {
            u.setRolCustomId(null);
        }
    }

    public void eliminar(Long id) {
        repo.deleteById(id);
    }

    public void asignarRolCustom(Long id, Long rolCustomId) {
        String tenantId = TenantContext.getCurrentTenant();
        if (rolCustomId == null) {
            repo.quitarRolCustomByIdAndTenantId(id, tenantId);
        } else {
            repo.asignarRolCustomByIdAndTenantId(id, tenantId, rolCustomId);
        }
    }
}