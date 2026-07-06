package com.alera.service;

import com.alera.model.InsumoInventario;
import com.alera.model.MovimientoInventario;
import com.alera.model.enums.TipoMovimiento;
import com.alera.repository.InsumoInventarioRepository;
import com.alera.repository.MovimientoInventarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InsumoInventarioService")
class InsumoInventarioServiceTest {

    @Mock
    private InsumoInventarioRepository repo;

    @Mock
    private MovimientoInventarioRepository movimientoRepo;

    @InjectMocks
    private InsumoInventarioService service;

    private InsumoInventario insumoConStock;

    @BeforeEach
    void setUp() {
        insumoConStock = new InsumoInventario();
        insumoConStock.setNombre("Swaen Ale");
        insumoConStock.setTipo("Malta");
        insumoConStock.setCantidad(new BigDecimal("5000"));
        insumoConStock.setUnidad("gr");
        insumoConStock.setStockMinimo(new BigDecimal("500"));
        lenient().when(movimientoRepo.save(any(MovimientoInventario.class))).thenReturn(new MovimientoInventario());
    }

    // ── parsearCantidad ─────────────────────────────────────────────

    @Test
    @DisplayName("parsearCantidad extrae número de '500 gr'")
    void parsearCantidad_conUnidad() {
        assertThat(service.parsearCantidad("500 gr"))
                .isEqualByComparingTo(new BigDecimal("500"));
    }

    @Test
    @DisplayName("parsearCantidad maneja solo número")
    void parsearCantidad_soloNumero() {
        assertThat(service.parsearCantidad("1000"))
                .isEqualByComparingTo(new BigDecimal("1000"));
    }

