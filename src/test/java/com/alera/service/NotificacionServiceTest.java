package com.alera.service;

import com.alera.model.Equipo;
import com.alera.model.FacturaProveedor;
import com.alera.model.InsumoInventario;
import com.alera.model.Notificacion;
import com.alera.model.Tarea;
import com.alera.model.Tenant;
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

import java.time.LocalDate;
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

    // ── tiposVisibles ──────────────────────────────────────────────────────────

    @Test
    void tiposVisibles_adminVeTodo() {
        List<com.alera.model.enums.TipoNotificacion> tipos =
                service.tiposVisibles(List.of("ROLE_ADMIN"));
        assertThat(tipos).containsExactlyInAnyOrder(com.alera.model.enums.TipoNotificacion.values());
    }

    @Test
    void tiposVisibles_inventarioVeBajoStockYVencimiento() {
        List<com.alera.model.enums.TipoNotificacion> tipos =
                service.tiposVisibles(List.of("MODULO_INVENTARIO_VER"));
        assertThat(tipos).contains(TipoNotificacion.BAJO_STOCK, TipoNotificacion.VENCIMIENTO);
        assertThat(tipos).doesNotContain(TipoNotificacion.PLAN_VENCIMIENTO, TipoNotificacion.PLAN_LIMITE);
    }

    @Test
    void tiposVisibles_conModuloTareasVer_incluyeTareaAsignadaYVencimiento() {
        List<TipoNotificacion> tipos = service.tiposVisibles(List.of("MODULO_TAREAS_VER"));
        assertThat(tipos).contains(TipoNotificacion.TAREA_ASIGNADA, TipoNotificacion.TAREA_VENCIMIENTO);
        assertThat(tipos).doesNotContain(TipoNotificacion.PLAN_VENCIMIENTO, TipoNotificacion.PLAN_LIMITE);
    }

    @Test
    void tiposVisibles_sinPermisos_listaVacia() {
        assertThat(service.tiposVisibles(List.of())).isEmpty();
    }

    // ── listarRecientes ────────────────────────────────────────────────────────

    @Test
    void listarRecientes_delegaARepo() {
        Notificacion n = new Notificacion();
        when(repo.findTop5ByLeidaFalseAndTipoInOrderByCreatedAtDesc(anyList())).thenReturn(List.of(n));

        List<Notificacion> result = service.listarRecientes(List.of("ROLE_ADMIN"));

        assertThat(result).hasSize(1);
        verify(repo).findTop5ByLeidaFalseAndTipoInOrderByCreatedAtDesc(anyList());
    }

    @Test
    void listarRecientes_sinPermisos_listaVacia() {
        assertThat(service.listarRecientes(List.of())).isEmpty();
        verify(repo, never()).findTop5ByLeidaFalseAndTipoInOrderByCreatedAtDesc(anyList());
    }

    // ── contarNoLeidas ─────────────────────────────────────────────────────────

    @Test
    void contarNoLeidas_delegaARepo() {
        when(repo.countByLeidaFalseAndTipoIn(anyList())).thenReturn(7L);
        assertThat(service.contarNoLeidas(List.of("ROLE_ADMIN"))).isEqualTo(7L);
    }

    @Test
    void contarNoLeidas_sinPermisos_cero() {
        assertThat(service.contarNoLeidas(List.of())).isZero();
        verify(repo, never()).countByLeidaFalseAndTipoIn(anyList());
    }

    // ── listarTodas ────────────────────────────────────────────────────────────

    @Test
    void listarTodas_delegaARepo() {
        Page<Notificacion> page = new PageImpl<>(List.of());
        when(repo.findByTiposOrdenadas(anyList(), any(Pageable.class))).thenReturn(page);

        Page<Notificacion> result = service.listarTodas(0, List.of("ROLE_ADMIN"));

        assertThat(result).isNotNull();
        verify(repo).findByTiposOrdenadas(anyList(), any(Pageable.class));
    }

    @Test
    void listarTodas_sinPermisos_paginaVacia() {
        Page<Notificacion> result = service.listarTodas(0, List.of());
        assertThat(result.getContent()).isEmpty();
        verify(repo, never()).findByTiposOrdenadas(anyList(), any(Pageable.class));
    }

    // ── crearAlertaBpmSalud ────────────────────────────────────────────────────

    @Test
    void crearAlertaBpmSalud_sinDuplicado_creaNotificacion() {
        when(repo.existeEnPeriodo(eq(TipoNotificacion.BPM_SALUD), any(), any())).thenReturn(false);

        service.crearAlertaBpmSalud("Juan");

        ArgumentCaptor<Notificacion> cap = ArgumentCaptor.forClass(Notificacion.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getTipo()).isEqualTo(TipoNotificacion.BPM_SALUD);
        assertThat(cap.getValue().getMensaje()).contains("Juan");
        assertThat(cap.getValue().getUrlAccion()).isEqualTo("/bpm/salud/autorizaciones");
    }

    @Test
    void crearAlertaBpmSalud_yaDuplicadoHoy_noCreaNada() {
        when(repo.existeEnPeriodo(eq(TipoNotificacion.BPM_SALUD), any(), any())).thenReturn(true);

        service.crearAlertaBpmSalud("Juan");

        verify(repo, never()).save(any());
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

    // ── crearAlertaPlan ────────────────────────────────────────────────────────

    private Tenant tenant(LocalDate planFin, Integer maxLotes, Integer maxUsuarios) {
        Tenant t = new Tenant();
        t.setSubdomain("test");
        t.setPlanFin(planFin);
        t.setMaxLotes(maxLotes);
        t.setMaxUsuarios(maxUsuarios);
        return t;
    }

    @Test
    void crearAlertaPlan_sinPlanFinNiLimites_noCreaNada() {
        when(repo.existeEnPeriodo(any(), any(), any())).thenReturn(false);

        service.crearAlertaPlan(tenant(null, null, null), 5, 2);

        verify(repo, never()).save(any());
    }

    @Test
    void crearAlertaPlan_planVencido_creaNotificacionVencimiento() {
        when(repo.existeEnPeriodo(any(), any(), any())).thenReturn(false);

        service.crearAlertaPlan(tenant(LocalDate.now().minusDays(1), null, null), 0, 0);

        ArgumentCaptor<Notificacion> cap = ArgumentCaptor.forClass(Notificacion.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getTipo()).isEqualTo(TipoNotificacion.PLAN_VENCIMIENTO);
        assertThat(cap.getValue().getTitulo()).isEqualTo("Plan vencido");
    }

    @Test
    void crearAlertaPlan_planPorVencer_creaNotificacionPorVencer() {
        when(repo.existeEnPeriodo(any(), any(), any())).thenReturn(false);

        service.crearAlertaPlan(tenant(LocalDate.now().plusDays(3), null, null), 0, 0);

        ArgumentCaptor<Notificacion> cap = ArgumentCaptor.forClass(Notificacion.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getTipo()).isEqualTo(TipoNotificacion.PLAN_VENCIMIENTO);
        assertThat(cap.getValue().getTitulo()).isEqualTo("Plan por vencer");
    }

    @Test
    void crearAlertaPlan_planVencido_yaNotificadoHoy_noDuplica() {
        when(repo.existeEnPeriodo(eq(TipoNotificacion.PLAN_VENCIMIENTO), any(), any())).thenReturn(true);

        service.crearAlertaPlan(tenant(LocalDate.now().minusDays(1), null, null), 0, 0);

        verify(repo, never()).save(any());
    }

    @Test
    void crearAlertaPlan_limiteLotesAlcanzado_creaNotificacionLimite() {
        when(repo.existeEnPeriodo(any(), any(), any())).thenReturn(false);

        service.crearAlertaPlan(tenant(null, 10, null), 10, 0);

        ArgumentCaptor<Notificacion> cap = ArgumentCaptor.forClass(Notificacion.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getTipo()).isEqualTo(TipoNotificacion.PLAN_LIMITE);
        assertThat(cap.getValue().getTitulo()).isEqualTo("Límite de lotes alcanzado");
    }

    @Test
    void crearAlertaPlan_cercaDelLimiteDeLotes_creaNotificacionCerca() {
        when(repo.existeEnPeriodo(any(), any(), any())).thenReturn(false);

        service.crearAlertaPlan(tenant(null, 10, null), 9, 0);

        ArgumentCaptor<Notificacion> cap = ArgumentCaptor.forClass(Notificacion.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getTipo()).isEqualTo(TipoNotificacion.PLAN_LIMITE);
        assertThat(cap.getValue().getTitulo()).isEqualTo("Cerca del límite de lotes");
    }

    @Test
    void crearAlertaPlan_limiteUsuariosAlcanzado_creaNotificacionLimite() {
        when(repo.existeEnPeriodo(any(), any(), any())).thenReturn(false);

        service.crearAlertaPlan(tenant(null, null, 5), 0, 5);

        ArgumentCaptor<Notificacion> cap = ArgumentCaptor.forClass(Notificacion.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getTipo()).isEqualTo(TipoNotificacion.PLAN_LIMITE);
        assertThat(cap.getValue().getTitulo()).isEqualTo("Límite de usuarios alcanzado");
    }

    @Test
    void crearAlertaPlan_debajoDeLosLimites_noCreaNada() {
        when(repo.existeEnPeriodo(any(), any(), any())).thenReturn(false);

        service.crearAlertaPlan(tenant(null, 10, 10), 5, 5);

        verify(repo, never()).save(any());
    }

    @Test
    void crearAlertaPlan_limiteLotesYaNotificadoHoy_noDuplica() {
        when(repo.existeEnPeriodo(eq(TipoNotificacion.PLAN_LIMITE), any(), any())).thenReturn(true);

        service.crearAlertaPlan(tenant(null, 10, null), 10, 0);

        verify(repo, never()).save(any());
    }

    // ── crearAlertaTareaAsignada ───────────────────────────────────────────────

    private Tarea tareaConId(Long id, String titulo, String asignadoA) {
        Tarea t = new Tarea();
        t.setId(id);
        t.setTitulo(titulo);
        t.setAsignadoA(asignadoA);
        return t;
    }

    @Test
    void crearAlertaTareaAsignada_guardaNotificacionConTipoCorrectamente() {
        Tarea tarea = tareaConId(1L, "Limpiar fermentador", "juan");

        service.crearAlertaTareaAsignada(tarea);

        ArgumentCaptor<Notificacion> cap = ArgumentCaptor.forClass(Notificacion.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getTipo()).isEqualTo(TipoNotificacion.TAREA_ASIGNADA);
        assertThat(cap.getValue().getTitulo()).contains("Limpiar fermentador");
        assertThat(cap.getValue().getUrlAccion()).isEqualTo("/tareas/1");
    }

    @Test
    void crearAlertaTareaAsignada_sinFechaVencimiento_mensajeSinFecha() {
        Tarea tarea = tareaConId(2L, "Revisar presión", "karen");

        service.crearAlertaTareaAsignada(tarea);

        ArgumentCaptor<Notificacion> cap = ArgumentCaptor.forClass(Notificacion.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getMensaje()).doesNotContain("vence");
    }

    @Test
    void crearAlertaTareaAsignada_conFechaVencimiento_mensajeContieneFecha() {
        Tarea tarea = tareaConId(3L, "Calibrar sensor", "pedro");
        tarea.setFechaVencimiento(LocalDate.of(2026, 8, 1));

        service.crearAlertaTareaAsignada(tarea);

        ArgumentCaptor<Notificacion> cap = ArgumentCaptor.forClass(Notificacion.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getMensaje()).contains("vence");
    }

    // ── crearAlertaTareaVencimiento ────────────────────────────────────────────

    @Test
    void crearAlertaTareaVencimiento_listaVacia_noGuarda() {
        service.crearAlertaTareaVencimiento(List.of());
        verify(repo, never()).save(any());
    }

    @Test
    void crearAlertaTareaVencimiento_conDuplicadoHoy_noGuarda() {
        when(repo.existeEnPeriodo(eq(TipoNotificacion.TAREA_VENCIMIENTO), any(), any())).thenReturn(true);

        service.crearAlertaTareaVencimiento(List.of(tareaConId(1L, "T1", null)));

        verify(repo, never()).save(any());
    }

    @Test
    void crearAlertaTareaVencimiento_unaTarea_guardaConMensajeSingular() {
        when(repo.existeEnPeriodo(eq(TipoNotificacion.TAREA_VENCIMIENTO), any(), any())).thenReturn(false);

        service.crearAlertaTareaVencimiento(List.of(tareaConId(1L, "Calibrar fermentador", null)));

        ArgumentCaptor<Notificacion> cap = ArgumentCaptor.forClass(Notificacion.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getTipo()).isEqualTo(TipoNotificacion.TAREA_VENCIMIENTO);
        assertThat(cap.getValue().getTitulo()).contains("1 tarea");
        assertThat(cap.getValue().getMensaje()).contains("Calibrar fermentador");
        assertThat(cap.getValue().getUrlAccion()).isEqualTo("/tareas");
    }

    @Test
    void crearAlertaTareaVencimiento_variasTareas_mensajePlural() {
        when(repo.existeEnPeriodo(eq(TipoNotificacion.TAREA_VENCIMIENTO), any(), any())).thenReturn(false);
        List<Tarea> tareas = List.of(
            tareaConId(1L, "T1", null),
            tareaConId(2L, "T2", null),
            tareaConId(3L, "T3", null)
        );

        service.crearAlertaTareaVencimiento(tareas);

        ArgumentCaptor<Notificacion> cap = ArgumentCaptor.forClass(Notificacion.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getTitulo()).contains("3 tareas");
    }
}
