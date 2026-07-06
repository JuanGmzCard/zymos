package com.alera.service;

import com.alera.config.ExportBranding;
import com.alera.model.LecturaFermentacion;
import com.alera.model.LoteCerveza;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PdfExportService")
class PdfExportServiceTest {

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

    private final PdfExportService service = new PdfExportService(MSG);
    private static final ExportBranding BRANDING = ExportBranding.defaults("Alera");

    // ── Helpers ───────────────────────────────────────────────────────

    private LoteCerveza loteMinimo() {
        LoteCerveza lote = new LoteCerveza();
        lote.setCodigoLote("IPA-001");
        lote.setEstilo("India Pale Ale");
        lote.setFechaElaboracion(LocalDate.of(2025, 3, 1));
        return lote;
    }

    private LoteCerveza loteCompleto() {
        LoteCerveza lote = loteMinimo();
        lote.setDensidadInicial(1058);
        lote.setDensidadFinal(1012);
        lote.setLitrosFinales(new BigDecimal("20"));
        lote.setFermFechaInicial(LocalDate.of(2025, 3, 1));
        lote.setAcondFechaInicial(LocalDate.of(2025, 3, 10));
        lote.setObservaciones("Sin observaciones relevantes");
        lote.setNotasCata("Amargor equilibrado, aroma cítrico");
        return lote;
    }

    private LecturaFermentacion lectura(LocalDate fecha, Integer densidad, BigDecimal temp) {
        LecturaFermentacion l = new LecturaFermentacion();
        l.setFecha(fecha);
        l.setDensidad(densidad);
        l.setTemperatura(temp);
        return l;
    }

    private void assertEsPdf(byte[] bytes) {
        assertThat(bytes).isNotNull().isNotEmpty();
        assertThat((char) bytes[0]).isEqualTo('%');
        assertThat((char) bytes[1]).isEqualTo('P');
        assertThat((char) bytes[2]).isEqualTo('D');
        assertThat((char) bytes[3]).isEqualTo('F');
    }

