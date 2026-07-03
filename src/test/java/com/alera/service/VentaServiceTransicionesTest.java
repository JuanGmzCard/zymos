package com.alera.service;

import com.alera.dto.VentaFormDto;
import com.alera.dto.VentaItemFormDto;
import com.alera.model.Venta;
import com.alera.model.VentaHistorialEstado;
import com.alera.model.enums.EstadoVenta;
import com.alera.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VentaService — transiciones de estado y expiración")
class VentaServiceTransicionesTest {

    @Mock private VentaRepository                ventaRepo;
    @Mock private VentaItemRepository            ventaItemRepo;
    @Mock private LoteCervezaRepository          loteRepo;
    @Mock private VentaHistorialEstadoRepository historialRepo;
    @Mock private NotificacionService            notificacionService;
    @Mock private ClienteRepository              clienteRepo;
    @Mock private InsumoInventarioService        insumoService;
    @Mock private StockAjusteRepository          ajusteRepo;
    @Mock private EntityManager                  em;
    @Mock private Query                          nativeQuery;

    @InjectMocks
    private VentaService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "pageSize", 15);
        ReflectionTestUtils.setField(service, "expiracionDias", 15);
        ReflectionTestUtils.setField(service, "em", em);
        lenient().when(historialRepo.save(any())).thenReturn(new VentaHistorialEstado());
        lenient().when(em.createNativeQuery(anyString())).thenReturn(nativeQuery);
        lenient().when(nativeQuery.setParameter(anyString(), any())).thenReturn(nativeQuery);
        lenient().when(nativeQuery.getSingleResult()).thenReturn(0);
        lenient().when(ventaItemRepo.findItemsConEnvase(anyLong())).thenReturn(List.of());
    }

    private Venta ventaConEstado(EstadoVenta estado) {
        Venta v = new Venta();
        v.setEstado(estado);
        v.setCliente("Cliente Test");
        return v;
    }

    // ── cambiarEstado — transiciones inválidas ────────────────────────────────

    @Test
    @DisplayName("cambiarEstado — DESPACHADO → PENDIENTE lanza excepción (transición inválida)")
    void cambiarEstado_despachado_a_pendiente_invalida() {
        Venta v = ventaConEstado(EstadoVenta.DESPACHADO);
        when(ventaRepo.findById(1L)).thenReturn(Optional.of(v));

        assertThatThrownBy(() -> service.cambiarEstado(1L, EstadoVenta.PENDIENTE))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DESPACHADO")
                .hasMessageContaining("PENDIENTE");
    }

    @Test
    @DisplayName("cambiarEstado — CANCELADO → PENDIENTE lanza excepción (transición inválida)")
    void cambiarEstado_cancelado_a_pendiente_invalida() {
        Venta v = ventaConEstado(EstadoVenta.CANCELADO);
        when(ventaRepo.findById(1L)).thenReturn(Optional.of(v));

        assertThatThrownBy(() -> service.cambiarEstado(1L, EstadoVenta.PENDIENTE))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("CANCELADO");
    }

    @Test
    @DisplayName("cambiarEstado — EXPIRADO → PENDIENTE lanza excepción (transición inválida)")
    void cambiarEstado_expirado_a_pendiente_invalida() {
        Venta v = ventaConEstado(EstadoVenta.EXPIRADO);
        when(ventaRepo.findById(1L)).thenReturn(Optional.of(v));

        assertThatThrownBy(() -> service.cambiarEstado(1L, EstadoVenta.PENDIENTE))
                .isInstanceOf(RuntimeException.class);
    }

    // ── cambiarEstado — transiciones válidas ──────────────────────────────────

    @Test
    @DisplayName("cambiarEstado — COTIZACION → PENDIENTE es válida")
    void cambiarEstado_cotizacion_a_pendiente_valida() {
        Venta v = ventaConEstado(EstadoVenta.COTIZACION);
        when(ventaRepo.findById(1L)).thenReturn(Optional.of(v));
        when(ventaRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatCode(() -> service.cambiarEstado(1L, EstadoVenta.PENDIENTE))
                .doesNotThrowAnyException();
        assertThat(v.getEstado()).isEqualTo(EstadoVenta.PENDIENTE);
    }

    @Test
    @DisplayName("cambiarEstado — COTIZACION → CANCELADO es válida")
    void cambiarEstado_cotizacion_a_cancelado_valida() {
        Venta v = ventaConEstado(EstadoVenta.COTIZACION);
        when(ventaRepo.findById(1L)).thenReturn(Optional.of(v));
        when(ventaRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatCode(() -> service.cambiarEstado(1L, EstadoVenta.CANCELADO))
                .doesNotThrowAnyException();
        assertThat(v.getEstado()).isEqualTo(EstadoVenta.CANCELADO);
    }

    @Test
    @DisplayName("cambiarEstado — PENDIENTE → CANCELADO es válida")
    void cambiarEstado_pendiente_a_cancelado_valida() {
        Venta v = ventaConEstado(EstadoVenta.PENDIENTE);
        when(ventaRepo.findById(1L)).thenReturn(Optional.of(v));
        when(ventaRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatCode(() -> service.cambiarEstado(1L, EstadoVenta.CANCELADO))
                .doesNotThrowAnyException();
        assertThat(v.getEstado()).isEqualTo(EstadoVenta.CANCELADO);
    }

    // ── expirarCotizaciones ───────────────────────────────────────────────────

    @Test
    @DisplayName("expirarCotizaciones — retorna 0 cuando no hay cotizaciones vencidas")
    void expirarCotizaciones_sinVencidas_retornaCero() {
        when(ventaRepo.findCotizacionesVencidas(any())).thenReturn(List.of());

        assertThat(service.expirarCotizaciones()).isEqualTo(0);
    }

    @Test
    @DisplayName("expirarCotizaciones — cambia estado a EXPIRADO y retorna el conteo")
    void expirarCotizaciones_expiraYRetornaConteo() {
        Venta v1 = ventaConEstado(EstadoVenta.COTIZACION);
        Venta v2 = ventaConEstado(EstadoVenta.COTIZACION);
        v2.setCliente("Otro Cliente");
        when(ventaRepo.findCotizacionesVencidas(any())).thenReturn(List.of(v1, v2));
        when(ventaRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = service.expirarCotizaciones();

        assertThat(resultado).isEqualTo(2);
        assertThat(v1.getEstado()).isEqualTo(EstadoVenta.EXPIRADO);
        assertThat(v2.getEstado()).isEqualTo(EstadoVenta.EXPIRADO);
    }

    @Test
    @DisplayName("expirarCotizaciones — guarda historial de transición COTIZACION → EXPIRADO")
    void expirarCotizaciones_guardaHistorial() {
        Venta v = ventaConEstado(EstadoVenta.COTIZACION);
        v.setCotizacionExpiraEn(LocalDate.now().minusDays(1));
        when(ventaRepo.findCotizacionesVencidas(any())).thenReturn(List.of(v));
        when(ventaRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.expirarCotizaciones();

        ArgumentCaptor<VentaHistorialEstado> captor = ArgumentCaptor.forClass(VentaHistorialEstado.class);
        verify(historialRepo).save(captor.capture());
        assertThat(captor.getValue().getEstadoAnterior()).isEqualTo(EstadoVenta.COTIZACION);
        assertThat(captor.getValue().getEstadoNuevo()).isEqualTo(EstadoVenta.EXPIRADO);
    }

    @Test
    @DisplayName("expirarCotizaciones — crea notificación por cada cotización expirada")
    void expirarCotizaciones_creaNotificacion() {
        Venta v = ventaConEstado(EstadoVenta.COTIZACION);
        v.setCliente("Bar El Lúpulo");
        when(ventaRepo.findCotizacionesVencidas(any())).thenReturn(List.of(v));
        when(ventaRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.expirarCotizaciones();

        verify(notificacionService).crear(any(), contains("Bar El Lúpulo"), anyString(), anyString());
    }

    // ── guardar — fecha expiración de cotización ──────────────────────────────

    @Test
    @DisplayName("guardar — COTIZACION sin fecha explícita setea cotizacionExpiraEn = hoy + expiracionDias")
    void guardar_cotizacion_setaFechaExpiracion() {
        VentaFormDto dto = new VentaFormDto();
        dto.setCliente("Cliente");
        dto.setEstado(EstadoVenta.COTIZACION);
        dto.setCotizacionExpiraEn(null);
        VentaItemFormDto item = new VentaItemFormDto();
        item.setCantidad(BigDecimal.ONE);
        item.setPrecioUnitario(BigDecimal.ONE);
        item.setDescuentoPct(BigDecimal.ZERO);
        dto.getItems().add(item);

        Venta saved = new Venta();
        saved.setEstado(EstadoVenta.COTIZACION);
        when(ventaRepo.save(any())).thenReturn(saved);

        service.guardar(dto);

        ArgumentCaptor<Venta> captor = ArgumentCaptor.forClass(Venta.class);
        verify(ventaRepo).save(captor.capture());
        assertThat(captor.getValue().getCotizacionExpiraEn())
                .isEqualTo(LocalDate.now().plusDays(15));
    }

    @Test
    @DisplayName("guardar — COTIZACION con fecha explícita usa esa fecha")
    void guardar_cotizacion_usaFechaExplicita() {
        LocalDate fechaExplicita = LocalDate.now().plusDays(30);
        VentaFormDto dto = new VentaFormDto();
        dto.setCliente("Cliente");
        dto.setEstado(EstadoVenta.COTIZACION);
        dto.setCotizacionExpiraEn(fechaExplicita);
        VentaItemFormDto item = new VentaItemFormDto();
        item.setCantidad(BigDecimal.ONE);
        item.setPrecioUnitario(BigDecimal.ONE);
        item.setDescuentoPct(BigDecimal.ZERO);
        dto.getItems().add(item);

        Venta saved = new Venta();
        saved.setEstado(EstadoVenta.COTIZACION);
        when(ventaRepo.save(any())).thenReturn(saved);

        service.guardar(dto);

        ArgumentCaptor<Venta> captor = ArgumentCaptor.forClass(Venta.class);
        verify(ventaRepo).save(captor.capture());
        assertThat(captor.getValue().getCotizacionExpiraEn()).isEqualTo(fechaExplicita);
    }

    @Test
    @DisplayName("guardar — PENDIENTE no setea cotizacionExpiraEn")
    void guardar_pendiente_noSetaFechaExpiracion() {
        VentaFormDto dto = new VentaFormDto();
        dto.setCliente("Cliente");
        dto.setEstado(EstadoVenta.PENDIENTE);
        VentaItemFormDto item = new VentaItemFormDto();
        item.setCantidad(BigDecimal.ONE);
        item.setPrecioUnitario(BigDecimal.ONE);
        item.setDescuentoPct(BigDecimal.ZERO);
        dto.getItems().add(item);

        Venta saved = new Venta();
        saved.setEstado(EstadoVenta.PENDIENTE);
        when(ventaRepo.save(any())).thenReturn(saved);

        service.guardar(dto);

        ArgumentCaptor<Venta> captor = ArgumentCaptor.forClass(Venta.class);
        verify(ventaRepo).save(captor.capture());
        assertThat(captor.getValue().getCotizacionExpiraEn()).isNull();
    }
}
