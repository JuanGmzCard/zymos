package com.alera.service;

import com.alera.config.ExportBranding;
import com.alera.model.*;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class BpmPdfService {

    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_TIME = DateTimeFormatter.ofPattern("HH:mm");

    private static final Font F_HEADER_TITLE = new Font(Font.HELVETICA, 13, Font.BOLD, Color.WHITE);
    private static final Font F_HEADER_SUB   = new Font(Font.HELVETICA, 8,  Font.NORMAL, new Color(200, 220, 200));
    private static final Font F_SECTION      = new Font(Font.HELVETICA, 8,  Font.BOLD,   Color.WHITE);
    private static final Font F_TH           = new Font(Font.HELVETICA, 7,  Font.BOLD,   new Color(60, 80, 30));
    private static final Font F_TD           = new Font(Font.HELVETICA, 7,  Font.NORMAL, new Color(30, 30, 30));
    private static final Font F_TD_BOLD      = new Font(Font.HELVETICA, 7,  Font.BOLD,   new Color(30, 30, 30));

    // ── Registro de síntomas ─────────────────────────────────────────────────

    public byte[] generarSintomas(List<RegistroSintomas> registros, ExportBranding b, String logoUrl,
                                   String titulo, String subtitulo) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4.rotate(), 30, 30, 45, 40);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Color verde  = b.primary();
            Color oscuro = b.primaryDark();
            Color dorado = b.accent();
            Color fondo  = ExportBranding.lighten(b.background(), 0.3f);

            addCabecera(doc, b.name(), logoUrl, titulo, subtitulo, verde, oscuro, dorado);
            addSeccion(doc, "REGISTRO CONTROL ESTADO DE SALUD MANIPULADORES DE ALIMENTOS", verde);

            String[] ths = {"Nombre Manipulador", "Fecha", "Diarrea", "Vómito", "Fiebre",
                    "Inf. Resp.", "Lesión Piel", "Observaciones",
                    "Firma Manipulador", "Firma Responsable"};
            PdfPTable tabla = new PdfPTable(new float[]{2.5f, 1.2f, 0.8f, 0.8f, 0.8f, 0.8f, 0.9f, 2f, 1.8f, 1.8f});
            tabla.setWidthPercentage(100);
            tabla.setSpacingBefore(4);

            for (String th : ths) {
                addTh(tabla, th, ExportBranding.lighten(verde, 0.6f), F_TH);
            }

            boolean alt = false;
            for (RegistroSintomas r : registros) {
                Color bg = alt ? fondo : Color.WHITE;
                addTd(tabla, r.getNombreManipulador(), bg);
                addTd(tabla, r.getFecha() != null ? r.getFecha().format(FMT_DATE) : "—", bg);
                addTdBool(tabla, r.isDiarrea(), bg);
                addTdBool(tabla, r.isVomito(), bg);
                addTdBool(tabla, r.isFiebre(), bg);
                addTdBool(tabla, r.isInfeccionRespiratoria(), bg);
                addTdBool(tabla, r.isLesionPiel(), bg);
                addTd(tabla, nvl(r.getObservaciones()), bg);
                addTdFirma(tabla, r.getFirmaManipulador(), bg);
                addTdFirma(tabla, r.getFirmaResponsable(), bg);
                alt = !alt;
            }
            if (registros.isEmpty()) addFilaVacia(tabla, 10);

            doc.add(tabla);
            addFirmas(doc, verde);
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF síntomas BPM", e);
        }
    }

    // ── Soluciones desinfectantes ─────────────────────────────────────────────

    public byte[] generarSoluciones(List<SolucionDesinfectante> registros, ExportBranding b, String logoUrl,
                                     String titulo, String subtitulo) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4.rotate(), 30, 30, 45, 40);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Color verde  = b.primary();
            Color oscuro = b.primaryDark();
            Color dorado = b.accent();
            Color fondo  = ExportBranding.lighten(b.background(), 0.3f);

            addCabecera(doc, b.name(), logoUrl, titulo, subtitulo, verde, oscuro, dorado);
            addSeccion(doc, "REGISTRO DE SOLUCIONES DESINFECTANTES", verde);

            PdfPTable tabla = new PdfPTable(new float[]{1.2f, 1f, 2f, 1.5f, 1f, 1.5f, 1f, 1.5f, 1.5f, 1.5f});
            tabla.setWidthPercentage(100);
            tabla.setSpacingBefore(4);

            String[] ths = {"Fecha", "Hora", "Producto", "Cant. Agua", "Unidad Agua",
                    "Cant. Producto", "Unidad Prod.", "Conc. Final (%)", "Responsable", "Firma"};
            for (String th : ths) addTh(tabla, th, ExportBranding.lighten(verde, 0.6f), F_TH);

            boolean alt = false;
            for (SolucionDesinfectante r : registros) {
                Color bg = alt ? fondo : Color.WHITE;
                addTd(tabla, r.getFecha() != null ? r.getFecha().format(FMT_DATE) : "—", bg);
                addTd(tabla, r.getHora() != null ? r.getHora().format(FMT_TIME) : "—", bg);
                addTd(tabla, nvl(r.getProducto()), bg);
                addTd(tabla, r.getCantidadAgua() != null ? r.getCantidadAgua().stripTrailingZeros().toPlainString() : "—", bg);
                addTd(tabla, nvl(r.getUnidadAgua()), bg);
                addTd(tabla, r.getCantidadProducto() != null ? r.getCantidadProducto().stripTrailingZeros().toPlainString() : "—", bg);
                addTd(tabla, nvl(r.getUnidadProducto()), bg);
                addTd(tabla, r.getConcentracionFinal() != null ? r.getConcentracionFinal().stripTrailingZeros().toPlainString() : "—", bg);
                addTd(tabla, nvl(r.getResponsable()), bg);
                addTdFirma(tabla, r.getFirma(), bg);
                alt = !alt;
            }
            if (registros.isEmpty()) addFilaVacia(tabla, 10);

            doc.add(tabla);
            addFirmas(doc, verde);
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF soluciones BPM", e);
        }
    }

    // ── Avistamiento de plagas ────────────────────────────────────────────────

    public byte[] generarPlagas(List<AvistamientoPlagas> registros, ExportBranding b, String logoUrl,
                                 String titulo, String subtitulo) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4.rotate(), 30, 30, 45, 40);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Color verde  = b.primary();
            Color oscuro = b.primaryDark();
            Color dorado = b.accent();
            Color fondo  = ExportBranding.lighten(b.background(), 0.3f);

            addCabecera(doc, b.name(), logoUrl, titulo, subtitulo, verde, oscuro, dorado);
            addSeccion(doc, "REGISTRO DE AVISTAMIENTO Y CONTROL DE PLAGAS", verde);

            PdfPTable tabla = new PdfPTable(new float[]{1.2f, 1f, 1.5f, 1.5f, 2.5f, 1.5f});
            tabla.setWidthPercentage(100);
            tabla.setSpacingBefore(4);

            String[] ths = {"Fecha", "¿Plagas?", "Tipo de Plagas", "Estado Vent./Puertas",
                    "Acción Tomada", "Firma"};
            for (String th : ths) addTh(tabla, th, ExportBranding.lighten(verde, 0.6f), F_TH);

            boolean alt = false;
            for (AvistamientoPlagas r : registros) {
                Color bg = alt ? fondo : Color.WHITE;
                addTd(tabla, r.getFecha() != null ? r.getFecha().format(FMT_DATE) : "—", bg);
                addTd(tabla, r.isPresenciaPlagas() ? "Sí" : "No", bg);
                addTd(tabla, nvl(r.getTipoPlagas()), bg);
                addTd(tabla, nvl(r.getEstadoVentanasPuertas()), bg);
                addTd(tabla, nvl(r.getAccionTomada()), bg);
                addTdFirma(tabla, r.getFirma(), bg);
                alt = !alt;
            }
            if (registros.isEmpty()) addFilaVacia(tabla, 6);

            doc.add(tabla);
            addFirmas(doc, verde);
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF plagas BPM", e);
        }
    }

    // ── Evacuación de residuos ────────────────────────────────────────────────

    public byte[] generarResiduos(List<EvacuacionResiduos> registros, ExportBranding b, String logoUrl,
                                   String titulo, String subtitulo) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 36, 36, 50, 45);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Color verde  = b.primary();
            Color oscuro = b.primaryDark();
            Color dorado = b.accent();
            Color fondo  = ExportBranding.lighten(b.background(), 0.3f);

            addCabecera(doc, b.name(), logoUrl, titulo, subtitulo, verde, oscuro, dorado);
            addSeccion(doc, "REGISTRO DE EVACUACIÓN DE RESIDUOS", verde);

            PdfPTable tabla = new PdfPTable(new float[]{1.5f, 1.2f, 2f, 1.5f, 2f, 1.5f});
            tabla.setWidthPercentage(100);
            tabla.setSpacingBefore(4);

            String[] ths = {"Fecha", "Hora Salida", "Tipo Residuo", "Recipientes Limpios", "Responsable", "Firma"};
            for (String th : ths) addTh(tabla, th, ExportBranding.lighten(verde, 0.6f), F_TH);

            boolean alt = false;
            for (EvacuacionResiduos r : registros) {
                Color bg = alt ? fondo : Color.WHITE;
                addTd(tabla, r.getFecha() != null ? r.getFecha().format(FMT_DATE) : "—", bg);
                addTd(tabla, r.getHoraSalida() != null ? r.getHoraSalida().format(FMT_TIME) : "—", bg);
                addTd(tabla, r.getTipoResiduo() != null ? r.getTipoResiduo().getDisplayName() : "—", bg);
                addTd(tabla, r.isRecipientesLimpios() ? "Sí" : "No", bg);
                addTd(tabla, nvl(r.getResponsable()), bg);
                addTdFirma(tabla, r.getFirma(), bg);
                alt = !alt;
            }
            if (registros.isEmpty()) addFilaVacia(tabla, 6);

            doc.add(tabla);
            addFirmas(doc, verde);
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF residuos BPM", e);
        }
    }

    // ── Limpieza y desinfección ───────────────────────────────────────────────

    public byte[] generarLimpieza(List<LimpiezaDesinfeccion> registros, ExportBranding b, String logoUrl,
                                   String titulo, String subtitulo) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4.rotate(), 30, 30, 45, 40);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Color verde  = b.primary();
            Color oscuro = b.primaryDark();
            Color dorado = b.accent();
            Color fondo  = ExportBranding.lighten(b.background(), 0.3f);

            addCabecera(doc, b.name(), logoUrl, titulo, subtitulo, verde, oscuro, dorado);
            addSeccion(doc, "REGISTRO DE LIMPIEZA Y DESINFECCIÓN", verde);

            PdfPTable tabla = new PdfPTable(new float[]{1.2f, 1f, 2f, 1.5f, 1.5f, 1f, 1.5f, 1f, 1.5f});
            tabla.setWidthPercentage(100);
            tabla.setSpacingBefore(4);

            String[] ths = {"Fecha", "Día", "Área / Utensilio", "Detergente", "Sanitizador",
                    "Concentración", "Responsable", "Visto Bueno", "Firma"};
            for (String th : ths) addTh(tabla, th, ExportBranding.lighten(verde, 0.6f), F_TH);

            boolean alt = false;
            for (LimpiezaDesinfeccion r : registros) {
                Color bg = alt ? fondo : Color.WHITE;
                addTd(tabla, r.getFecha() != null ? r.getFecha().format(FMT_DATE) : "—", bg);
                addTd(tabla, nvl(r.getDia()), bg);
                addTd(tabla, nvl(r.getAreaUtensilio()), bg);
                addTd(tabla, nvl(r.getDetergenteUsado()), bg);
                addTd(tabla, nvl(r.getSanitizadorUsado()), bg);
                addTd(tabla, nvl(r.getConcentracion()), bg);
                addTd(tabla, nvl(r.getResponsable()), bg);
                addTd(tabla, r.isVistoBueno() ? "✓" : "—", bg);
                addTdFirma(tabla, r.getFirma(), bg);
                alt = !alt;
            }
            if (registros.isEmpty()) addFilaVacia(tabla, 9);

            doc.add(tabla);
            addFirmas(doc, verde);
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF limpieza BPM", e);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void addCabecera(Document doc, String brandName, String logoUrl, String titulo, String subtitulo,
                              Color verde, Color oscuro, Color dorado) throws DocumentException {
        PdfPTable h = new PdfPTable(new float[]{3, 2});
        h.setWidthPercentage(100);
        h.setSpacingAfter(8);

        PdfPCell izq = new PdfPCell();
        izq.setBackgroundColor(verde);
        izq.setBorder(0);
        izq.setPaddingTop(10); izq.setPaddingBottom(10);
        izq.setPaddingLeft(12); izq.setPaddingRight(8);

        Image logo = cargarLogo(logoUrl);
        if (logo != null) {
            logo.scaleToFit(120, 36);
            izq.addElement(logo);
        } else {
            izq.addElement(new Paragraph(brandName.toUpperCase(),
                    new Font(Font.HELVETICA, 7, Font.NORMAL, dorado)));
        }
        Paragraph t = new Paragraph(titulo, F_HEADER_TITLE);
        t.setSpacingBefore(4);
        izq.addElement(t);
        if (subtitulo != null && !subtitulo.isBlank()) {
            Paragraph s = new Paragraph(subtitulo, F_HEADER_SUB);
            s.setSpacingBefore(4);
            izq.addElement(s);
        }
        h.addCell(izq);

        PdfPCell der = new PdfPCell();
        der.setBackgroundColor(oscuro);
        der.setBorder(0);
        der.setPaddingTop(10); der.setPaddingBottom(10);
        der.setPaddingLeft(8); der.setPaddingRight(12);
        Paragraph code = new Paragraph("BPM",
                new Font(Font.HELVETICA, 22, Font.BOLD, dorado));
        code.setAlignment(Element.ALIGN_RIGHT);
        der.addElement(code);
        Paragraph fecha = new Paragraph("Buenas Prácticas de Manufactura",
                new Font(Font.HELVETICA, 7, Font.NORMAL, new Color(200, 220, 200)));
        fecha.setAlignment(Element.ALIGN_RIGHT); fecha.setSpacingBefore(4);
        der.addElement(fecha);
        h.addCell(der);

        doc.add(h);
    }

    private void addSeccion(Document doc, String texto, Color verde) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(6);
        t.setSpacingAfter(2);
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(verde);
        c.setBorder(0);
        c.setPaddingTop(4); c.setPaddingBottom(4); c.setPaddingLeft(8);
        c.addElement(new Paragraph(texto, F_SECTION));
        t.addCell(c);
        doc.add(t);
    }

    private void addFirmas(Document doc, Color verde) throws DocumentException {
        doc.add(new Paragraph(" "));
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(60);
        t.setHorizontalAlignment(Element.ALIGN_LEFT);
        t.setSpacingBefore(20);
        PdfPCell c1 = new PdfPCell(new Paragraph("Firma Manipulador: ___________________",
                new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(60, 60, 60))));
        c1.setBorder(0); c1.setPaddingBottom(4);
        PdfPCell c2 = new PdfPCell(new Paragraph("Firma Responsable: ___________________",
                new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(60, 60, 60))));
        c2.setBorder(0); c2.setPaddingBottom(4);
        t.addCell(c1); t.addCell(c2);
        doc.add(t);
    }

    private void addTh(PdfPTable tabla, String texto, Color bg, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(texto, f));
        c.setBackgroundColor(bg);
        c.setBorderColor(new Color(180, 200, 150));
        c.setPadding(4);
        tabla.addCell(c);
    }

    private void addTd(PdfPTable tabla, String texto, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(texto, F_TD));
        c.setBackgroundColor(bg);
        c.setBorderColor(new Color(210, 220, 200));
        c.setPaddingTop(3); c.setPaddingBottom(3); c.setPaddingLeft(4); c.setPaddingRight(4);
        tabla.addCell(c);
    }

    private void addTdBool(PdfPTable tabla, boolean val, Color bg) {
        addTd(tabla, val ? "Sí" : "No", bg);
    }

    private void addFilaVacia(PdfPTable tabla, int cols) {
        PdfPCell c = new PdfPCell(new Phrase("Sin registros", new Font(Font.HELVETICA, 8, Font.ITALIC, Color.GRAY)));
        c.setColspan(cols);
        c.setBorderColor(new Color(210, 220, 200));
        c.setPadding(6);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        tabla.addCell(c);
    }

    private Image cargarLogo(String logoUrl) {
        if (logoUrl == null || logoUrl.isBlank()) return null;
        try {
            if (logoUrl.startsWith("http://") || logoUrl.startsWith("https://")) {
                return Image.getInstance(new URL(logoUrl));
            }
            String path = "static/" + (logoUrl.startsWith("/") ? logoUrl.substring(1) : logoUrl);
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
                if (is == null) return null;
                return Image.getInstance(is.readAllBytes());
            }
        } catch (Exception e) {
            return null;
        }
    }

    private void addTdFirma(PdfPTable tabla, String firmaData, Color bg) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(bg);
        c.setBorderColor(new Color(210, 220, 200));
        c.setPaddingTop(3); c.setPaddingBottom(3); c.setPaddingLeft(4); c.setPaddingRight(4);
        c.setFixedHeight(28);
        if (firmaData != null && firmaData.startsWith("data:image/")) {
            try {
                String base64 = firmaData.substring(firmaData.indexOf(',') + 1);
                byte[] imgBytes = java.util.Base64.getDecoder().decode(base64);
                Image img = Image.getInstance(imgBytes);
                img.scaleToFit(60, 20);
                c.addElement(img);
            } catch (Exception e) {
                c.addElement(new Phrase("Firmado", F_TD));
            }
        } else {
            c.addElement(new Phrase(nvl(firmaData), F_TD));
        }
        tabla.addCell(c);
    }

    private static String nvl(String s) {
        return s != null && !s.isBlank() ? s : "—";
    }
}