    private String extractPdfText(byte[] bytes) throws Exception {
        PdfReader reader = new PdfReader(bytes);
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
            sb.append(extractor.getTextFromPage(i));
        }
        reader.close();
        return sb.toString();
    }

    // ── generarPdfLote ────────────────────────────────────────────────

    @Test
    @DisplayName("genera PDF válido con lote mínimo y sin lecturas")
    void generarPdfLote_loteMinimo_sinLecturas_producePdf() {
        byte[] resultado = service.generarPdfLote(loteMinimo(), BRANDING, List.of(), List.of(), List.of(), ES);

        assertEsPdf(resultado);
    }

    @Test
    @DisplayName("genera PDF válido con lote completo (densidades, fases, observaciones)")
    void generarPdfLote_loteCompleto_producePdf() {
        byte[] resultado = service.generarPdfLote(loteCompleto(), ExportBranding.defaults("Mosto Cervecería"), List.of(), List.of(), List.of(), ES);

        assertEsPdf(resultado);
    }

    @Test
    @DisplayName("genera PDF válido con lecturas de fermentación (curva densidad + temperatura)")
    void generarPdfLote_conLecturas_producePdf() {
        LoteCerveza lote = loteCompleto();
        List<LecturaFermentacion> lecturas = List.of(
                lectura(LocalDate.of(2025, 3, 1),  1058, new BigDecimal("20.5")),
                lectura(LocalDate.of(2025, 3, 3),  1042, new BigDecimal("19.8")),
                lectura(LocalDate.of(2025, 3, 7),  1015, new BigDecimal("19.2")),
                lectura(LocalDate.of(2025, 3, 10), 1012, new BigDecimal("18.9"))
        );

        byte[] resultado = service.generarPdfLote(lote, BRANDING, lecturas, List.of(), List.of(), ES);

        assertEsPdf(resultado);
    }

    @Test
    @DisplayName("genera PDF válido con lecturas que tienen solo densidad (sin temperatura)")
    void generarPdfLote_lecturasSinTemperatura_producePdf() {
        LoteCerveza lote = loteCompleto();
        List<LecturaFermentacion> lecturas = List.of(
                lectura(LocalDate.of(2025, 3, 1),  1058, null),
                lectura(LocalDate.of(2025, 3, 5),  1035, null),
                lectura(LocalDate.of(2025, 3, 10), 1012, null)
        );

        byte[] resultado = service.generarPdfLote(lote, BRANDING, lecturas, List.of(), List.of(), ES);

        assertEsPdf(resultado);
    }

    @Test
    @DisplayName("genera PDF válido con lecturas que tienen solo temperatura (sin densidad)")
    void generarPdfLote_lecturasSinDensidad_producePdf() {
        LoteCerveza lote = loteCompleto();
        List<LecturaFermentacion> lecturas = List.of(
                lectura(LocalDate.of(2025, 3, 1), null, new BigDecimal("20.5")),
                lectura(LocalDate.of(2025, 3, 5), null, new BigDecimal("19.5"))
        );

        byte[] resultado = service.generarPdfLote(lote, BRANDING, lecturas, List.of(), List.of(), ES);

        assertEsPdf(resultado);
    }

    @Test
    @DisplayName("genera PDF válido con lecturas null (trata igual que lista vacía)")
    void generarPdfLote_lecturasNull_producePdf() {
        byte[] resultado = service.generarPdfLote(loteMinimo(), BRANDING, null, List.of(), List.of(), ES);

        assertEsPdf(resultado);
    }

    @Test
    @DisplayName("el PDF generado tiene un tamaño razonable (mayor a 1KB)")
    void generarPdfLote_tamanioRazonable() {
        byte[] resultado = service.generarPdfLote(loteCompleto(), BRANDING, List.of(), List.of(), List.of(), ES);

        assertThat(resultado.length).isGreaterThan(1024);
    }

    @Test
    @DisplayName("lotes distintos generan PDFs de distinto contenido")
    void generarPdfLote_lotesDistintos_generanPdfsDistintos() {
        LoteCerveza lote1 = loteMinimo();
        LoteCerveza lote2 = loteMinimo();
        lote2.setCodigoLote("STOUT-001");
        lote2.setEstilo("Stout");

        byte[] pdf1 = service.generarPdfLote(lote1, BRANDING, List.of(), List.of(), List.of(), ES);
        byte[] pdf2 = service.generarPdfLote(lote2, BRANDING, List.of(), List.of(), List.of(), ES);

        assertThat(pdf1).isNotEqualTo(pdf2);
    }

    @Test
    @DisplayName("genera PDF en inglés sin excepciones")
    void generarPdfLote_localeIngles_producePdf() {
        byte[] resultado = service.generarPdfLote(loteCompleto(), BRANDING, List.of(), List.of(), List.of(), EN);

        assertEsPdf(resultado);
    }

    // ── Contenido del PDF ─────────────────────────────────────────────

    @Test
    @DisplayName("el PDF contiene el código de lote en la cabecera")
    void generarPdfLote_contieneCodigoLote() throws Exception {
        byte[] pdf = service.generarPdfLote(loteMinimo(), BRANDING, List.of(), List.of(), List.of(), ES);

        assertThat(extractPdfText(pdf)).contains("IPA-001");
    }

    @Test
    @DisplayName("el PDF contiene el estilo de la cerveza")
    void generarPdfLote_contieneEstilo() throws Exception {
        byte[] pdf = service.generarPdfLote(loteMinimo(), BRANDING, List.of(), List.of(), List.of(), ES);

        assertThat(extractPdfText(pdf)).contains("India Pale Ale");
    }

    @Test
    @DisplayName("el PDF contiene la fecha de elaboración formateada dd/MM/yyyy")
    void generarPdfLote_contieneFechaElaboracion() throws Exception {
        byte[] pdf = service.generarPdfLote(loteMinimo(), BRANDING, List.of(), List.of(), List.of(), ES);

        assertThat(extractPdfText(pdf)).contains("01/03/2025");
    }

    @Test
    @DisplayName("el PDF contiene el nombre del tenant en mayúsculas")
    void generarPdfLote_contieneBrandName() throws Exception {
        byte[] pdf = service.generarPdfLote(loteMinimo(), ExportBranding.defaults("Cervecería Mosto"), List.of(), List.of(), List.of(), ES);

        assertThat(extractPdfText(pdf)).containsIgnoringCase("Cervecería Mosto");
    }

    @Test
    @DisplayName("el PDF contiene el ABV calculado (OG=1058, FG=1012 → 6.04%)")
    void generarPdfLote_contieneAbvCalculado() throws Exception {
        byte[] pdf = service.generarPdfLote(loteCompleto(), BRANDING, List.of(), List.of(), List.of(), ES);

        // ABV = (1058 - 1012) * 131.25 / 1000 = 6.04
        assertThat(extractPdfText(pdf)).contains("6.04");
    }

    @Test
    @DisplayName("el PDF contiene el texto de observaciones cuando está presente")
    void generarPdfLote_contieneObservaciones() throws Exception {
        byte[] pdf = service.generarPdfLote(loteCompleto(), BRANDING, List.of(), List.of(), List.of(), ES);

        assertThat(extractPdfText(pdf)).contains("Sin observaciones relevantes");
    }

    @Test
    @DisplayName("el PDF contiene las notas de cata cuando están presentes")
    void generarPdfLote_contieneNotasCata() throws Exception {
        byte[] pdf = service.generarPdfLote(loteCompleto(), BRANDING, List.of(), List.of(), List.of(), ES);

        assertThat(extractPdfText(pdf)).contains("Amargor equilibrado");
    }

    @Test
    @DisplayName("el PDF en español contiene el título de sección 'INFORMACIÓN DEL LOTE'")
    void generarPdfLote_esParanol_contieneTituloSeccionES() throws Exception {
        byte[] pdf = service.generarPdfLote(loteMinimo(), BRANDING, List.of(), List.of(), List.of(), ES);

        assertThat(extractPdfText(pdf))
                .contains(MSG.getMessage("pdf.title.info_lote", null, ES));
    }

    @Test
    @DisplayName("el PDF en inglés contiene el título de sección 'BATCH INFORMATION'")
    void generarPdfLote_ingles_contieneTituloSeccionEN() throws Exception {
        byte[] pdf = service.generarPdfLote(loteMinimo(), BRANDING, List.of(), List.of(), List.of(), EN);

        assertThat(extractPdfText(pdf))
                .contains(MSG.getMessage("pdf.title.info_lote", null, EN));
    }

    @Test
    @DisplayName("los títulos de sección difieren entre ES e EN (i18n real)")
    void generarPdfLote_tituloDifiereSegunLocale() throws Exception {
        String tituloES = MSG.getMessage("pdf.title.info_lote", null, ES);
        String tituloEN = MSG.getMessage("pdf.title.info_lote", null, EN);

        byte[] pdfES = service.generarPdfLote(loteMinimo(), BRANDING, List.of(), List.of(), List.of(), ES);
        byte[] pdfEN = service.generarPdfLote(loteMinimo(), BRANDING, List.of(), List.of(), List.of(), EN);

        assertThat(extractPdfText(pdfES)).contains(tituloES);
        assertThat(extractPdfText(pdfEN)).contains(tituloEN);
        assertThat(tituloES).isNotEqualTo(tituloEN);
    }

    @Test
    @DisplayName("el PDF con lecturas contiene las densidades de fermentación en la tabla")
    void generarPdfLote_conLecturas_contieneDensidadEnTabla() throws Exception {
        LoteCerveza lote = loteCompleto();
        List<LecturaFermentacion> lecturas = List.of(
                lectura(LocalDate.of(2025, 3, 1),  1058, new BigDecimal("20.5")),
                lectura(LocalDate.of(2025, 3, 5),  1030, new BigDecimal("19.5")),
                lectura(LocalDate.of(2025, 3, 10), 1012, new BigDecimal("18.9"))
        );

        byte[] pdf = service.generarPdfLote(lote, BRANDING, lecturas, List.of(), List.of(), ES);
        String text = extractPdfText(pdf);

        // La tabla de lecturas incluye densidades y fechas
        assertThat(text).contains("1030");
        assertThat(text).contains("05/03/2025");
    }

    @Test
    @DisplayName("el PDF con lecturas contiene la fecha de la primera lectura")
    void generarPdfLote_conLecturas_contieneFechaLectura() throws Exception {
        LoteCerveza lote = loteCompleto();
        List<LecturaFermentacion> lecturas = List.of(
                lectura(LocalDate.of(2025, 3, 1),  1058, null),
                lectura(LocalDate.of(2025, 3, 8),  1015, null)
        );

        byte[] pdf = service.generarPdfLote(lote, BRANDING, lecturas, List.of(), List.of(), ES);

        assertThat(extractPdfText(pdf)).contains("08/03/2025");
    }

    @Test
    @DisplayName("PDFs de distintos lotes contienen sus respectivos códigos")
    void generarPdfLote_lotesDistintos_cadaUnoContienesSuCodigo() throws Exception {
        LoteCerveza lote1 = loteMinimo();
        LoteCerveza lote2 = loteMinimo();
        lote2.setCodigoLote("STOUT-007");
        lote2.setEstilo("Stout Imperial");

        String text1 = extractPdfText(service.generarPdfLote(lote1, BRANDING, List.of(), List.of(), List.of(), ES));
        String text2 = extractPdfText(service.generarPdfLote(lote2, BRANDING, List.of(), List.of(), List.of(), ES));

        assertThat(text1).contains("IPA-001").doesNotContain("STOUT-007");
        assertThat(text2).contains("STOUT-007").doesNotContain("IPA-001");
    }
}
