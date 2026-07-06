package com.alera.service;

import com.alera.dto.FacturaFormDto;
import com.alera.dto.FacturaItemDto;
import com.alera.model.Equipo;
import com.alera.model.FacturaHistorialEstado;
import com.alera.model.FacturaItem;
import com.alera.model.FacturaProveedor;
import com.alera.model.enums.EstadoEquipo;
import com.alera.model.enums.EstadoFactura;
import com.alera.model.enums.TipoItemFactura;
import com.alera.repository.EquipoRepository;
import com.alera.repository.FacturaHistorialEstadoRepository;
import com.alera.repository.FacturaProveedorRepository;
import com.alera.repository.InsumoInventarioRepository;
import com.alera.repository.ProveedorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FacturaProveedorService — cálculo de totales")
class FacturaProveedorServiceTest {

    @Mock private FacturaProveedorRepository    repo;
    @Mock private FacturaHistorialEstadoRepository historialRepo;
    @Mock private InsumoInventarioRepository    insumoRepo;
    @Mock private EquipoRepository              equipoRepo;
    @Mock private InsumoInventarioService       insumoService;
    @Mock private ProveedorRepository           proveedorRepo;

    @InjectMocks
    private FacturaProveedorService service;

    private FacturaFormDto buildDto(BigDecimal cantidad, BigDecimal vUnit,
                                    BigDecimal desc, BigDecimal iva) {
        FacturaItemDto item = new FacturaItemDto();
        item.setTipoItem(TipoItemFactura.INSUMO);
        item.setNombre("Malta Test");
        item.setCantidad(cantidad);
        item.setUnidad("kg");
        item.setValorUnitario(vUnit);
        item.setPorcentajeDescuento(desc);
        item.setPorcentajeIvaItem(iva);

        FacturaFormDto dto = new FacturaFormDto();
        dto.setProveedor("Proveedor Test");
        dto.setCostoEnvio(BigDecimal.ZERO);
        dto.setItems(List.of(item));
        return dto;
    }

