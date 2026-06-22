package com.alera.service;

import com.alera.dto.FacturaFormDto;
import com.alera.dto.FacturaItemDto;
import com.alera.model.FacturaProveedor;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
        when(repo.save(any())).thenAnswer(inv -> {
            FacturaProveedor f = inv.getArgument(0);
            return f;
        });
        when(historialRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
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
}
