package com.alera.service;

import com.alera.model.Equipo;
import com.alera.model.FacturaProveedor;
import com.alera.model.InsumoInventario;
import com.alera.model.Notificacion;
import com.alera.model.enums.TipoNotificacion;
import com.alera.repository.NotificacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    @Mock NotificacionRepository repo;
    @InjectMocks NotificacionService service;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(service, "pageSize", 15);
        lenient().when(repo.save(any(Notificacion.class)))
                 .thenAnswer(inv -> inv.getArgument(0));
    }

    private InsumoInventario insumo(String nombre) {
        InsumoInventario i = new InsumoInventario();
        i.setNombre(nombre);
        return i;
    }

    private Equipo equipo(String nombre) {
        Equipo e = new Equipo();
        e.setNombre(nombre);
        return e;
    }

    private FacturaProveedor factura(String proveedor) {
        FacturaProveedor f = new FacturaProveedor();
        f.setProveedor(proveedor);
        return f;
    }

    // ── crear ──────────────────────────────────────────────────────────────────

    @Test
    void crear_guardaNotificacionCorrectamente() {
        service.crear(TipoNotificacion.SISTEMA, "Título", "Mensaje", "/url");
        ArgumentCaptor<Notificacion> cap = ArgumentCaptor.forClass(Notificacion.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getTitulo()).isEqualTo("Título");
        assertThat(cap.getValue().getMensaje()).isEqualTo("Mensaje");
        assertThat(cap.getValue().getUrlAccion()).isEqualTo("/url");
        assertThat(cap.getValue().getTipo()).isEqualTo(TipoNotificacion.SISTEMA);
    }

    // ── crearAlertas ───────────────────────────────────────────────────────────

    @Test
    void crearAlertas_listasVacias_noCreaNada() {
        int creadas = service.crearAlertas(List.of(), List.of(), List.of());
        assertThat(creadas).isZero();
        verify(repo, never()).save(any());
    }

    @Test
    void crearAlertas_bajoStock_sinDuplicado_creaNotificacion() {
        when(repo.existeEnPeriodo(eq(TipoNotificacion.BAJO_STOCK), any(), any())).thenReturn(false);

        int creadas = service.crearAlertas(List.of(insumo("Malta Pilsen")), List.of(), List.of());

        assertThat(creadas).isEqualTo(1);
        ArgumentCaptor<Notificacion> cap = ArgumentCaptor.forClass(Notificacion.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getTipo()).isEqualTo(TipoNotificacion.BAJO_STOCK);
        assertThat(cap.getValue().getUrlAccion()).isEqualTo("/inventario");
    }

    @Test
    void crearAlertas_bajoStock_yaDuplicado_noCreaNada() {
        when(repo.existeEnPeriodo(eq(TipoNotificacion.BAJO_STOCK), any(), any())).thenReturn(true);

        int creadas = service.crearAlertas(List.of(insumo("Malta")), List.of(), List.of());

        assertThat(creadas).isZero();
        verify(repo, never()).save(any());
    }

    @Test
    void crearAlertas_unSoloInsumo_mensajeSingular() {
        when(repo.existeEnPeriodo(any(), any(), any())).thenReturn(false);

        service.crearAlertas(List.of(insumo("Malta Pilsen")), List.of(), List.of());

        ArgumentCaptor<Notificacion> cap = ArgumentCaptor.forClass(Notificacion.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getMensaje()).contains("Malta Pilsen");
        assertThat(cap.getValue().getTitulo()).contains("1 insumo");
    }

    @Test
    void crearAlertas_variosInsumos_mensajePlural() {
        when(repo.existeEnPeriodo(any(), any(), any())).thenReturn(false);
        List<InsumoInventario> muchos = List.of(insumo("A"), insumo("B"), insumo("C"), insumo("D"));

        service.crearAlertas(muchos, List.of(), List.of());

        ArgumentCaptor<Notificacion> cap = ArgumentCaptor.forClass(Notificacion.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getTitulo()).contains("4 insumos");
        assertThat(cap.getValue().getMensaje()).contains("y 1 más");
    }

    @Test
    void crearAlertas_proximosAVencer_creaNotificacion() {
        when(repo.existeEnPeriodo(any(), any(), any())).thenReturn(false);

        int creadas = service.crearAlertas(List.of(), List.of(insumo("Lúpulo Cascade")), List.of());

        assertThat(creadas).isEqualTo(1);
        ArgumentCaptor<Notificacion> cap = ArgumentCaptor.forClass(Notificacion.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getTipo()).isEqualTo(TipoNotificacion.VENCIMIENTO);
    }

    @Test
    void crearAlertas_mantenimiento_creaNotificacion() {
        when(repo.existeEnPeriodo(any(), any(), any())).thenReturn(false);

        int creadas = service.crearAlertas(List.of(), List.of(), List.of(equipo("Fermentador 1")));

        assertThat(creadas).isEqualTo(1);
        ArgumentCaptor<Notificacion> cap = ArgumentCaptor.forClass(Notificacion.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getTipo()).isEqualTo(TipoNotificacion.MANTENIMIENTO);
        assertThat(cap.getValue().getUrlAccion()).isEqualTo("/equipos");
    }

    @Test
    void crearAlertas_tresTipos_creaLas3() {
        when(repo.existeEnPeriodo(any(), any(), any())).thenReturn(false);

        int creadas = service.crearAlertas(
            List.of(insumo("Malta")),
            List.of(insumo("Lúpulo")),
            List.of(equipo("Olla"))
        );

        assertThat(creadas).isEqualTo(3);
        verify(repo, times(3)).save(any());
    }

    // ── listarRecientes ────────────────────────────────────────────────────────

    @Test
    void listarRecientes_delegaARepo() {
        Notificacion n = new Notificacion();
        when(repo.findTop5ByLeidaFalseOrderByCreatedAtDesc()).thenReturn(List.of(n));

        List<Notificacion> result = service.listarRecientes();

        assertThat(result).hasSize(1);
        verify(repo).findTop5ByLeidaFalseOrderByCreatedAtDesc();
    }

    // ── contarNoLeidas ─────────────────────────────────────────────────────────

    @Test
    void contarNoLeidas_delegaARepo() {
        when(repo.countByLeidaFalse()).thenReturn(7L);
        assertThat(service.contarNoLeidas()).isEqualTo(7L);
    }

    // ── listarTodas ────────────────────────────────────────────────────────────

    @Test
    void listarTodas_delegaARepo() {
        Page<Notificacion> page = new PageImpl<>(List.of());
        when(repo.findAllOrdenadas(any(Pageable.class))).thenReturn(page);

        Page<Notificacion> result = service.listarTodas(0);

        assertThat(result).isNotNull();
        verify(repo).findAllOrdenadas(any(Pageable.class));
    }

    // ── marcarLeida ────────────────────────────────────────────────────────────

    @Test
    void marcarLeida_seteaLeidaTrue() {
        Notificacion n = new Notificacion();
        n.setLeida(false);
        when(repo.findById(1L)).thenReturn(Optional.of(n));

        service.marcarLeida(1L);

        ArgumentCaptor<Notificacion> cap = ArgumentCaptor.forClass(Notificacion.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().isLeida()).isTrue();
    }

    @Test
    void marcarLeida_noExiste_noOp() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        service.marcarLeida(99L);
        verify(repo, never()).save(any());
    }

    // ── marcarTodasLeidas ──────────────────────────────────────────────────────

    @Test
    void marcarTodasLeidas_delegaARepo() {
        service.marcarTodasLeidas();
        verify(repo).marcarTodasLeidas();
    }

    // ── crearAlertaFacturas ────────────────────────────────────────────────────

    @Test
    void crearAlertaFacturas_listaVacia_noCreaNada() {
        service.crearAlertaFacturas(List.of(), 30);
        verify(repo, never()).save(any());
    }

    @Test
    void crearAlertaFacturas_yaDuplicado_noCreaNada() {
        when(repo.existeEnPeriodo(eq(TipoNotificacion.SISTEMA), any(), any())).thenReturn(true);
        service.crearAlertaFacturas(List.of(factura("Proveedor A")), 30);
        verify(repo, never()).save(any());
    }

    @Test
    void crearAlertaFacturas_unaFactura_creaNotificacion() {
        when(repo.existeEnPeriodo(eq(TipoNotificacion.SISTEMA), any(), any())).thenReturn(false);

        service.crearAlertaFacturas(List.of(factura("Cervecero SAS")), 30);

        ArgumentCaptor<Notificacion> cap = ArgumentCaptor.forClass(Notificacion.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getTipo()).isEqualTo(TipoNotificacion.SISTEMA);
        assertThat(cap.getValue().getUrlAccion()).isEqualTo("/facturas");
        assertThat(cap.getValue().getMensaje()).contains("Cervecero SAS");
        assertThat(cap.getValue().getMensaje()).contains("30");
    }

    @Test
    void crearAlertaFacturas_variosProveedores_mensajeConMas() {
        when(repo.existeEnPeriodo(eq(TipoNotificacion.SISTEMA), any(), any())).thenReturn(false);

        List<FacturaProveedor> facturas = List.of(
            factura("A"), factura("B"), factura("C"), factura("D")
        );
        service.crearAlertaFacturas(facturas, 30);

        ArgumentCaptor<Notificacion> cap = ArgumentCaptor.forClass(Notificacion.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getMensaje()).contains("y 1 más");
        assertThat(cap.getValue().getTitulo()).contains("4 facturas");
    }
}
