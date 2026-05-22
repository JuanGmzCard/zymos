package com.alera.service;

import com.alera.model.InsumoInventario;
import com.alera.model.enums.TipoInsumo;
import com.alera.repository.InsumoInventarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InsumoInventarioService")
class InsumoInventarioServiceTest {

    @Mock
    private InsumoInventarioRepository repo;

    @InjectMocks
    private InsumoInventarioService service;

    private InsumoInventario insumoConStock;

    @BeforeEach
    void setUp() {
        insumoConStock = new InsumoInventario();
        insumoConStock.setNombre("Swaen Ale");
        insumoConStock.setTipo(TipoInsumo.MALTA);
        insumoConStock.setCantidad(new BigDecimal("5000"));
        insumoConStock.setUnidad("gr");
        insumoConStock.setStockMinimo(new BigDecimal("500"));
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
        when(repo.findByNombreExacto("Swaen Ale")).thenReturn(Optional.of(insumoConStock));

        String advertencia = service.descontarIngrediente("Swaen Ale", "3000 gr");

        assertThat(advertencia).isNull();
        assertThat(insumoConStock.getCantidad()).isEqualByComparingTo(new BigDecimal("2000"));
        verify(repo).save(insumoConStock);
    }

    @Test
    @DisplayName("descontarIngrediente con stock insuficiente retorna nombre del insumo")
    void descontarIngrediente_stockInsuficiente() {
        when(repo.findByNombreExacto("Swaen Ale")).thenReturn(Optional.of(insumoConStock));

        String advertencia = service.descontarIngrediente("Swaen Ale", "9000 gr");

        assertThat(advertencia).isEqualTo("Swaen Ale");
        // Stock no puede quedar negativo
        assertThat(insumoConStock.getCantidad()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(repo).save(insumoConStock);
    }

    @Test
    @DisplayName("descontarIngrediente ignora insumo no registrado en inventario")
    void descontarIngrediente_noExiste() {
        when(repo.findByNombreExacto("Ingrediente Desconocido")).thenReturn(Optional.empty());

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
        when(repo.findByNombreExacto("Swaen Ale")).thenReturn(Optional.of(insumoConStock));

        service.restaurarIngrediente("Swaen Ale", "3000 gr");

        assertThat(insumoConStock.getCantidad()).isEqualByComparingTo(new BigDecimal("5000"));
        verify(repo).save(insumoConStock);
    }

    // ── detectarTipo ────────────────────────────────────────────────

    @Test
    @DisplayName("detectarTipo identifica malta correctamente")
    void detectarTipo_malta() {
        assertThat(service.detectarTipo("Malta Pilsen")).isEqualTo(TipoInsumo.MALTA);
        assertThat(service.detectarTipo("Swaen Malt")).isEqualTo(TipoInsumo.MALTA);
    }

    @Test
    @DisplayName("detectarTipo identifica lúpulo correctamente")
    void detectarTipo_lupulo() {
        assertThat(service.detectarTipo("Lupulo Citra")).isEqualTo(TipoInsumo.LUPULO);
        assertThat(service.detectarTipo("Warrior Hop")).isEqualTo(TipoInsumo.LUPULO);
    }

    @Test
    @DisplayName("detectarTipo retorna OTRO para nombre desconocido")
    void detectarTipo_desconocido() {
        assertThat(service.detectarTipo("Producto X")).isEqualTo(TipoInsumo.OTRO);
    }
}
