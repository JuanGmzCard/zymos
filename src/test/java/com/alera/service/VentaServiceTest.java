package com.alera.service;

import com.alera.dto.VentaFormDto;
import com.alera.model.LoteCerveza;
import com.alera.model.Venta;
import com.alera.model.VentaHistorialEstado;
import com.alera.model.enums.EstadoVenta;
import com.alera.repository.LoteCervezaRepository;
import com.alera.repository.VentaHistorialEstadoRepository;
import com.alera.repository.VentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VentaService")
class VentaServiceTest {

    @Mock private VentaRepository                  ventaRepo;
    @Mock private LoteCervezaRepository            loteRepo;
    @Mock private VentaHistorialEstadoRepository   historialRepo;
    @Mock private NotificacionService              notificacionService;

    @InjectMocks
    private VentaService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "pageSize", 15);
        lenient().when(historialRepo.save(any())).thenReturn(new VentaHistorialEstado());
    }

    private VentaFormDto buildDto(String cliente, BigDecimal cantidad, BigDecimal precio) {
        VentaFormDto dto = new VentaFormDto();
        dto.setCliente(cliente);
        dto.setFechaDespacho(LocalDate.now());
        dto.setCantidad(cantidad);
        dto.setUnidad("L");
        dto.setPrecioUnitario(precio);
        dto.setDescuentoPct(BigDecimal.ZERO);
        dto.setEstado(EstadoVenta.PENDIENTE);
        return dto;
    }

    // ── guardar ───────────────────────────────────────────────────────

    @Test
    @DisplayName("guardar — persiste venta con campos básicos")
    void guardar_persisteVenta() {
        VentaFormDto dto = buildDto("Cervecería Prueba", BigDecimal.TEN, new BigDecimal("5000"));
        Venta saved = new Venta();
        saved.setCliente("Cervecería Prueba");
        saved.setEstado(EstadoVenta.PENDIENTE);
        when(ventaRepo.save(any())).thenReturn(saved);

        service.guardar(dto);

        ArgumentCaptor<Venta> captor = ArgumentCaptor.forClass(Venta.class);
        verify(ventaRepo).save(captor.capture());
        Venta captured = captor.getValue();
        assertThat(captured.getCliente()).isEqualTo("Cervecería Prueba");
        assertThat(captured.getCantidad()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(captured.getPrecioUnitario()).isEqualByComparingTo(new BigDecimal("5000"));
        assertThat(captured.getEstado()).isEqualTo(EstadoVenta.PENDIENTE);
    }

    @Test
    @DisplayName("guardar — registra historial con estadoAnterior null")
    void guardar_registraHistorial() {
        VentaFormDto dto = buildDto("Cliente", BigDecimal.ONE, BigDecimal.ONE);
        Venta saved = new Venta();
        saved.setEstado(EstadoVenta.PENDIENTE);
        when(ventaRepo.save(any())).thenReturn(saved);

        service.guardar(dto);

        ArgumentCaptor<VentaHistorialEstado> captor = ArgumentCaptor.forClass(VentaHistorialEstado.class);
        verify(historialRepo).save(captor.capture());
        assertThat(captor.getValue().getEstadoAnterior()).isNull();
        assertThat(captor.getValue().getEstadoNuevo()).isEqualTo(EstadoVenta.PENDIENTE);
    }

    @Test
    @DisplayName("guardar — vincula lote cuando loteId existe")
    void guardar_vinculaLote() {
        LoteCerveza lote = new LoteCerveza();
        lote.setCodigoLote("IPA-001");
        when(loteRepo.findById(1L)).thenReturn(Optional.of(lote));
        Venta saved = new Venta();
        saved.setEstado(EstadoVenta.PENDIENTE);
        when(ventaRepo.save(any())).thenReturn(saved);

        VentaFormDto dto = buildDto("Cliente", BigDecimal.ONE, BigDecimal.ONE);
        dto.setLoteId(1L);
        service.guardar(dto);

        ArgumentCaptor<Venta> captor = ArgumentCaptor.forClass(Venta.class);
        verify(ventaRepo).save(captor.capture());
        assertThat(captor.getValue().getLote()).isEqualTo(lote);
        assertThat(captor.getValue().getCodigoLote()).isEqualTo("IPA-001");
    }

    @Test
    @DisplayName("guardar — loteId null deja lote en null")
    void guardar_sinLote() {
        Venta saved = new Venta();
        saved.setEstado(EstadoVenta.PENDIENTE);
        when(ventaRepo.save(any())).thenReturn(saved);

        VentaFormDto dto = buildDto("Cliente", BigDecimal.ONE, BigDecimal.ONE);
        service.guardar(dto);

        ArgumentCaptor<Venta> captor = ArgumentCaptor.forClass(Venta.class);
        verify(ventaRepo).save(captor.capture());
        assertThat(captor.getValue().getLote()).isNull();
        assertThat(captor.getValue().getCodigoLote()).isNull();
    }

    @Test
    @DisplayName("guardar — estado null usa PENDIENTE como default")
    void guardar_estadoNullDefaultPendiente() {
        Venta saved = new Venta();
        saved.setEstado(EstadoVenta.PENDIENTE);
        when(ventaRepo.save(any())).thenReturn(saved);

        VentaFormDto dto = buildDto("Cliente", BigDecimal.ONE, BigDecimal.ONE);
        dto.setEstado(null);
        service.guardar(dto);

        ArgumentCaptor<Venta> captor = ArgumentCaptor.forClass(Venta.class);
        verify(ventaRepo).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoVenta.PENDIENTE);
    }

    // ── actualizar ────────────────────────────────────────────────────

    @Test
    @DisplayName("actualizar — modifica campos y persiste")
    void actualizar_modificaCampos() {
        Venta existente = new Venta();
        existente.setCliente("Anterior");
        existente.setEstado(EstadoVenta.PENDIENTE);
        when(ventaRepo.findById(1L)).thenReturn(Optional.of(existente));
        when(ventaRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VentaFormDto dto = buildDto("Nuevo Cliente", new BigDecimal("50"), new BigDecimal("3000"));
        service.actualizar(1L, dto);

        ArgumentCaptor<Venta> captor = ArgumentCaptor.forClass(Venta.class);
        verify(ventaRepo).save(captor.capture());
        assertThat(captor.getValue().getCliente()).isEqualTo("Nuevo Cliente");
    }

    @Test
    @DisplayName("actualizar — registra historial cuando cambia el estado")
    void actualizar_registraHistorialAlCambiarEstado() {
        Venta existente = new Venta();
        existente.setEstado(EstadoVenta.PENDIENTE);
        when(ventaRepo.findById(1L)).thenReturn(Optional.of(existente));
        when(ventaRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VentaFormDto dto = buildDto("Cliente", BigDecimal.ONE, BigDecimal.ONE);
        dto.setEstado(EstadoVenta.DESPACHADO);
        service.actualizar(1L, dto);

        ArgumentCaptor<VentaHistorialEstado> captor = ArgumentCaptor.forClass(VentaHistorialEstado.class);
        verify(historialRepo).save(captor.capture());
        assertThat(captor.getValue().getEstadoAnterior()).isEqualTo(EstadoVenta.PENDIENTE);
        assertThat(captor.getValue().getEstadoNuevo()).isEqualTo(EstadoVenta.DESPACHADO);
    }

    @Test
    @DisplayName("actualizar — no registra historial si el estado no cambia")
    void actualizar_noRegistraHistorialSinCambioEstado() {
        Venta existente = new Venta();
        existente.setEstado(EstadoVenta.PENDIENTE);
        when(ventaRepo.findById(1L)).thenReturn(Optional.of(existente));
        when(ventaRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VentaFormDto dto = buildDto("Cliente", BigDecimal.ONE, BigDecimal.ONE);
        dto.setEstado(EstadoVenta.PENDIENTE);
        service.actualizar(1L, dto);

        verify(historialRepo, never()).save(any());
    }

    @Test
    @DisplayName("actualizar — lanza excepcion si no existe")
    void actualizar_noExiste_lanzaExcepcion() {
        when(ventaRepo.findById(99L)).thenReturn(Optional.empty());
        VentaFormDto dto = buildDto("x", BigDecimal.ONE, BigDecimal.ONE);
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> service.actualizar(99L, dto));
    }

    // ── eliminar (soft delete) ────────────────────────────────────────

    @Test
    @DisplayName("eliminar — soft delete: setea deletedAt, no borra físicamente")
    void eliminar_softDelete() {
        Venta v = new Venta();
        v.setEstado(EstadoVenta.PENDIENTE);
        when(ventaRepo.findById(1L)).thenReturn(Optional.of(v));
        when(ventaRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.eliminar(1L);

        verify(ventaRepo, never()).deleteById(any());
        ArgumentCaptor<Venta> captor = ArgumentCaptor.forClass(Venta.class);
        verify(ventaRepo).save(captor.capture());
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("eliminar — no-op si no existe")
    void eliminar_noExiste_noOp() {
        when(ventaRepo.findById(99L)).thenReturn(Optional.empty());
        service.eliminar(99L);
        verify(ventaRepo, never()).save(any());
        verify(ventaRepo, never()).deleteById(any());
    }

    // ── cambiarEstado ─────────────────────────────────────────────────

    @Test
    @DisplayName("cambiarEstado — actualiza estado y persiste")
    void cambiarEstado_actualizaEstado() {
        Venta v = new Venta();
        v.setEstado(EstadoVenta.PENDIENTE);
        when(ventaRepo.findById(1L)).thenReturn(Optional.of(v));
        when(ventaRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.cambiarEstado(1L, EstadoVenta.DESPACHADO);

        ArgumentCaptor<Venta> captor = ArgumentCaptor.forClass(Venta.class);
        verify(ventaRepo).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoVenta.DESPACHADO);
    }

    @Test
    @DisplayName("cambiarEstado — registra historial de transición")
    void cambiarEstado_registraHistorial() {
        Venta v = new Venta();
        v.setEstado(EstadoVenta.PENDIENTE);
        when(ventaRepo.findById(1L)).thenReturn(Optional.of(v));
        when(ventaRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.cambiarEstado(1L, EstadoVenta.CANCELADO);

        ArgumentCaptor<VentaHistorialEstado> captor = ArgumentCaptor.forClass(VentaHistorialEstado.class);
        verify(historialRepo).save(captor.capture());
        assertThat(captor.getValue().getEstadoAnterior()).isEqualTo(EstadoVenta.PENDIENTE);
        assertThat(captor.getValue().getEstadoNuevo()).isEqualTo(EstadoVenta.CANCELADO);
    }

    @Test
    @DisplayName("cambiarEstado — crea notificacion al despachar")
    void cambiarEstado_despachado_creaNotificacion() {
        Venta v = new Venta();
        v.setEstado(EstadoVenta.PENDIENTE);
        v.setCliente("Bar La Espuma");
        v.setCodigoLote("IPA-001");
        v.setCantidad(new BigDecimal("20"));
        v.setUnidad("L");
        when(ventaRepo.findById(1L)).thenReturn(Optional.of(v));
        when(ventaRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.cambiarEstado(1L, EstadoVenta.DESPACHADO);

        verify(notificacionService).crear(any(), contains("Bar La Espuma"), any(), any());
    }

    // ── validarCantidadDisponible ─────────────────────────────────────

    @Test
    @DisplayName("validarCantidadDisponible — retorna null si no hay lote")
    void validarCantidad_sinLote_retornaNull() {
        assertThat(service.validarCantidadDisponible(null, BigDecimal.TEN, null)).isNull();
    }

    @Test
    @DisplayName("validarCantidadDisponible — retorna null si no supera litros")
    void validarCantidad_dentroDelLimite_retornaNull() {
        LoteCerveza lote = new LoteCerveza();
        lote.setCodigoLote("IPA-001");
        lote.setLitrosFinales(new BigDecimal("100"));
        when(loteRepo.findById(1L)).thenReturn(Optional.of(lote));
        when(ventaRepo.sumCantidadActivaByLote(1L, null)).thenReturn(new BigDecimal("50"));

        assertThat(service.validarCantidadDisponible(1L, new BigDecimal("30"), null)).isNull();
    }

    @Test
    @DisplayName("validarCantidadDisponible — retorna advertencia al superar litros")
    void validarCantidad_superaLimite_retornaAdvertencia() {
        LoteCerveza lote = new LoteCerveza();
        lote.setCodigoLote("IPA-001");
        lote.setLitrosFinales(new BigDecimal("100"));
        when(loteRepo.findById(1L)).thenReturn(Optional.of(lote));
        when(ventaRepo.sumCantidadActivaByLote(1L, null)).thenReturn(new BigDecimal("80"));

        String advertencia = service.validarCantidadDisponible(1L, new BigDecimal("30"), null);

        assertThat(advertencia).isNotNull();
        assertThat(advertencia).contains("IPA-001");
    }

    // ── listarHistorial ───────────────────────────────────────────────

    @Test
    @DisplayName("listarHistorial — delega a repo ordenado por fecha desc")
    void listarHistorial_delegaARepo() {
        var h1 = new VentaHistorialEstado();
        when(historialRepo.findByVentaIdOrderByFechaDesc(5L)).thenReturn(List.of(h1));

        var result = service.listarHistorial(5L);

        assertThat(result).hasSize(1);
        verify(historialRepo).findByVentaIdOrderByFechaDesc(5L);
    }

    // ── suggest ───────────────────────────────────────────────────────

    @Test
    @DisplayName("suggest — query corta retorna lista vacía")
    void suggest_queryCorta_retornaVacio() {
        assertThat(service.suggest("a")).isEmpty();
        assertThat(service.suggest(null)).isEmpty();
        assertThat(service.suggest("  ")).isEmpty();
    }

    @Test
    @DisplayName("suggest — retorna estructura correcta con cliente y url")
    void suggest_retornaEstructura() {
        Venta v = new Venta();
        v.setCliente("Distribuidora Norte");
        v.setCodigoLote("IPA-001");
        v.setFechaDespacho(LocalDate.of(2025, 6, 1));
        v.setId(5L);

        when(ventaRepo.search(anyString(), any(Pageable.class)))
                .thenReturn(List.of(v));

        List<Map<String, Object>> result = service.suggest("Distribu");
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsKey("titulo");
        assertThat(result.get(0)).containsKey("url");
        assertThat(result.get(0).get("titulo")).isEqualTo("Distribuidora Norte");
        assertThat(result.get(0).get("url").toString()).contains("/ventas/ver/");
    }

    // ── stats ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("countTotal — delega a repo")
    void countTotal_delegaARepo() {
        when(ventaRepo.count()).thenReturn(42L);
        assertThat(service.countTotal()).isEqualTo(42L);
    }

    @Test
    @DisplayName("countByEstado — delega a repo")
    void countByEstado_delegaARepo() {
        when(ventaRepo.countByEstado(EstadoVenta.PENDIENTE)).thenReturn(7L);
        assertThat(service.countByEstado(EstadoVenta.PENDIENTE)).isEqualTo(7L);
    }

    @Test
    @DisplayName("sumIngresosDespachados — retorna ZERO si repo devuelve null")
    void sumIngresos_nullRetornaZero() {
        when(ventaRepo.sumIngresosDespachados()).thenReturn(null);
        assertThat(service.sumIngresosDespachados()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("listarPaginado — filtra por estado y rango de fechas")
    void listarPaginado_delegaARepo() {
        when(ventaRepo.findAllFiltered(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        var page = service.listarPaginado(EstadoVenta.PENDIENTE, LocalDate.now().minusDays(7), LocalDate.now(), 0);
        assertThat(page).isNotNull();
        verify(ventaRepo).findAllFiltered(eq(EstadoVenta.PENDIENTE), any(), any(), any());
    }
}
