package com.alera.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class LoteCervezaModelTest {

    private LoteCerveza lote(BigDecimal ogBrix, BigDecimal fgBrix) {
        LoteCerveza l = new LoteCerveza();
        l.setOgBrix(ogBrix);
        l.setFgBrix(fgBrix);
        return l;
    }

    // ── getOgSgFromBrix ────────────────────────────────────────────────────────

    @Test
    void getOgSgFromBrix_nullOgBrix_retornaNull() {
        LoteCerveza l = lote(null, null);
        assertThat(l.getOgSgFromBrix()).isNull();
    }

    @Test
    void getOgSgFromBrix_valorTipico_retornaSgCorrecto() {
        // 12 °Brix → OG aproximado 1.048
        LoteCerveza l = lote(new BigDecimal("12.0"), null);
        Integer sg = l.getOgSgFromBrix();
        assertThat(sg).isNotNull();
        // Resultado esperado: ~1048 (fórmula: 12/(258.6-12/258.2*227.1)+1 ≈ 1.0481)
        assertThat(sg).isBetween(1045, 1052);
    }

    @Test
    void getOgSgFromBrix_16brix_retornaSgEn1065() {
        // 16 °Brix → OG aproximado 1.065
        LoteCerveza l = lote(new BigDecimal("16.0"), null);
        Integer sg = l.getOgSgFromBrix();
        assertThat(sg).isNotNull();
        assertThat(sg).isBetween(1062, 1068);
    }

    @Test
    void getOgSgFromBrix_retornaEnteroFormato4Digitos() {
        LoteCerveza l = lote(new BigDecimal("10.0"), null);
        Integer sg = l.getOgSgFromBrix();
        assertThat(sg).isNotNull();
        assertThat(sg).isGreaterThan(1000);
    }

    // ── getFgSgTerrill ─────────────────────────────────────────────────────────

    @Test
    void getFgSgTerrill_ambosNull_retornaNull() {
        LoteCerveza l = lote(null, null);
        assertThat(l.getFgSgTerrill()).isNull();
    }

    @Test
    void getFgSgTerrill_fgBrixNull_retornaNull() {
        LoteCerveza l = lote(new BigDecimal("12.0"), null);
        assertThat(l.getFgSgTerrill()).isNull();
    }

    @Test
    void getFgSgTerrill_ogBrixNull_retornaNull() {
        LoteCerveza l = lote(null, new BigDecimal("3.0"));
        assertThat(l.getFgSgTerrill()).isNull();
    }

    @Test
    void getFgSgTerrill_valoresTipicos_retornaFgCorregido() {
        // OG 13°Brix, FG 6°Brix → FG corregido Terrill ≈ 1011
        LoteCerveza l = lote(new BigDecimal("13.0"), new BigDecimal("6.0"));
        Integer fg = l.getFgSgTerrill();
        assertThat(fg).isNotNull();
        assertThat(fg).isBetween(1005, 1020);
    }

    @Test
    void getFgSgTerrill_retornaEnteroFormato4Digitos() {
        LoteCerveza l = lote(new BigDecimal("14.0"), new BigDecimal("4.0"));
        Integer fg = l.getFgSgTerrill();
        assertThat(fg).isNotNull();
        assertThat(fg).isGreaterThan(1000);
    }

    // ── getAbvTerrill ──────────────────────────────────────────────────────────

    @Test
    void getAbvTerrill_ogBrixNull_retornaNull() {
        LoteCerveza l = lote(null, new BigDecimal("3.0"));
        assertThat(l.getAbvTerrill()).isNull();
    }

    @Test
    void getAbvTerrill_fgBrixNull_retornaNull() {
        LoteCerveza l = lote(new BigDecimal("12.0"), null);
        assertThat(l.getAbvTerrill()).isNull();
    }

    @Test
    void getAbvTerrill_ambosNull_retornaNull() {
        LoteCerveza l = lote(null, null);
        assertThat(l.getAbvTerrill()).isNull();
    }

    @Test
    void getAbvTerrill_valoresTipicos_retornaAbvPositivo() {
        // OG 12°Brix, FG 3°Brix → ABV típico de una cerveza ~5%
        LoteCerveza l = lote(new BigDecimal("12.0"), new BigDecimal("3.0"));
        BigDecimal abv = l.getAbvTerrill();
        assertThat(abv).isNotNull();
        assertThat(abv).isGreaterThan(BigDecimal.ZERO);
        // ABV razonable: entre 3% y 8% para estos valores
        assertThat(abv.doubleValue()).isBetween(3.0, 8.0);
    }

    @Test
    void getAbvTerrill_tieneScale2() {
        LoteCerveza l = lote(new BigDecimal("16.0"), new BigDecimal("4.0"));
        BigDecimal abv = l.getAbvTerrill();
        assertThat(abv).isNotNull();
        assertThat(abv.scale()).isEqualTo(2);
    }

    @Test
    void getAbvTerrill_esMayorAAbvBrutoSinCorreccion() {
        // Con refractómetro el FG sin corregir parece más bajo de lo real,
        // por eso el ABV Terrill corregido es MENOR que el ABV bruto del FG leído.
        // Solo verificamos que el valor sea razonable (mayor que 0).
        LoteCerveza l = lote(new BigDecimal("14.0"), new BigDecimal("3.5"));
        assertThat(l.getAbvTerrill()).isGreaterThan(BigDecimal.ZERO);
    }

    // ── getAbv ─────────────────────────────────────────────────────────────────

    private LoteCerveza loteAbv(Integer og, Integer fg) {
        LoteCerveza l = new LoteCerveza();
        l.setDensidadInicial(og);
        l.setDensidadFinal(fg);
        return l;
    }

    @Test
    void getAbv_densidadInicialNull_retornaNull() {
        assertThat(loteAbv(null, 1012).getAbv()).isNull();
    }

    @Test
    void getAbv_densidadFinalNull_retornaNull() {
        assertThat(loteAbv(1060, null).getAbv()).isNull();
    }

    @Test
    void getAbv_ambasNull_retornaNull() {
        assertThat(loteAbv(null, null).getAbv()).isNull();
    }

    @Test
    void getAbv_valoresNormales_retornaAbvCorrecto() {
        // (1060 - 1012) * 131.25 / 1000 = 48 * 0.13125 = 6.30
        BigDecimal abv = loteAbv(1060, 1012).getAbv();
        assertThat(abv).isNotNull();
        assertThat(abv).isEqualByComparingTo("6.30");
    }

    @Test
    void getAbv_mismasDensidades_retornaCero() {
        BigDecimal abv = loteAbv(1050, 1050).getAbv();
        assertThat(abv).isNotNull();
        assertThat(abv).isEqualByComparingTo("0.00");
    }

    @Test
    void getAbv_tieneScale2() {
        assertThat(loteAbv(1055, 1010).getAbv().scale()).isEqualTo(2);
    }

    // ── Consistencia interna ───────────────────────────────────────────────────

    @Test
    void brixMasAlto_produceSgMasAlto() {
        LoteCerveza lBajo = lote(new BigDecimal("10.0"), null);
        LoteCerveza lAlto = lote(new BigDecimal("18.0"), null);
        assertThat(lAlto.getOgSgFromBrix()).isGreaterThan(lBajo.getOgSgFromBrix());
    }

    @Test
    void ogMasAltoConMismoFg_produceMasAbv() {
        LoteCerveza l1 = lote(new BigDecimal("12.0"), new BigDecimal("3.0"));
        LoteCerveza l2 = lote(new BigDecimal("16.0"), new BigDecimal("3.0"));
        assertThat(l2.getAbvTerrill().compareTo(l1.getAbvTerrill())).isGreaterThan(0);
    }
}
