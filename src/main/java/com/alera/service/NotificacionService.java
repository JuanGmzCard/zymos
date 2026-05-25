package com.alera.service;

import com.alera.model.Equipo;
import com.alera.model.FacturaProveedor;
import com.alera.model.InsumoInventario;
import com.alera.model.Notificacion;
import com.alera.model.enums.TipoNotificacion;
import com.alera.repository.NotificacionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class NotificacionService {

    private final NotificacionRepository repo;

    @Value("${app.page-size:15}")
    private int pageSize;

    public NotificacionService(NotificacionRepository repo) {
        this.repo = repo;
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
    public List<Notificacion> listarRecientes() {
        return repo.findTop5ByLeidaFalseOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public long contarNoLeidas() {
        return repo.countByLeidaFalse();
    }

    @Transactional(readOnly = true)
    public Page<Notificacion> listarTodas(int page) {
        return repo.findAllOrdenadas(PageRequest.of(page, pageSize));
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