    @Test
    @DisplayName("parsearCantidad devuelve 0 para texto no numérico")
    void parsearCantidad_textoInvalido() {
        assertThat(service.parsearCantidad("abc")).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("parsearCantidad devuelve 0 para null")
    void parsearCantidad_null() {
        assertThat(service.parsearCantidad(null)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("parsearCantidad acepta coma decimal '1,5'")
    void parsearCantidad_comaDecimal() {
        assertThat(service.parsearCantidad("1,5 kg"))
                .isEqualByComparingTo(new BigDecimal("1.5"));
    }

    // ── descontarIngrediente ────────────────────────────────────────

    @Test
    @DisplayName("descontarIngrediente con stock suficiente retorna null y descuenta")
    void descontarIngrediente_stockSuficiente() {
        when(repo.findByNombreExacto("Swaen Ale")).thenReturn(List.of(insumoConStock));

        String advertencia = service.descontarIngrediente("Swaen Ale", "3000 gr");

        assertThat(advertencia).isNull();
        assertThat(insumoConStock.getCantidad()).isEqualByComparingTo(new BigDecimal("2000"));
        verify(repo).save(insumoConStock);
    }

    @Test
    @DisplayName("descontarIngrediente con stock insuficiente retorna nombre del insumo")
    void descontarIngrediente_stockInsuficiente() {
        when(repo.findByNombreExacto("Swaen Ale")).thenReturn(List.of(insumoConStock));

        String advertencia = service.descontarIngrediente("Swaen Ale", "9000 gr");

        assertThat(advertencia).isEqualTo("Swaen Ale");
        // Stock no puede quedar negativo
        assertThat(insumoConStock.getCantidad()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(repo).save(insumoConStock);
    }

    @Test
    @DisplayName("descontarIngrediente ignora insumo no registrado en inventario")
    void descontarIngrediente_noExiste() {
        when(repo.findByNombreExacto("Ingrediente Desconocido")).thenReturn(List.of());

        String advertencia = service.descontarIngrediente("Ingrediente Desconocido", "100 gr");

        assertThat(advertencia).isNull();
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("descontarIngrediente ignora nombre null o vacío")
    void descontarIngrediente_nombreVacio() {
        String advertencia = service.descontarIngrediente("", "100 gr");

        assertThat(advertencia).isNull();
        verify(repo, never()).findByNombreExacto(any());
    }

    // ── restaurarIngrediente ────────────────────────────────────────

    @Test
    @DisplayName("restaurarIngrediente suma cantidad al stock existente")
    void restaurarIngrediente_suma() {
        insumoConStock.setCantidad(new BigDecimal("2000"));
        when(repo.findByNombreExacto("Swaen Ale")).thenReturn(List.of(insumoConStock));

        service.restaurarIngrediente("Swaen Ale", "3000 gr");

        assertThat(insumoConStock.getCantidad()).isEqualByComparingTo(new BigDecimal("5000"));
        verify(repo).save(insumoConStock);
    }

    // ── detectarTipo ────────────────────────────────────────────────

    @Test
    @DisplayName("detectarTipo identifica malta correctamente")
    void detectarTipo_malta() {
        assertThat(service.detectarTipo("Malta Pilsen")).isEqualTo("Malta");
        assertThat(service.detectarTipo("Swaen Malt")).isEqualTo("Malta");
    }

    @Test
    @DisplayName("detectarTipo identifica lúpulo correctamente")
    void detectarTipo_lupulo() {
        assertThat(service.detectarTipo("Lupulo Citra")).isEqualTo("Lúpulo");
        assertThat(service.detectarTipo("Warrior Hop")).isEqualTo("Lúpulo");
    }

    @Test
    @DisplayName("detectarTipo retorna Otro para nombre desconocido")
    void detectarTipo_desconocido() {
        assertThat(service.detectarTipo("Producto X")).isEqualTo("Otro");
    }

    // ── ajustar ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("ajustar ENTRADA: suma cantidad al stock actual")
    void ajustar_entrada_sumaAlStock() {
        insumoConStock.setId(1L);
        when(repo.findById(1L)).thenReturn(Optional.of(insumoConStock));

        service.ajustar(1L, TipoMovimiento.ENTRADA, new BigDecimal("200"), "Compra directa");

        assertThat(insumoConStock.getCantidad()).isEqualByComparingTo("5200");
        verify(repo).save(insumoConStock);
    }

    @Test
    @DisplayName("ajustar SALIDA: resta cantidad del stock, nunca negativo")
    void ajustar_salida_restaDelStock() {
        insumoConStock.setId(1L);
        when(repo.findById(1L)).thenReturn(Optional.of(insumoConStock));

        service.ajustar(1L, TipoMovimiento.SALIDA, new BigDecimal("1000"), "Merma");

        assertThat(insumoConStock.getCantidad()).isEqualByComparingTo("4000");
    }

    @Test
    @DisplayName("ajustar SALIDA mayor que stock: deja en 0, nunca negativo")
    void ajustar_salidaMayorQueStock_noBajaDeIero() {
        insumoConStock.setId(1L);
        when(repo.findById(1L)).thenReturn(Optional.of(insumoConStock));

        service.ajustar(1L, TipoMovimiento.SALIDA, new BigDecimal("9999"), "Merma excesiva");

        assertThat(insumoConStock.getCantidad()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("ajustar AJUSTE (inventario físico): reemplaza el stock con el valor exacto")
    void ajustar_ajusteManual_reemplazaStock() {
        insumoConStock.setId(1L);
        when(repo.findById(1L)).thenReturn(Optional.of(insumoConStock));

        service.ajustar(1L, TipoMovimiento.AJUSTE, new BigDecimal("3500"), "Inventario físico");

        assertThat(insumoConStock.getCantidad()).isEqualByComparingTo("3500");
    }

    @Test
    @DisplayName("ajustar insumo no encontrado: lanza RuntimeException con el ID")
    void ajustar_insumoNoEncontrado_lanzaExcepcion() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ajustar(99L, TipoMovimiento.ENTRADA, BigDecimal.TEN, "test"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    // ── ingresarDeFactura ────────────────────────────────────────────────────

    @Test
    @DisplayName("ingresarDeFactura — insumo existente: suma cantidad al stock actual")
    void ingresarDeFactura_insumoExistente_sumaAlStock() {
        when(repo.findByNombreExacto("Swaen Ale")).thenReturn(List.of(insumoConStock));

        service.ingresarDeFactura("Swaen Ale", new BigDecimal("500"), "gr",
                "Malta", "Proveedor SA", "FAC-001");

        assertThat(insumoConStock.getCantidad()).isEqualByComparingTo("5500");
        verify(repo).save(insumoConStock);
    }

    @Test
    @DisplayName("ingresarDeFactura — insumo nuevo: crea el insumo con el stock recibido")
    void ingresarDeFactura_insumoNuevo_creaInsumoConStockRecibido() {
        InsumoInventario insumoCreado = new InsumoInventario();
        insumoCreado.setId(10L);
        insumoCreado.setNombre("Malta Vienna");
        insumoCreado.setCantidad(BigDecimal.ZERO);
        insumoCreado.setUnidad("gr");
        when(repo.findByNombreExacto("Malta Vienna")).thenReturn(List.of());
        when(repo.save(any())).thenReturn(insumoCreado);

        service.ingresarDeFactura("Malta Vienna", new BigDecimal("200"), "gr",
                "Malta", "Proveedor SA", "FAC-001");

        assertThat(insumoCreado.getCantidad()).isEqualByComparingTo("200");
        verify(repo, atLeast(2)).save(any());
    }

    @Test
    @DisplayName("ingresarDeFactura — nombre nulo: no hace nada")
    void ingresarDeFactura_nombreNulo_noHaceNada() {
        service.ingresarDeFactura(null, new BigDecimal("10"), "kg", "Malta", "Prov", "FAC-001");
        verifyNoInteractions(repo);
    }

    @Test
    @DisplayName("ingresarDeFactura — cantidad cero: no hace nada")
    void ingresarDeFactura_cantidadCero_noHaceNada() {
        service.ingresarDeFactura("Malta Pale Ale", BigDecimal.ZERO, "kg", "Malta", "Prov", "FAC-001");
        verifyNoInteractions(repo);
    }

    // ── revertirEntradaFactura ────────────────────────────────────────────────

    @Test
    @DisplayName("revertirEntradaFactura — insumo existe: resta del stock y registra REVERSION_FACTURA")
    void revertirEntradaFactura_insumoExiste_restaDelStockYRegistraMovimiento() {
        insumoConStock.setCantidad(new BigDecimal("2000"));
        when(repo.findByNombreExacto("Swaen Ale")).thenReturn(List.of(insumoConStock));

        service.revertirEntradaFactura("Swaen Ale", new BigDecimal("500"), "FAC-001");

        assertThat(insumoConStock.getCantidad()).isEqualByComparingTo("1500");
        verify(repo).save(insumoConStock);
        verify(movimientoRepo).save(argThat(m -> m.getTipo() == TipoMovimiento.REVERSION_FACTURA));
    }

    @Test
    @DisplayName("revertirEntradaFactura — insumo no existe: no hace nada")
    void revertirEntradaFactura_insumoNoExiste_noHaceNada() {
        when(repo.findByNombreExacto("Fantasma")).thenReturn(List.of());

        service.revertirEntradaFactura("Fantasma", new BigDecimal("100"), "FAC-001");

        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("revertirEntradaFactura — cantidad mayor que stock: deja en 0, nunca negativo")
    void revertirEntradaFactura_cantidadMayorQueStock_noBajaDeIero() {
        insumoConStock.setCantidad(new BigDecimal("50"));
        when(repo.findByNombreExacto("Swaen Ale")).thenReturn(List.of(insumoConStock));

        service.revertirEntradaFactura("Swaen Ale", new BigDecimal("200"), "FAC-001");

        assertThat(insumoConStock.getCantidad()).isEqualByComparingTo("0");
    }
}
