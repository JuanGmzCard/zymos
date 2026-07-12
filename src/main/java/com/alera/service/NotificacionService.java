package com.alera.service;

import com.alera.model.Equipo;
import com.alera.model.FacturaProveedor;
import com.alera.model.InsumoInventario;
import com.alera.model.Notificacion;
import com.alera.model.Tarea;
import com.alera.model.Tenant;
import com.alera.model.enums.TipoNotificacion;
import com.alera.repository.NotificacionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class NotificacionService {

    // Qué authority de módulo debe tener el usuario para ver cada tipo de notificación.
    // PLAN_* y tipos sin entrada → solo ROLE_ADMIN / ROLE_SUPERADMIN.
    private static final Map<TipoNotificacion, String> TIPO_AUTHORITY = Map.ofEntries(
        Map.entry(TipoNotificacion.BAJO_STOCK,        "MODULO_INVENTARIO_VER"),
        Map.entry(TipoNotificacion.VENCIMIENTO,       "MODULO_INVENTARIO_VER"),
        Map.entry(TipoNotificacion.MANTENIMIENTO,     "MODULO_EQUIPOS_VER"),
        Map.entry(TipoNotificacion.SISTEMA,           "MODULO_FACTURACION_VER"),
        Map.entry(TipoNotificacion.BPM_SALUD,         "MODULO_BPM_VER"),
        Map.entry(TipoNotificacion.TAREA_ASIGNADA,    "MODULO_TAREAS_VER"),
        Map.entry(TipoNotificacion.TAREA_VENCIMIENTO, "MODULO_TAREAS_VER")
    );

    private final NotificacionRepository repo;

    @Value("${app.page-size:15}")
    private int pageSize;

    public NotificacionService(NotificacionRepository repo) {
        this.repo = repo;
    }

    /** Calcula qué TipoNotificacion puede ver este usuario según sus authorities. */
    public List<TipoNotificacion> tiposVisibles(Collection<String> authorities) {
        boolean isAdmin = authorities.contains("ROLE_ADMIN") || authorities.contains("ROLE_SUPERADMIN");
        return Arrays.stream(TipoNotificacion.values())
                .filter(tipo -> {
                    if (isAdmin) return true;
                    String required = TIPO_AUTHORITY.get(tipo);
                    // tipos sin entrada en el mapa (PLAN_*) → solo admins
                    return required != null && authorities.contains(required);
                })
                .toList();
    }

    public void crear(TipoNotificacion tipo, String titulo, String mensaje, String urlAccion) {
        repo.save(Notificacion.of(tipo, titulo, mensaje, urlAccion));
    }

    /**
     * Crea una notificación in-app por cada tipo de alerta que tenga elementos,
     * evitando duplicados si ya se creó una del mismo tipo hoy.
     * Retorna la cantidad de notificaciones creadas.
     */
    public int crearAlertas(List<InsumoInventario> bajoStock,
                             List<InsumoInventario> proximosAVencer,
                             List<Equipo> mantenimiento) {
        LocalDateTime hoy     = LocalDate.now().atStartOfDay();
        LocalDateTime maniana = hoy.plusDays(1);
        int creadas = 0;

        if (!bajoStock.isEmpty() && !repo.existeEnPeriodo(TipoNotificacion.BAJO_STOCK, hoy, maniana)) {
            String msg = bajoStock.size() == 1
                    ? bajoStock.get(0).getNombre() + " está por debajo del stock mínimo."
                    : bajoStock.stream().limit(3).map(InsumoInventario::getNombre)
                               .reduce((a, b) -> a + ", " + b).orElse("")
                      + (bajoStock.size() > 3 ? " y " + (bajoStock.size() - 3) + " más." : ".");
            repo.save(Notificacion.of(
                    TipoNotificacion.BAJO_STOCK,
                    bajoStock.size() + " insumo" + (bajoStock.size() > 1 ? "s" : "") + " bajo stock",
                    msg,
                    "/inventario"));
            creadas++;
        }

        if (!proximosAVencer.isEmpty() && !repo.existeEnPeriodo(TipoNotificacion.VENCIMIENTO, hoy, maniana)) {
            String msg = proximosAVencer.size() == 1
                    ? proximosAVencer.get(0).getNombre() + " vence pronto."
                    : proximosAVencer.stream().limit(3).map(InsumoInventario::getNombre)
                                     .reduce((a, b) -> a + ", " + b).orElse("")
                      + (proximosAVencer.size() > 3 ? " y " + (proximosAVencer.size() - 3) + " más." : ".");
            repo.save(Notificacion.of(
                    TipoNotificacion.VENCIMIENTO,
                    proximosAVencer.size() + " insumo" + (proximosAVencer.size() > 1 ? "s" : "") + " próximo" + (proximosAVencer.size() > 1 ? "s" : "") + " a vencer",
                    msg,
                    "/inventario"));
            creadas++;
        }

        if (!mantenimiento.isEmpty() && !repo.existeEnPeriodo(TipoNotificacion.MANTENIMIENTO, hoy, maniana)) {
            String msg = mantenimiento.size() == 1
                    ? mantenimiento.get(0).getNombre() + " requiere mantenimiento."
                    : mantenimiento.stream().limit(3).map(Equipo::getNombre)
                                   .reduce((a, b) -> a + ", " + b).orElse("")
                      + (mantenimiento.size() > 3 ? " y " + (mantenimiento.size() - 3) + " más." : ".");
            repo.save(Notificacion.of(
                    TipoNotificacion.MANTENIMIENTO,
                    mantenimiento.size() + " equipo" + (mantenimiento.size() > 1 ? "s" : "") + " con mantenimiento pendiente",
                    msg,
                    "/equipos"));
            creadas++;
        }

        return creadas;
    }

    @Transactional(readOnly = true)
    public List<Notificacion> listarRecientes(Collection<String> authorities) {
        List<TipoNotificacion> tipos = tiposVisibles(authorities);
        if (tipos.isEmpty()) return List.of();
        return repo.findTop5ByLeidaFalseAndTipoInOrderByCreatedAtDesc(tipos);
    }

    @Transactional(readOnly = true)
    public long contarNoLeidas(Collection<String> authorities) {
        List<TipoNotificacion> tipos = tiposVisibles(authorities);
        if (tipos.isEmpty()) return 0;
        return repo.countByLeidaFalseAndTipoIn(tipos);
    }

    @Transactional(readOnly = true)
    public Page<Notificacion> listarTodas(int page, Collection<String> authorities) {
        List<TipoNotificacion> tipos = tiposVisibles(authorities);
        if (tipos.isEmpty()) return Page.empty();
        return repo.findByTiposOrdenadas(tipos, PageRequest.of(page, pageSize));
    }

    public void marcarLeida(Long id) {
        repo.findById(id).ifPresent(n -> {
            n.setLeida(true);
            repo.save(n);
        });
    }

    public void marcarTodasLeidas() {
        repo.marcarTodasLeidas();
    }

    /**
     * Crea notificaciones in-app sobre el estado del plan del tenant: vencimiento
     * (vencido o por vencer) y cercanía a los límites de lotes/usuarios (>= 90%).
     * Evita duplicados si ya se creó una notificación del mismo tipo hoy.
     */
    public void crearAlertaPlan(Tenant tenant, long totalLotes, long totalUsuarios) {
        LocalDateTime hoy     = LocalDate.now().atStartOfDay();
        LocalDateTime maniana = hoy.plusDays(1);

        if ((tenant.isPlanVencido() || tenant.isPlanPorVencer())
                && !repo.existeEnPeriodo(TipoNotificacion.PLAN_VENCIMIENTO, hoy, maniana)) {
            if (tenant.isPlanVencido()) {
                repo.save(Notificacion.of(
                        TipoNotificacion.PLAN_VENCIMIENTO,
                        "Plan vencido",
                        "El plan venció el " + tenant.getPlanFin() + ". Contacta al administrador para renovarlo.",
                        null));
            } else {
                repo.save(Notificacion.of(
                        TipoNotificacion.PLAN_VENCIMIENTO,
                        "Plan por vencer",
                        "El plan vence el " + tenant.getPlanFin() + ".",
                        null));
            }
        }

        if (!repo.existeEnPeriodo(TipoNotificacion.PLAN_LIMITE, hoy, maniana)) {
            Integer maxLotes    = tenant.getMaxLotes();
            Integer maxUsuarios = tenant.getMaxUsuarios();
            String titulo  = null;
            String mensaje = null;

            if (maxLotes != null && totalLotes >= maxLotes) {
                titulo  = "Límite de lotes alcanzado";
                mensaje = "Se alcanzó el límite de " + maxLotes + " lotes incluidos en el plan.";
            } else if (maxLotes != null && totalLotes >= maxLotes * 0.9) {
                titulo  = "Cerca del límite de lotes";
                mensaje = "Se usaron " + totalLotes + " de " + maxLotes + " lotes incluidos en el plan.";
            }

            if (maxUsuarios != null && totalUsuarios >= maxUsuarios) {
                String t2 = "Límite de usuarios alcanzado";
                String m2 = "Se alcanzó el límite de " + maxUsuarios + " usuarios incluidos en el plan.";
                titulo  = titulo  == null ? t2 : titulo  + " / " + t2;
                mensaje = mensaje == null ? m2 : mensaje + " " + m2;
            } else if (maxUsuarios != null && totalUsuarios >= maxUsuarios * 0.9) {
                String t2 = "Cerca del límite de usuarios";
                String m2 = "Se usaron " + totalUsuarios + " de " + maxUsuarios + " usuarios incluidos en el plan.";
                titulo  = titulo  == null ? t2 : titulo  + " / " + t2;
                mensaje = mensaje == null ? m2 : mensaje + " " + m2;
            }

            if (titulo != null) {
                repo.save(Notificacion.of(TipoNotificacion.PLAN_LIMITE, titulo, mensaje, null));
            }
        }
    }

    public void crearAlertaBpmSalud(String nombreManipulador) {
        LocalDateTime hoy     = LocalDate.now().atStartOfDay();
        LocalDateTime maniana = hoy.plusDays(1);
        // Una notificación por día máximo; si ya existe una hoy se actualiza el mensaje en la misma
        if (!repo.existeEnPeriodo(TipoNotificacion.BPM_SALUD, hoy, maniana)) {
            repo.save(Notificacion.of(
                    TipoNotificacion.BPM_SALUD,
                    "Síntomas reportados",
                    nombreManipulador + " reportó síntomas hoy. Revisá las autorizaciones de salud.",
                    "/bpm/salud/autorizaciones"));
        }
    }

    public void crearAlertaTareaAsignada(Tarea tarea) {
        String titulo  = "Tarea asignada: " + tarea.getTitulo();
        String mensaje = "Te asignaron la tarea \"" + tarea.getTitulo() + "\""
                + (tarea.getFechaVencimiento() != null ? " — vence el " + tarea.getFechaVencimiento() : "") + ".";
        repo.save(Notificacion.of(TipoNotificacion.TAREA_ASIGNADA, titulo, mensaje, "/tareas/" + tarea.getId()));
    }

    public void crearAlertaTareaVencimiento(List<Tarea> tareas) {
        if (tareas.isEmpty()) return;
        LocalDateTime hoy     = LocalDate.now().atStartOfDay();
        LocalDateTime maniana = hoy.plusDays(1);
        if (repo.existeEnPeriodo(TipoNotificacion.TAREA_VENCIMIENTO, hoy, maniana)) return;
        int n = tareas.size();
        String msg = n == 1
                ? "\"" + tareas.get(0).getTitulo() + "\" vence mañana."
                : tareas.stream().limit(3).map(Tarea::getTitulo)
                        .reduce((a, b) -> a + ", " + b).orElse("")
                  + (n > 3 ? " y " + (n - 3) + " más vencen" : " vencen") + " mañana.";
        repo.save(Notificacion.of(
                TipoNotificacion.TAREA_VENCIMIENTO,
                n + " tarea" + (n > 1 ? "s" : "") + " por vencer",
                msg,
                "/tareas"));
    }

    public void crearAlertaFacturas(List<FacturaProveedor> sinProcesar, int dias) {
        if (sinProcesar.isEmpty()) return;
        LocalDateTime hoy     = LocalDate.now().atStartOfDay();
        LocalDateTime maniana = hoy.plusDays(1);
        if (repo.existeEnPeriodo(TipoNotificacion.SISTEMA, hoy, maniana)) return;
        int n = sinProcesar.size();
        String msg = n == 1
                ? "La factura de " + sinProcesar.get(0).getProveedor() + " lleva más de " + dias + " días sin procesar."
                : sinProcesar.stream().limit(3)
                             .map(f -> f.getProveedor() != null ? f.getProveedor() : "#" + f.getId())
                             .reduce((a, b) -> a + ", " + b).orElse("")
                  + (n > 3 ? " y " + (n - 3) + " más llevan" : " llevan")
                  + " más de " + dias + " días sin procesar.";
        repo.save(Notificacion.of(
                TipoNotificacion.SISTEMA,
                n + " factura" + (n > 1 ? "s" : "") + " sin procesar",
                msg,
                "/facturas"));
    }
}
