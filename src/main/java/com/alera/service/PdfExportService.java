package com.alera.service;

import com.alera.config.ExportBranding;
import com.alera.model.AdicionHervor;
import com.alera.model.EscalonMacerado;
import com.alera.model.Ingrediente;
import com.alera.model.LecturaFermentacion;
import com.alera.model.LoteCerveza;
import com.alera.model.LoteItemFactura;
import com.alera.model.Receta;
import com.alera.model.RecetaIngrediente;
import com.alera.model.Venta;
import com.alera.model.VentaItem;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.*;
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
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

@Service
public class PdfExportService {

    // Colores neutros fijos (no son parte del branding del tenant)
    private static final Color C_GRIS  = new Color(108, 117, 125);
    private static final Color C_BORDE = new Color(222, 226, 230);

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_DT    = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

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
                                  List<LecturaFermentacion> lecturas) {
        Pal pal = Pal.of(branding);
        String brandName = branding.name();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 36, 36, 50, 45);
        try {
            PdfWriter.getInstance(doc, baos);
            doc.open();

            addCabeceraPdf(doc, lote, brandName, pal);

            addTituloPdf(doc, "INFORMACIÓN DEL LOTE", pal);
            addTablaInfoLote(doc, lote, pal);

            if (lote.getDensidadInicial() != null) {
                addTituloPdf(doc, "PARÁMETROS Y MÉTRICAS DE CALIDAD", pal);
                addTablaMetricas(doc, lote, pal);
            }

            if (lote.getReceta() != null &&
                    (lote.getReceta().getOgObjetivo() != null || lote.getReceta().getFgObjetivo() != null)) {
                addTituloPdf(doc, "COMPARATIVA RECETA VS LOTE", pal);
                addComparativaRecetaLote(doc, lote, pal);
            }

            if (!lote.getIngredientes().isEmpty()) {
                addTituloPdf(doc, "INGREDIENTES", pal);
                addIngredientes(doc, lote, pal);
            }

            addTituloPdf(doc, "FASES DEL PROCESO", pal);
            addTablaFases(doc, lote, pal);

            if (lecturas != null && !lecturas.isEmpty()) {
                addCurvaFermentacion(doc, lote, lecturas, pal);
            }

            if (lote.getCarbMetodo() != null || lote.getCarbCo2Objetivo() != null
                    || lote.getCarbDestino() != null) {
                addTituloPdf(doc, "CARBONATACIÓN — DETALLE", pal);
                addDetalleCarbonacion(doc, lote, pal);
            }

            if (lote.getCostoTotal() != null) {
                addTituloPdf(doc, "COSTO DE PRODUCCIÓN", pal);
                addCostos(doc, lote, pal);
            }

            boolean hayObs  = lote.getObservaciones() != null && !lote.getObservaciones().isBlank();
            boolean hayCata = lote.getNotasCata() != null && !lote.getNotasCata().isBlank();
            if (hayObs || hayCata) {
                addTituloPdf(doc, "OBSERVACIONES Y NOTAS DE CATA", pal);
                addNotas(doc, lote, hayObs, hayCata, pal);
            }

            doc.add(new Paragraph("\n"));
            Paragraph pie = new Paragraph(
                    "Generado el " + LocalDateTime.now().format(FMT_DT) + "  ·  " + brandName + " — Sistema de Trazabilidad",
                    new Font(Font.HELVETICA, 7, Font.ITALIC, C_GRIS));
            pie.setAlignment(Element.ALIGN_RIGHT);
            doc.add(pie);

            doc.close();
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF del lote " + lote.getCodigoLote(), e);
        }
        return baos.toByteArray();
    }

    // ── PDF Receta ───────────────────────────────────────────────────

    public byte[] generarPdfReceta(Receta receta, ExportBranding branding) {
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
            Paragraph tituloH = new Paragraph("FICHA DE RECETA",
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
            addTituloPdf(doc, "INFORMACIÓN GENERAL", pal);
            Font lbl = new Font(Font.HELVETICA, 8, Font.BOLD, pal.verde());
            Font val = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
            PdfPTable info = new PdfPTable(new float[]{1.2f, 2, 1.2f, 2});
            info.setWidthPercentage(100);
            par(info, "Nombre",    receta.getNombre() != null ? receta.getNombre() : "—", lbl, val, pal);
            par(info, "Estilo",    receta.getEstilo() != null ? receta.getEstilo() : "—", lbl, val, pal);
            par(info, "Estado",    receta.isActiva() ? "Activa" : "Inactiva", lbl, val, pal);
            par(info, "Versión",   receta.getVersion() != null ? "v" + receta.getVersion() : "v1", lbl, val, pal);
            par(info, "Hervor",    receta.getTiempoHervorMinutos() != null
                    ? receta.getTiempoHervorMinutos() + " min" : "—", lbl, val, pal);
            par(info, "Vol. base", receta.getVolumenBase() != null
                    ? receta.getVolumenBase() + " L" : "—", lbl, val, pal);
            if (receta.getAguaMacerado() != null) {
                par(info, "Agua macerado", receta.getAguaMacerado() + " "
                        + (receta.getUnidadAguaMacerado() != null ? receta.getUnidadAguaMacerado() : "L"), lbl, val, pal);
                par(info, "Agua sparge", receta.getAguaSparge() != null
                        ? receta.getAguaSparge() + " "
                          + (receta.getUnidadAguaSparge() != null ? receta.getUnidadAguaSparge() : "L") : "—",
                        lbl, val, pal);
            }
            if (receta.getPhAgua() != null) {
                par(info, "pH Agua", receta.getPhAgua().toString(), lbl, val, pal);
                par(info, "", "", lbl, val, pal);
            }
            doc.add(info);

            if (notBlank(receta.getDescripcion())) {
                Font lblD = new Font(Font.HELVETICA, 8, Font.BOLD, pal.verde());
                Paragraph pd = new Paragraph("Descripción", lblD);
                pd.setSpacingBefore(4);
                doc.add(pd);
                doc.add(new Paragraph(receta.getDescripcion(),
                        new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY)));
            }

            // Métricas objetivo
            if (receta.getOgObjetivo() != null || receta.getFgObjetivo() != null) {
                addTituloPdf(doc, "PARÁMETROS OBJETIVO", pal);
                Font lblM = new Font(Font.HELVETICA, 7, Font.BOLD, pal.verde());
                Font valM = new Font(Font.HELVETICA, 10, Font.BOLD, pal.verdeOscuro());
                Font subM = new Font(Font.HELVETICA, 8, Font.NORMAL, C_GRIS);
                PdfPTable metricasR = new PdfPTable(3);
                metricasR.setWidthPercentage(60);
                if (receta.getOgObjetivo() != null)
                    metricaCell(metricasR, "OG", String.valueOf(receta.getOgObjetivo()),
                            lblM, valM, subM, "Gravedad inicial", pal);
                if (receta.getFgObjetivo() != null)
                    metricaCell(metricasR, "FG", String.valueOf(receta.getFgObjetivo()),
                            lblM, valM, subM, "Gravedad final", pal);
                if (receta.getOgObjetivo() != null && receta.getFgObjetivo() != null) {
                    double abvObj = (receta.getOgObjetivo() - receta.getFgObjetivo()) * 0.13125;
                    metricaCell(metricasR, "ABV",
                            String.format(java.util.Locale.US, "%.2f%%", abvObj),
                            lblM, valM, subM, "Estimado", pal);
                }
                doc.add(metricasR);
            }

            // Ingredientes
            if (!receta.getIngredientes().isEmpty()) {
                addTituloPdf(doc, "INGREDIENTES", pal);
                addIngredientesReceta(doc, receta, pal);
            }

            // Escalones de macerado
            if (!receta.getEscalones().isEmpty()) {
                addTituloPdf(doc, "ESCALONES DE MACERADO", pal);
                Font thF = new Font(Font.HELVETICA, 8, Font.BOLD, pal.crema());
                Font tdF = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.DARK_GRAY);
                PdfPTable tEsc = new PdfPTable(new float[]{0.5f, 2, 1, 1});
                tEsc.setWidthPercentage(100);
                for (String h : new String[]{"#", "Escalón", "Duración", "Temp."}) {
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
                addTituloPdf(doc, "HERVOR Y ADICIONES", pal);
                Font thF = new Font(Font.HELVETICA, 8, Font.BOLD, pal.crema());
                Font tdF = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.DARK_GRAY);
                PdfPTable tAdic = new PdfPTable(new float[]{3, 1.5f, 1.5f});
                tAdic.setWidthPercentage(100);
                for (String h : new String[]{"Insumo", "Min. restantes", "Cantidad"}) {
                    PdfPCell c = new PdfPCell(new Phrase(h, thF));
                    c.setBackgroundColor(pal.verdeOscuro()); c.setBorder(0); c.setPadding(5);
                    tAdic.addCell(c);
                }
                for (AdicionHervor a : receta.getAdicionesHervor()) {
                    tableCell(tAdic, a.getNombre() != null ? a.getNombre() : "—", tdF);
                    String mins = a.getMinutosRestantes() != null
                            ? (a.getMinutosRestantes() == 0 ? "Flameout" : a.getMinutosRestantes() + " min") : "—";
                    tableCell(tAdic, mins, tdF);
                    tableCell(tAdic, a.getCantidad() != null
                            ? a.getCantidad() + " " + (a.getUnidad() != null ? a.getUnidad() : "") : "—", tdF);
                }
                doc.add(tAdic);
            }

            // Notas
            if (notBlank(receta.getNotas())) {
                addTituloPdf(doc, "NOTAS TÉCNICAS", pal);
                doc.add(new Paragraph(receta.getNotas(),
                        new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY)));
            }

            // Pie
            doc.add(new Paragraph("\n"));
            Paragraph pie = new Paragraph(
                    "Generado el " + LocalDateTime.now().format(FMT_DT) + "  ·  " + brandName + " — Sistema de Trazabilidad",
                    new Font(Font.HELVETICA, 7, Font.ITALIC, C_GRIS));
            pie.setAlignment(Element.ALIGN_RIGHT);
            doc.add(pie);

            doc.close();
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF de receta " + receta.getNombre(), e);
        }
        return baos.toByteArray();
    }

    // ── PDF Comparativa ──────────────────────────────────────────────

    public byte[] generarPdfComparativa(List<LoteCerveza> lotes,
                                         Map<String, Long> mejoresMax,
                                         Long mejorCpl,
                                         ExportBranding branding) {
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
            Paragraph titulo = new Paragraph("COMPARATIVA DE LOTES",
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

            addCeldaHeader(tabla, "Métrica", fHeader, pal);
            for (LoteCerveza l : lotes) addCeldaHeader(tabla, l.getCodigoLote(), fHeader, pal);

            String[][] metricas = {
                {"Estilo",          null},
                {"OG",              null},
                {"FG",              null},
                {"ABV (%)",         "abv"},
                {"Atenuación (%)",  "atenuacion"},
                {"Eficiencia (%)",  "eficiencia"},
                {"Litros",          "litros"},
                {"Costo total",     null},
                {"Costo/litro",     "cpl"}
            };

            boolean alt = false;
            for (String[] m : metricas) {
                Color bg = alt ? pal.fondo() : Color.WHITE;
                alt = !alt;
                addCeldaLabel(tabla, m[0], fLabel, bg);
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
                doc.add(new Paragraph("Notas de cata",
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
                    "Generado el " + LocalDateTime.now().format(FMT_DT) + "  ·  " + brandName,
                    new Font(Font.HELVETICA, 7, Font.ITALIC, C_GRIS));
            pie.setAlignment(Element.ALIGN_RIGHT);
            doc.add(pie);

            doc.close();
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF de comparativa", e);
        }
        return baos.toByteArray();
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
        Paragraph tituloP = new Paragraph("FICHA DE TRAZABILIDAD",
                new Font(Font.HELVETICA, 13, Font.BOLD, pal.crema()));
        tituloP.setSpacingBefore(4);
        String fase = lote.isCompletado() ? "COMPLETADO" : lote.getFaseActual().toUpperCase();
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
        par(t, "Código",  lote.getCodigoLote(), lbl, val, pal);
        par(t, "Estilo",  lote.getEstilo(), lbl, val, pal);
        par(t, "Fecha elaboración",
                lote.getFechaElaboracion() != null ? lote.getFechaElaboracion().format(FMT_FECHA) : "—", lbl, val, pal);
        par(t, "Fermentador",
                lote.getEquipoFermentador() != null ? lote.getEquipoFermentador().getNombre() : "—", lbl, val, pal);
        par(t, "Receta",
                lote.getReceta() != null ? lote.getReceta().getNombre() : "—", lbl, val, pal);
        par(t, "Creado por",
                lote.getCreatedBy() != null ? lote.getCreatedBy() : "—", lbl, val, pal);
        doc.add(t);
    }

    private void addTablaMetricas(Document doc, LoteCerveza lote, Pal pal) throws DocumentException {
        Font lbl  = new Font(Font.HELVETICA, 7,  Font.BOLD,   pal.verde());
        Font val  = new Font(Font.HELVETICA, 10, Font.BOLD,   pal.verdeOscuro());
        Font sub  = new Font(Font.HELVETICA, 8,  Font.NORMAL, C_GRIS);
        Font lblP = new Font(Font.HELVETICA, 8,  Font.BOLD,   pal.verde());
        Font valP = new Font(Font.HELVETICA, 9,  Font.NORMAL, Color.DARK_GRAY);

        PdfPTable tp = new PdfPTable(new float[]{1.2f, 2, 1.2f, 2});
        tp.setWidthPercentage(100); tp.setSpacingAfter(6);
        par(tp, "Agua utilizada",
                lote.getAguaUtilizada() != null ? lote.getAguaUtilizada() + " L" : "—", lblP, valP, pal);
        par(tp, "pH agua",
                lote.getPhAgua() != null ? lote.getPhAgua().toString() : "—", lblP, valP, pal);
        par(tp, "Vol. final",
                lote.getLitrosFinales() != null ? lote.getLitrosFinales() + " L" : "—", lblP, valP, pal);
        par(tp, "Clarificante",
                notBlank(lote.getClarificante()) ? lote.getClarificante() : "—", lblP, valP, pal);
        doc.add(tp);

        PdfPTable tm = new PdfPTable(6);
        tm.setWidthPercentage(100);
        metricaCell(tm, "OG",         lote.getDensidadInicial() != null ? String.valueOf(lote.getDensidadInicial()) : "—",    lbl, val, sub, "Densidad inicial", pal);
        String fgSub = lote.getDensidadFinalFecha() != null
                ? "Densidad final · " + lote.getDensidadFinalFecha().format(FMT_FECHA) : "Densidad final";
        metricaCell(tm, "FG",         lote.getDensidadFinal()   != null ? String.valueOf(lote.getDensidadFinal())   : "Pendiente", lbl, val, sub, fgSub, pal);
        metricaCell(tm, "ABV",        lote.getAbv()               != null ? lote.getAbv()               + "%" : "—", lbl, val, sub, "% vol.",   pal);
        metricaCell(tm, "Atenuación", lote.getAtenuacionAparente()  != null ? lote.getAtenuacionAparente()  + "%" : "—", lbl, val, sub, "Aparente", pal);
        metricaCell(tm, "Eficiencia", lote.getEficienciaMacerado() != null ? lote.getEficienciaMacerado() + "%" : "—", lbl, val, sub, "Macerado", pal);
        metricaCell(tm, "Litros",     lote.getLitrosFinales()      != null ? lote.getLitrosFinales()      + " L" : "—", lbl, val, sub, "Vol. final", pal);
        doc.add(tm);
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
        PdfPTable t = new PdfPTable(new float[]{1, 4});
        t.setWidthPercentage(100);
        addGrupoIngredientes(t, "MALTAS",        lote.getMaltas(),        grupoFont, ingFont, pal);
        addGrupoIngredientes(t, "LÚPULOS",       lote.getLupulos(),       grupoFont, ingFont, pal);
        addGrupoIngredientes(t, "LEVADURAS",     lote.getLevaduras(),     grupoFont, ingFont, pal);
        addGrupoIngredientes(t, "CLARIFICANTES", lote.getClarificantes(), grupoFont, ingFont, pal);
        doc.add(t);
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
        PdfPTable t = new PdfPTable(new float[]{1, 4});
        t.setWidthPercentage(100);
        addGrupoRecetaIngredientes(t, "MALTAS",        receta.getMaltas(),        grupoFont, ingFont, pal);
        addGrupoRecetaIngredientes(t, "LÚPULOS",       receta.getLupulos(),       grupoFont, ingFont, pal);
        addGrupoRecetaIngredientes(t, "LEVADURAS",     receta.getLevaduras(),     grupoFont, ingFont, pal);
        addGrupoRecetaIngredientes(t, "CLARIFICANTES", receta.getClarificantes(), grupoFont, ingFont, pal);
        doc.add(t);
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
                : "NATURAL".equals(lote.getCarbMetodo()) ? "Natural / Priming" : "Forzada / Inyección CO₂";
        par(t1, "Método", metodoTexto, lbl, val, pal);

        String co2 = "—";
        if (lote.getCarbCo2Objetivo() != null && lote.getCarbCo2Real() != null)
            co2 = lote.getCarbCo2Objetivo() + " vol → " + lote.getCarbCo2Real() + " vol (real)";
        else if (lote.getCarbCo2Objetivo() != null)
            co2 = lote.getCarbCo2Objetivo() + " vol (objetivo)";
        else if (lote.getCarbCo2Real() != null)
            co2 = lote.getCarbCo2Real() + " vol (real)";
        par(t1, "CO₂", co2, lbl, val, pal);

        String validacion = lote.getCarbValidacion() == null ? "—" : switch (lote.getCarbValidacion()) {
            case "ADECUADA"           -> "Espuma y retención adecuadas";
            case "RETENCION_CORRECTA" -> "Retención correcta";
            case "SOBRECARBONATADA"   -> "Sobrecarbonatada";
            case "BAJA_CARBONATACION" -> "Baja carbonatación";
            default                   -> lote.getCarbValidacion();
        };
        par(t1, "Validación organoléptica", validacion, lbl, val, pal);
        par(t1, "Destino / empaque", lote.getCarbDestino() != null ? lote.getCarbDestino() : "—", lbl, val, pal);
        doc.add(t1);

        // Fila 2: parámetros específicos del método
        if ("NATURAL".equals(lote.getCarbMetodo())
                && (lote.getCarbAzucarTipo() != null || lote.getCarbAzucarGramos() != null)) {
            PdfPTable t2 = new PdfPTable(new float[]{1.2f, 2, 1.2f, 2});
            t2.setWidthPercentage(100); t2.setSpacingAfter(4);
            par(t2, "Tipo de azúcar",
                    lote.getCarbAzucarTipo() != null ? lote.getCarbAzucarTipo() : "—", lbl, val, pal);
            par(t2, "Gramos añadidos",
                    lote.getCarbAzucarGramos() != null ? lote.getCarbAzucarGramos() + " g" : "—", lbl, val, pal);
            doc.add(t2);
        }

        if ("FORZADA".equals(lote.getCarbMetodo())
                && (lote.getCarbPresionPsi() != null || lote.getCarbTiempoHoras() != null
                    || lote.getCarbTecnica() != null)) {
            PdfPTable t2 = new PdfPTable(new float[]{1.2f, 2, 1.2f, 2});
            t2.setWidthPercentage(100); t2.setSpacingAfter(4);
            par(t2, "Presión",
                    lote.getCarbPresionPsi() != null ? lote.getCarbPresionPsi() + " PSI" : "—", lbl, val, pal);
            par(t2, "Tiempo exposición",
                    lote.getCarbTiempoHoras() != null ? lote.getCarbTiempoHoras() + " horas" : "—", lbl, val, pal);
            String tecnica = lote.getCarbTecnica() == null ? "—"
                    : "PIEDRA".equals(lote.getCarbTecnica()) ? "Piedra de difusión" : "Presión fija (superficie / agitación)";
            par(t2, "Técnica", tecnica, lbl, val, pal);
            doc.add(t2);
        }
    }

    private void addTablaFases(Document doc, LoteCerveza lote, Pal pal) throws DocumentException {
        Font thFont  = new Font(Font.HELVETICA, 8, Font.BOLD,   pal.crema());
        Font lblFont = new Font(Font.HELVETICA, 7, Font.BOLD,   C_GRIS);
        Font valFont = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.DARK_GRAY);

        PdfPTable t = new PdfPTable(new float[]{1.2f, 1, 1, 1, 1});
        t.setWidthPercentage(100);
        for (String h : new String[]{"", "Fermentación", "Acondic.", "Maduración", "Carbonatación"}) {
            PdfPCell c = new PdfPCell(new Phrase(h, thFont));
            c.setBackgroundColor(pal.verde()); c.setBorder(0);
            c.setPadding(5); c.setHorizontalAlignment(Element.ALIGN_CENTER);
            t.addCell(c);
        }
        faseRow(t, "Inicio",
                fmt(lote.getFermFechaInicial()),    fmt(lote.getAcondFechaInicial()),
                fmt(lote.getMadurFechaInicial()),   fmt(lote.getCarbFechaInicial()),    lblFont, valFont, pal);
        faseRow(t, "Fin ideal",
                fmt(lote.getFermFechaFinalIdeal()), fmt(lote.getAcondFechaFinalIdeal()),
                fmt(lote.getMadurFechaFinalIdeal()), fmt(lote.getCarbFechaFinalIdeal()), lblFont, valFont, pal);
        faseRow(t, "Fin real",
                fmt(lote.getFermFechaFinal()),      fmt(lote.getAcondFechaFinal()),
                fmt(lote.getMadurFechaFinal()),     fmt(lote.getCarbFechaFinal()),      lblFont, valFont, pal);
        faseRow(t, "Temperatura",
                temp(lote.getFermTemperatura()),    temp(lote.getAcondTemperatura()),
                temp(lote.getMadurTemperatura()),   temp(lote.getCarbTemperatura()),    lblFont, valFont, pal);
        doc.add(t);
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
        addTituloPdf(doc, "CURVA DE FERMENTACIÓN", pal);

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
            g.drawString("Densidad", plotLeft + r * 2 + 3, 10);
            if (hayTemp2) {
                int lx2 = plotLeft + r * 2 + 58 * sc;
                g.setColor(new java.awt.Color(2, 136, 209));
                g.fillOval(lx2, 2, r * 2, r * 2);
                g.setColor(C_GRIS);
                g.drawString("Temp. (°C)", lx2 + r * 2 + 3, 10);
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
        PdfPTable t = new PdfPTable(widths);
        t.setWidthPercentage(100); t.setSpacingBefore(6);
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, thF));
            c.setBackgroundColor(pal.verdeOscuro()); c.setBorder(0); c.setPadding(4);
            t.addCell(c);
        }
        for (LecturaFermentacion lec : lecturas) {
            boolean tieneAbv = lec.getDensidad() != null && lote.getDensidadInicial() != null
                               && lec.getDensidad() < lote.getDensidadInicial();
            String abvStr = tieneAbv
                    ? lec.getAbvParcial(lote.getDensidadInicial()).toPlainString() + "%" : "—";
            tableCell(t, FMT_FECHA.format(lec.getFecha()), tdF);
            tableCell(t, lec.getDensidad() != null ? String.valueOf(lec.getDensidad()) : "—", tdF);
            tableCell(t, abvStr, tdF);
            if (hayTemp)  tableCell(t, lec.getTemperatura() != null ? lec.getTemperatura() + " °C" : "—", tdF);
            if (hayNotas) tableCell(t, notBlank(lec.getNotas()) ? lec.getNotas() : "—", tdF);
        }
        doc.add(t);
    }

    private void addCostos(Document doc, LoteCerveza lote, Pal pal) throws DocumentException {
        Font lbl = new Font(Font.HELVETICA, 8, Font.BOLD,   pal.verde());
        Font val = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);

        PdfPTable resumen = new PdfPTable(new float[]{1.2f, 2, 1.2f, 2});
        resumen.setWidthPercentage(100); resumen.setSpacingAfter(6);
        par(resumen, "Costo total",     "$" + fmt2(lote.getCostoTotal()), lbl, val, pal);
        par(resumen, "Costo por litro",
                lote.getCostoPorLitro() != null ? "$" + fmt2(lote.getCostoPorLitro()) : "—", lbl, val, pal);
        par(resumen, "Ítems asignados", String.valueOf(lote.getItemsFactura().size()), lbl, val, pal);
        par(resumen, "", "", lbl, val, pal);
        doc.add(resumen);

        Font thF = new Font(Font.HELVETICA, 7, Font.BOLD,   pal.crema());
        Font tdF = new Font(Font.HELVETICA, 7, Font.NORMAL, Color.DARK_GRAY);
        PdfPTable dt = new PdfPTable(new float[]{1.5f, 1.5f, 2, 1, 1});
        dt.setWidthPercentage(100);
        for (String h : new String[]{"Factura", "Proveedor", "Ítem", "Cantidad", "Valor"}) {
            PdfPCell c = new PdfPCell(new Phrase(h, thF));
            c.setBackgroundColor(pal.verdeOscuro()); c.setBorder(0); c.setPadding(4);
            dt.addCell(c);
        }
        for (LoteItemFactura li : lote.getItemsFactura()) {
            String nroFact = li.getItem().getFactura().getNumeroFactura();
            if (nroFact == null || nroFact.isBlank()) nroFact = "#" + li.getItem().getFactura().getId();
            String cant = li.getCantidadAsignada().doubleValue() == 0.0
                    ? "Total"
                    : li.getCantidadAsignada() + " " + (li.getItem().getUnidad() != null ? li.getItem().getUnidad() : "");
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

        PdfPTable t = new PdfPTable(cols);
        t.setWidthPercentage(cols == 1 ? 35 : cols == 2 ? 60 : 90);
        t.setSpacingAfter(6);

        if (tieneOg) {
            PdfPCell cell = comparativaCell(lblF, valF, subF,
                    "OG OBJETIVO", String.valueOf(r.getOgObjetivo()), pal);
            if (lote.getDensidadInicial() != null) {
                int diff = lote.getDensidadInicial() - r.getOgObjetivo();
                String diffStr = (diff > 0 ? "+" : "") + diff;
                Paragraph p = new Paragraph("Real: " + lote.getDensidadInicial() + "  (" + diffStr + ")",
                        diff >= 0 ? posF : negF);
                p.setAlignment(Element.ALIGN_CENTER);
                cell.addElement(p);
            } else {
                addPendiente(cell, subF);
            }
            t.addCell(cell);
        }

        if (tieneFg) {
            PdfPCell cell = comparativaCell(lblF, valF, subF,
                    "FG OBJETIVO", String.valueOf(r.getFgObjetivo()), pal);
            if (lote.getDensidadFinal() != null) {
                Paragraph p = new Paragraph("Real: " + lote.getDensidadFinal(), subF);
                p.setAlignment(Element.ALIGN_CENTER);
                cell.addElement(p);
            } else {
                addPendiente(cell, subF);
            }
            t.addCell(cell);
        }

        if (tieneAbv) {
            double abvObj = (r.getOgObjetivo() - r.getFgObjetivo()) * 0.13125;
            PdfPCell cell = comparativaCell(lblF, valF, subF,
                    "ABV OBJETIVO", String.format(java.util.Locale.US, "%.2f%%", abvObj), pal);
            if (lote.getAbv() != null) {
                Paragraph p = new Paragraph("Real: " + lote.getAbv() + "%", subF);
                p.setAlignment(Element.ALIGN_CENTER);
                cell.addElement(p);
            } else {
                addPendiente(cell, subF);
            }
            t.addCell(cell);
        }

        doc.add(t);
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
        Paragraph p = new Paragraph("Pendiente", font);
        p.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(p);
    }

    private void addNotas(Document doc, LoteCerveza lote,
                          boolean hayObs, boolean hayCata, Pal pal) throws DocumentException {
        Font lbl = new Font(Font.HELVETICA, 8, Font.BOLD,   pal.verde());
        Font txt = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
        if (hayObs) {
            Paragraph p = new Paragraph("Observaciones", lbl); p.setSpacingBefore(4);
            doc.add(p); doc.add(new Paragraph(lote.getObservaciones(), txt));
        }
        if (hayCata) {
            Paragraph p = new Paragraph("Notas de cata", lbl); p.setSpacingBefore(hayObs ? 8 : 4);
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
            case "Estilo"          -> l.getEstilo() != null ? l.getEstilo() : "—";
            case "OG"              -> l.getDensidadInicial() != null ? String.valueOf(l.getDensidadInicial()) : "—";
            case "FG"              -> l.getDensidadFinal()   != null ? String.valueOf(l.getDensidadFinal())   : "—";
            case "ABV (%)"         -> l.getAbv()               != null ? l.getAbv()               + "%" : "—";
            case "Atenuación (%)"  -> l.getAtenuacionAparente()  != null ? l.getAtenuacionAparente()  + "%" : "—";
            case "Eficiencia (%)"  -> l.getEficienciaMacerado() != null ? l.getEficienciaMacerado() + "%" : "—";
            case "Litros"          -> l.getLitrosFinales()      != null ? l.getLitrosFinales()      + " L" : "—";
            case "Costo total"     -> l.getCostoTotal()         != null
                    ? "$ " + l.getCostoTotal().setScale(0, java.math.RoundingMode.HALF_UP) : "—";
            case "Costo/litro"     -> l.getCostoPorLitro()      != null
                    ? "$ " + l.getCostoPorLitro().setScale(0, java.math.RoundingMode.HALF_UP) : "—";
            default -> "—";
        };
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

    private String[] buildHeaders(boolean hayTemp, boolean hayNotas) {
        if (hayTemp && hayNotas) return new String[]{"Fecha", "Densidad", "ABV parcial", "Temp.", "Notas"};
        if (hayTemp)  return new String[]{"Fecha", "Densidad", "ABV parcial", "Temp."};
        if (hayNotas) return new String[]{"Fecha", "Densidad", "ABV parcial", "Notas"};
        return new String[]{"Fecha", "Densidad", "ABV parcial"};
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
                                               ExportBranding branding) {
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
            Paragraph tituloH = new Paragraph("REPORTE DE PRODUCCIÓN",
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

            addTituloPdf(doc, "RESUMEN DEL PERÍODO", pal);
            Font lbl = new Font(Font.HELVETICA, 8, Font.BOLD, pal.verde());
            Font val = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
            PdfPTable stats = new PdfPTable(new float[]{1.2f, 1.5f, 1.2f, 1.5f, 1.2f, 1.5f, 1.2f, 1.5f});
            stats.setWidthPercentage(100);
            par(stats, "Total Lotes",       String.valueOf(totalLotes),       lbl, val, pal);
            par(stats, "Litros Producidos", fmt2(totalLitros) + " L",          lbl, val, pal);
            par(stats, "ABV Promedio",      avgAbvStr,                         lbl, val, pal);
            par(stats, "Eficiencia Prom.",  avgEfStr,                          lbl, val, pal);
            par(stats, "Estilos Distintos", String.valueOf(lotes.stream().map(LoteCerveza::getEstilo).distinct().count()), lbl, val, pal);
            par(stats, "Completados",       completados + " (" + tasaComp + "%)", lbl, val, pal);
            par(stats, "Costo Total",       costoTotal != null ? "$" + fmt2(costoTotal) : "—", lbl, val, pal);
            par(stats, "Generado",          LocalDate.now().format(FMT_FECHA), lbl, val, pal);
            doc.add(stats);

            // ── Tabla de lotes ────────────────────────────────────────
            if (!lotes.isEmpty()) {
                addTituloPdf(doc, "LOTES EN EL PERÍODO", pal);
                PdfPTable tabla = new PdfPTable(new float[]{1.4f, 1.5f, 1.8f, 1f, 0.8f, 0.8f, 0.9f, 0.9f, 1.2f});
                tabla.setWidthPercentage(100);
                Font th = new Font(Font.HELVETICA, 7, Font.BOLD, pal.crema());
                Font td = new Font(Font.HELVETICA, 7, Font.NORMAL, Color.DARK_GRAY);
                Font tdGreen = new Font(Font.HELVETICA, 7, Font.BOLD, pal.verde());
                for (String h : new String[]{"Código","Estilo","Receta","Fecha","Litros","OG","ABV","Efic.","Estado"}) {
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
                    String abvTxt = lote.getAbv() != null ? lote.getAbv() + "%" : "—";
                    PdfPCell abvCell = new PdfPCell(new Phrase(abvTxt, lote.getAbv() != null ? tdGreen : td));
                    abvCell.setBackgroundColor(bg); abvCell.setBorderColor(C_BORDE); abvCell.setPadding(4);
                    tabla.addCell(abvCell);
                    tablaCelda(tabla, lote.getEficienciaMacerado() != null ? lote.getEficienciaMacerado() + "%" : "—", td, bg);
                    tablaCelda(tabla, lote.getFaseActual() != null ? lote.getFaseActual() : "—", td, bg);
                }
                doc.add(tabla);
            }

            // ── Resumen por estilo ────────────────────────────────────
            Map<String, BigDecimal[]> resAgg = new LinkedHashMap<>();
            for (var lote : lotes) {
                String est = lote.getEstilo() != null ? lote.getEstilo() : "Sin estilo";
                BigDecimal[] agg = resAgg.computeIfAbsent(est, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                agg[0] = agg[0].add(BigDecimal.ONE);
                agg[1] = agg[1].add(lote.getLitrosFinales() != null ? lote.getLitrosFinales() : BigDecimal.ZERO);
            }
            if (resAgg.size() > 1) {
                addTituloPdf(doc, "RESUMEN POR ESTILO", pal);
                PdfPTable resTabla = new PdfPTable(new float[]{3f, 1f, 1.5f, 1f});
                resTabla.setWidthPercentage(60);
                resTabla.setHorizontalAlignment(Element.ALIGN_LEFT);
                Font rth = new Font(Font.HELVETICA, 7, Font.BOLD, pal.crema());
                for (String h : new String[]{"Estilo","Lotes","Litros","% Vol."}) {
                    PdfPCell c = new PdfPCell(new Phrase(h, rth));
                    c.setBackgroundColor(pal.verde()); c.setBorderColor(C_BORDE); c.setPadding(4);
                    resTabla.addCell(c);
                }
                boolean a2 = false;
                for (var e : resAgg.entrySet().stream()
                        .sorted((x, y) -> y.getValue()[1].compareTo(x.getValue()[1]))
                        .collect(Collectors.toList())) {
                    Color bg = a2 ? pal.fondo() : Color.WHITE; a2 = !a2;
                    BigDecimal litE = e.getValue()[1];
                    int pct = totalLitros.compareTo(BigDecimal.ZERO) > 0
                            ? litE.multiply(BigDecimal.valueOf(100))
                                    .divide(totalLitros, 0, java.math.RoundingMode.HALF_UP).intValue() : 0;
                    Font rtd = new Font(Font.HELVETICA, 7, Font.NORMAL, Color.DARK_GRAY);
                    tablaCelda(resTabla, e.getKey(), rtd, bg);
                    tablaCelda(resTabla, String.valueOf(e.getValue()[0].intValue()), rtd, bg);
                    tablaCelda(resTabla, fmt2(litE) + " L", rtd, bg);
                    tablaCelda(resTabla, pct + "%", rtd, bg);
                }
                doc.add(resTabla);
            }

            // ── Pie ───────────────────────────────────────────────────
            doc.add(new Paragraph("\n"));
            Paragraph pie = new Paragraph(
                    "Generado el " + LocalDateTime.now().format(FMT_DT) + "  ·  " + brandName + " — Sistema de Trazabilidad",
                    new Font(Font.HELVETICA, 7, Font.ITALIC, C_GRIS));
            pie.setAlignment(Element.ALIGN_RIGHT);
            doc.add(pie);
            doc.close();
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF del reporte de producción", e);
        }
        return baos.toByteArray();
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

    // ── PDF Venta (Remisión) ─────────────────────────────────────────

    public byte[] generarPdfVenta(Venta venta, ExportBranding branding) {
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

            Paragraph cabTipo = new Paragraph("REMISIÓN / NOTA DE DESPACHO", fTitulo);
            cabTipo.setAlignment(Element.ALIGN_LEFT);
            doc.add(cabTipo);

            Paragraph cabNum = new Paragraph("Ref. Venta #" + venta.getId(), fSub);
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
            addTituloPdf(doc, "DATOS DEL DESPACHO", pal);

            PdfPTable datos = new PdfPTable(new float[]{2f, 3f, 2f, 3f});
            datos.setWidthPercentage(100);
            datos.setSpacingAfter(12f);

            Font fLbl = new Font(Font.HELVETICA, 8, Font.BOLD, C_GRIS);
            Font fVal = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);

            par(datos, "Cliente",         venta.getCliente(), fLbl, fVal, pal);
            par(datos, "Fecha de Despacho",
                venta.getFechaDespacho() != null ? venta.getFechaDespacho().format(FMT_FECHA) : "—",
                fLbl, fVal, pal);
            String primerLote = venta.getPrimerCodigoLote();
            par(datos, "Lote",   primerLote != null ? primerLote : "Sin lote",
                fLbl, fVal, pal);
            par(datos, "Estado", venta.getEstado().getDisplayName(), fLbl, fVal, pal);
            doc.add(datos);

            // ── Tabla de ítems ────────────────────────────────────────
            addTituloPdf(doc, "DETALLE DE LA VENTA", pal);

            PdfPTable tabla = new PdfPTable(new float[]{1.8f, 2.2f, 1.2f, 1.8f, 1.2f, 1.8f});
            tabla.setWidthPercentage(100);
            tabla.setSpacingAfter(8f);

            Font fTh = new Font(Font.HELVETICA, 8, Font.BOLD, pal.crema());
            for (String h : new String[]{"Lote", "Descripción", "Cantidad", "Precio Unit.", "Desc.%", "Total"}) {
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
                PdfPCell emptyCell = new PdfPCell(new Phrase("Sin ítems registrados", fTd));
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

            PdfPCell lblCell = new PdfPCell(new Phrase("TOTAL A COBRAR", fTotLbl));
            lblCell.setBackgroundColor(pal.fondo()); lblCell.setBorderColor(C_BORDE); lblCell.setPadding(6);
            totBox.addCell(lblCell);

            PdfPCell valCell = new PdfPCell(new Phrase(totalStr, fTotVal));
            valCell.setBackgroundColor(pal.fondo()); valCell.setBorderColor(C_BORDE);
            valCell.setPadding(6); valCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totBox.addCell(valCell);
            doc.add(totBox);

            // ── Notas ─────────────────────────────────────────────────
            if (venta.getNotas() != null && !venta.getNotas().isBlank()) {
                addTituloPdf(doc, "NOTAS", pal);
                Paragraph notaP = new Paragraph(venta.getNotas(),
                        new Font(Font.HELVETICA, 9, Font.ITALIC, C_GRIS));
                notaP.setSpacingAfter(12f);
                doc.add(notaP);
            }

            // ── Pie ───────────────────────────────────────────────────
            doc.add(new Paragraph("\n"));
            Paragraph pie = new Paragraph(
                    "Generado el " + LocalDateTime.now().format(FMT_DT) + "  ·  " + brandName + " — Sistema de Trazabilidad",
                    new Font(Font.HELVETICA, 7, Font.ITALIC, C_GRIS));
            pie.setAlignment(Element.ALIGN_RIGHT);
            doc.add(pie);

            doc.close();
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF de venta #" + venta.getId(), e);
        }
        return baos.toByteArray();
    }

    private void tableCell(PdfPTable t, String text, Font font, Color bg, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text != null ? text : "—", font));
        c.setBackgroundColor(bg); c.setBorderColor(C_BORDE);
        c.setPadding(5); c.setHorizontalAlignment(align);
        t.addCell(c);
    }
}
