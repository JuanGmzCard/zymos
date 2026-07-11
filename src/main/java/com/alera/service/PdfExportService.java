package com.alera.service;

import com.alera.config.ExportBranding;
import com.alera.model.AdicionHervor;
import com.alera.model.EscalonMacerado;
import com.alera.model.EvaluacionSensorial;
import com.alera.model.Ingrediente;
import com.alera.model.LecturaFermentacion;
import com.alera.model.LoteCerveza;
import com.alera.model.LoteItemFactura;
import com.alera.model.Receta;
import com.alera.model.RecetaIngrediente;
import com.alera.dto.RentabilidadLoteDto;
import com.alera.model.Venta;
import com.alera.model.VentaItem;
import com.alera.model.enums.EstadoVenta;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.*;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

@Service
public class PdfExportService {

    private final MessageSource messageSource;

    public PdfExportService(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    // Colores neutros fijos (no son parte del branding del tenant)
    private static final Color C_GRIS  = new Color(108, 117, 125);
    private static final Color C_BORDE = new Color(222, 226, 230);

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_DT    = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_HORA  = DateTimeFormatter.ofPattern("HH:mm");

    private static final ThreadLocal<Locale> LOCALE_HOLDER = new ThreadLocal<>();

    private String t(String key) {
        Locale loc = LOCALE_HOLDER.get();
        return messageSource.getMessage(key, null, key, loc != null ? loc : Locale.forLanguageTag("es"));
    }

    private String tf(String key, Object... args) {
        Locale loc = LOCALE_HOLDER.get();
        return messageSource.getMessage(key, args, key, loc != null ? loc : Locale.forLanguageTag("es"));
    }

    /** Paleta de colores calculada por request a partir del branding del tenant. */
    private record Pal(
            Color verde, Color verdeOscuro, Color dorado,
            Color crema, Color fondo, Color verdeClaro
    ) {
        static Pal of(ExportBranding b) {
            return new Pal(
                    b.primary(), b.primaryDark(), b.accent(), b.cream(), b.background(),
                    ExportBranding.lighten(b.primary(), 0.45f)
            );
        }
    }

    // ── PDF Lote ─────────────────────────────────────────────────────

    public byte[] generarPdfLote(LoteCerveza lote, ExportBranding branding,
                                  List<LecturaFermentacion> lecturas,
                                  List<EvaluacionSensorial> evaluaciones,
                                  List<Venta> ventas,
                                  Locale locale) {
        LOCALE_HOLDER.set(locale);
        try {
            Pal pal = Pal.of(branding);
            String brandName = branding.name();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 36, 36, 50, 45);
            try {
                PdfWriter.getInstance(doc, baos);
                doc.open();

                addCabeceraPdf(doc, lote, brandName, pal);

                addTituloPdf(doc, t("pdf.title.info_lote"), pal);
                addTablaInfoLote(doc, lote, pal);

                if (lote.getDensidadInicial() != null) {
                    addTituloPdf(doc, t("pdf.title.parametros_calidad"), pal);
                    addTablaMetricas(doc, lote, pal);
                }

                if (lote.getReceta() != null &&
                        (lote.getReceta().getOgObjetivo() != null || lote.getReceta().getFgObjetivo() != null)) {
                    addTituloPdf(doc, t("pdf.title.comparativa_receta_lote"), pal);
                    addComparativaRecetaLote(doc, lote, pal);
                }

                if (!lote.getIngredientes().isEmpty()) {
                    addTituloPdf(doc, t("pdf.title.ingredientes"), pal);
                    addIngredientes(doc, lote, pal);
                }

                addTituloPdf(doc, t("pdf.title.fases_proceso"), pal);
                addTablaFases(doc, lote, pal);

                if (lecturas != null && !lecturas.isEmpty()) {
                    addCurvaFermentacion(doc, lote, lecturas, pal);
                }

                if (lote.getCarbMetodo() != null || lote.getCarbCo2Objetivo() != null
                        || lote.getCarbDestino() != null) {
                    addTituloPdf(doc, t("pdf.title.carbonatacion_detalle"), pal);
                    addDetalleCarbonacion(doc, lote, pal);
                }

                if (lote.getCostoTotal() != null) {
                    addTituloPdf(doc, t("pdf.title.costo_produccion"), pal);
                    addCostos(doc, lote, pal);
                }

                boolean hayObs  = lote.getObservaciones() != null && !lote.getObservaciones().isBlank();
                boolean hayCata = lote.getNotasCata() != null && !lote.getNotasCata().isBlank();
                if (hayObs || hayCata) {
                    addTituloPdf(doc, t("pdf.title.observaciones_cata"), pal);
                    addNotas(doc, lote, hayObs, hayCata, pal);
                }

                if (evaluaciones != null && !evaluaciones.isEmpty()) {
                    addTituloPdf(doc, t("pdf.title.evaluacion_sensorial"), pal);
                    addEvaluaciones(doc, evaluaciones, pal);
                }

                if (ventas != null && !ventas.isEmpty()) {
                    addTituloPdf(doc, t("pdf.title.ventas_despacho"), pal);
                    addVentas(doc, ventas, pal);
                }

                doc.add(new Paragraph("\n"));
                Paragraph pie = new Paragraph(
                        tf("pdf.text.pie", LocalDateTime.now().format(FMT_DT), brandName),
                        new Font(Font.HELVETICA, 7, Font.ITALIC, C_GRIS));
                pie.setAlignment(Element.ALIGN_RIGHT);
                doc.add(pie);

                doc.close();
            } catch (Exception e) {
                throw new RuntimeException("Error generando PDF del lote " + lote.getCodigoLote(), e);
            }
            return baos.toByteArray();
        } finally {
            LOCALE_HOLDER.remove();
        }
    }

    // ── PDF Receta ───────────────────────────────────────────────────

    public byte[] generarPdfReceta(Receta receta, ExportBranding branding, Locale locale) {
        LOCALE_HOLDER.set(locale);
        try {
            Pal pal = Pal.of(branding);
            String brandName = branding.name();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 36, 36, 50, 45);
            try {
                PdfWriter.getInstance(doc, baos);
                doc.open();

                // Cabecera
                PdfPTable header = new PdfPTable(1);
                header.setWidthPercentage(100);
                header.setSpacingAfter(10);
                PdfPCell hCell = new PdfPCell();
                hCell.setBackgroundColor(pal.verde());
                hCell.setBorder(0);
                hCell.setPaddingTop(12);
                hCell.setPaddingBottom(12);
                hCell.setPaddingLeft(14);
                hCell.addElement(new Paragraph(brandName.toUpperCase(),
                        new Font(Font.HELVETICA, 7, Font.NORMAL, pal.dorado())));
                Paragraph tituloH = new Paragraph(t("pdf.title.ficha_receta"),
                        new Font(Font.HELVETICA, 13, Font.BOLD, pal.crema()));
                tituloH.setSpacingBefore(4);
                hCell.addElement(tituloH);
                Paragraph subH = new Paragraph(
                        receta.getNombre() + (receta.getEstilo() != null ? "  ·  " + receta.getEstilo() : ""),
                        new Font(Font.HELVETICA, 9, Font.ITALIC, pal.verdeClaro()));
                subH.setSpacingBefore(4);
                hCell.addElement(subH);
                header.addCell(hCell);
                doc.add(header);

                // Información básica
                addTituloPdf(doc, t("pdf.title.info_general"), pal);
                Font lbl = new Font(Font.HELVETICA, 8, Font.BOLD, pal.verde());
                Font val = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
                PdfPTable info = new PdfPTable(new float[]{1.2f, 2, 1.2f, 2});
                info.setWidthPercentage(100);
                par(info, t("pdf.label.nombre"),    receta.getNombre() != null ? receta.getNombre() : "—", lbl, val, pal);
                par(info, t("pdf.label.estilo"),    receta.getEstilo() != null ? receta.getEstilo() : "—", lbl, val, pal);
                par(info, t("pdf.label.estado"),    receta.isActiva() ? t("pdf.text.activa") : t("pdf.text.inactiva"), lbl, val, pal);
                par(info, t("pdf.label.version"),   receta.getVersion() != null ? "v" + receta.getVersion() : "v1", lbl, val, pal);
                par(info, t("pdf.label.hervor"),    receta.getTiempoHervorMinutos() != null
                        ? receta.getTiempoHervorMinutos() + " min" : "—", lbl, val, pal);
                par(info, t("pdf.label.vol_base"), receta.getVolumenBase() != null
                        ? receta.getVolumenBase() + " L" : "—", lbl, val, pal);
                if (receta.getAguaMacerado() != null) {
                    par(info, t("pdf.label.agua_macerado"), receta.getAguaMacerado() + " "
                            + (receta.getUnidadAguaMacerado() != null ? receta.getUnidadAguaMacerado() : "L"), lbl, val, pal);
                    par(info, t("pdf.label.agua_sparge"), receta.getAguaSparge() != null
                            ? receta.getAguaSparge() + " "
                              + (receta.getUnidadAguaSparge() != null ? receta.getUnidadAguaSparge() : "L") : "—",
                            lbl, val, pal);
                }
                if (receta.getPhAgua() != null) {
                    par(info, t("pdf.label.ph_agua_cap"), receta.getPhAgua().toString(), lbl, val, pal);
                    par(info, "", "", lbl, val, pal);
                }
                doc.add(info);

                if (notBlank(receta.getDescripcion())) {
                    Font lblD = new Font(Font.HELVETICA, 8, Font.BOLD, pal.verde());
                    Paragraph pd = new Paragraph(t("pdf.label.descripcion"), lblD);
                    pd.setSpacingBefore(4);
                    doc.add(pd);
                    doc.add(new Paragraph(receta.getDescripcion(),
                            new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY)));
                }

                // Métricas objetivo
                if (receta.getOgObjetivo() != null || receta.getFgObjetivo() != null) {
                    addTituloPdf(doc, t("pdf.title.parametros_objetivo"), pal);
                    Font lblM = new Font(Font.HELVETICA, 7, Font.BOLD, pal.verde());
                    Font valM = new Font(Font.HELVETICA, 10, Font.BOLD, pal.verdeOscuro());
                    Font subM = new Font(Font.HELVETICA, 8, Font.NORMAL, C_GRIS);
                    PdfPTable metricasR = new PdfPTable(3);
                    metricasR.setWidthPercentage(60);
                    if (receta.getOgObjetivo() != null)
                        metricaCell(metricasR, "OG", String.valueOf(receta.getOgObjetivo()),
                                lblM, valM, subM, t("pdf.label.gravedad_inicial"), pal);
                    if (receta.getFgObjetivo() != null)
                        metricaCell(metricasR, "FG", String.valueOf(receta.getFgObjetivo()),
                                lblM, valM, subM, t("pdf.label.gravedad_final"), pal);
                    if (receta.getOgObjetivo() != null && receta.getFgObjetivo() != null) {
                        double abvObj = (receta.getOgObjetivo() - receta.getFgObjetivo()) * 0.13125;
                        metricaCell(metricasR, "ABV",
                                String.format(java.util.Locale.US, "%.2f%%", abvObj),
                                lblM, valM, subM, t("pdf.label.estimado"), pal);
                    }
                    doc.add(metricasR);
                }

                // Ingredientes
                if (!receta.getIngredientes().isEmpty()) {
                    addTituloPdf(doc, t("pdf.title.ingredientes"), pal);
                    addIngredientesReceta(doc, receta, pal);
                }

                // Escalones de macerado
                if (!receta.getEscalones().isEmpty()) {
                    addTituloPdf(doc, t("pdf.title.escalones_macerado"), pal);
                    Font thF = new Font(Font.HELVETICA, 8, Font.BOLD, pal.crema());
                    Font tdF = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.DARK_GRAY);
                    PdfPTable tEsc = new PdfPTable(new float[]{0.5f, 2, 1, 1});
                    tEsc.setWidthPercentage(100);
                    for (String h : new String[]{t("pdf.header.num"), t("pdf.header.escalon"), t("pdf.header.duracion"), t("pdf.header.temp")}) {
                        PdfPCell c = new PdfPCell(new Phrase(h, thF));
                        c.setBackgroundColor(pal.verdeOscuro()); c.setBorder(0); c.setPadding(5);
                        tEsc.addCell(c);
                    }
                    int idx = 1;
                    for (EscalonMacerado e : receta.getEscalones()) {
                        tableCell(tEsc, String.valueOf(idx++), tdF);
                        tableCell(tEsc, e.getNombre() != null ? e.getNombre() : "—", tdF);
                        tableCell(tEsc, e.getDuracionMinutos() != null ? e.getDuracionMinutos() + " min" : "—", tdF);
                        tableCell(tEsc, e.getTemperaturaC() != null ? e.getTemperaturaC() + " °C" : "—", tdF);
                    }
                    doc.add(tEsc);
                }

