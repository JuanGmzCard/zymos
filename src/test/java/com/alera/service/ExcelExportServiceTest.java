package com.alera.service;

import com.alera.config.ExportBranding;
import com.alera.model.LoteCerveza;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ExcelExportService")
class ExcelExportServiceTest {

    private static final MessageSource MSG;
    static {
        var ms = new ResourceBundleMessageSource();
        ms.setBasename("messages");
        ms.setDefaultEncoding("UTF-8");
        ms.setFallbackToSystemLocale(false);
        MSG = ms;
    }

    private static final Locale ES = Locale.forLanguageTag("es");
    private static final Locale EN = Locale.ENGLISH;

    private final ExcelExportService service = new ExcelExportService(MSG);
    private static final ExportBranding BRANDING = ExportBranding.defaults("Alera");

    private static final LocalDate DESDE = LocalDate.of(2025, 1, 1);
    private static final LocalDate HASTA = LocalDate.of(2025, 3, 31);

    // ── Helpers ───────────────────────────────────────────────────────

    private LoteCerveza lote(String codigo, String estilo) {
        LoteCerveza l = new LoteCerveza();
        l.setCodigoLote(codigo);
        l.setEstilo(estilo);
        l.setFechaElaboracion(LocalDate.of(2025, 2, 1));
        return l;
    }

    private LoteCerveza loteConMetricas(String codigo, String estilo) {
        LoteCerveza l = lote(codigo, estilo);
        l.setDensidadInicial(1060);
        l.setDensidadFinal(1014);
        l.setLitrosFinales(new BigDecimal("20"));
        return l;
    }

    private Object[] resumenEstilo(String estilo, long cantidad, double litros) {
        return new Object[]{estilo, cantidad, litros};
    }

    private void assertEsExcel(byte[] bytes) {
        assertThat(bytes).isNotNull().isNotEmpty();
        // XLSX es ZIP — magic bytes: PK (0x50 0x4B)
        assertThat(bytes[0]).isEqualTo((byte) 0x50); // 'P'
        assertThat(bytes[1]).isEqualTo((byte) 0x4B); // 'K'
    }

    // ── generarExcelReporteProduccion ─────────────────────────────────

    @Test
    @DisplayName("genera Excel válido con listas vacías")
    void generarExcel_listasVacias_produceExcel() {
        byte[] resultado = service.generarExcelReporteProduccion(
                List.of(), List.of(), DESDE, HASTA, BRANDING, ES);

        assertEsExcel(resultado);
    }

    @Test
    @DisplayName("genera Excel válido con un lote mínimo")
    void generarExcel_unLoteMinimo_produceExcel() {
        byte[] resultado = service.generarExcelReporteProduccion(
                List.of(lote("IPA-001", "IPA")),
                List.of(),
                DESDE, HASTA, BRANDING, ES);

        assertEsExcel(resultado);
    }

    @Test
    @DisplayName("genera Excel válido con lotes que tienen métricas completas")
    void generarExcel_lotesConMetricas_produceExcel() {
        List<LoteCerveza> lotes = List.of(
                loteConMetricas("IPA-001", "IPA"),
                loteConMetricas("STOUT-001", "Stout"),
                loteConMetricas("IPA-002", "IPA")
        );

        byte[] resultado = service.generarExcelReporteProduccion(
                lotes, List.of(), DESDE, HASTA, BRANDING, ES);

        assertEsExcel(resultado);
    }

    @Test
    @DisplayName("genera Excel válido con resumen por estilo")
    void generarExcel_conResumenEstilos_produceExcel() {
        List<Object[]> resumen = new java.util.ArrayList<>();
        resumen.add(resumenEstilo("IPA",   3L, 65.0));
        resumen.add(resumenEstilo("Stout", 1L, 20.0));

        byte[] resultado = service.generarExcelReporteProduccion(
                List.of(lote("IPA-001", "IPA")),
                resumen, DESDE, HASTA, ExportBranding.defaults("Mosto Cervecería"), ES);

        assertEsExcel(resultado);
    }

    @Test
    @DisplayName("genera Excel válido cuando las fechas desde/hasta son nulas (muestra '—')")
    void generarExcel_fechasNulas_produceExcelValido() {
        byte[] resultado = service.generarExcelReporteProduccion(
                List.of(lote("APA-001", "APA")), List.of(), null, null, BRANDING, ES);

        assertEsExcel(resultado);
    }

    @Test
    @DisplayName("el Excel generado tiene un tamaño razonable (mayor a 2KB)")
    void generarExcel_tamanioRazonable() {
        List<Object[]> resumen = new java.util.ArrayList<>();
        resumen.add(resumenEstilo("IPA", 1L, 20.0));
        byte[] resultado = service.generarExcelReporteProduccion(
                List.of(loteConMetricas("IPA-001", "IPA")),
                resumen, DESDE, HASTA, BRANDING, ES);

        assertThat(resultado.length).isGreaterThan(2048);
    }

    @Test
    @DisplayName("reporte con muchos lotes no lanza excepción")
    void generarExcel_muchoLotes_noLanzaExcepcion() {
        List<LoteCerveza> lotes = new java.util.ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            lotes.add(loteConMetricas("IPA-" + String.format("%03d", i), "IPA"));
        }

        assertThatCode(() ->
                service.generarExcelReporteProduccion(lotes, List.of(), DESDE, HASTA, BRANDING, ES))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("lotes distintos generan Excels de distinto contenido")
    void generarExcel_contenidoDistinto() {
        byte[] excel1 = service.generarExcelReporteProduccion(
                List.of(lote("IPA-001", "IPA")), List.of(), DESDE, HASTA, BRANDING, ES);
        byte[] excel2 = service.generarExcelReporteProduccion(
                List.of(lote("STOUT-001", "Stout")), List.of(), DESDE, HASTA, BRANDING, ES);

        assertThat(excel1).isNotEqualTo(excel2);
    }

    @Test
    @DisplayName("genera Excel válido en locale inglés")
    void generarExcel_localeIngles_produceExcel() {
        byte[] resultado = service.generarExcelReporteProduccion(
                List.of(loteConMetricas("IPA-001", "IPA")),
                List.of(), DESDE, HASTA, BRANDING, EN);

        assertEsExcel(resultado);
    }
}
