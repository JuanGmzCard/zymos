package com.alera.service;

import com.alera.config.UnidadUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UnidadUtils — conversión centralizada de unidades")
class UnidadUtilsTest {

    @ParameterizedTest(name = "{0} {1} → {2} {3}")
    @CsvSource({
        "2,     kg,    2000,   gr",
        "1,     L,     1000,   mL",
        "1,     lt,    1000,   mL",
        "1,     gal,   3785.41, mL",
        "500,   gr,    500,    gr",
        "250,   mL,    250,    mL",
    })
    @DisplayName("convertirAUnidadBase convierte correctamente")
    void convertirAUnidadBase(String valor, String unidad, String esperado, String unidadEsperada) {
        BigDecimal resultado = UnidadUtils.convertirAUnidadBase(new BigDecimal(valor), unidad);
        assertThat(resultado).isEqualByComparingTo(new BigDecimal(esperado));
        assertThat(UnidadUtils.unidadBase(unidad)).isEqualTo(unidadEsperada);
    }

    @Test
    @DisplayName("normalizarParaAlmacenamiento formatea '2 kg' → '2000 gr'")
    void normalizarParaAlmacenamiento_kg() {
        assertThat(UnidadUtils.normalizarParaAlmacenamiento("2", "kg")).isEqualTo("2000 gr");
    }

    @Test
    @DisplayName("normalizarParaAlmacenamiento formatea '1 L' → '1000 mL'")
    void normalizarParaAlmacenamiento_litros() {
        assertThat(UnidadUtils.normalizarParaAlmacenamiento("1", "L")).isEqualTo("1000 mL");
    }

    @Test
    @DisplayName("normalizarParaAlmacenamiento maneja null como '0 gr'")
    void normalizarParaAlmacenamiento_null() {
        assertThat(UnidadUtils.normalizarParaAlmacenamiento(null, "gr")).isEqualTo("0 gr");
        assertThat(UnidadUtils.normalizarParaAlmacenamiento("", "gr")).isEqualTo("0 gr");
    }

    @Test
    @DisplayName("parsearYConvertir extrae y convierte '2 kg' → 2000")
    void parsearYConvertir_kgAGr() {
        assertThat(UnidadUtils.parsearYConvertir("2 kg"))
                .isEqualByComparingTo(new BigDecimal("2000"));
    }

    @Test
    @DisplayName("parsearYConvertir maneja solo número sin unidad como gramos")
    void parsearYConvertir_soloNumero() {
        assertThat(UnidadUtils.parsearYConvertir("500"))
                .isEqualByComparingTo(new BigDecimal("500"));
    }
}
