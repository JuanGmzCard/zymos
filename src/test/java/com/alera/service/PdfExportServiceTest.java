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

    // ── generarPdfLote ────────────────────────────────────────────────

    @Test
    @DisplayName("genera PDF válido con lote mínimo y sin lecturas")
    void generarPdfLote_loteMinimo_sinLecturas_producePdf() {
        byte[] resultado = service.generarPdfLote(loteMinimo(), BRANDING, List.of(), ES);

        assertEsPdf(resultado);
    }

    @Test
    @DisplayName("genera PDF válido con lote completo (densidades, fases, observaciones)")
    void generarPdfLote_loteCompleto_producePdf() {
        byte[] resultado = service.generarPdfLote(loteCompleto(), ExportBranding.defaults("Mosto Cervecería"), List.of(), ES);

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

        byte[] resultado = service.generarPdfLote(lote, BRANDING, lecturas, ES);

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

        byte[] resultado = service.generarPdfLote(lote, BRANDING, lecturas, ES);

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

        byte[] resultado = service.generarPdfLote(lote, BRANDING, lecturas, ES);

        assertEsPdf(resultado);
    }

    @Test
    @DisplayName("genera PDF válido con lecturas null (trata igual que lista vacía)")
    void generarPdfLote_lecturasNull_producePdf() {
        byte[] resultado = service.generarPdfLote(loteMinimo(), BRANDING, null, ES);

        assertEsPdf(resultado);
    }

    @Test
    @DisplayName("el PDF generado tiene un tamaño razonable (mayor a 1KB)")
    void generarPdfLote_tamanioRazonable() {
        byte[] resultado = service.generarPdfLote(loteCompleto(), BRANDING, List.of(), ES);

        assertThat(resultado.length).isGreaterThan(1024);
    }

    @Test
    @DisplayName("lotes distintos generan PDFs de distinto contenido")
    void generarPdfLote_lotesDistintos_generanPdfsDistintos() {
        LoteCerveza lote1 = loteMinimo();
        LoteCerveza lote2 = loteMinimo();
        lote2.setCodigoLote("STOUT-001");
        lote2.setEstilo("Stout");

        byte[] pdf1 = service.generarPdfLote(lote1, BRANDING, List.of(), ES);
        byte[] pdf2 = service.generarPdfLote(lote2, BRANDING, List.of(), ES);

        assertThat(pdf1).isNotEqualTo(pdf2);
    }

    @Test
    @DisplayName("genera PDF en inglés sin excepciones")
    void generarPdfLote_localeIngles_producePdf() {
        byte[] resultado = service.generarPdfLote(loteCompleto(), BRANDING, List.of(), EN);

        assertEsPdf(resultado);
    }
}
