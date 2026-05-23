package com.alera.service;

import com.alera.model.LoteCerveza;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Smoke tests para ExcelExportService.
 * Verifican que el output es un archivo XLSX válido (magic bytes PK — formato ZIP),
 * no vacío, y que el servicio no lanza excepciones ante distintas entradas.
 * No verifican el contenido de las celdas — eso pertenece a pruebas manuales.
 */
@DisplayName("ExcelExportService")
class ExcelExportServiceTest {

    private final ExcelExportService service = new ExcelExportService();

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
                List.of(), List.of(), DESDE, HASTA, "Alera");

        assertEsExcel(resultado);
    }

    @Test
    @DisplayName("genera Excel válido con un lote mínimo")
    void generarExcel_unLoteMinimo_produceExcel() {
        byte[] resultado = service.generarExcelReporteProduccion(
                List.of(lote("IPA-001", "IPA")),
                List.of(),
                DESDE, HASTA, "Alera");

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
                lotes, List.of(), DESDE, HASTA, "Alera");

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
                resumen, DESDE, HASTA, "Mosto Cervecería");

        assertEsExcel(resultado);
    }

    @Test
    @DisplayName("lanza RuntimeException cuando las fechas desde/hasta son nulas")
    void generarExcel_fechasNulas_lanzaExcepcion() {
        // El servicio formatea las fechas directamente — null causa NPE interno
        assertThatThrownBy(() ->
                service.generarExcelReporteProduccion(
                        List.of(lote("APA-001", "APA")), List.of(), null, null, "Alera"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error generando Excel");
    }

    @Test
    @DisplayName("el Excel generado tiene un tamaño razonable (mayor a 2KB)")
    void generarExcel_tamanioRazonable() {
        List<Object[]> resumen = new java.util.ArrayList<>();
        resumen.add(resumenEstilo("IPA", 1L, 20.0));
        byte[] resultado = service.generarExcelReporteProduccion(
                List.of(loteConMetricas("IPA-001", "IPA")),
                resumen, DESDE, HASTA, "Alera");

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
                service.generarExcelReporteProduccion(lotes, List.of(), DESDE, HASTA, "Alera"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("lotes distintos generan Excels de distinto contenido")
    void generarExcel_contenidoDistinto() {
        byte[] excel1 = service.generarExcelReporteProduccion(
                List.of(lote("IPA-001", "IPA")), List.of(), DESDE, HASTA, "Alera");
        byte[] excel2 = service.generarExcelReporteProduccion(
                List.of(lote("STOUT-001", "Stout")), List.of(), DESDE, HASTA, "Alera");

        assertThat(excel1).isNotEqualTo(excel2);
    }
}
