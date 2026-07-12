package com.alera.service;

import com.alera.model.RolModuloPermiso;
import com.alera.model.RolTenant;
import com.alera.model.enums.ModuloApp;
import com.alera.repository.RolTenantRepository;
import com.alera.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class RolTenantService {

    private final RolTenantRepository repo;
    private final UsuarioRepository usuarioRepo;

    public RolTenantService(RolTenantRepository repo,
                            UsuarioRepository usuarioRepo) {
        this.repo = repo;
        this.usuarioRepo = usuarioRepo;
    }

    @Transactional(readOnly = true)
    public List<RolTenant> listarTodos() {
        return repo.findAllByOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public List<RolTenant> listarActivos() {
        return repo.findAllByActivoTrueOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public Optional<RolTenant> buscarPorId(Long id) {
        return repo.findById(id);
    }

    @Transactional(readOnly = true)
    public long contarUsuariosConRol(Long rolId) {
        return usuarioRepo.countByRolCustomId(rolId);
    }

    public RolTenant guardar(String nombre, String descripcion,
                             List<String> modulosVer,
                             List<String> modulosCrear,
                             List<String> modulosEditar,
                             List<String> modulosEliminar) {
        RolTenant rol = new RolTenant();
        rol.setNombre(nombre.trim());
        rol.setDescripcion(descripcion != null ? descripcion.trim() : null);
        rol.setActivo(true);
        rol.setEsSistema(false);
        rol = repo.save(rol);
        guardarPermisos(rol, modulosVer, modulosCrear, modulosEditar, modulosEliminar);
        return rol;
    }

    public void actualizar(Long id,
                           String nombre, String descripcion, boolean activo,
                           List<String> modulosVer,
                           List<String> modulosCrear,
                           List<String> modulosEditar,
                           List<String> modulosEliminar) {
        RolTenant rol = repo.findById(id).orElseThrow();
        rol.setNombre(nombre.trim());
        rol.setDescripcion(descripcion != null ? descripcion.trim() : null);
        if (!rol.isEsSistema()) rol.setActivo(activo);
        rol.getPermisos().clear();
        repo.flush();  // DELETE orphans before inserting replacements (avoids unique constraint)
        guardarPermisos(rol, modulosVer, modulosCrear, modulosEditar, modulosEliminar);
    }

    public void toggleActivo(Long id) {
        repo.findById(id).ifPresent(r -> {
            if (!r.isEsSistema()) {
                r.setActivo(!r.isActivo());
                repo.save(r);
            }
        });
    }

    public void eliminar(Long id) {
        RolTenant rol = repo.findById(id).orElseThrow();
        if (rol.isEsSistema()) throw new IllegalStateException("No se puede eliminar un rol de sistema");
        // Desvincular usuarios antes de eliminar
        usuarioRepo.limpiarRolCustom(id);
        repo.delete(rol);
    }

    private void guardarPermisos(RolTenant rol,
                                 List<String> ver, List<String> crear,
                                 List<String> editar, List<String> eliminar) {
        List<String> verL      = ver      != null ? ver      : List.of();
        List<String> crearL    = crear    != null ? crear    : List.of();
        List<String> editarL   = editar   != null ? editar   : List.of();
        List<String> eliminarL = eliminar != null ? eliminar : List.of();

        for (ModuloApp modulo : ModuloApp.values()) {
            String m = modulo.name();
            boolean pVer = verL.contains(m);
            boolean pCrear = crearL.contains(m);
            boolean pEditar = editarL.contains(m);
            boolean pEliminar = eliminarL.contains(m);

            if (pVer || pCrear || pEditar || pEliminar) {
                RolModuloPermiso p = new RolModuloPermiso();
                p.setRol(rol);
                p.setModulo(m);
                p.setPuedeVer(pVer);
                p.setPuedeCrear(pCrear);
                p.setPuedeEditar(pEditar);
                p.setPuedeEliminar(pEliminar);
                rol.getPermisos().add(p);  // mantiene bidireccional; CascadeType.ALL hace el INSERT
            }
        }
    }

    // Creación de todos los roles por defecto para un nuevo tenant — usa native SQL (cross-tenant safe).
    // Solo actúa si el tenant no tiene ningún rol aún.
    public void crearRolesPorDefectoSiNoTiene(String tenantId) {
        if (repo.countByTenantId(tenantId) > 0) return;

        // ── 5 roles de sistema ────────────────────────────────────────
        repo.insertarRolNativo(tenantId, "Administrador",
                "Acceso completo al sistema", true, true);
        crearPermisosParaRol(tenantId, "Administrador",
                new String[]{"TRAZABILIDAD","RECETAS","INVENTARIO","FACTURACION","COMERCIAL",
                             "EQUIPOS","REPORTES","PLANIFICACION","BPM","BARRILES","TAREAS"},
                true, true, true, true);

        repo.insertarRolNativo(tenantId, "Producción",
                "Gestión de lotes, recetas e inventario de producción", true, false);
        crearPermisosParaRol(tenantId, "Producción",
                new String[]{"TRAZABILIDAD","RECETAS","PLANIFICACION","INVENTARIO","EQUIPOS","BARRILES","TAREAS"},
                true, true, true, true);
        crearPermisosParaRol(tenantId, "Producción",
                new String[]{"REPORTES"}, true, false, false, false);

        repo.insertarRolNativo(tenantId, "Inventario",
                "Gestión de inventario, equipos y barriles", true, false);
        crearPermisosParaRol(tenantId, "Inventario",
                new String[]{"INVENTARIO","RECETAS","EQUIPOS","BARRILES"},
                true, true, true, true);

        repo.insertarRolNativo(tenantId, "Facturación",
                "Gestión de facturas, ventas y proveedores", true, false);
        crearPermisosParaRol(tenantId, "Facturación",
                new String[]{"FACTURACION","COMERCIAL","REPORTES"},
                true, true, true, true);

        repo.insertarRolNativo(tenantId, "Equipos",
                "Gestión y mantenimiento de equipos", true, false);
        crearPermisosParaRol(tenantId, "Equipos",
                new String[]{"EQUIPOS"}, true, true, true, true);

        // ── Rol adicional: Recursos Humanos ───────────────────────────
        repo.insertarRolNativo(tenantId, "Recursos Humanos",
                "Acceso al módulo BPM (Buenas Prácticas de Manufactura)", false, false);
        crearPermisosParaRol(tenantId, "Recursos Humanos",
                new String[]{"BPM","TAREAS"}, true, true, true, true);
    }

    private void crearPermisosParaRol(String tenantId, String nombreRol,
                                      String[] modulos,
                                      boolean ver, boolean crear,
                                      boolean editar, boolean eliminar) {
        repo.findIdByTenantIdAndNombre(tenantId, nombreRol).ifPresent(rolId -> {
            for (String modulo : modulos) {
                repo.insertarPermisoNativo(rolId, modulo, ver, crear, editar, eliminar);
            }
        });
    }
}