    @BeforeEach
    void setUp() {
        lenient().when(repo.save(any())).thenAnswer(inv -> {
            FacturaProveedor f = inv.getArgument(0);
            return f;
        });
        lenient().when(historialRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("calcularTotales sin descuento ni IVA: total = cantidad × vUnit")
    void calcularTotales_sinDescuentoNiIva() {
        FacturaFormDto dto = buildDto(
                new BigDecimal("10"),
                new BigDecimal("1000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO);

        FacturaProveedor factura = service.guardar(dto);

        assertThat(factura.getSubtotal()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(factura.getValorIva()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(factura.getValorTotal()).isEqualByComparingTo(new BigDecimal("10000"));
    }

    @Test
    @DisplayName("calcularTotales con IVA 19%: subtotal es base, valorIva es 19% de la base")
    void calcularTotales_conIva19() {
        FacturaFormDto dto = buildDto(
                new BigDecimal("1"),
                new BigDecimal("10000"),
                BigDecimal.ZERO,
                new BigDecimal("19"));

        FacturaProveedor factura = service.guardar(dto);

        assertThat(factura.getSubtotal()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(factura.getValorIva()).isEqualByComparingTo(new BigDecimal("1900"));
        assertThat(factura.getValorTotal()).isEqualByComparingTo(new BigDecimal("11900"));
    }

    @Test
    @DisplayName("calcularTotales aplica descuento antes de calcular IVA")
    void calcularTotales_descuentoAntesDeIva() {
        // 1 unidad × $10000 - 10% desc = $9000 base | IVA 19% = $1710 | total = $10710
        FacturaFormDto dto = buildDto(
                BigDecimal.ONE,
                new BigDecimal("10000"),
                new BigDecimal("10"),   // 10% descuento
                new BigDecimal("19"));  // 19% IVA

        FacturaProveedor factura = service.guardar(dto);

        assertThat(factura.getSubtotal()).isEqualByComparingTo(new BigDecimal("9000.00"));
        assertThat(factura.getValorIva()).isEqualByComparingTo(new BigDecimal("1710.00"));
        assertThat(factura.getValorTotal()).isEqualByComparingTo(new BigDecimal("10710.00"));
    }

    @Test
    @DisplayName("calcularTotales suma múltiples ítems correctamente")
    void calcularTotales_multiplesItems() {
        FacturaItemDto item1 = new FacturaItemDto();
        item1.setTipoItem(TipoItemFactura.INSUMO);
        item1.setNombre("Malta A");
        item1.setCantidad(new BigDecimal("2"));
        item1.setValorUnitario(new BigDecimal("5000"));
        item1.setPorcentajeDescuento(BigDecimal.ZERO);
        item1.setPorcentajeIvaItem(BigDecimal.ZERO);
        item1.setUnidad("kg");

        FacturaItemDto item2 = new FacturaItemDto();
        item2.setTipoItem(TipoItemFactura.INSUMO);
        item2.setNombre("Lupulo B");
        item2.setCantidad(BigDecimal.ONE);
        item2.setValorUnitario(new BigDecimal("3000"));
        item2.setPorcentajeDescuento(BigDecimal.ZERO);
        item2.setPorcentajeIvaItem(BigDecimal.ZERO);
        item2.setUnidad("gr");

        FacturaFormDto dto = new FacturaFormDto();
        dto.setProveedor("Proveedor");
        dto.setCostoEnvio(BigDecimal.ZERO);
        dto.setItems(List.of(item1, item2));

        FacturaProveedor factura = service.guardar(dto);

        // 2×5000 + 1×3000 = 13000
        assertThat(factura.getSubtotal()).isEqualByComparingTo(new BigDecimal("13000"));
        assertThat(factura.getValorTotal()).isEqualByComparingTo(new BigDecimal("13000"));
    }

    @Test
    @DisplayName("calcularTotales incluye costo de envío en el total")
    void calcularTotales_conCostoEnvio() {
        FacturaFormDto dto = buildDto(BigDecimal.ONE, new BigDecimal("10000"),
                BigDecimal.ZERO, BigDecimal.ZERO);
        dto.setCostoEnvio(new BigDecimal("500"));

        FacturaProveedor factura = service.guardar(dto);

        assertThat(factura.getValorTotal()).isEqualByComparingTo(new BigDecimal("10500"));
    }

    @Test
    @DisplayName("ivaIncluido=true: subtotal es base extraída, total = cantidad × valorUnitario")
    void calcularTotales_ivaIncluido() {
        // 1 unidad × $11900 (precio con IVA 19%) → base = $10000, IVA = $1900, total = $11900
        FacturaFormDto dto = buildDto(
                BigDecimal.ONE,
                new BigDecimal("11900"),
                BigDecimal.ZERO,
                new BigDecimal("19"));
        dto.setIvaIncluido(true);

        FacturaProveedor factura = service.guardar(dto);

        assertThat(factura.getSubtotal()).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(factura.getValorIva()).isEqualByComparingTo(new BigDecimal("1900.00"));
        assertThat(factura.getValorTotal()).isEqualByComparingTo(new BigDecimal("11900.00"));
    }

    @Test
    @DisplayName("ivaIncluido=true con descuento: descuento se aplica al precio con IVA")
    void calcularTotales_ivaIncluido_conDescuento() {
        // 1 unidad × $11900 (c/IVA 19%) - 10% desc → total = $10710, base = $9000, IVA = $1710
        FacturaFormDto dto = buildDto(
                BigDecimal.ONE,
                new BigDecimal("11900"),
                new BigDecimal("10"),
                new BigDecimal("19"));
        dto.setIvaIncluido(true);

        FacturaProveedor factura = service.guardar(dto);

        assertThat(factura.getSubtotal()).isEqualByComparingTo(new BigDecimal("9000.00"));
        assertThat(factura.getValorIva()).isEqualByComparingTo(new BigDecimal("1710.00"));
        assertThat(factura.getValorTotal()).isEqualByComparingTo(new BigDecimal("10710.00"));
    }

    // ── guardar — inventario e historial ─────────────────────────────────────

    @Test
    @DisplayName("guardar — ítem INSUMO: llama a ingresarDeFactura con cantidad normalizada")
    void guardar_itemInsumo_ingresaAlInventario() {
        FacturaFormDto dto = buildDto(new BigDecimal("10"), new BigDecimal("500"),
                BigDecimal.ZERO, BigDecimal.ZERO);
        dto.setProveedor("Proveedor SA");
        dto.getItems().get(0).setUnidad("kg");
        dto.getItems().get(0).setTipoInsumo("Malta");

        service.guardar(dto);

        // 10 kg → 10000 gr normalizados
        verify(insumoService).ingresarDeFactura(
                eq("Malta Test"),
                argThat(bd -> bd.compareTo(new BigDecimal("10000")) == 0),
                eq("gr"),
                eq("Malta"),
                eq("Proveedor SA"),
                any()
        );
    }

    @Test
    @DisplayName("guardar — registra historial con el estado inicial de la factura")
    void guardar_registraHistorialDeEstadoInicial() {
        FacturaFormDto dto = buildDto(BigDecimal.ONE, new BigDecimal("5000"),
                BigDecimal.ZERO, BigDecimal.ZERO);
        dto.setEstado(EstadoFactura.RECIBIDA);

        service.guardar(dto);

        verify(historialRepo).save(argThat(h ->
                h instanceof FacturaHistorialEstado fh
                && fh.getEstadoAnterior() == null
                && fh.getEstadoNuevo() == EstadoFactura.RECIBIDA
        ));
    }

    @Test
    @DisplayName("guardar — ítem EQUIPO nuevo: crea el equipo en el repositorio de equipos")
    void guardar_itemEquipoNuevo_creaEquipo() {
        when(equipoRepo.findByNombreIgnoreCase("Fermentador 100L")).thenReturn(Optional.empty());
        FacturaFormDto dto = new FacturaFormDto();
        dto.setProveedor("Proveedor SA");
        dto.setFechaFactura(LocalDate.of(2025, 1, 15));
        dto.setEstado(EstadoFactura.RECIBIDA);
        dto.setCostoEnvio(BigDecimal.ZERO);
        FacturaItemDto item = new FacturaItemDto();
        item.setTipoItem(TipoItemFactura.EQUIPO);
        item.setNombre("Fermentador 100L");
        item.setTipoEquipo("Fermentador");
        item.setCantidad(BigDecimal.ONE);
        item.setUnidad("und");
        item.setValorUnitario(BigDecimal.ZERO);
        item.setPorcentajeIvaItem(BigDecimal.ZERO);
        item.setPorcentajeDescuento(BigDecimal.ZERO);
        dto.getItems().add(item);

        service.guardar(dto);

        ArgumentCaptor<Equipo> captor = ArgumentCaptor.forClass(Equipo.class);
        verify(equipoRepo).save(captor.capture());
        Equipo equipoCreado = captor.getValue();
        assertThat(equipoCreado.getNombre()).isEqualTo("Fermentador 100L");
        assertThat(equipoCreado.getEstado()).isEqualTo(EstadoEquipo.OPERATIVO);
    }

    @Test
    @DisplayName("guardar — ítem EQUIPO ya existente: actualiza observaciones, no crea uno nuevo")
    void guardar_itemEquipoExistente_actualizaObservaciones() {
        Equipo existente = new Equipo();
        existente.setNombre("Fermentador 100L");
        when(equipoRepo.findByNombreIgnoreCase("Fermentador 100L")).thenReturn(Optional.of(existente));
        FacturaFormDto dto = new FacturaFormDto();
        dto.setProveedor("Proveedor SA");
        dto.setFechaFactura(LocalDate.of(2025, 1, 15));
        dto.setEstado(EstadoFactura.RECIBIDA);
        dto.setCostoEnvio(BigDecimal.ZERO);
        FacturaItemDto item = new FacturaItemDto();
        item.setTipoItem(TipoItemFactura.EQUIPO);
        item.setNombre("Fermentador 100L");
        item.setCantidad(BigDecimal.ONE);
        item.setUnidad("und");
        item.setValorUnitario(BigDecimal.ZERO);
        item.setPorcentajeIvaItem(BigDecimal.ZERO);
        item.setPorcentajeDescuento(BigDecimal.ZERO);
        dto.getItems().add(item);

        service.guardar(dto);

        verify(equipoRepo).save(argThat(e ->
                e.getObservaciones() != null && e.getObservaciones().contains("Recomprado")));
    }

    // ── actualizar ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("actualizar — revierte inventario anterior y procesa el nuevo")
    void actualizar_revierteAntesYProcesaNuevo() {
        FacturaProveedor existente = new FacturaProveedor();
        existente.setId(1L);
        existente.setNumeroFactura("FAC-001");
        existente.setProveedor("Proveedor SA");
        FacturaItem itemAnterior = new FacturaItem();
        itemAnterior.setTipoItem(TipoItemFactura.INSUMO);
        itemAnterior.setNombre("Malta Pale Ale");
        itemAnterior.setCantidad(new BigDecimal("10"));
        itemAnterior.setUnidad("kg");
        itemAnterior.setValorUnitario(BigDecimal.ZERO);
        itemAnterior.setPorcentajeIvaItem(BigDecimal.ZERO);
        itemAnterior.setPorcentajeDescuento(BigDecimal.ZERO);
        itemAnterior.setImpuestoConsumo(BigDecimal.ZERO);
        existente.getItems().add(itemAnterior);

        when(repo.findByIdWithItems(1L)).thenReturn(Optional.of(existente));

        FacturaFormDto dto = new FacturaFormDto();
        dto.setProveedor("Proveedor SA");
        dto.setFechaFactura(LocalDate.of(2025, 6, 1));
        dto.setEstado(EstadoFactura.RECIBIDA);
        dto.setCostoEnvio(BigDecimal.ZERO);
        FacturaItemDto nuevoItem = new FacturaItemDto();
        nuevoItem.setTipoItem(TipoItemFactura.INSUMO);
        nuevoItem.setNombre("Malta Vienna");
        nuevoItem.setTipoInsumo("Malta");
        nuevoItem.setCantidad(new BigDecimal("5"));
        nuevoItem.setUnidad("kg");
        nuevoItem.setValorUnitario(new BigDecimal("2000"));
        nuevoItem.setPorcentajeIvaItem(BigDecimal.ZERO);
        nuevoItem.setPorcentajeDescuento(BigDecimal.ZERO);
        dto.getItems().add(nuevoItem);

        service.actualizar(1L, dto);

        // Verifica que se revirtió el ítem anterior (10 kg = 10000 gr)
        verify(insumoService).revertirEntradaFactura(
                eq("Malta Pale Ale"),
                argThat(bd -> bd.compareTo(new BigDecimal("10000")) == 0),
                eq("FAC-001")
        );
        // Verifica que se ingresó el nuevo ítem (5 kg = 5000 gr)
        verify(insumoService).ingresarDeFactura(
                eq("Malta Vienna"),
                argThat(bd -> bd.compareTo(new BigDecimal("5000")) == 0),
                any(), any(), any(), any()
        );
    }

    @Test
    @DisplayName("actualizar — factura no encontrada: lanza RuntimeException")
    void actualizar_facturaNoEncontrada_lanzaExcepcion() {
        when(repo.findByIdWithItems(99L)).thenReturn(Optional.empty());

        FacturaFormDto dto = new FacturaFormDto();
        dto.setProveedor("Prov");
        dto.setEstado(EstadoFactura.RECIBIDA);
        dto.setCostoEnvio(BigDecimal.ZERO);

        assertThatThrownBy(() -> service.actualizar(99L, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no encontrada");
    }

    // ── eliminar ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("eliminar — factura con ítem INSUMO: revierte inventario y elimina la factura")
    void eliminar_conItemInsumo_revierteInventarioYElimina() {
        FacturaProveedor factura = new FacturaProveedor();
        factura.setId(1L);
        factura.setNumeroFactura("FAC-001");
        FacturaItem item = new FacturaItem();
        item.setTipoItem(TipoItemFactura.INSUMO);
        item.setNombre("Malta Pale Ale");
        item.setCantidad(new BigDecimal("5"));
        item.setUnidad("kg");
        factura.getItems().add(item);
        when(repo.findByIdWithItems(1L)).thenReturn(Optional.of(factura));

        service.eliminar(1L);

        // 5 kg → 5000 gr
        verify(insumoService).revertirEntradaFactura(
                eq("Malta Pale Ale"),
                argThat(bd -> bd.compareTo(new BigDecimal("5000")) == 0),
                any()
        );
        verify(repo).delete(factura);
    }

    @Test
    @DisplayName("eliminar — ítem EQUIPO: no llama a revertirEntradaFactura (solo INSUMOs se revierten)")
    void eliminar_conItemEquipo_noRevierteInventario() {
        FacturaProveedor factura = new FacturaProveedor();
        factura.setId(1L);
        factura.setNumeroFactura("FAC-001");
        FacturaItem item = new FacturaItem();
        item.setTipoItem(TipoItemFactura.EQUIPO);
        item.setNombre("Fermentador 100L");
        item.setCantidad(BigDecimal.ONE);
        item.setUnidad("und");
        factura.getItems().add(item);
        when(repo.findByIdWithItems(1L)).thenReturn(Optional.of(factura));

        service.eliminar(1L);

        verify(insumoService, never()).revertirEntradaFactura(any(), any(), any());
        verify(repo).delete(factura);
    }

    @Test
    @DisplayName("eliminar — factura no encontrada: no hace nada (silencioso)")
    void eliminar_facturaNoExiste_noHaceNada() {
        when(repo.findByIdWithItems(99L)).thenReturn(Optional.empty());

        service.eliminar(99L);

        verify(repo, never()).delete(any());
        verifyNoInteractions(insumoService);
    }

    // ── cambiarEstado ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("cambiarEstado — actualiza estado de la factura y registra historial")
    void cambiarEstado_actualizaEstadoYRegistraHistorial() {
        FacturaProveedor factura = new FacturaProveedor();
        factura.setId(1L);
        factura.setEstado(EstadoFactura.RECIBIDA);
        when(repo.findById(1L)).thenReturn(Optional.of(factura));

        service.cambiarEstado(1L, EstadoFactura.PAGADA);

        assertThat(factura.getEstado()).isEqualTo(EstadoFactura.PAGADA);
        verify(repo).save(factura);
        verify(historialRepo).save(argThat(h ->
                h instanceof FacturaHistorialEstado fh
                && fh.getEstadoAnterior() == EstadoFactura.RECIBIDA
                && fh.getEstadoNuevo()    == EstadoFactura.PAGADA
        ));
    }

    @Test
    @DisplayName("cambiarEstado — factura no encontrada: no hace nada")
    void cambiarEstado_facturaNoExiste_noHaceNada() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        service.cambiarEstado(99L, EstadoFactura.PAGADA);

        verify(repo, never()).save(any());
        verifyNoInteractions(historialRepo);
    }
}
