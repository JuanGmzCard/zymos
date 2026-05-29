package com.alera.service;

import com.alera.dto.VentaFormDto;
import com.alera.model.LoteCerveza;
import com.alera.model.Venta;
import com.alera.model.enums.EstadoVenta;
import com.alera.repository.LoteCervezaRepository;
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

    @Mock private VentaRepository         ventaRepo;
    @Mock private LoteCervezaRepository   loteRepo;

    @InjectMocks
    private VentaService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "pageSize", 15);
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
        when(ventaRepo.save(any())).thenReturn(saved);

        Venta result = service.guardar(dto);

        ArgumentCaptor<Venta> captor = ArgumentCaptor.forClass(Venta.class);
        verify(ventaRepo).save(captor.capture());
        Venta captured = captor.getValue();
        assertThat(captured.getCliente()).isEqualTo("Cervecería Prueba");
        assertThat(captured.getCantidad()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(captured.getPrecioUnitario()).isEqualByComparingTo(new BigDecimal("5000"));
        assertThat(captured.getEstado()).isEqualTo(EstadoVenta.PENDIENTE);
    }

    @Test
    @DisplayName("guardar — vincula lote cuando loteId existe")
    void guardar_vinculaLote() {
        LoteCerveza lote = new LoteCerveza();
        lote.setCodigoLote("IPA-001");
        when(loteRepo.findById(1L)).thenReturn(Optional.of(lote));
        when(ventaRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

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
        when(ventaRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
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
        when(ventaRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
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
    @DisplayName("actualizar — lanza excepcion si no existe")
    void actualizar_noExiste_lanzaExcepcion() {
        when(ventaRepo.findById(99L)).thenReturn(Optional.empty());
        VentaFormDto dto = buildDto("x", BigDecimal.ONE, BigDecimal.ONE);
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> service.actualizar(99L, dto));
    }

    // ── eliminar ──────────────────────────────────────────────────────

    @Test
    @DisplayName("eliminar — llama deleteById")
    void eliminar_llamaDeleteById() {
        service.eliminar(1L);
        verify(ventaRepo).deleteById(1L);
    }

    // ── cambiarEstado ─────────────────────────────────────────────────

    @Test
    @DisplayName("cambiarEstado — actualiza y persiste estado")
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