                // Hervor / adiciones
                if (!receta.getAdicionesHervor().isEmpty()) {
                    addTituloPdf(doc, t("pdf.title.hervor_adiciones"), pal);
                    Font thF = new Font(Font.HELVETICA, 8, Font.BOLD, pal.crema());
                    Font tdF = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.DARK_GRAY);
                    PdfPTable tAdic = new PdfPTable(new float[]{3, 1.5f, 1.5f});
                    tAdic.setWidthPercentage(100);
                    for (String h : new String[]{t("pdf.header.insumo"), t("pdf.header.min_restantes"), t("pdf.header.cantidad")}) {
                        PdfPCell c = new PdfPCell(new Phrase(h, thF));
                        c.setBackgroundColor(pal.verdeOscuro()); c.setBorder(0); c.setPadding(5);
                        tAdic.addCell(c);
                    }
                    for (AdicionHervor a : receta.getAdicionesHervor()) {
                        tableCell(tAdic, a.getNombre() != null ? a.getNombre() : "—", tdF);
                        String mins = a.getMinutosRestantes() != null
                                ? (a.getMinutosRestantes() == 0 ? t("pdf.text.flameout") : a.getMinutosRestantes() + " min") : "—";
                        tableCell(tAdic, mins, tdF);
                        tableCell(tAdic, a.getCantidad() != null
                                ? a.getCantidad() + " " + (a.getUnidad() != null ? a.getUnidad() : "") : "—", tdF);
                    }
                    doc.add(tAdic);
                }

                // Notas
                if (notBlank(receta.getNotas())) {
                    addTituloPdf(doc, t("pdf.title.notas_tecnicas"), pal);
                    doc.add(new Paragraph(receta.getNotas(),
                            new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY)));
                }

                // Pie
                doc.add(new Paragraph("\n"));
                Paragraph pie = new Paragraph(
                        tf("pdf.text.pie", LocalDateTime.now().format(FMT_DT), brandName),
                        new Font(Font.HELVETICA, 7, Font.ITALIC, C_GRIS));
                pie.setAlignment(Element.ALIGN_RIGHT);
                doc.add(pie);

                doc.close();
            } catch (Exception e) {
                throw new RuntimeException("Error generando PDF de receta " + receta.getNombre(), e);
            }
            return baos.toByteArray();
        } finally {
            LOCALE_HOLDER.remove();
        }
    }

    // ── PDF Comparativa ──────────────────────────────────────────────

    public byte[] generarPdfComparativa(List<LoteCerveza> lotes,
                                         Map<String, Long> mejoresMax,
                                         Long mejorCpl,
                                         ExportBranding branding,
                                         Locale locale) {
        LOCALE_HOLDER.set(locale);
        try {
            Pal pal = Pal.of(branding);
            String brandName = branding.name();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4.rotate(), 36, 36, 50, 40);
            try {
                PdfWriter.getInstance(doc, baos);
                doc.open();

                // Cabecera
                PdfPTable header = new PdfPTable(1);
                header.setWidthPercentage(100);
                header.setSpacingAfter(14);
                PdfPCell hCell = new PdfPCell();
                hCell.setBackgroundColor(pal.verde());
                hCell.setBorder(0);
                hCell.setPadding(12);
                hCell.addElement(new Paragraph(brandName.toUpperCase(),
                        new Font(Font.HELVETICA, 7, Font.NORMAL, pal.dorado())));
                Paragraph titulo = new Paragraph(t("pdf.title.comparativa_lotes"),
                        new Font(Font.HELVETICA, 14, Font.BOLD, pal.crema()));
                titulo.setSpacingBefore(4);
                hCell.addElement(titulo);
                String subtitulo = lotes.stream().map(LoteCerveza::getCodigoLote)
                        .reduce((a, b) -> a + "  ·  " + b).orElse("");
                hCell.addElement(new Paragraph(subtitulo,
                        new Font(Font.HELVETICA, 8, Font.ITALIC, pal.crema())));
                header.addCell(hCell);
                doc.add(header);

                // Tabla de métricas (filas = métricas, columnas = lotes)
                int cols = lotes.size() + 1;
                PdfPTable tabla = new PdfPTable(cols);
                float[] anchos = new float[cols];
                anchos[0] = 2.5f;
                for (int i = 1; i < cols; i++) anchos[i] = 1.5f;
                tabla.setWidths(anchos);
                tabla.setWidthPercentage(100);
                tabla.setSpacingAfter(14);

                Font fHeader   = new Font(Font.HELVETICA, 8, Font.BOLD,  pal.crema());
                Font fLabel    = new Font(Font.HELVETICA, 7, Font.BOLD,  pal.verde());
                Font fValor    = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.BLACK);
                Font fMejorMax = new Font(Font.HELVETICA, 8, Font.BOLD, new Color(180, 130, 0));
                Font fMejorMin = new Font(Font.HELVETICA, 8, Font.BOLD, new Color(25, 135, 84));

                addCeldaHeader(tabla, t("pdf.header.metrica"), fHeader, pal);
                for (LoteCerveza l : lotes) addCeldaHeader(tabla, l.getCodigoLote(), fHeader, pal);

                // codes: internal key used for valorMetrica switch + pdf.metric.{code} for display
                String[][] metricas = {
                    {"estilo",      null},
                    {"og",          null},
                    {"fg",          null},
                    {"abv",         "abv"},
                    {"atenuacion",  "atenuacion"},
                    {"eficiencia",  "eficiencia"},
                    {"litros",      "litros"},
                    {"costo_total", null},
                    {"cpl",         "cpl"}
                };

                boolean alt = false;
                for (String[] m : metricas) {
                    Color bg = alt ? pal.fondo() : Color.WHITE;
                    alt = !alt;
                    addCeldaLabel(tabla, t("pdf.metric." + m[0]), fLabel, bg);
                    for (LoteCerveza l : lotes) {
                        String texto = valorMetrica(l, m[0]);
                        boolean esMejorMax = m[1] != null && !m[1].equals("cpl")
                                && mejoresMax.containsKey(m[1]) && mejoresMax.get(m[1]).equals(l.getId());
                        boolean esMejorMin = "cpl".equals(m[1]) && l.getId().equals(mejorCpl);
                        Font f = esMejorMax ? fMejorMax : esMejorMin ? fMejorMin : fValor;
                        String celText = texto + (esMejorMax ? " ★" : esMejorMin ? " ↓" : "");
                        PdfPCell c = new PdfPCell(new Phrase(celText, f));
                        c.setHorizontalAlignment(Element.ALIGN_CENTER);
                        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
                        c.setBackgroundColor(bg);
                        c.setBorderColor(C_BORDE);
                        c.setPadding(5);
                        tabla.addCell(c);
                    }
                }
                doc.add(tabla);

                // Notas de cata
                boolean hayNotas = lotes.stream().anyMatch(l ->
                        l.getNotasCata() != null && !l.getNotasCata().isBlank());
                if (hayNotas) {
                    doc.add(new Paragraph(t("pdf.label.notas_cata"),
                            new Font(Font.HELVETICA, 9, Font.BOLD, pal.verde())));
                    for (LoteCerveza l : lotes) {
                        if (l.getNotasCata() != null && !l.getNotasCata().isBlank()) {
                            doc.add(new Paragraph(l.getCodigoLote() + ": " + l.getNotasCata(),
                                    new Font(Font.HELVETICA, 7, Font.NORMAL, C_GRIS)));
                        }
                    }
                }

                // Pie
                Paragraph pie = new Paragraph(
                        tf("pdf.text.pie_corto", LocalDateTime.now().format(FMT_DT), brandName),
                        new Font(Font.HELVETICA, 7, Font.ITALIC, C_GRIS));
                pie.setAlignment(Element.ALIGN_RIGHT);
                doc.add(pie);

                doc.close();
            } catch (Exception e) {
                throw new RuntimeException("Error generando PDF de comparativa", e);
            }
            return baos.toByteArray();
        } finally {
            LOCALE_HOLDER.remove();
        }
    }

    // ── Helpers PDF Lote ─────────────────────────────────────────────

    private void addCabeceraPdf(Document doc, LoteCerveza lote, String brandName, Pal pal)
            throws DocumentException {
        PdfPTable header = new PdfPTable(new float[]{3, 2});
        header.setWidthPercentage(100);
        header.setSpacingAfter(10);

        PdfPCell izq = new PdfPCell();
        izq.setBackgroundColor(pal.verde());
        izq.setBorder(0);
        izq.setPaddingTop(12);  izq.setPaddingBottom(12);
        izq.setPaddingLeft(14); izq.setPaddingRight(8);

        Paragraph brand = new Paragraph(brandName.toUpperCase(),
                new Font(Font.HELVETICA, 7, Font.NORMAL, pal.dorado()));
        Paragraph tituloP = new Paragraph(t("pdf.title.ficha_trazabilidad"),
                new Font(Font.HELVETICA, 13, Font.BOLD, pal.crema()));
        tituloP.setSpacingBefore(4);
        String fase = lote.isCompletado() ? t("pdf.text.completado") : lote.getFaseActual().toUpperCase();
        Paragraph faseP = new Paragraph(fase,
                new Font(Font.HELVETICA, 8, Font.NORMAL, pal.verdeClaro()));
        faseP.setSpacingBefore(6);
        izq.addElement(brand); izq.addElement(tituloP); izq.addElement(faseP);
        header.addCell(izq);

        PdfPCell der = new PdfPCell();
        der.setBackgroundColor(pal.verdeOscuro());
        der.setBorder(0);
        der.setPaddingTop(12);  der.setPaddingBottom(12);
        der.setPaddingLeft(8);  der.setPaddingRight(14);

        Paragraph codigo = new Paragraph(lote.getCodigoLote(),
                new Font(Font.HELVETICA, 18, Font.BOLD, pal.dorado()));
        codigo.setAlignment(Element.ALIGN_RIGHT);
        Paragraph estilo = new Paragraph(lote.getEstilo(),
                new Font(Font.HELVETICA, 9, Font.NORMAL, pal.crema()));
        estilo.setAlignment(Element.ALIGN_RIGHT); estilo.setSpacingBefore(3);
        String fechaStr = lote.getFechaElaboracion() != null
                ? lote.getFechaElaboracion().format(FMT_FECHA) : "—";
        Paragraph fecha = new Paragraph(fechaStr,
                new Font(Font.HELVETICA, 8, Font.NORMAL, pal.verdeClaro()));
        fecha.setAlignment(Element.ALIGN_RIGHT); fecha.setSpacingBefore(4);
        der.addElement(codigo); der.addElement(estilo); der.addElement(fecha);
        header.addCell(der);

        doc.add(header);
    }

    private void addTituloPdf(Document doc, String titulo, Pal pal) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(10);
        t.setSpacingAfter(4);
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(pal.verde());
        cell.setBorder(0);
        cell.setPaddingTop(5); cell.setPaddingBottom(5); cell.setPaddingLeft(8);
        cell.addElement(new Paragraph(titulo, new Font(Font.HELVETICA, 8, Font.BOLD, pal.crema())));
        t.addCell(cell);
        doc.add(t);
    }

    private void addTablaInfoLote(Document doc, LoteCerveza lote, Pal pal) throws DocumentException {
        Font lbl = new Font(Font.HELVETICA, 8, Font.BOLD, pal.verde());
        Font val = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
        PdfPTable t = new PdfPTable(new float[]{1.2f, 2, 1.2f, 2});
        t.setWidthPercentage(100);
        par(t, t("pdf.label.codigo"),  lote.getCodigoLote(), lbl, val, pal);
        par(t, t("pdf.label.estilo"),  lote.getEstilo(), lbl, val, pal);

        int numCoc = lote.getNumeroElaboraciones() != null ? lote.getNumeroElaboraciones() : 1;
        String fechaLabel = numCoc >= 2
                ? t("pdf.label.fecha_elaboracion") + " (S1)"
                : t("pdf.label.fecha_elaboracion");
        par(t, fechaLabel,
                lote.getFechaElaboracion() != null ? lote.getFechaElaboracion().format(FMT_FECHA) : "—", lbl, val, pal);
        par(t, t("pdf.label.fermentador"),
                lote.getEquipoFermentador() != null ? lote.getEquipoFermentador().getNombre() : "—", lbl, val, pal);

        if (numCoc >= 2) {
            par(t, t("pdf.label.elaboraciones"), String.valueOf(numCoc), lbl, val, pal);
            par(t, t("pdf.label.fecha_s2"),
                    lote.getFechaSegundaElaboracion() != null ? lote.getFechaSegundaElaboracion().format(FMT_FECHA) : "—",
                    lbl, val, pal);
        }
        if (numCoc >= 3) {
            par(t, t("pdf.label.fecha_s3"),
                    lote.getFechaTerceraElaboracion() != null ? lote.getFechaTerceraElaboracion().format(FMT_FECHA) : "—",
                    lbl, val, pal);
        }
        if (numCoc >= 4) {
            par(t, t("pdf.label.fecha_s4"),
                    lote.getFechaCuartaElaboracion() != null ? lote.getFechaCuartaElaboracion().format(FMT_FECHA) : "—",
                    lbl, val, pal);
        }

        // Hora inicio / fin por sesión
        String horaIni1 = lote.getHoraInicioPrimeraElaboracion() != null ? lote.getHoraInicioPrimeraElaboracion().format(FMT_HORA) : "—";
        String horaFin1 = lote.getHoraFinPrimeraElaboracion()    != null ? lote.getHoraFinPrimeraElaboracion().format(FMT_HORA)    : "—";
        String labelIni = numCoc >= 2 ? t("pdf.label.hora_inicio") + " S1" : t("pdf.label.hora_inicio");
        String labelFin = numCoc >= 2 ? t("pdf.label.hora_fin")    + " S1" : t("pdf.label.hora_fin");
        par(t, labelIni, horaIni1, lbl, val, pal);
        par(t, labelFin, horaFin1, lbl, val, pal);
        if (numCoc >= 2) {
            String horaIni2 = lote.getHoraInicioSegundaElaboracion() != null ? lote.getHoraInicioSegundaElaboracion().format(FMT_HORA) : "—";
            String horaFin2 = lote.getHoraFinSegundaElaboracion()    != null ? lote.getHoraFinSegundaElaboracion().format(FMT_HORA)    : "—";
            par(t, t("pdf.label.hora_inicio") + " S2", horaIni2, lbl, val, pal);
            par(t, t("pdf.label.hora_fin")    + " S2", horaFin2, lbl, val, pal);
        }
        if (numCoc >= 3) {
            String horaIni3 = lote.getHoraInicioTerceraElaboracion() != null ? lote.getHoraInicioTerceraElaboracion().format(FMT_HORA) : "—";
            String horaFin3 = lote.getHoraFinTerceraElaboracion()    != null ? lote.getHoraFinTerceraElaboracion().format(FMT_HORA)    : "—";
            par(t, t("pdf.label.hora_inicio") + " S3", horaIni3, lbl, val, pal);
            par(t, t("pdf.label.hora_fin")    + " S3", horaFin3, lbl, val, pal);
        }
        if (numCoc >= 4) {
            String horaIni4 = lote.getHoraInicioCuartaElaboracion() != null ? lote.getHoraInicioCuartaElaboracion().format(FMT_HORA) : "—";
            String horaFin4 = lote.getHoraFinCuartaElaboracion()    != null ? lote.getHoraFinCuartaElaboracion().format(FMT_HORA)    : "—";
            par(t, t("pdf.label.hora_inicio") + " S4", horaIni4, lbl, val, pal);
            par(t, t("pdf.label.hora_fin")    + " S4", horaFin4, lbl, val, pal);
        }

        String recetaLabel = numCoc >= 2 ? t("pdf.label.receta_s1") : t("pdf.label.receta");
        par(t, recetaLabel,
                lote.getReceta() != null ? lote.getReceta().getNombre() : "—", lbl, val, pal);
        if (numCoc >= 2 && lote.getReceta2() != null) {
            par(t, t("pdf.label.receta_s2"), lote.getReceta2().getNombre(), lbl, val, pal);
        }
        if (numCoc >= 3 && lote.getReceta3() != null) {
            par(t, t("pdf.label.receta_s3"), lote.getReceta3().getNombre(), lbl, val, pal);
        }
        if (numCoc >= 4 && lote.getReceta4() != null) {
            par(t, t("pdf.label.receta_s4"), lote.getReceta4().getNombre(), lbl, val, pal);
        }
        par(t, t("pdf.label.creado_por"),
                lote.getCreatedBy() != null ? lote.getCreatedBy() : "—", lbl, val, pal);
        doc.add(t);
    }

    private void addTablaMetricas(Document doc, LoteCerveza lote, Pal pal) throws DocumentException {
        Font lbl  = new Font(Font.HELVETICA, 7,  Font.BOLD,   pal.verde());
        Font val  = new Font(Font.HELVETICA, 10, Font.BOLD,   pal.verdeOscuro());
        Font sub  = new Font(Font.HELVETICA, 8,  Font.NORMAL, C_GRIS);
        Font lblP = new Font(Font.HELVETICA, 8,  Font.BOLD,   pal.verde());
        Font valP = new Font(Font.HELVETICA, 9,  Font.NORMAL, Color.DARK_GRAY);

        int numCoc = lote.getNumeroElaboraciones() != null ? lote.getNumeroElaboraciones() : 1;

        PdfPTable tp = new PdfPTable(new float[]{1.2f, 2, 1.2f, 2});
        tp.setWidthPercentage(100); tp.setSpacingAfter(6);

        if (numCoc >= 2) {
            java.math.BigDecimal totalAgua = java.math.BigDecimal.ZERO;
            if (lote.getAguaUtilizada()          != null) totalAgua = totalAgua.add(lote.getAguaUtilizada());
            if (lote.getAguaSegundaElaboracion()  != null) totalAgua = totalAgua.add(lote.getAguaSegundaElaboracion());
            if (numCoc >= 3 && lote.getAguaTerceraElaboracion() != null) totalAgua = totalAgua.add(lote.getAguaTerceraElaboracion());
            if (numCoc >= 4 && lote.getAguaCuartaElaboracion()  != null) totalAgua = totalAgua.add(lote.getAguaCuartaElaboracion());
            par(tp, t("pdf.label.agua_total"),
                    totalAgua.compareTo(java.math.BigDecimal.ZERO) > 0
                            ? totalAgua.stripTrailingZeros().toPlainString() + " L" : "—",
                    lblP, valP, pal);
        }
        String aguaS1Label = numCoc >= 2 ? t("pdf.label.agua_s1") : t("pdf.label.agua_utilizada");
        par(tp, aguaS1Label,
                lote.getAguaUtilizada() != null
                        ? lote.getAguaUtilizada().stripTrailingZeros().toPlainString() + " L" : "—",
                lblP, valP, pal);
        par(tp, t("pdf.label.ph_agua"),
                lote.getPhAgua() != null ? lote.getPhAgua().toString() : "—", lblP, valP, pal);
        if (numCoc >= 2) {
            par(tp, t("pdf.label.agua_s2"),
                    lote.getAguaSegundaElaboracion() != null
                            ? lote.getAguaSegundaElaboracion().stripTrailingZeros().toPlainString() + " L" : "—",
                    lblP, valP, pal);
        }
        if (numCoc >= 3) {
            par(tp, t("pdf.label.agua_s3"),
                    lote.getAguaTerceraElaboracion() != null
                            ? lote.getAguaTerceraElaboracion().stripTrailingZeros().toPlainString() + " L" : "—",
                    lblP, valP, pal);
        }
        if (numCoc >= 4) {
            par(tp, t("pdf.label.agua_s4"),
                    lote.getAguaCuartaElaboracion() != null
                            ? lote.getAguaCuartaElaboracion().stripTrailingZeros().toPlainString() + " L" : "—",
                    lblP, valP, pal);
        }

        String volLabel = numCoc >= 2 ? t("pdf.label.vol_final") + " (Total)" : t("pdf.label.vol_final");
        par(tp, volLabel,
                lote.getLitrosFinales() != null
                        ? lote.getLitrosFinales().stripTrailingZeros().toPlainString() + " L" : "—",
                lblP, valP, pal);
        if (numCoc >= 2) {
            par(tp, t("pdf.label.vol_s1"),
                    lote.getVolumenFinalPrimeraElaboracion() != null
                            ? lote.getVolumenFinalPrimeraElaboracion().stripTrailingZeros().toPlainString() + " L" : "—",
                    lblP, valP, pal);
            par(tp, t("pdf.label.vol_s2"),
                    lote.getVolumenFinalSegundaElaboracion() != null
                            ? lote.getVolumenFinalSegundaElaboracion().stripTrailingZeros().toPlainString() + " L" : "—",
                    lblP, valP, pal);
        }
        if (numCoc >= 3) {
            par(tp, t("pdf.label.vol_s3"),
                    lote.getVolumenFinalTerceraElaboracion() != null
                            ? lote.getVolumenFinalTerceraElaboracion().stripTrailingZeros().toPlainString() + " L" : "—",
                    lblP, valP, pal);
        }
        if (numCoc >= 4) {
            par(tp, t("pdf.label.vol_s4"),
                    lote.getVolumenFinalCuartaElaboracion() != null
                            ? lote.getVolumenFinalCuartaElaboracion().stripTrailingZeros().toPlainString() + " L" : "—",
                    lblP, valP, pal);
        }

        par(tp, t("pdf.label.clarificante"),
                notBlank(lote.getClarificante()) ? lote.getClarificante() : "—", lblP, valP, pal);
        doc.add(tp);

        PdfPTable tm = new PdfPTable(6);
        tm.setWidthPercentage(100);
        metricaCell(tm, "OG",         lote.getDensidadInicial() != null ? String.valueOf(lote.getDensidadInicial()) : "—",    lbl, val, sub, t("pdf.label.gravedad_inicial"), pal);
        String fgSub = lote.getDensidadFinalFecha() != null
                ? tf("pdf.text.gravedad_final_fecha", lote.getDensidadFinalFecha().format(FMT_FECHA))
                : t("pdf.label.gravedad_final");
        metricaCell(tm, "FG",         lote.getDensidadFinal()   != null ? String.valueOf(lote.getDensidadFinal())   : t("pdf.text.pendiente"), lbl, val, sub, fgSub, pal);
        metricaCell(tm, "ABV",        lote.getAbv()               != null ? lote.getAbv()               + "%" : "—", lbl, val, sub, t("pdf.label.pct_vol"),   pal);
        metricaCell(tm, t("pdf.label.atenuacion"), lote.getAtenuacionAparente()  != null ? lote.getAtenuacionAparente()  + "%" : "—", lbl, val, sub, t("pdf.label.aparente"), pal);
        metricaCell(tm, t("pdf.label.eficiencia"), lote.getEficienciaMacerado() != null ? lote.getEficienciaMacerado() + "%" : "—", lbl, val, sub, t("pdf.label.macerado"), pal);
        metricaCell(tm, t("pdf.label.litros"),     lote.getLitrosFinales()      != null ? lote.getLitrosFinales()      + " L" : "—", lbl, val, sub, t("pdf.label.vol_final"), pal);
        doc.add(tm);

        if (numCoc >= 2) {
            int numOgCols = numCoc >= 4 ? 4 : numCoc >= 3 ? 3 : 2;
            PdfPTable tog = new PdfPTable(numOgCols);
            tog.setWidthPercentage(100); tog.setSpacingBefore(6);

            String ogS1Val = lote.getOgPrimeraElaboracion() != null ? String.valueOf(lote.getOgPrimeraElaboracion()) : "—";
            metricaCell(tog, t("pdf.label.og_s1"), ogS1Val, lbl, val, sub, "S1", pal);

            String ogS2Val = lote.getOgSegundaElaboracion() != null ? String.valueOf(lote.getOgSegundaElaboracion()) : "—";
            String ogS2Sub = lote.getOgBrixSegundaElaboracion() != null
                    ? lote.getOgBrixSegundaElaboracion().stripTrailingZeros().toPlainString() + " °Brix"
                    : "S2";
            metricaCell(tog, t("pdf.label.og_s2"), ogS2Val, lbl, val, sub, ogS2Sub, pal);

            if (numCoc >= 3) {
                String ogS3Val = lote.getOgTerceraElaboracion() != null ? String.valueOf(lote.getOgTerceraElaboracion()) : "—";
                String ogS3Sub = lote.getOgBrixTerceraElaboracion() != null
                        ? lote.getOgBrixTerceraElaboracion().stripTrailingZeros().toPlainString() + " °Brix"
                        : "S3";
                metricaCell(tog, t("pdf.label.og_s3"), ogS3Val, lbl, val, sub, ogS3Sub, pal);
            }
            if (numCoc >= 4) {
                String ogS4Val = lote.getOgCuartaElaboracion() != null ? String.valueOf(lote.getOgCuartaElaboracion()) : "—";
                String ogS4Sub = lote.getOgBrixCuartaElaboracion() != null
                        ? lote.getOgBrixCuartaElaboracion().stripTrailingZeros().toPlainString() + " °Brix"
                        : "S4";
                metricaCell(tog, t("pdf.label.og_s4"), ogS4Val, lbl, val, sub, ogS4Sub, pal);
            }
            doc.add(tog);
        }
    }

    private void metricaCell(PdfPTable t, String label, String value,
                              Font lbl, Font val, Font sub, String subtitulo, Pal pal) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(pal.fondo());
        cell.setBorderColor(C_BORDE);
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph p1 = new Paragraph(label, lbl); p1.setAlignment(Element.ALIGN_CENTER);
        Paragraph p2 = new Paragraph(value, val); p2.setAlignment(Element.ALIGN_CENTER); p2.setSpacingBefore(3);
        Paragraph p3 = new Paragraph(subtitulo, sub); p3.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(p1); cell.addElement(p2); cell.addElement(p3);
        t.addCell(cell);
    }

    private void addIngredientes(Document doc, LoteCerveza lote, Pal pal) throws DocumentException {
        Font grupoFont = new Font(Font.HELVETICA, 8, Font.BOLD, pal.verde());
        Font ingFont   = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.DARK_GRAY);
        PdfPTable tbl = new PdfPTable(new float[]{1, 4});
        tbl.setWidthPercentage(100);
        addGrupoIngredientes(tbl, t("pdf.group.maltas"),        lote.getMaltas(),        grupoFont, ingFont, pal);
        addGrupoIngredientes(tbl, t("pdf.group.lupulos"),       lote.getLupulos(),       grupoFont, ingFont, pal);
        addGrupoIngredientes(tbl, t("pdf.group.levaduras"),     lote.getLevaduras(),     grupoFont, ingFont, pal);
        addGrupoIngredientes(tbl, t("pdf.group.clarificantes"), lote.getClarificantes(), grupoFont, ingFont, pal);
        doc.add(tbl);
    }

    private void addGrupoIngredientes(PdfPTable t, String grupo, List<Ingrediente> lista,
                                      Font grupoFont, Font ingFont, Pal pal) {
        if (lista.isEmpty()) return;
        PdfPCell cGrupo = new PdfPCell(new Phrase(grupo, grupoFont));
        cGrupo.setBackgroundColor(pal.fondo()); cGrupo.setBorderColor(C_BORDE);
        cGrupo.setPadding(6); cGrupo.setVerticalAlignment(Element.ALIGN_MIDDLE);
        t.addCell(cGrupo);
        String texto = lista.stream()
                .map(i -> i.getNombre() + (notBlank(i.getCantidad()) ? " · " + i.getCantidad() : ""))
                .collect(Collectors.joining("   |   "));
        PdfPCell cVal = new PdfPCell(new Phrase(texto, ingFont));
        cVal.setBorderColor(C_BORDE); cVal.setPadding(6);
        t.addCell(cVal);
    }

    private void addIngredientesReceta(Document doc, Receta receta, Pal pal) throws DocumentException {
        Font grupoFont = new Font(Font.HELVETICA, 8, Font.BOLD, pal.verde());
        Font ingFont   = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.DARK_GRAY);
        PdfPTable tbl = new PdfPTable(new float[]{1, 4});
        tbl.setWidthPercentage(100);
        addGrupoRecetaIngredientes(tbl, t("pdf.group.maltas"),        receta.getMaltas(),        grupoFont, ingFont, pal);
        addGrupoRecetaIngredientes(tbl, t("pdf.group.lupulos"),       receta.getLupulos(),       grupoFont, ingFont, pal);
        addGrupoRecetaIngredientes(tbl, t("pdf.group.levaduras"),     receta.getLevaduras(),     grupoFont, ingFont, pal);
        addGrupoRecetaIngredientes(tbl, t("pdf.group.clarificantes"), receta.getClarificantes(), grupoFont, ingFont, pal);
        doc.add(tbl);
    }

    private void addGrupoRecetaIngredientes(PdfPTable t, String grupo,
                                            List<RecetaIngrediente> lista,
                                            Font grupoFont, Font ingFont, Pal pal) {
        if (lista.isEmpty()) return;
        PdfPCell cGrupo = new PdfPCell(new Phrase(grupo, grupoFont));
        cGrupo.setBackgroundColor(pal.fondo()); cGrupo.setBorderColor(C_BORDE);
        cGrupo.setPadding(6); cGrupo.setVerticalAlignment(Element.ALIGN_MIDDLE);
        t.addCell(cGrupo);
        String texto = lista.stream()
                .map(ri -> ri.getNombre() + (notBlank(ri.getCantidad()) ? " · " + ri.getCantidad() : ""))
                .collect(Collectors.joining("   |   "));
        PdfPCell cVal = new PdfPCell(new Phrase(texto, ingFont));
        cVal.setBorderColor(C_BORDE); cVal.setPadding(6);
        t.addCell(cVal);
    }

    private void addDetalleCarbonacion(Document doc, LoteCerveza lote, Pal pal) throws DocumentException {
        Font lbl = new Font(Font.HELVETICA, 8, Font.BOLD,   pal.verde());
        Font val = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);

        // Fila 1: método, CO₂ objetivo/real, validación, destino
        PdfPTable t1 = new PdfPTable(new float[]{1.2f, 2, 1.2f, 2});
        t1.setWidthPercentage(100); t1.setSpacingAfter(6);

        String metodoTexto = lote.getCarbMetodo() == null ? "—"
                : "NATURAL".equals(lote.getCarbMetodo()) ? t("pdf.text.carb_natural") : t("pdf.text.carb_forzada");
        par(t1, t("pdf.label.metodo"), metodoTexto, lbl, val, pal);

        String co2 = "—";
        if (lote.getCarbCo2Objetivo() != null && lote.getCarbCo2Real() != null)
            co2 = lote.getCarbCo2Objetivo() + " vol → " + lote.getCarbCo2Real() + " vol (" + t("pdf.text.real_lc") + ")";
        else if (lote.getCarbCo2Objetivo() != null)
            co2 = lote.getCarbCo2Objetivo() + " vol (" + t("pdf.text.objetivo_lc") + ")";
        else if (lote.getCarbCo2Real() != null)
            co2 = lote.getCarbCo2Real() + " vol (" + t("pdf.text.real_lc") + ")";
        par(t1, t("pdf.label.co2"), co2, lbl, val, pal);

        String validacion = lote.getCarbValidacion() == null ? "—" : switch (lote.getCarbValidacion()) {
            case "ADECUADA"           -> t("pdf.text.carb_adecuada");
            case "RETENCION_CORRECTA" -> t("pdf.text.carb_retencion");
            case "SOBRECARBONATADA"   -> t("pdf.text.carb_sobre");
            case "BAJA_CARBONATACION" -> t("pdf.text.carb_baja");
            default                   -> lote.getCarbValidacion();
        };
        par(t1, t("pdf.label.validacion"), validacion, lbl, val, pal);
        String destinoPdf = lote.getCarbDestino() == null ? "—"
                : lote.getCarbDestino().replace(" | ", "\n");
        par(t1, t("pdf.label.destino_empaque"), destinoPdf, lbl, val, pal);
        doc.add(t1);

        // Fila 2: parámetros específicos del método
        if ("NATURAL".equals(lote.getCarbMetodo())
                && (lote.getCarbAzucarTipo() != null || lote.getCarbAzucarGramos() != null)) {
            PdfPTable t2 = new PdfPTable(new float[]{1.2f, 2, 1.2f, 2});
            t2.setWidthPercentage(100); t2.setSpacingAfter(4);
            par(t2, t("pdf.label.tipo_azucar"),
                    lote.getCarbAzucarTipo() != null ? lote.getCarbAzucarTipo() : "—", lbl, val, pal);
            par(t2, t("pdf.label.gramos_anadidos"),
                    lote.getCarbAzucarGramos() != null ? lote.getCarbAzucarGramos() + " g" : "—", lbl, val, pal);
            doc.add(t2);
        }

        if ("FORZADA".equals(lote.getCarbMetodo())
                && (lote.getCarbPresionPsi() != null || lote.getCarbTiempoHoras() != null
                    || lote.getCarbTecnica() != null)) {
            PdfPTable t2 = new PdfPTable(new float[]{1.2f, 2, 1.2f, 2});
            t2.setWidthPercentage(100); t2.setSpacingAfter(4);
            par(t2, t("pdf.label.presion"),
                    lote.getCarbPresionPsi() != null ? lote.getCarbPresionPsi() + " PSI" : "—", lbl, val, pal);
            par(t2, t("pdf.label.tiempo_exposicion"),
                    lote.getCarbTiempoHoras() != null ? lote.getCarbTiempoHoras() + " horas" : "—", lbl, val, pal);
            String tecnica = lote.getCarbTecnica() == null ? "—"
                    : "PIEDRA".equals(lote.getCarbTecnica()) ? t("pdf.text.carb_piedra") : t("pdf.text.carb_presion_fija");
            par(t2, t("pdf.label.tecnica"), tecnica, lbl, val, pal);
            doc.add(t2);
        }
    }

    private void addTablaFases(Document doc, LoteCerveza lote, Pal pal) throws DocumentException {
        Font thFont  = new Font(Font.HELVETICA, 8, Font.BOLD,   pal.crema());
        Font lblFont = new Font(Font.HELVETICA, 7, Font.BOLD,   C_GRIS);
        Font valFont = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.DARK_GRAY);

        PdfPTable tbl = new PdfPTable(new float[]{1.2f, 1, 1, 1, 1});
        tbl.setWidthPercentage(100);
        for (String h : new String[]{"", t("pdf.header.fermentacion"), t("pdf.header.acondic"), t("pdf.header.maduracion"), t("pdf.header.carbonatacion")}) {
            PdfPCell c = new PdfPCell(new Phrase(h, thFont));
            c.setBackgroundColor(pal.verde()); c.setBorder(0);
            c.setPadding(5); c.setHorizontalAlignment(Element.ALIGN_CENTER);
            tbl.addCell(c);
        }
        faseRow(tbl, t("pdf.header.inicio"),
                fmt(lote.getFermFechaInicial()),    fmt(lote.getAcondFechaInicial()),
                fmt(lote.getMadurFechaInicial()),   fmt(lote.getCarbFechaInicial()),    lblFont, valFont, pal);
        faseRow(tbl, t("pdf.header.fin_ideal"),
                fmt(lote.getFermFechaFinalIdeal()), fmt(lote.getAcondFechaFinalIdeal()),
                fmt(lote.getMadurFechaFinalIdeal()), fmt(lote.getCarbFechaFinalIdeal()), lblFont, valFont, pal);
        faseRow(tbl, t("pdf.header.fin_real"),
                fmt(lote.getFermFechaFinal()),      fmt(lote.getAcondFechaFinal()),
                fmt(lote.getMadurFechaFinal()),     fmt(lote.getCarbFechaFinal()),      lblFont, valFont, pal);
        faseRow(tbl, t("pdf.header.temperatura"),
                temp(lote.getFermTemperatura()),    temp(lote.getAcondTemperatura()),
                temp(lote.getMadurTemperatura()),   temp(lote.getCarbTemperatura()),    lblFont, valFont, pal);
        doc.add(tbl);
    }

    private void faseRow(PdfPTable t, String label,
                         String f1, String f2, String f3, String f4,
                         Font lblFont, Font valFont, Pal pal) {
        PdfPCell lbl = new PdfPCell(new Phrase(label, lblFont));
        lbl.setBackgroundColor(pal.fondo()); lbl.setBorderColor(C_BORDE); lbl.setPadding(5);
        t.addCell(lbl);
        for (String v : new String[]{f1, f2, f3, f4}) {
            PdfPCell c = new PdfPCell(new Phrase(v != null ? v : "—", valFont));
            c.setBorderColor(C_BORDE); c.setPadding(5); c.setHorizontalAlignment(Element.ALIGN_CENTER);
            t.addCell(c);
        }
    }

    private void addCurvaFermentacion(Document doc, LoteCerveza lote,
                                      List<LecturaFermentacion> lecturas, Pal pal) throws Exception {
        addTituloPdf(doc, t("pdf.title.curva_fermentacion"), pal);

        List<LecturaFermentacion> conD = lecturas.stream()
                .filter(l -> l.getDensidad() != null).collect(Collectors.toList());
        List<LecturaFermentacion> conT = lecturas.stream()
                .filter(l -> l.getTemperatura() != null).collect(Collectors.toList());
        boolean hayTemp2 = !conT.isEmpty();

        if (conD.size() >= 2) {
            final int sc = 2;
            final int imgW = 460 * sc, imgH = 120 * sc;
            final int mL = 45 * sc, mB = 22 * sc, mR = (hayTemp2 ? 40 : 8) * sc, mT = 10 * sc;
            final int pW = imgW - mL - mR, pH = imgH - mB - mT;
            final int plotTop = mT, plotBottom = mT + pH, plotLeft = mL, plotRight = mL + pW;

            int minD = conD.stream().mapToInt(LecturaFermentacion::getDensidad).min().orElse(1010);
            int maxD = conD.stream().mapToInt(LecturaFermentacion::getDensidad).max().orElse(1060);
            if (lote.getDensidadInicial() != null) maxD = Math.max(maxD, lote.getDensidadInicial());
            if (lote.getDensidadFinal()   != null) minD = Math.min(minD, lote.getDensidadFinal());
            int padD = Math.max(3, (maxD - minD) / 8);
            minD = Math.max(990, minD - padD); maxD = Math.min(1150, maxD + padD);
            final int dRange = maxD - minD;

            LocalDate firstDate = lecturas.get(0).getFecha();
            LocalDate lastDate  = lecturas.get(lecturas.size() - 1).getFecha();
            long totalDays = Math.max(1, ChronoUnit.DAYS.between(firstDate, lastDate));

            double minT = 10, tRange = 20;
            if (hayTemp2) {
                double tMin2 = conT.stream().mapToDouble(l -> l.getTemperatura().doubleValue()).min().orElse(10);
                double tMax2 = conT.stream().mapToDouble(l -> l.getTemperatura().doubleValue()).max().orElse(30);
                double padT = Math.max(0.5, (tMax2 - tMin2) * 0.12);
                minT   = Math.max(0, tMin2 - padT);
                tRange = Math.max(1, Math.min(60, tMax2 + padT) - minT);
            }

            java.awt.image.BufferedImage bi =
                    new java.awt.image.BufferedImage(imgW, imgH, java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = bi.createGraphics();
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                               java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                               java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g.setColor(Color.WHITE);
            g.fillRect(0, 0, imgW, imgH);
            g.setColor(pal.fondo());
            g.fillRect(plotLeft, plotTop, pW, pH);

            java.awt.Font smallFont = new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 10 * sc / 2);
            g.setFont(smallFont);
            for (int i = 0; i <= 4; i++) {
                int jy = plotBottom - pH * i / 4;
                g.setColor(new Color(210, 210, 210));
                g.drawLine(plotLeft, jy, plotRight, jy);
                g.setColor(C_GRIS);
                g.drawString(String.valueOf(minD + dRange * i / 4), 2, jy + 4);
            }

            g.setColor(new Color(170, 170, 170));
            g.setStroke(new java.awt.BasicStroke(sc));
            g.drawLine(plotLeft, plotTop, plotLeft, plotBottom);
            g.drawLine(plotLeft, plotBottom, plotRight, plotBottom);

            if (hayTemp2) {
                java.awt.Color tempColor = new java.awt.Color(2, 136, 209);
                g.setColor(new Color(170, 170, 170));
                g.setStroke(new java.awt.BasicStroke(sc));
                g.drawLine(plotRight, plotTop, plotRight, plotBottom);
                g.setFont(smallFont);
                for (int i = 0; i <= 4; i++) {
                    int jy = plotBottom - pH * i / 4;
                    double tLbl = minT + tRange * i / 4;
                    g.setColor(tempColor);
                    g.drawString(String.format(java.util.Locale.US, "%.1f°", tLbl), plotRight + 4, jy + 4);
                }
            }

            if (lote.getDensidadFinal() != null) {
                int fgJy = plotBottom - (int) ((float) (lote.getDensidadFinal() - minD) / dRange * pH);
                if (fgJy >= plotTop && fgJy <= plotBottom) {
                    g.setColor(pal.verde());
                    float[] dash = {5f * sc, 3f * sc};
                    g.setStroke(new java.awt.BasicStroke(sc, java.awt.BasicStroke.CAP_BUTT,
                                java.awt.BasicStroke.JOIN_MITER, 10f, dash, 0));
                    g.drawLine(plotLeft, fgJy, plotRight, fgJy);
                    g.setStroke(new java.awt.BasicStroke(sc));
                    g.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 9 * sc / 2));
                    g.drawString("FG " + lote.getDensidadFinal(), plotLeft + 3, fgJy - 2);
                    g.setFont(smallFont);
                }
            }

            int[] xs = new int[conD.size()], ys = new int[conD.size()];
            for (int i = 0; i < conD.size(); i++) {
                long days = ChronoUnit.DAYS.between(firstDate, conD.get(i).getFecha());
                xs[i] = plotLeft + (int) ((float) days / totalDays * pW);
                ys[i] = plotBottom - (int) ((float) (conD.get(i).getDensidad() - minD) / dRange * pH);
            }

            g.setColor(pal.dorado());
            g.setStroke(new java.awt.BasicStroke(2 * sc,
                    java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
            for (int i = 1; i < xs.length; i++) g.drawLine(xs[i - 1], ys[i - 1], xs[i], ys[i]);
            g.setStroke(new java.awt.BasicStroke(sc));
            int r = 3 * sc;
            for (int i = 0; i < xs.length; i++) g.fillOval(xs[i] - r, ys[i] - r, r * 2, r * 2);

            if (hayTemp2) {
                java.awt.Color tempColor = new java.awt.Color(2, 136, 209);
                g.setColor(tempColor);
                g.setStroke(new java.awt.BasicStroke(1.5f * sc,
                        java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                int prevTx = -1, prevTy = -1;
                for (LecturaFermentacion lec : conT) {
                    long days = ChronoUnit.DAYS.between(firstDate, lec.getFecha());
                    int tx = plotLeft + (int) ((float) days / totalDays * pW);
                    int ty = plotBottom - (int) ((lec.getTemperatura().doubleValue() - minT) / tRange * pH);
                    if (prevTx >= 0) g.drawLine(prevTx, prevTy, tx, ty);
                    prevTx = tx; prevTy = ty;
                }
                int rt = 2 * sc;
                g.setStroke(new java.awt.BasicStroke(sc));
                for (LecturaFermentacion lec : conT) {
                    long days = ChronoUnit.DAYS.between(firstDate, lec.getFecha());
                    int tx = plotLeft + (int) ((float) days / totalDays * pW);
                    int ty = plotBottom - (int) ((lec.getTemperatura().doubleValue() - minT) / tRange * pH);
                    g.fillOval(tx - rt, ty - rt, rt * 2, rt * 2);
                }
            }

            DateTimeFormatter fmtShort = DateTimeFormatter.ofPattern("dd/MM");
            g.setColor(C_GRIS); g.setFont(smallFont);
            g.drawString(firstDate.format(fmtShort), plotLeft, imgH - 2);
            g.drawString(lastDate.format(fmtShort), plotRight - 16 * sc, imgH - 2);
            if (totalDays > 3) {
                g.drawString(firstDate.plusDays(totalDays / 2).format(fmtShort),
                             plotLeft + pW / 2 - 8 * sc, imgH - 2);
            }

            g.setColor(pal.dorado());
            g.fillOval(plotLeft, 2, r * 2, r * 2);
            g.setColor(C_GRIS);
            g.drawString(t("pdf.text.densidad_leyenda"), plotLeft + r * 2 + 3, 10);
            if (hayTemp2) {
                int lx2 = plotLeft + r * 2 + 58 * sc;
                g.setColor(new java.awt.Color(2, 136, 209));
                g.fillOval(lx2, 2, r * 2, r * 2);
                g.setColor(C_GRIS);
                g.drawString(t("pdf.text.temp_leyenda"), lx2 + r * 2 + 3, 10);
            }

            g.dispose();
            ByteArrayOutputStream chartBaos = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(bi, "PNG", chartBaos);
            Image chartImg = Image.getInstance(chartBaos.toByteArray());
            chartImg.scaleToFit(460, 120); chartImg.setSpacingBefore(4);
            doc.add(chartImg);
        }

        boolean hayTemp  = lecturas.stream().anyMatch(l -> l.getTemperatura() != null);
        boolean hayNotas = lecturas.stream().anyMatch(l -> notBlank(l.getNotas()));
        Font thF = new Font(Font.HELVETICA, 7, Font.BOLD,   pal.crema());
        Font tdF = new Font(Font.HELVETICA, 7, Font.NORMAL, Color.DARK_GRAY);

        String[] headers = buildHeaders(hayTemp, hayNotas);
        float[]  widths  = buildWidths(hayTemp, hayNotas);
        PdfPTable tbl = new PdfPTable(widths);
        tbl.setWidthPercentage(100); tbl.setSpacingBefore(6);
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, thF));
            c.setBackgroundColor(pal.verdeOscuro()); c.setBorder(0); c.setPadding(4);
            tbl.addCell(c);
        }
        for (LecturaFermentacion lec : lecturas) {
            boolean tieneAbv = lec.getDensidad() != null && lote.getDensidadInicial() != null
                               && lec.getDensidad() < lote.getDensidadInicial();
            String abvStr = tieneAbv
                    ? lec.getAbvParcial(lote.getDensidadInicial()).toPlainString() + "%" : "—";
            tableCell(tbl, FMT_FECHA.format(lec.getFecha()), tdF);
            tableCell(tbl, lec.getDensidad() != null ? String.valueOf(lec.getDensidad()) : "—", tdF);
            tableCell(tbl, abvStr, tdF);
            if (hayTemp)  tableCell(tbl, lec.getTemperatura() != null ? lec.getTemperatura() + " °C" : "—", tdF);
            if (hayNotas) tableCell(tbl, notBlank(lec.getNotas()) ? lec.getNotas() : "—", tdF);
        }
        doc.add(tbl);
    }

    private void addCostos(Document doc, LoteCerveza lote, Pal pal) throws DocumentException {
        Font lbl = new Font(Font.HELVETICA, 8, Font.BOLD,   pal.verde());
        Font val = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);

        PdfPTable resumen = new PdfPTable(new float[]{1.2f, 2, 1.2f, 2});
        resumen.setWidthPercentage(100); resumen.setSpacingAfter(6);
        par(resumen, t("pdf.label.costo_total"),     "$" + fmt2(lote.getCostoTotal()), lbl, val, pal);
        par(resumen, t("pdf.label.costo_por_litro"),
                lote.getCostoPorLitro() != null ? "$" + fmt2(lote.getCostoPorLitro()) : "—", lbl, val, pal);
        par(resumen, t("pdf.label.items_asignados"), String.valueOf(lote.getItemsFactura().size()), lbl, val, pal);
        par(resumen, "", "", lbl, val, pal);
        doc.add(resumen);

        Font thF = new Font(Font.HELVETICA, 7, Font.BOLD,   pal.crema());
        Font tdF = new Font(Font.HELVETICA, 7, Font.NORMAL, Color.DARK_GRAY);
        PdfPTable dt = new PdfPTable(new float[]{1.5f, 1.5f, 2, 1, 1});
        dt.setWidthPercentage(100);
        for (String h : new String[]{t("pdf.header.factura"), t("pdf.label.proveedor"), t("pdf.header.item"), t("pdf.header.cantidad"), t("pdf.header.valor")}) {
            PdfPCell c = new PdfPCell(new Phrase(h, thF));
            c.setBackgroundColor(pal.verdeOscuro()); c.setBorder(0); c.setPadding(4);
            dt.addCell(c);
        }
        for (LoteItemFactura li : lote.getItemsFactura()) {
            String nroFact = li.getItem().getFactura().getNumeroFactura();
            if (nroFact == null || nroFact.isBlank()) nroFact = "#" + li.getItem().getFactura().getId();
            String cant = li.getCantidadAsignada().doubleValue() == 0.0
                    ? t("pdf.text.total_completo")
                    : li.getCantidadAsignadaDisplay().stripTrailingZeros().toPlainString()
                      + " " + (li.getUnidadAsignadaDisplay() != null ? li.getUnidadAsignadaDisplay() : "");
            for (String v : new String[]{nroFact, li.getItem().getFactura().getProveedor(),
                    li.getItem().getNombre(), cant, "$" + fmt2(li.getValorAsignado())}) {
                PdfPCell c = new PdfPCell(new Phrase(v != null ? v : "—", tdF));
                c.setBorderColor(C_BORDE); c.setPadding(4);
                dt.addCell(c);
            }
        }
        doc.add(dt);
    }

    private void addComparativaRecetaLote(Document doc, LoteCerveza lote, Pal pal) throws DocumentException {
        Receta r = lote.getReceta();
        Font lblF = new Font(Font.HELVETICA, 7,  Font.BOLD,   pal.verde());
        Font valF = new Font(Font.HELVETICA, 10, Font.BOLD,   pal.verdeOscuro());
        Font subF = new Font(Font.HELVETICA, 8,  Font.NORMAL, C_GRIS);
        Font posF = new Font(Font.HELVETICA, 8,  Font.BOLD,   new Color(25, 135, 84));
        Font negF = new Font(Font.HELVETICA, 8,  Font.BOLD,   new Color(220, 53, 69));

        boolean tieneOg  = r.getOgObjetivo() != null;
        boolean tieneFg  = r.getFgObjetivo() != null;
        boolean tieneAbv = tieneOg && tieneFg;
        int cols = (tieneOg ? 1 : 0) + (tieneFg ? 1 : 0) + (tieneAbv ? 1 : 0);
        if (cols == 0) return;

        PdfPTable tbl = new PdfPTable(cols);
        tbl.setWidthPercentage(cols == 1 ? 35 : cols == 2 ? 60 : 90);
        tbl.setSpacingAfter(6);

        if (tieneOg) {
            PdfPCell cell = comparativaCell(lblF, valF, subF,
                    t("pdf.label.og_objetivo"), String.valueOf(r.getOgObjetivo()), pal);
            if (lote.getDensidadInicial() != null) {
                int diff = lote.getDensidadInicial() - r.getOgObjetivo();
                String diffStr = (diff > 0 ? "+" : "") + diff;
                Paragraph p = new Paragraph(tf("pdf.text.real_con_diff", lote.getDensidadInicial(), diffStr),
                        diff >= 0 ? posF : negF);
                p.setAlignment(Element.ALIGN_CENTER);
                cell.addElement(p);
            } else {
                addPendiente(cell, subF);
            }
            tbl.addCell(cell);
        }

        if (tieneFg) {
            PdfPCell cell = comparativaCell(lblF, valF, subF,
                    t("pdf.label.fg_objetivo"), String.valueOf(r.getFgObjetivo()), pal);
            if (lote.getDensidadFinal() != null) {
                Paragraph p = new Paragraph(tf("pdf.text.real", lote.getDensidadFinal()), subF);
                p.setAlignment(Element.ALIGN_CENTER);
                cell.addElement(p);
            } else {
                addPendiente(cell, subF);
            }
            tbl.addCell(cell);
        }

        if (tieneAbv) {
            double abvObj = (r.getOgObjetivo() - r.getFgObjetivo()) * 0.13125;
            PdfPCell cell = comparativaCell(lblF, valF, subF,
                    t("pdf.label.abv_objetivo"), String.format(java.util.Locale.US, "%.2f%%", abvObj), pal);
            if (lote.getAbv() != null) {
                Paragraph p = new Paragraph(tf("pdf.text.real", lote.getAbv() + "%"), subF);
                p.setAlignment(Element.ALIGN_CENTER);
                cell.addElement(p);
            } else {
                addPendiente(cell, subF);
            }
            tbl.addCell(cell);
        }

        doc.add(tbl);
    }

    private PdfPCell comparativaCell(Font lbl, Font val, Font sub,
                                      String label, String valor, Pal pal) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(pal.fondo());
        cell.setBorderColor(C_BORDE);
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph p1 = new Paragraph(label, lbl); p1.setAlignment(Element.ALIGN_CENTER);
        Paragraph p2 = new Paragraph(valor,  val); p2.setAlignment(Element.ALIGN_CENTER); p2.setSpacingBefore(3);
        cell.addElement(p1); cell.addElement(p2);
        return cell;
    }

    private void addPendiente(PdfPCell cell, Font font) {
        Paragraph p = new Paragraph(t("pdf.text.pendiente"), font);
        p.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(p);
    }

    private void addNotas(Document doc, LoteCerveza lote,
                          boolean hayObs, boolean hayCata, Pal pal) throws DocumentException {
        Font lbl = new Font(Font.HELVETICA, 8, Font.BOLD,   pal.verde());
        Font txt = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
        if (hayObs) {
            Paragraph p = new Paragraph(t("pdf.label.observaciones"), lbl); p.setSpacingBefore(4);
            doc.add(p); doc.add(new Paragraph(lote.getObservaciones(), txt));
        }
        if (hayCata) {
            Paragraph p = new Paragraph(t("pdf.label.notas_cata"), lbl); p.setSpacingBefore(hayObs ? 8 : 4);
            doc.add(p); doc.add(new Paragraph(lote.getNotasCata(), txt));
        }
    }

    // ── Helpers comparativa ──────────────────────────────────────────

    private void addCeldaHeader(PdfPTable tabla, String texto, Font font, Pal pal) {
        PdfPCell c = new PdfPCell(new Phrase(texto, font));
        c.setBackgroundColor(pal.verdeOscuro());
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setBorder(0); c.setPadding(6);
        tabla.addCell(c);
    }

    private void addCeldaLabel(PdfPTable tabla, String texto, Font font, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(texto, font));
        c.setBackgroundColor(bg); c.setBorderColor(C_BORDE); c.setPadding(5);
        tabla.addCell(c);
    }

    private String valorMetrica(LoteCerveza l, String metrica) {
        return switch (metrica) {
            case "estilo"      -> l.getEstilo() != null ? l.getEstilo() : "—";
            case "og"          -> l.getDensidadInicial() != null ? String.valueOf(l.getDensidadInicial()) : "—";
            case "fg"          -> l.getDensidadFinal()   != null ? String.valueOf(l.getDensidadFinal())   : "—";
            case "abv"         -> l.getAbv()               != null ? l.getAbv()               + "%" : "—";
            case "atenuacion"  -> l.getAtenuacionAparente()  != null ? l.getAtenuacionAparente()  + "%" : "—";
            case "eficiencia"  -> l.getEficienciaMacerado() != null ? l.getEficienciaMacerado() + "%" : "—";
            case "litros"      -> l.getLitrosFinales()      != null ? l.getLitrosFinales()      + " L" : "—";
            case "costo_total" -> l.getCostoTotal()         != null
                    ? "$ " + l.getCostoTotal().setScale(0, java.math.RoundingMode.HALF_UP) : "—";
            case "cpl"         -> l.getCostoPorLitro()      != null
                    ? "$ " + l.getCostoPorLitro().setScale(0, java.math.RoundingMode.HALF_UP) : "—";
            default -> "—";
        };
    }

    private void addEvaluaciones(Document doc, List<EvaluacionSensorial> evaluaciones, Pal pal) throws DocumentException {
        Font thFont   = new Font(Font.HELVETICA, 7, Font.BOLD,   pal.crema());
        Font valFont  = new Font(Font.HELVETICA, 7, Font.NORMAL, Color.DARK_GRAY);
        Font boldFont = new Font(Font.HELVETICA, 7, Font.BOLD,   pal.verdeOscuro());

        // Promedio si hay más de una evaluación con puntaje
        List<EvaluacionSensorial> conPuntaje = evaluaciones.stream()
                .filter(e -> e.getPuntajeTotal() != null).collect(Collectors.toList());
        if (conPuntaje.size() > 1) {
            double avg = conPuntaje.stream().mapToInt(EvaluacionSensorial::getPuntajeTotal).average().orElse(0);
            Font lbl2 = new Font(Font.HELVETICA, 8, Font.BOLD,   pal.verde());
            Font val2 = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
            PdfPTable tp = new PdfPTable(new float[]{1.5f, 2, 1.5f, 2});
            tp.setWidthPercentage(100); tp.setSpacingAfter(6);
            par(tp, t("pdf.label.promedio_bjcp"), String.format("%.1f / 50", avg), lbl2, val2, pal);
            par(tp, "", "", lbl2, val2, pal);
            doc.add(tp);
        }

        // Tabla de evaluaciones
        PdfPTable tbl = new PdfPTable(new float[]{1.5f, 1.5f, 0.85f, 0.85f, 0.85f, 0.85f, 0.85f, 1f, 2f});
        tbl.setWidthPercentage(100);
        for (String h : new String[]{
                t("pdf.header.fecha"),         t("pdf.header.catador"),
                t("pdf.header.aroma_bjcp"),    t("pdf.header.apariencia_bjcp"),
                t("pdf.header.sabor_bjcp"),    t("pdf.header.boca_bjcp"),
                t("pdf.header.general_bjcp"),  t("pdf.header.total"),
                t("pdf.header.clasificacion")}) {
            PdfPCell c = new PdfPCell(new Phrase(h, thFont));
            c.setBackgroundColor(pal.verde()); c.setBorder(0);
            c.setPadding(4); c.setHorizontalAlignment(Element.ALIGN_CENTER);
            tbl.addCell(c);
        }
        for (EvaluacionSensorial ev : evaluaciones) {
            tableCell(tbl, ev.getFecha().format(FMT_FECHA),                                    valFont);
            tableCell(tbl, ev.getCatador() != null ? ev.getCatador() : "—",                   valFont);
            tableCellC(tbl, ev.getAroma()           != null ? String.valueOf(ev.getAroma())           : "—", valFont);
            tableCellC(tbl, ev.getApariencia()      != null ? String.valueOf(ev.getApariencia())      : "—", valFont);
            tableCellC(tbl, ev.getSabor()           != null ? String.valueOf(ev.getSabor())           : "—", valFont);
            tableCellC(tbl, ev.getSensacionBoca()   != null ? String.valueOf(ev.getSensacionBoca())   : "—", valFont);
            tableCellC(tbl, ev.getImpresionGeneral()!= null ? String.valueOf(ev.getImpresionGeneral()): "—", valFont);
            tableCellC(tbl, ev.getPuntajeTotal()    != null ? String.valueOf(ev.getPuntajeTotal())    : "—", boldFont);
            tableCell(tbl, ev.getClasificacion()   != null ? ev.getClasificacion()   : "—",   valFont);
        }
        doc.add(tbl);

        // Notas de evaluación (solo si alguna tiene)
        boolean hayNotas = evaluaciones.stream().anyMatch(e -> notBlank(e.getNotas()));
        if (hayNotas) {
            Font lblN = new Font(Font.HELVETICA, 7, Font.BOLD,   pal.verde());
            Font valN = new Font(Font.HELVETICA, 7, Font.NORMAL, Color.DARK_GRAY);
            PdfPTable tn = new PdfPTable(new float[]{2f, 6f});
            tn.setWidthPercentage(100); tn.setSpacingBefore(4);
            for (EvaluacionSensorial ev : evaluaciones) {
                if (!notBlank(ev.getNotas())) continue;
                String key = ev.getFecha().format(FMT_FECHA)
                        + (notBlank(ev.getCatador()) ? " · " + ev.getCatador() : "");
                par(tn, key, ev.getNotas(), lblN, valN, pal);
            }
            doc.add(tn);
        }
    }

    private void addVentas(Document doc, List<Venta> ventas, Pal pal) throws DocumentException {
        Font thFont  = new Font(Font.HELVETICA, 8, Font.BOLD,   pal.crema());
        Font valFont = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.DARK_GRAY);
        Font totFont = new Font(Font.HELVETICA, 8, Font.BOLD,   pal.verdeOscuro());

        PdfPTable tbl = new PdfPTable(new float[]{3, 1.5f, 1.5f, 1f, 2f});
        tbl.setWidthPercentage(100);
        for (String h : new String[]{
                t("pdf.header.cliente"),       t("pdf.header.fecha"),
                t("pdf.label.estado"),         t("pdf.header.cantidad"),
                t("pdf.header.valor_total")}) {
            PdfPCell c = new PdfPCell(new Phrase(h, thFont));
            c.setBackgroundColor(pal.verde()); c.setBorder(0);
            c.setPadding(5); c.setHorizontalAlignment(Element.ALIGN_CENTER);
            tbl.addCell(c);
        }
        for (Venta v : ventas) {
            tableCell(tbl, v.getCliente(), valFont);
            tableCell(tbl, v.getFechaDespacho() != null ? v.getFechaDespacho().format(FMT_FECHA) : "—", valFont);
            tableCell(tbl, v.getEstado().getDisplayName(), valFont);
            tableCellC(tbl, String.valueOf(v.getItemsCount()), valFont);
            tableCellR(tbl, "$ " + String.format("%,.0f", v.getValorTotal().doubleValue()), totFont);
        }
        doc.add(tbl);
    }

    // ── Helpers comunes ──────────────────────────────────────────────

    private void par(PdfPTable t, String label, String value, Font lbl, Font val, Pal pal) {
        PdfPCell cLbl = new PdfPCell(new Phrase(label, lbl));
        cLbl.setBackgroundColor(pal.fondo()); cLbl.setBorderColor(C_BORDE); cLbl.setPadding(6);
        t.addCell(cLbl);
        PdfPCell cVal = new PdfPCell(new Phrase(value != null ? value : "—", val));
        cVal.setBorderColor(C_BORDE); cVal.setPadding(6);
        t.addCell(cVal);
    }

    private void tableCell(PdfPTable t, String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text != null ? text : "—", font));
        c.setBorderColor(C_BORDE); c.setPadding(4);
        t.addCell(c);
    }

    private void tableCellC(PdfPTable t, String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text != null ? text : "—", font));
        c.setBorderColor(C_BORDE); c.setPadding(4);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.addCell(c);
    }

    private void tableCellR(PdfPTable t, String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text != null ? text : "—", font));
        c.setBorderColor(C_BORDE); c.setPadding(4);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(c);
    }

    private String[] buildHeaders(boolean hayTemp, boolean hayNotas) {
        String fecha     = t("pdf.header.fecha");
        String densidad  = t("pdf.header.densidad");
        String abvParcial = t("pdf.header.abv_parcial");
        String temp      = t("pdf.header.temp");
        String notas     = t("pdf.header.notas");
        if (hayTemp && hayNotas) return new String[]{fecha, densidad, abvParcial, temp, notas};
        if (hayTemp)  return new String[]{fecha, densidad, abvParcial, temp};
        if (hayNotas) return new String[]{fecha, densidad, abvParcial, notas};
        return new String[]{fecha, densidad, abvParcial};
    }

    private float[] buildWidths(boolean hayTemp, boolean hayNotas) {
        if (hayTemp && hayNotas) return new float[]{1.2f, 1f, 1f, 1f, 2f};
        if (hayTemp)  return new float[]{1.5f, 1.2f, 1.2f, 1.2f};
        if (hayNotas) return new float[]{1.5f, 1.2f, 1.2f, 2f};
        return new float[]{2f, 1.5f, 1.5f};
    }

    // ── PDF Reporte de Producción ────────────────────────────────────

    public byte[] generarPdfReporteProduccion(List<LoteCerveza> lotes,
                                               LocalDate desde, LocalDate hasta,
                                               String estiloFiltro,
                                               ExportBranding branding,
                                               Locale locale) {
        LOCALE_HOLDER.set(locale);
        try {
            Pal pal = Pal.of(branding);
            String brandName = branding.name();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4.rotate(), 36, 36, 45, 40);
            try {
                PdfWriter.getInstance(doc, baos);
                doc.open();

                // ── Cabecera ──────────────────────────────────────────────
                PdfPTable header = new PdfPTable(1);
                header.setWidthPercentage(100);
                header.setSpacingAfter(10);
                PdfPCell hCell = new PdfPCell();
                hCell.setBackgroundColor(pal.verde()); hCell.setBorder(0);
                hCell.setPaddingTop(10); hCell.setPaddingBottom(10); hCell.setPaddingLeft(14);
                hCell.addElement(new Paragraph(brandName.toUpperCase(),
                        new Font(Font.HELVETICA, 7, Font.NORMAL, pal.dorado())));
                Paragraph tituloH = new Paragraph(t("pdf.title.reporte_produccion"),
                        new Font(Font.HELVETICA, 13, Font.BOLD, pal.crema()));
                tituloH.setSpacingBefore(3);
                hCell.addElement(tituloH);
                String periodoStr = desde.format(FMT_FECHA) + " — " + hasta.format(FMT_FECHA);
                if (estiloFiltro != null && !estiloFiltro.isBlank()) periodoStr += "  ·  " + estiloFiltro;
                hCell.addElement(new Paragraph(periodoStr,
                        new Font(Font.HELVETICA, 9, Font.ITALIC, pal.verdeClaro())));
                header.addCell(hCell);
                doc.add(header);

                // ── Estadísticas del período ──────────────────────────────
                long totalLotes = lotes.size();
                BigDecimal totalLitros = lotes.stream()
                        .map(l -> l.getLitrosFinales() != null ? l.getLitrosFinales() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                long completados = lotes.stream().filter(LoteCerveza::isCompletado).count();
                int tasaComp = totalLotes > 0 ? (int)(completados * 100 / totalLotes) : 0;
                OptionalDouble avgAbvOpt = lotes.stream()
                        .filter(l -> l.getAbv() != null)
                        .mapToDouble(l -> l.getAbv().doubleValue()).average();
                String avgAbvStr = avgAbvOpt.isPresent()
                        ? String.format("%.2f%%", avgAbvOpt.getAsDouble()) : "—";
                OptionalDouble avgEfOpt = lotes.stream()
                        .filter(l -> l.getEficienciaMacerado() != null)
                        .mapToDouble(l -> l.getEficienciaMacerado().doubleValue()).average();
                String avgEfStr = avgEfOpt.isPresent()
                        ? String.format("%.1f%%", avgEfOpt.getAsDouble()) : "—";
                boolean hayCostos = lotes.stream().anyMatch(l -> l.getCostoTotal() != null);
                BigDecimal costoTotal = hayCostos
                        ? lotes.stream().map(l -> l.getCostoTotal() != null ? l.getCostoTotal() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add) : null;

                addTituloPdf(doc, t("pdf.title.resumen_periodo"), pal);
                Font lbl = new Font(Font.HELVETICA, 8, Font.BOLD, pal.verde());
                Font val = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
                PdfPTable stats = new PdfPTable(new float[]{1.2f, 1.5f, 1.2f, 1.5f, 1.2f, 1.5f, 1.2f, 1.5f});
                stats.setWidthPercentage(100);
                par(stats, t("pdf.label.total_lotes"),       String.valueOf(totalLotes),       lbl, val, pal);
                par(stats, t("pdf.label.litros_producidos"), fmt2(totalLitros) + " L",          lbl, val, pal);
                par(stats, t("pdf.label.abv_promedio"),      avgAbvStr,                         lbl, val, pal);
                par(stats, t("pdf.label.eficiencia_prom"),   avgEfStr,                          lbl, val, pal);
                par(stats, t("pdf.label.estilos_distintos"), String.valueOf(lotes.stream().map(LoteCerveza::getEstilo).distinct().count()), lbl, val, pal);
                par(stats, t("pdf.label.completados"),       completados + " (" + tasaComp + "%)", lbl, val, pal);
                par(stats, t("pdf.label.costo_total"),       costoTotal != null ? "$" + fmt2(costoTotal) : "—", lbl, val, pal);
                par(stats, t("pdf.label.generado"),          LocalDate.now().format(FMT_FECHA), lbl, val, pal);
                doc.add(stats);

                // ── Tabla de lotes ────────────────────────────────────────
                if (!lotes.isEmpty()) {
                    addTituloPdf(doc, t("pdf.title.lotes_periodo"), pal);
                    PdfPTable tabla = new PdfPTable(
                            new float[]{1.3f, 1.3f, 1.6f, 0.9f, 0.8f, 0.75f, 0.75f, 0.85f, 0.85f, 1.0f, 1.1f});
                    tabla.setWidthPercentage(100);
                    Font th = new Font(Font.HELVETICA, 7, Font.BOLD, pal.crema());
                    Font td = new Font(Font.HELVETICA, 7, Font.NORMAL, Color.DARK_GRAY);
                    Font tdGreen = new Font(Font.HELVETICA, 7, Font.BOLD, pal.verde());
                    for (String h : new String[]{
                            t("pdf.label.codigo"), t("pdf.label.estilo"), t("pdf.label.receta"),
                            t("pdf.header.fecha"), t("pdf.label.litros"),
                            "OG", "FG", "ABV %", t("pdf.header.efic"),
                            t("pdf.label.costo_por_litro"), t("pdf.label.estado")}) {
                        PdfPCell c = new PdfPCell(new Phrase(h, th));
                        c.setBackgroundColor(pal.verde()); c.setBorderColor(C_BORDE);
                        c.setPadding(4); tabla.addCell(c);
                    }
                    boolean alt = false;
                    for (LoteCerveza lote : lotes) {
                        Color bg = alt ? pal.fondo() : Color.WHITE;
                        alt = !alt;
                        PdfPCell codCell = new PdfPCell(new Phrase(lote.getCodigoLote(), tdGreen));
                        codCell.setBackgroundColor(bg); codCell.setBorderColor(C_BORDE); codCell.setPadding(4);
                        tabla.addCell(codCell);
                        tablaCelda(tabla, lote.getEstilo(), td, bg);
                        tablaCelda(tabla, lote.getReceta() != null ? lote.getReceta().getNombre() : "—", td, bg);
                        tablaCelda(tabla, fmt(lote.getFechaElaboracion()), td, bg);
                        tablaCelda(tabla, lote.getLitrosFinales() != null ? lote.getLitrosFinales() + " L" : "—", td, bg);
                        tablaCelda(tabla, lote.getDensidadInicial() != null ? String.valueOf(lote.getDensidadInicial()) : "—", td, bg);
                        tablaCelda(tabla, lote.getDensidadFinal()   != null ? String.valueOf(lote.getDensidadFinal())   : "—", td, bg);
                        String abvTxt = lote.getAbv() != null ? lote.getAbv() + "%" : "—";
                        PdfPCell abvCell = new PdfPCell(new Phrase(abvTxt, lote.getAbv() != null ? tdGreen : td));
                        abvCell.setBackgroundColor(bg); abvCell.setBorderColor(C_BORDE); abvCell.setPadding(4);
                        tabla.addCell(abvCell);
                        tablaCelda(tabla, lote.getEficienciaMacerado() != null ? lote.getEficienciaMacerado() + "%" : "—", td, bg);
                        tablaCelda(tabla, lote.getCostoPorLitro() != null ? "$" + fmt2(lote.getCostoPorLitro()) + "/L" : "—", td, bg);
                        tablaCelda(tabla, lote.getFaseActual() != null ? lote.getFaseActual() : "—", td, bg);
                    }
                    doc.add(tabla);
                }

                // ── Resumen por estilo ────────────────────────────────────
                // [0]=count, [1]=litros, [2]=abvSum, [3]=abvCount
                Map<String, double[]> resAgg = new LinkedHashMap<>();
                for (var lote : lotes) {
                    String est = lote.getEstilo() != null ? lote.getEstilo() : t("pdf.text.sin_estilo");
                    double[] agg = resAgg.computeIfAbsent(est, k -> new double[4]);
                    agg[0]++;
                    agg[1] += lote.getLitrosFinales() != null ? lote.getLitrosFinales().doubleValue() : 0;
                    if (lote.getAbv() != null) { agg[2] += lote.getAbv().doubleValue(); agg[3]++; }
                }
                if (resAgg.size() > 1) {
                    addTituloPdf(doc, t("pdf.title.resumen_estilo"), pal);
                    PdfPTable resTabla = new PdfPTable(new float[]{3f, 1f, 1.5f, 1f, 1.2f});
                    resTabla.setWidthPercentage(70);
                    resTabla.setHorizontalAlignment(Element.ALIGN_LEFT);
                    Font rth = new Font(Font.HELVETICA, 7, Font.BOLD, pal.crema());
                    for (String h : new String[]{t("pdf.label.estilo"), t("pdf.header.lotes"),
                            t("pdf.label.litros"), t("pdf.header.pct_vol"), t("pdf.label.abv_promedio")}) {
                        PdfPCell c = new PdfPCell(new Phrase(h, rth));
                        c.setBackgroundColor(pal.verde()); c.setBorderColor(C_BORDE); c.setPadding(4);
                        resTabla.addCell(c);
                    }
                    boolean[] a2 = {false};
                    resAgg.entrySet().stream()
                            .sorted((x, y) -> Double.compare(y.getValue()[1], x.getValue()[1]))
                            .forEach(e -> {
                                Color bg = a2[0] ? pal.fondo() : Color.WHITE; a2[0] = !a2[0];
                                double litE = e.getValue()[1];
                                int pct = totalLitros.compareTo(BigDecimal.ZERO) > 0
                                        ? (int) Math.round(litE * 100 / totalLitros.doubleValue()) : 0;
                                double[] d = e.getValue();
                                String abvPromStr = d[3] > 0
                                        ? String.format("%.2f%%", d[2] / d[3]) : "—";
                                Font rtd = new Font(Font.HELVETICA, 7, Font.NORMAL, Color.DARK_GRAY);
                                tablaCelda(resTabla, e.getKey(), rtd, bg);
                                tablaCelda(resTabla, String.valueOf((int) d[0]), rtd, bg);
                                tablaCelda(resTabla, fmt2(BigDecimal.valueOf(litE)) + " L", rtd, bg);
                                tablaCelda(resTabla, pct + "%", rtd, bg);
                                tablaCelda(resTabla, abvPromStr, rtd, bg);
                            });
                    doc.add(resTabla);
                }

                // ── Pie ───────────────────────────────────────────────────
                doc.add(new Paragraph("\n"));
                Paragraph pie = new Paragraph(
                        tf("pdf.text.pie", LocalDateTime.now().format(FMT_DT), brandName),
                        new Font(Font.HELVETICA, 7, Font.ITALIC, C_GRIS));
                pie.setAlignment(Element.ALIGN_RIGHT);
                doc.add(pie);
                doc.close();
            } catch (Exception e) {
                throw new RuntimeException("Error generando PDF del reporte de producción", e);
            }
            return baos.toByteArray();
        } finally {
            LOCALE_HOLDER.remove();
        }
    }

    // ── PDF Reporte de Rentabilidad ──────────────────────────────────────────

    public byte[] generarPdfReporteRentabilidad(List<RentabilidadLoteDto> filas,
                                                 String estiloFiltro,
                                                 LocalDate desde, LocalDate hasta,
                                                 ExportBranding branding,
                                                 Locale locale) {
        LOCALE_HOLDER.set(locale);
        try {
            Pal pal = Pal.of(branding);
            String brandName = branding.name();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4.rotate(), 36, 36, 45, 40);
            try {
                PdfWriter.getInstance(doc, baos);
                doc.open();

                // ── Cabecera ──────────────────────────────────────────────
                PdfPTable header = new PdfPTable(1);
                header.setWidthPercentage(100);
                header.setSpacingAfter(10);
                PdfPCell hCell = new PdfPCell();
                hCell.setBackgroundColor(pal.verde()); hCell.setBorder(0);
                hCell.setPaddingTop(10); hCell.setPaddingBottom(10); hCell.setPaddingLeft(14);
                hCell.addElement(new Paragraph(brandName.toUpperCase(),
                        new Font(Font.HELVETICA, 7, Font.NORMAL, pal.dorado())));
                Paragraph tituloH = new Paragraph(t("pdf.title.reporte_rentabilidad"),
                        new Font(Font.HELVETICA, 13, Font.BOLD, pal.crema()));
                tituloH.setSpacingBefore(3);
                hCell.addElement(tituloH);
                String periodoStr = (desde != null ? desde.format(FMT_FECHA) : "—")
                        + " — " + (hasta != null ? hasta.format(FMT_FECHA) : "—");
                if (estiloFiltro != null && !estiloFiltro.isBlank()) periodoStr += "  ·  " + estiloFiltro;
                hCell.addElement(new Paragraph(periodoStr,
                        new Font(Font.HELVETICA, 9, Font.ITALIC, pal.verdeClaro())));
                header.addCell(hCell);
                doc.add(header);

                // ── Estadísticas ──────────────────────────────────────────
                BigDecimal totalIngresos = filas.stream().filter(r -> r.ingresos() != null)
                        .map(RentabilidadLoteDto::ingresos).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalCosto = filas.stream().filter(r -> r.costo() != null)
                        .map(RentabilidadLoteDto::costo).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalMargen = totalIngresos.subtract(totalCosto)
                        .setScale(0, java.math.RoundingMode.HALF_UP);
                long lotesConDatos  = filas.stream().filter(r -> !r.sinCosto() && !r.sinVentas()).count();
                long lotesRentables = filas.stream().filter(RentabilidadLoteDto::rentable).count();
                long lotesEnRojo    = filas.stream().filter(RentabilidadLoteDto::enRojo).count();
                String margenStr = (totalMargen.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "")
                        + "$" + fmt2(totalMargen);

                addTituloPdf(doc, t("pdf.title.resumen_periodo"), pal);
                Font lbl = new Font(Font.HELVETICA, 8, Font.BOLD, pal.verde());
                Font val = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
                PdfPTable stats = new PdfPTable(new float[]{1.5f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f});
                stats.setWidthPercentage(100);
                par(stats, t("pdf.label.total_lotes"),       String.valueOf(filas.size()),  lbl, val, pal);
                par(stats, t("pdf.label.lotes_con_datos"),   String.valueOf(lotesConDatos), lbl, val, pal);
                par(stats, t("pdf.label.lotes_rentables"),   String.valueOf(lotesRentables),lbl, val, pal);
                par(stats, t("pdf.label.lotes_en_rojo"),     String.valueOf(lotesEnRojo),   lbl, val, pal);
                par(stats, t("pdf.label.ingresos_ventas"),   "$" + fmt2(totalIngresos),     lbl, val, pal);
                par(stats, t("pdf.label.costos_produccion"), "$" + fmt2(totalCosto),        lbl, val, pal);
                par(stats, t("pdf.label.margen_bruto"),      margenStr,                     lbl, val, pal);
                par(stats, t("pdf.label.generado"),          LocalDate.now().format(FMT_FECHA), lbl, val, pal);
                doc.add(stats);

                // ── Tabla de lotes ────────────────────────────────────────
                if (!filas.isEmpty()) {
                    addTituloPdf(doc, t("pdf.title.rentabilidad_lotes"), pal);
                    PdfPTable tabla = new PdfPTable(
                            new float[]{1.3f, 1.5f, 1.1f, 0.8f, 1.3f, 1.3f, 1.3f, 1f, 1.2f});
                    tabla.setWidthPercentage(100);
                    Font th   = new Font(Font.HELVETICA, 7, Font.BOLD,   pal.crema());
                    Font td   = new Font(Font.HELVETICA, 7, Font.NORMAL, Color.DARK_GRAY);
                    Font tdOk = new Font(Font.HELVETICA, 7, Font.BOLD,   new Color(22,  163, 74));
                    Font tdKo = new Font(Font.HELVETICA, 7, Font.BOLD,   new Color(220, 38,  38));
                    for (String h : new String[]{
                            t("pdf.label.codigo"), t("pdf.label.estilo"), t("pdf.label.fecha_completado"),
                            t("pdf.label.litros"), t("pdf.label.costo_prod"), t("pdf.label.ingresos_ventas"),
                            t("pdf.label.margen_bruto"), t("pdf.label.margen_pct"), t("pdf.label.estado")}) {
                        PdfPCell c = new PdfPCell(new Phrase(h, th));
                        c.setBackgroundColor(pal.verde()); c.setBorderColor(C_BORDE);
                        c.setPadding(4); tabla.addCell(c);
                    }
                    boolean alt = false;
                    for (RentabilidadLoteDto r : filas) {
                        Color bg = alt ? pal.fondo() : Color.WHITE;
                        alt = !alt;
                        tablaCelda(tabla, r.codigoLote(), td, bg);
                        tablaCelda(tabla, r.estilo() != null ? r.estilo() : "—", td, bg);
                        tablaCelda(tabla, r.fechaCompletado() != null
                                ? r.fechaCompletado().format(FMT_FECHA) : "—", td, bg);
                        tablaCelda(tabla, r.litrosFinales() != null
                                ? r.litrosFinales() + " L" : "—", td, bg);
                        tablaCelda(tabla, !r.sinCosto()   ? "$" + fmt2(r.costo())    : "—", td, bg);
                        tablaCelda(tabla, !r.sinVentas()  ? "$" + fmt2(r.ingresos()) : "—", td, bg);
                        boolean tieneMargen = r.margen() != null && !r.sinCosto() && !r.sinVentas();
                        String  mStr = tieneMargen ? (r.rentable() ? "+" : "") + "$" + fmt2(r.margen()) : "—";
                        PdfPCell mCell = new PdfPCell(new Phrase(mStr, tieneMargen
                                ? (r.rentable() ? tdOk : tdKo) : td));
                        mCell.setBackgroundColor(bg); mCell.setBorderColor(C_BORDE); mCell.setPadding(4);
                        tabla.addCell(mCell);
                        boolean tienePct = r.margenPct() != null && !r.sinCosto() && !r.sinVentas();
                        String  pStr = tienePct ? (r.rentable() ? "+" : "") + r.margenPct() + "%" : "—";
                        PdfPCell pCell = new PdfPCell(new Phrase(pStr, tienePct
                                ? (r.rentable() ? tdOk : tdKo) : td));
                        pCell.setBackgroundColor(bg); pCell.setBorderColor(C_BORDE); pCell.setPadding(4);
                        tabla.addCell(pCell);
                        String estadoStr;
                        if (!r.sinCosto() && !r.sinVentas()) {
                            estadoStr = r.rentable()
                                    ? t("reportes.rent.badge.rentable")
                                    : t("reportes.rent.badge.en.rojo");
                        } else if (r.sinVentas()) {
                            estadoStr = t("reportes.rent.badge.sin.ventas");
                        } else {
                            estadoStr = t("reportes.rent.badge.sin.costos");
                        }
                        tablaCelda(tabla, estadoStr, td, bg);
                    }
                    doc.add(tabla);
                }

                // ── Pie ───────────────────────────────────────────────────
                doc.add(new Paragraph("\n"));
                Paragraph pie = new Paragraph(
                        tf("pdf.text.pie", LocalDateTime.now().format(FMT_DT), brandName),
                        new Font(Font.HELVETICA, 7, Font.ITALIC, C_GRIS));
                pie.setAlignment(Element.ALIGN_RIGHT);
                doc.add(pie);
                doc.close();
            } catch (Exception e) {
                throw new RuntimeException("Error generando PDF de rentabilidad", e);
            }
            return baos.toByteArray();
        } finally {
            LOCALE_HOLDER.remove();
        }
    }

    private void tablaCelda(PdfPTable t, String text, Font font, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(text != null ? text : "—", font));
        c.setBackgroundColor(bg); c.setBorderColor(C_BORDE); c.setPadding(4);
        t.addCell(c);
    }

    // ── Helpers comunes ──────────────────────────────────────────────

    private String fmt(LocalDate d)    { return d != null ? d.format(FMT_FECHA) : null; }
    private String temp(BigDecimal t)  { return t != null ? t + " °C" : null; }
    private boolean notBlank(String s) { return s != null && !s.isBlank(); }
    private String fmt2(BigDecimal n)  {
        return n != null ? String.format("%,.0f", n.doubleValue()) : "—";
    }

    // ── PDF Reporte de Ventas ────────────────────────────────────────

    public byte[] generarPdfReporteVentas(List<Venta> ventas,
                                           EstadoVenta estadoFiltro,
                                           LocalDate desde, LocalDate hasta,
                                           ExportBranding branding,
                                           Locale locale) {
        LOCALE_HOLDER.set(locale);
        try {
            Pal pal = Pal.of(branding);
            String brandName = branding.name();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4.rotate(), 36, 36, 45, 40);
            try {
                PdfWriter.getInstance(doc, baos);
                doc.open();

                // ── Cabecera ──────────────────────────────────────────────
                PdfPTable header = new PdfPTable(1);
                header.setWidthPercentage(100);
                header.setSpacingAfter(10);
                PdfPCell hCell = new PdfPCell();
                hCell.setBackgroundColor(pal.verde()); hCell.setBorder(0);
                hCell.setPaddingTop(10); hCell.setPaddingBottom(10); hCell.setPaddingLeft(14);
                hCell.addElement(new Paragraph(brandName.toUpperCase(),
                        new Font(Font.HELVETICA, 7, Font.NORMAL, pal.dorado())));
                Paragraph tituloH = new Paragraph(t("pdf.title.reporte_ventas"),
                        new Font(Font.HELVETICA, 13, Font.BOLD, pal.crema()));
                tituloH.setSpacingBefore(3);
                hCell.addElement(tituloH);
                String periodoStr = desde.format(FMT_FECHA) + " — " + hasta.format(FMT_FECHA);
                if (estadoFiltro != null) periodoStr += "  ·  " + estadoFiltro.getDisplayName();
                hCell.addElement(new Paragraph(periodoStr,
                        new Font(Font.HELVETICA, 9, Font.ITALIC, pal.verdeClaro())));
                header.addCell(hCell);
                doc.add(header);

                // ── Estadísticas ──────────────────────────────────────────
                long totalVentas      = ventas.size();
                long totalDespachadas = ventas.stream().filter(v -> v.getEstado() == EstadoVenta.DESPACHADO).count();
                long totalPendientes  = ventas.stream().filter(v -> v.getEstado() == EstadoVenta.PENDIENTE).count();
                long totalCanceladas  = ventas.stream().filter(v -> v.getEstado() == EstadoVenta.CANCELADO).count();
                BigDecimal ingresos   = ventas.stream()
                        .filter(v -> v.getEstado() == EstadoVenta.DESPACHADO)
                        .map(Venta::getValorTotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                long clientesUnicos   = ventas.stream()
                        .map(Venta::getCliente).filter(c -> c != null).distinct().count();

                addTituloPdf(doc, t("pdf.title.resumen_periodo"), pal);
                Font lbl = new Font(Font.HELVETICA, 8, Font.BOLD, pal.verde());
                Font val = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
                PdfPTable stats = new PdfPTable(new float[]{1.5f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f});
                stats.setWidthPercentage(100);
                par(stats, t("pdf.label.total_ventas"),  String.valueOf(totalVentas),      lbl, val, pal);
                par(stats, t("pdf.label.despachadas"),   String.valueOf(totalDespachadas),  lbl, val, pal);
                par(stats, t("pdf.label.pendientes"),    String.valueOf(totalPendientes),   lbl, val, pal);
                par(stats, t("pdf.label.canceladas"),    String.valueOf(totalCanceladas),   lbl, val, pal);
                par(stats, t("pdf.label.ingresos"),      "$" + fmt2(ingresos),              lbl, val, pal);
                par(stats, t("pdf.label.clientes_unicos"), String.valueOf(clientesUnicos), lbl, val, pal);
                par(stats, t("pdf.label.generado"),      LocalDate.now().format(FMT_FECHA), lbl, val, pal);
                par(stats, "", "", lbl, val, pal);
                doc.add(stats);

                // ── Tabla de ventas ───────────────────────────────────────
                if (!ventas.isEmpty()) {
                    addTituloPdf(doc, t("pdf.title.ventas_periodo"), pal);
                    PdfPTable tabla = new PdfPTable(new float[]{1.2f, 2.5f, 1.2f, 1.5f, 0.6f, 1.5f, 1.2f});
                    tabla.setWidthPercentage(100);
                    Font th = new Font(Font.HELVETICA, 7, Font.BOLD, pal.crema());
                    Font td = new Font(Font.HELVETICA, 7, Font.NORMAL, Color.DARK_GRAY);
                    for (String h : new String[]{
                            t("pdf.label.remision"), t("pdf.label.cliente"),
                            t("pdf.label.fecha_despacho"), t("pdf.label.codigo"),
                            t("pdf.header.num"), t("pdf.header.valor_total"), t("pdf.label.estado")}) {
                        PdfPCell c = new PdfPCell(new Phrase(h, th));
                        c.setBackgroundColor(pal.verde()); c.setBorderColor(C_BORDE);
                        c.setPadding(4); tabla.addCell(c);
                    }
                    boolean alt = false;
                    for (Venta v : ventas) {
                        Color bg = alt ? pal.fondo() : Color.WHITE;
                        alt = !alt;
                        tablaCelda(tabla, v.getRemisionNumero() != null ? v.getRemisionNumero() : "—", td, bg);
                        tablaCelda(tabla, v.getCliente(), td, bg);
                        tablaCelda(tabla, fmt(v.getFechaDespacho()), td, bg);
                        tablaCelda(tabla, v.getPrimerCodigoLote() != null ? v.getPrimerCodigoLote() : "—", td, bg);
                        tablaCelda(tabla, String.valueOf(v.getItemsCount()), td, bg);
                        tablaCelda(tabla, "$" + fmt2(v.getValorTotal()), td, bg);
                        tablaCelda(tabla, v.getEstado().getDisplayName(), td, bg);
                    }
                    doc.add(tabla);
                }

                // ── Resumen por cliente ───────────────────────────────────
                Map<String, double[]> clienteAgg = new LinkedHashMap<>();
                for (Venta v : ventas) {
                    if (v.getCliente() == null) continue;
                    double[] agg = clienteAgg.computeIfAbsent(v.getCliente(), k -> new double[]{0, 0});
                    agg[0]++;
                    agg[1] += v.getValorTotal().doubleValue();
                }
                if (clienteAgg.size() > 1) {
                    addTituloPdf(doc, t("pdf.title.resumen_clientes"), pal);
                    PdfPTable resTabla = new PdfPTable(new float[]{3f, 1f, 1.5f, 0.9f});
                    resTabla.setWidthPercentage(65);
                    resTabla.setHorizontalAlignment(Element.ALIGN_LEFT);
                    Font rth = new Font(Font.HELVETICA, 7, Font.BOLD, pal.crema());
                    for (String h : new String[]{t("pdf.label.cliente"), t("pdf.header.n_ventas"),
                                                  t("pdf.header.total"),   t("pdf.header.pct_total")}) {
                        PdfPCell c = new PdfPCell(new Phrase(h, rth));
                        c.setBackgroundColor(pal.verde()); c.setBorderColor(C_BORDE); c.setPadding(4);
                        resTabla.addCell(c);
                    }
                    double totalClientes = clienteAgg.values().stream().mapToDouble(d -> d[1]).sum();
                    boolean[] a2 = {false};
                    clienteAgg.entrySet().stream()
                            .sorted((x, y) -> Double.compare(y.getValue()[1], x.getValue()[1]))
                            .limit(15)
                            .forEach(e -> {
                                Color bg = a2[0] ? pal.fondo() : Color.WHITE;
                                a2[0] = !a2[0];
                                Font rtd = new Font(Font.HELVETICA, 7, Font.NORMAL, Color.DARK_GRAY);
                                tablaCelda(resTabla, e.getKey(), rtd, bg);
                                tablaCelda(resTabla, String.valueOf((long) e.getValue()[0]), rtd, bg);
                                tablaCelda(resTabla, "$" + fmt2(BigDecimal.valueOf(e.getValue()[1])), rtd, bg);
                                tablaCelda(resTabla, totalClientes > 0
                                        ? String.format("%.1f%%", e.getValue()[1] * 100.0 / totalClientes) : "—",
                                        rtd, bg);
                            });
                    doc.add(resTabla);
                }

                // ── Resumen por estado ────────────────────────────────────
                if (!ventas.isEmpty()) {
                    addTituloPdf(doc, t("pdf.title.resumen_estados"), pal);
                    PdfPTable estTabla = new PdfPTable(new float[]{2f, 1f, 1.5f, 0.9f});
                    estTabla.setWidthPercentage(55);
                    estTabla.setHorizontalAlignment(Element.ALIGN_LEFT);
                    Font eth = new Font(Font.HELVETICA, 7, Font.BOLD, pal.crema());
                    for (String h : new String[]{t("pdf.label.estado"), t("pdf.header.n_ventas"),
                                                  t("pdf.header.total"), t("pdf.header.pct_total")}) {
                        PdfPCell c = new PdfPCell(new Phrase(h, eth));
                        c.setBackgroundColor(pal.verde()); c.setBorderColor(C_BORDE); c.setPadding(4);
                        estTabla.addCell(c);
                    }
                    Map<String, double[]> estadoAgg = new LinkedHashMap<>();
                    for (Venta v : ventas) {
                        String estNombre = v.getEstado() != null ? v.getEstado().getDisplayName() : "—";
                        estadoAgg.computeIfAbsent(estNombre, k -> new double[]{0, 0});
                        estadoAgg.get(estNombre)[0]++;
                        if (v.getValorTotal() != null)
                            estadoAgg.get(estNombre)[1] += v.getValorTotal().doubleValue();
                    }
                    double totalEst = estadoAgg.values().stream().mapToDouble(d -> d[1]).sum();
                    boolean[] a3 = {false};
                    estadoAgg.entrySet().stream()
                            .sorted((x, y) -> Double.compare(y.getValue()[1], x.getValue()[1]))
                            .forEach(e -> {
                                Color bg = a3[0] ? pal.fondo() : Color.WHITE;
                                a3[0] = !a3[0];
                                Font etd = new Font(Font.HELVETICA, 7, Font.NORMAL, Color.DARK_GRAY);
                                tablaCelda(estTabla, e.getKey(), etd, bg);
                                tablaCelda(estTabla, String.valueOf((long) e.getValue()[0]), etd, bg);
                                tablaCelda(estTabla, "$" + fmt2(BigDecimal.valueOf(e.getValue()[1])), etd, bg);
                                tablaCelda(estTabla, totalEst > 0
                                        ? String.format("%.1f%%", e.getValue()[1] * 100.0 / totalEst) : "—",
                                        etd, bg);
                            });
                    doc.add(estTabla);
                }

                // ── Pie ───────────────────────────────────────────────────
                doc.add(new Paragraph("\n"));
                Paragraph pie = new Paragraph(
                        tf("pdf.text.pie", LocalDateTime.now().format(FMT_DT), brandName),
                        new Font(Font.HELVETICA, 7, Font.ITALIC, C_GRIS));
                pie.setAlignment(Element.ALIGN_RIGHT);
                doc.add(pie);
                doc.close();
            } catch (Exception e) {
                throw new RuntimeException("Error generando PDF del reporte de ventas", e);
            }
            return baos.toByteArray();
        } finally {
            LOCALE_HOLDER.remove();
        }
    }

    // ── PDF Venta (Remisión) ─────────────────────────────────────────

    public byte[] generarPdfVenta(Venta venta, ExportBranding branding, Locale locale) {
        LOCALE_HOLDER.set(locale);
        try {
            Pal pal = Pal.of(branding);
            String brandName = branding.name();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 36, 36, 50, 45);
            try {
                PdfWriter.getInstance(doc, baos);
                doc.open();

                // ── Cabecera ─────────────────────────────────────────────
                Font fBrand  = new Font(Font.HELVETICA, 18, Font.BOLD,  pal.verde());
                Font fSub    = new Font(Font.HELVETICA,  9, Font.NORMAL, C_GRIS);
                Font fTitulo = new Font(Font.HELVETICA, 13, Font.BOLD,  pal.verde());

                Paragraph cabBrand = new Paragraph(brandName.toUpperCase(), fBrand);
                cabBrand.setAlignment(Element.ALIGN_LEFT);
                doc.add(cabBrand);

                Paragraph cabTipo = new Paragraph(t("pdf.title.remision"), fTitulo);
                cabTipo.setAlignment(Element.ALIGN_LEFT);
                doc.add(cabTipo);

                Paragraph cabNum = new Paragraph(tf("pdf.text.ref_venta", venta.getId()), fSub);
                cabNum.setAlignment(Element.ALIGN_LEFT);
                doc.add(cabNum);

                // línea divisora
                PdfPTable linea = new PdfPTable(1);
                linea.setWidthPercentage(100);
                linea.setSpacingBefore(8f);
                linea.setSpacingAfter(12f);
                PdfPCell lineaCell = new PdfPCell(new Phrase(""));
                lineaCell.setBackgroundColor(pal.verde());
                lineaCell.setBorder(PdfPCell.NO_BORDER);
                lineaCell.setFixedHeight(2f);
                linea.addCell(lineaCell);
                doc.add(linea);

                // ── Datos del cliente y despacho ─────────────────────────
                addTituloPdf(doc, t("pdf.title.datos_despacho"), pal);

                PdfPTable datos = new PdfPTable(new float[]{2f, 3f, 2f, 3f});
                datos.setWidthPercentage(100);
                datos.setSpacingAfter(12f);

                Font fLbl = new Font(Font.HELVETICA, 8, Font.BOLD, C_GRIS);
                Font fVal = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);

                par(datos, t("pdf.label.cliente"),         venta.getCliente(), fLbl, fVal, pal);
                par(datos, t("pdf.label.fecha_despacho"),
                    venta.getFechaDespacho() != null ? venta.getFechaDespacho().format(FMT_FECHA) : "—",
                    fLbl, fVal, pal);
                String primerLote = venta.getPrimerCodigoLote();
                par(datos, t("pdf.label.lote"), primerLote != null ? primerLote : t("pdf.text.sin_lote"),
                    fLbl, fVal, pal);
                par(datos, t("pdf.label.estado"), venta.getEstado().getDisplayName(), fLbl, fVal, pal);
                doc.add(datos);

                // ── Tabla de ítems ────────────────────────────────────────
                addTituloPdf(doc, t("pdf.title.detalle_venta"), pal);

                PdfPTable tabla = new PdfPTable(new float[]{1.8f, 2.2f, 1.2f, 1.8f, 1.2f, 1.8f});
                tabla.setWidthPercentage(100);
                tabla.setSpacingAfter(8f);

                Font fTh = new Font(Font.HELVETICA, 8, Font.BOLD, pal.crema());
                for (String h : new String[]{t("pdf.label.lote"), t("pdf.header.descripcion"), t("pdf.header.cantidad"),
                        t("pdf.header.precio_unit"), t("pdf.header.descuento_pct"), t("pdf.header.total")}) {
                    PdfPCell c = new PdfPCell(new Phrase(h, fTh));
                    c.setBackgroundColor(pal.verde());
                    c.setBorderColor(C_BORDE);
                    c.setPadding(5);
                    tabla.addCell(c);
                }

                Font fTd     = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
                Font fTdBold = new Font(Font.HELVETICA, 9, Font.BOLD,   pal.verde());
                Font fTdMono = new Font(Font.HELVETICA, 9, Font.NORMAL, pal.verde());

                java.util.List<VentaItem> itemsVenta = venta.getItems();
                if (itemsVenta == null || itemsVenta.isEmpty()) {
                    PdfPCell emptyCell = new PdfPCell(new Phrase(t("pdf.text.sin_items"), fTd));
                    emptyCell.setColspan(6);
                    emptyCell.setPadding(6);
                    emptyCell.setBorderColor(C_BORDE);
                    emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabla.addCell(emptyCell);
                } else {
                    for (VentaItem item : itemsVenta) {
                        String loteCol = item.getCodigoLote() != null ? item.getCodigoLote() : "—";
                        String descCol = item.getDescripcion() != null && !item.getDescripcion().isBlank()
                                ? item.getDescripcion() : "—";
                        String cantCol = item.getCantidad() != null
                                ? String.format("%,.3f", item.getCantidad())
                                  + (item.getUnidad() != null ? " " + item.getUnidad() : "")
                                : "—";
                        String precCol = item.getPrecioUnitario() != null
                                ? "$" + String.format("%,.2f", item.getPrecioUnitario()) : "—";
                        String descPctCol = item.getDescuentoPct() != null
                                && item.getDescuentoPct().compareTo(BigDecimal.ZERO) > 0
                                ? item.getDescuentoPct() + "%" : "—";
                        String linTotCol = "$" + String.format("%,.0f", item.getValorLinea());

                        tableCell(tabla, loteCol,    fTdMono, Color.WHITE, Element.ALIGN_LEFT);
                        tableCell(tabla, descCol,    fTd,     Color.WHITE, Element.ALIGN_LEFT);
                        tableCell(tabla, cantCol,    fTd,     Color.WHITE, Element.ALIGN_CENTER);
                        tableCell(tabla, precCol,    fTd,     Color.WHITE, Element.ALIGN_RIGHT);
                        tableCell(tabla, descPctCol, fTd,     Color.WHITE, Element.ALIGN_CENTER);
                        tableCell(tabla, linTotCol,  fTdBold, Color.WHITE, Element.ALIGN_RIGHT);
                    }
                }
                doc.add(tabla);

                // ── Total destacado ───────────────────────────────────────
                String totalStr = "$" + String.format("%,.0f", venta.getValorTotal());

                PdfPTable totBox = new PdfPTable(new float[]{3f, 1f});
                totBox.setWidthPercentage(50);
                totBox.setHorizontalAlignment(Element.ALIGN_RIGHT);
                totBox.setSpacingAfter(14f);

                Font fTotLbl = new Font(Font.HELVETICA, 9, Font.BOLD, pal.verde());
                Font fTotVal = new Font(Font.HELVETICA, 11, Font.BOLD, pal.verde());

                PdfPCell lblCell = new PdfPCell(new Phrase(t("pdf.text.total_cobrar"), fTotLbl));
                lblCell.setBackgroundColor(pal.fondo()); lblCell.setBorderColor(C_BORDE); lblCell.setPadding(6);
                totBox.addCell(lblCell);

                PdfPCell valCell = new PdfPCell(new Phrase(totalStr, fTotVal));
                valCell.setBackgroundColor(pal.fondo()); valCell.setBorderColor(C_BORDE);
                valCell.setPadding(6); valCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                totBox.addCell(valCell);
                doc.add(totBox);

                // ── Notas ─────────────────────────────────────────────────
                if (venta.getNotas() != null && !venta.getNotas().isBlank()) {
                    addTituloPdf(doc, t("pdf.title.notas"), pal);
                    Paragraph notaP = new Paragraph(venta.getNotas(),
                            new Font(Font.HELVETICA, 9, Font.ITALIC, C_GRIS));
                    notaP.setSpacingAfter(12f);
                    doc.add(notaP);
                }

                // ── Pie ───────────────────────────────────────────────────
                doc.add(new Paragraph("\n"));
                Paragraph pie = new Paragraph(
                        tf("pdf.text.pie", LocalDateTime.now().format(FMT_DT), brandName),
                        new Font(Font.HELVETICA, 7, Font.ITALIC, C_GRIS));
                pie.setAlignment(Element.ALIGN_RIGHT);
                doc.add(pie);

                doc.close();
            } catch (Exception e) {
                throw new RuntimeException("Error generando PDF de venta #" + venta.getId(), e);
            }
            return baos.toByteArray();
        } finally {
            LOCALE_HOLDER.remove();
        }
    }

    private void tableCell(PdfPTable t, String text, Font font, Color bg, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text != null ? text : "—", font));
        c.setBackgroundColor(bg); c.setBorderColor(C_BORDE);
        c.setPadding(5); c.setHorizontalAlignment(align);
        t.addCell(c);
    }

    // ── PDF Orden de Compra ──────────────────────────────────────────────

    public byte[] generarPdfOrdenCompra(com.alera.model.OrdenCompra oc, ExportBranding branding, Locale locale) {
        LOCALE_HOLDER.set(locale);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font fTitulo  = new Font(Font.HELVETICA, 18, Font.BOLD,  pal(branding).verde());
            Font fSub     = new Font(Font.HELVETICA, 10, Font.NORMAL, C_GRIS);
            Font fLabel   = new Font(Font.HELVETICA,  9, Font.BOLD,  pal(branding).verdeOscuro());
            Font fVal     = new Font(Font.HELVETICA,  9, Font.NORMAL, pal(branding).verdeOscuro());
            Font fHead    = new Font(Font.HELVETICA,  9, Font.BOLD,  Color.WHITE);
            Font fData    = new Font(Font.HELVETICA,  8, Font.NORMAL, pal(branding).verdeOscuro());
            Font fTotal   = new Font(Font.HELVETICA, 10, Font.BOLD,  pal(branding).verde());
            Pal pal       = pal(branding);

            // Cabecera
            Paragraph titulo = new Paragraph(t("pdf.title.orden_compra"), fTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            doc.add(titulo);
            Paragraph numOc = new Paragraph(
                    (oc.getNumeroOc() != null ? oc.getNumeroOc() : t("pdf.text.sin_numero")) + "  ·  " + branding.name(),
                    fSub);
            numOc.setAlignment(Element.ALIGN_CENTER);
            numOc.setSpacingAfter(12);
            doc.add(numOc);

            // Info general
            addTituloPdf(doc, t("pdf.title.info_general"), pal);
            PdfPTable info = new PdfPTable(4);
            info.setWidthPercentage(100);
            info.setWidths(new float[]{1.5f, 2f, 1.5f, 2f});
            info.setSpacingAfter(10);
            par(info, t("pdf.label.proveedor"),      oc.getProveedor() != null ? oc.getProveedor() : "—", fLabel, fVal, pal);
            par(info, t("pdf.label.estado"),         oc.getEstado().getDisplayName(),                       fLabel, fVal, pal);
            par(info, t("pdf.label.fecha_emision"),  fmt(oc.getFechaEmision()),                             fLabel, fVal, pal);
            par(info, t("pdf.label.fecha_requerida"), oc.getFechaRequerida() != null ? fmt(oc.getFechaRequerida()) : "—", fLabel, fVal, pal);
            doc.add(info);

            // Tabla de ítems
            if (!oc.getItems().isEmpty()) {
                addTituloPdf(doc, t("pdf.title.items_orden"), pal);
                boolean hayPrecio = oc.getItems().stream().anyMatch(i -> i.getPrecioUnitarioEstimado() != null);
                int cols = hayPrecio ? 6 : 4;
                PdfPTable items = new PdfPTable(cols);
                items.setWidthPercentage(100);
                if (hayPrecio) {
                    items.setWidths(new float[]{1.5f, 3f, 1f, 1f, 1.5f, 1.5f});
                } else {
                    items.setWidths(new float[]{2f, 4f, 1.5f, 1.5f});
                }
                items.setSpacingAfter(10);

                Color bg = pal.verde();
                ocHead(items, t("pdf.header.tipo"),              fHead, bg);
                ocHead(items, t("pdf.header.nombre_descripcion"), fHead, bg);
                ocHead(items, t("pdf.header.cantidad"),           fHead, bg);
                ocHead(items, t("pdf.header.unidad"),             fHead, bg);
                if (hayPrecio) {
                    ocHead(items, t("pdf.header.precio_unit_est"), fHead, bg);
                    ocHead(items, t("pdf.header.total_est"),       fHead, bg);
                }

                Color bgPar = new Color(248, 249, 250);
                int idx = 0;
                for (com.alera.model.OrdenCompraItem item : oc.getItems()) {
                    Color rowBg = (idx++ % 2 == 0) ? Color.WHITE : bgPar;
                    String tipo = item.getTipoItem() != null ? item.getTipoItem().getDisplayName() : "—";
                    String desc = item.getNombre()
                            + (notBlank(item.getDescripcion()) ? "\n" + item.getDescripcion() : "");
                    tableCell(items, tipo,   fData, rowBg, Element.ALIGN_CENTER);
                    tableCell(items, desc,   fData, rowBg, Element.ALIGN_LEFT);
                    tableCell(items, item.getCantidad() != null ? item.getCantidad().stripTrailingZeros().toPlainString() : "—", fData, rowBg, Element.ALIGN_RIGHT);
                    tableCell(items, item.getUnidad() != null ? item.getUnidad() : "—", fData, rowBg, Element.ALIGN_CENTER);
                    if (hayPrecio) {
                        tableCell(items, item.getPrecioUnitarioEstimado() != null ? fmt2(item.getPrecioUnitarioEstimado()) : "—", fData, rowBg, Element.ALIGN_RIGHT);
                        tableCell(items, fmt2(item.getValorLinea()), fData, rowBg, Element.ALIGN_RIGHT);
                    }
                }
                doc.add(items);

                // Totales
                if (hayPrecio) {
                    PdfPTable totales = new PdfPTable(2);
                    totales.setWidthPercentage(40);
                    totales.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    totales.setWidths(new float[]{2f, 1.5f});
                    totales.setSpacingAfter(12);
                    par(totales, t("pdf.label.subtotal_estimado"), fmt2(oc.getSubtotalEstimado()), fLabel, fVal, pal);
                    par(totales, t("pdf.label.total_estimado_iva"), fmt2(oc.getTotalEstimado()), fTotal, fTotal, pal);
                    doc.add(totales);
                }
            }

            // Notas
            if (notBlank(oc.getNotas())) {
                addTituloPdf(doc, t("pdf.title.notas"), pal);
                Paragraph nota = new Paragraph(oc.getNotas(), fVal);
                nota.setSpacingAfter(10);
                doc.add(nota);
            }

            // Pie de página
            Paragraph pie = new Paragraph(
                    tf("pdf.text.pie_oc", branding.name(), LocalDateTime.now().format(FMT_DT)),
                    new Font(Font.HELVETICA, 7, Font.ITALIC, C_GRIS));
            pie.setAlignment(Element.ALIGN_CENTER);
            pie.setSpacingBefore(16);
            doc.add(pie);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF de OC", e);
        } finally {
            LOCALE_HOLDER.remove();
        }
    }

    /** Helper para obtener la paleta dentro de generarPdfOrdenCompra sin un campo previo. */
    private Pal pal(ExportBranding b) { return Pal.of(b); }

    private void ocHead(PdfPTable t, String text, Font font, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBackgroundColor(bg); c.setBorderColor(C_BORDE);
        c.setPadding(5); c.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.addCell(c);
    }
}
