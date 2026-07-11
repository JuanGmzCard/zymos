package com.alera.service;

import com.alera.config.ExportBranding;
import com.alera.model.LoteCerveza;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
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

    private XSSFWorkbook openExcel(byte[] bytes) throws Exception {
        return new XSSFWorkbook(new ByteArrayInputStream(bytes));
    }

    private String cellStr(Sheet sheet, int rowIdx, int colIdx) {
        Row row = sheet.getRow(rowIdx);
        if (row == null) return "";
        Cell cell = row.getCell(colIdx);
        if (cell == null) return "";
        return cell.getCellType() == CellType.STRING ? cell.getStringCellValue() : "";
    }

    private double cellNum(Sheet sheet, int rowIdx, int colIdx) {
        Row row = sheet.getRow(rowIdx);
        if (row == null) return Double.NaN;
        Cell cell = row.getCell(colIdx);
        if (cell == null) return Double.NaN;
        return cell.getCellType() == CellType.NUMERIC ? cell.getNumericCellValue() : Double.NaN;
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

    // ── Contenido del Excel ───────────────────────────────────────────

    /*
     * Layout de construirSheetLotes (fila base 0):
     *   0: título, 1: período, 2: vacía, 3: resumen1, 4: resumen2, 5: vacía
     *   6: encabezados, 7+: datos
     */

    @Test
    @DisplayName("la hoja 1 tiene el nombre correcto según i18n en español")
    void excel_hoja1_nombreCorrecto_ES() throws Exception {
        byte[] bytes = service.generarExcelReporteProduccion(
                List.of(), List.of(), DESDE, HASTA, BRANDING, ES);
        try (XSSFWorkbook wb = openExcel(bytes)) {
            assertThat(wb.getSheetAt(0).getSheetName())
                    .isEqualTo(MSG.getMessage("xls.sheet.reporte_produccion", null, ES));
        }
    }

    @Test
    @DisplayName("la hoja 1 en inglés tiene nombre distinto al español")
    void excel_hoja1_nombreCorrecto_EN() throws Exception {
        byte[] bytesES = service.generarExcelReporteProduccion(List.of(), List.of(), DESDE, HASTA, BRANDING, ES);
        byte[] bytesEN = service.generarExcelReporteProduccion(List.of(), List.of(), DESDE, HASTA, BRANDING, EN);
        try (XSSFWorkbook wbES = openExcel(bytesES);
             XSSFWorkbook wbEN = openExcel(bytesEN)) {
            assertThat(wbES.getSheetAt(0).getSheetName())
                    .isNotEqualTo(wbEN.getSheetAt(0).getSheetName());
        }
    }

    @Test
    @DisplayName("la fila de encabezados (índice 6) contiene 'Código' en col 0 (ES)")
    void excel_filaEncabezados_contieneCodigo() throws Exception {
        byte[] bytes = service.generarExcelReporteProduccion(
                List.of(lote("IPA-001", "IPA")), List.of(), DESDE, HASTA, BRANDING, ES);
        try (XSSFWorkbook wb = openExcel(bytes)) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(cellStr(sheet, 6, 0))
                    .isEqualTo(MSG.getMessage("xls.header.codigo", null, ES));
        }
    }

    @Test
    @DisplayName("la fila de encabezados (índice 6) contiene 'Estilo' en col 1 (ES)")
    void excel_filaEncabezados_contieneEstilo() throws Exception {
        byte[] bytes = service.generarExcelReporteProduccion(
                List.of(lote("IPA-001", "IPA")), List.of(), DESDE, HASTA, BRANDING, ES);
        try (XSSFWorkbook wb = openExcel(bytes)) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(cellStr(sheet, 6, 1))
                    .isEqualTo(MSG.getMessage("xls.header.estilo", null, ES));
        }
    }

    @Test
    @DisplayName("la fila de datos (índice 7) contiene el código de lote en col 0")
    void excel_filaDatos_contieneCodigoLote() throws Exception {
        byte[] bytes = service.generarExcelReporteProduccion(
                List.of(lote("IPA-001", "IPA")), List.of(), DESDE, HASTA, BRANDING, ES);
        try (XSSFWorkbook wb = openExcel(bytes)) {
            assertThat(cellStr(wb.getSheetAt(0), 7, 0)).isEqualTo("IPA-001");
        }
    }

    @Test
    @DisplayName("la fila de datos (índice 7) contiene el estilo en col 1")
    void excel_filaDatos_contieneEstilo() throws Exception {
        byte[] bytes = service.generarExcelReporteProduccion(
                List.of(lote("IPA-001", "IPA")), List.of(), DESDE, HASTA, BRANDING, ES);
        try (XSSFWorkbook wb = openExcel(bytes)) {
            assertThat(cellStr(wb.getSheetAt(0), 7, 1)).isEqualTo("IPA");
        }
    }

    @Test
    @DisplayName("la fila de datos contiene el ABV calculado (OG=1060, FG=1014 → 6.04) en col 7")
    void excel_filaDatos_contieneAbvNumerico() throws Exception {
        byte[] bytes = service.generarExcelReporteProduccion(
                List.of(loteConMetricas("IPA-001", "IPA")),
                List.of(), DESDE, HASTA, BRANDING, ES);
        try (XSSFWorkbook wb = openExcel(bytes)) {
            double abv = cellNum(wb.getSheetAt(0), 7, 7);
            // ABV = (1060 - 1014) * 131.25 / 1000 = 6.04
            assertThat(abv).isCloseTo(6.04, within(0.01));
        }
    }

    @Test
    @DisplayName("la fila de datos contiene los litros finales en col 10")
    void excel_filaDatos_contieneLitros() throws Exception {
        byte[] bytes = service.generarExcelReporteProduccion(
                List.of(loteConMetricas("IPA-001", "IPA")),
                List.of(), DESDE, HASTA, BRANDING, ES);
        try (XSSFWorkbook wb = openExcel(bytes)) {
            double litros = cellNum(wb.getSheetAt(0), 7, 10);
            assertThat(litros).isCloseTo(20.0, within(0.001));
        }
    }

    @Test
    @DisplayName("múltiples lotes generan filas de datos consecutivas (7, 8, 9)")
    void excel_variosLotes_generanFilasConsecutivas() throws Exception {
        byte[] bytes = service.generarExcelReporteProduccion(
                List.of(
                        lote("IPA-001", "IPA"),
                        lote("STOUT-001", "Stout"),
                        lote("APA-001", "Pale Ale")
                ), List.of(), DESDE, HASTA, BRANDING, ES);
        try (XSSFWorkbook wb = openExcel(bytes)) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(cellStr(sheet, 7, 0)).isEqualTo("IPA-001");
            assertThat(cellStr(sheet, 8, 0)).isEqualTo("STOUT-001");
            assertThat(cellStr(sheet, 9, 0)).isEqualTo("APA-001");
        }
    }

    /*
     * Layout de construirSheetEstilos (hoja índice 1):
     *   0: título, 1: vacía, 2: encabezados, 3+: datos
     */

    @Test
    @DisplayName("la hoja 2 ('Por Estilo') tiene el nombre correcto en español")
    void excel_hoja2_nombreCorrecto() throws Exception {
        byte[] bytes = service.generarExcelReporteProduccion(
                List.of(), List.of(), DESDE, HASTA, BRANDING, ES);
        try (XSSFWorkbook wb = openExcel(bytes)) {
            assertThat(wb.getSheetAt(1).getSheetName())
                    .isEqualTo(MSG.getMessage("xls.sheet.por_estilo", null, ES));
        }
    }

    @Test
    @DisplayName("la hoja 2 tiene encabezado 'Estilo' en col 0 (fila 2)")
    void excel_hoja2_encabezadoEstilo() throws Exception {
        java.util.List<Object[]> resumen = new java.util.ArrayList<>();
        resumen.add(resumenEstilo("IPA", 3L, 65.0));
        byte[] bytes = service.generarExcelReporteProduccion(
                List.of(), resumen, DESDE, HASTA, BRANDING, ES);
        try (XSSFWorkbook wb = openExcel(bytes)) {
            assertThat(cellStr(wb.getSheetAt(1), 2, 0))
                    .isEqualTo(MSG.getMessage("xls.header.estilo", null, ES));
        }
    }

    @Test
    @DisplayName("la hoja 2 contiene el nombre del estilo en los datos (fila 3, col 0)")
    void excel_hoja2_contieneDatoEstilo() throws Exception {
        LoteCerveza l = lote("IMP-001", "Stout Imperial");
        byte[] bytes = service.generarExcelReporteProduccion(
                List.of(l), List.of(), DESDE, HASTA, BRANDING, ES);
        try (XSSFWorkbook wb = openExcel(bytes)) {
            assertThat(cellStr(wb.getSheetAt(1), 3, 0)).isEqualTo("Stout Imperial");
        }
    }

    @Test
    @DisplayName("la hoja 2 contiene los litros totales del resumen de estilos (fila 3, col 2)")
    void excel_hoja2_contieneLitrosTotalesEstilo() throws Exception {
        LoteCerveza l = lote("IPA-001", "IPA");
        l.setLitrosFinales(new BigDecimal("65.0"));
        byte[] bytes = service.generarExcelReporteProduccion(
                List.of(l), List.of(), DESDE, HASTA, BRANDING, ES);
        try (XSSFWorkbook wb = openExcel(bytes)) {
            double litros = cellNum(wb.getSheetAt(1), 3, 2);
            assertThat(litros).isEqualTo(65.0, within(0.001));
        }
    }

    @Test
    @DisplayName("los encabezados de hoja 1 difieren entre ES e EN (i18n real)")
    void excel_encabezadosDifiereenSegunLocale() throws Exception {
        byte[] bytesES = service.generarExcelReporteProduccion(
                List.of(lote("X-001", "X")), List.of(), DESDE, HASTA, BRANDING, ES);
        byte[] bytesEN = service.generarExcelReporteProduccion(
                List.of(lote("X-001", "X")), List.of(), DESDE, HASTA, BRANDING, EN);
        try (XSSFWorkbook wbES = openExcel(bytesES);
             XSSFWorkbook wbEN = openExcel(bytesEN)) {
            String headerES = cellStr(wbES.getSheetAt(0), 6, 0);
            String headerEN = cellStr(wbEN.getSheetAt(0), 6, 0);
            assertThat(headerES).isNotEmpty();
            assertThat(headerEN).isNotEmpty();
            assertThat(headerES).isNotEqualTo(headerEN);
        }
    }
}
