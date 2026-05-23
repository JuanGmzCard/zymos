package com.alera.service;

import com.alera.model.Ingrediente;
import com.alera.model.LecturaFermentacion;
import com.alera.model.LoteCerveza;
import com.alera.model.LoteItemFactura;
import java.util.Map;
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
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PdfExportService {

    private static final Color C_VERDE        = new Color(54, 67, 24);
    private static final Color C_VERDE_OSCURO = new Color(36, 46, 13);
    private static final Color C_DORADO       = new Color(201, 160, 40);
    private static final Color C_CREMA        = new Color(245, 237, 208);
    private static final Color C_FONDO        = new Color(240, 237, 226);
    private static final Color C_GRIS         = new Color(108, 117, 125);
    private static final Color C_BORDE        = new Color(222, 226, 230);
    private static final Color C_VERDE_CLARO  = new Color(198, 211, 180);

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_DT    = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generarPdfLote(LoteCerveza lote, String brandName,
                                   List<LecturaFermentacion> lecturas) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 36, 36, 50, 45);
        try {
            PdfWriter.getInstance(doc, baos);
            doc.open();

            addCabeceraPdf(doc, lote, brandName);

            addTituloPdf(doc, "INFORMACIÓN DEL LOTE");
            addTablaInfoLote(doc, lote);

            if (lote.getDensidadInicial() != null) {
                addTituloPdf(doc, "PARÁMETROS Y MÉTRICAS DE CALIDAD");
                addTablaMetricas(doc, lote);
            }

            if (!lote.getIngredientes().isEmpty()) {
                addTituloPdf(doc, "INGREDIENTES");
                addIngredientes(doc, lote);
            }

            addTituloPdf(doc, "FASES DEL PROCESO");
            addTablaFases(doc, lote);

            if (lecturas != null && !lecturas.isEmpty()) {
                addCurvaFermentacion(doc, lote, lecturas);
            }

            if (lote.getCostoTotal() != null) {
                addTituloPdf(doc, "COSTO DE PRODUCCIÓN");
                addCostos(doc, lote);
            }

            boolean hayObs  = lote.getObservaciones() != null && !lote.getObservaciones().isBlank();
            boolean hayCata = lote.getNotasCata() != null && !lote.getNotasCata().isBlank();
            if (hayObs || hayCata) {
                addTituloPdf(doc, "OBSERVACIONES Y NOTAS DE CATA");
                addNotas(doc, lote, hayObs, hayCata);
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

    // ── PDF Comparativa ──────────────────────────────────────────────

    public byte[] generarPdfComparativa(List<LoteCerveza> lotes,
                                          Map<String, Long> mejoresMax,
                                          Long mejorCpl,
                                          String brandName) {
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
            hCell.setBackgroundColor(C_VERDE);
            hCell.setBorder(0);
            hCell.setPadding(12);
            hCell.addElement(new Paragraph(brandName.toUpperCase(),
                    new Font(Font.HELVETICA, 7, Font.NORMAL, C_DORADO)));
            Paragraph titulo = new Paragraph("COMPARATIVA DE LOTES",
                    new Font(Font.HELVETICA, 14, Font.BOLD, C_CREMA));
            titulo.setSpacingBefore(4);
            hCell.addElement(titulo);
            String subtitulo = lotes.stream().map(LoteCerveza::getCodigoLote)
                    .reduce((a, b) -> a + "  ·  " + b).orElse("");
            hCell.addElement(new Paragraph(subtitulo,
                    new Font(Font.HELVETICA, 8, Font.ITALIC, C_CREMA)));
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

            Font fHeader = new Font(Font.HELVETICA, 8, Font.BOLD,  C_CREMA);
            Font fLabel  = new Font(Font.HELVETICA, 7, Font.BOLD,  C_VERDE);
            Font fValor  = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.BLACK);
            Font fMejorMax = new Font(Font.HELVETICA, 8, Font.BOLD, new Color(180, 130, 0));
            Font fMejorMin = new Font(Font.HELVETICA, 8, Font.BOLD, new Color(25, 135, 84));

            // Fila header
            addCeldaHeader(tabla, "Métrica", fHeader);
            for (LoteCerveza l : lotes) addCeldaHeader(tabla, l.getCodigoLote(), fHeader);

            // Filas de datos
            String[][] metricas = {
                {"Estilo",       null},
                {"OG",           null},
                {"FG",           null},
                {"ABV (%)",      "abv"},
                {"Atenuación (%)", "atenuacion"},
                {"Eficiencia (%)", "eficiencia"},
                {"Litros",       "litros"},
                {"Costo total",  null},
                {"Costo/litro",  "cpl"}
            };

            boolean alt = false;
            for (String[] m : metricas) {
                Color bg = alt ? C_FONDO : Color.WHITE;
                alt = !alt;
                addCeldaLabel(tabla, m[0], fLabel, bg);
                for (LoteCerveza l : lotes) {
                    String val = valorMetrica(l, m[0]);
                    boolean esMejorMax = m[1] != null && !m[1].equals("cpl")
                            && mejoresMax.containsKey(m[1]) && mejoresMax.get(m[1]).equals(l.getId());
                    boolean esMejorMin = "cpl".equals(m[1]) && l.getId().equals(mejorCpl);
                    Font f = esMejorMax ? fMejorMax : esMejorMin ? fMejorMin : fValor;
                    String texto = val + (esMejorMax ? " ★" : esMejorMin ? " ↓" : "");
                    PdfPCell c = new PdfPCell(new Phrase(texto, f));
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
                        new Font(Font.HELVETICA, 9, Font.BOLD, C_VERDE)));
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

    private void addCeldaHeader(PdfPTable tabla, String texto, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(texto, font));
        c.setBackgroundColor(C_VERDE_OSCURO);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setBorder(0);
        c.setPadding(6);
        tabla.addCell(c);
    }

    private void addCeldaLabel(PdfPTable tabla, String texto, Font font, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(texto, font));
        c.setBackgroundColor(bg);
        c.setBorderColor(C_BORDE);
        c.setPadding(5);
        tabla.addCell(c);
    }

    private String valorMetrica(LoteCerveza l, String metrica) {
        return switch (metrica) {
            case "Estilo"          -> l.getEstilo() != null ? l.getEstilo() : "—";
            case "OG"              -> l.getDensidadInicial() != null ? String.valueOf(l.getDensidadInicial()) : "—";
            case "FG"              -> l.getDensidadFinal() != null ? String.valueOf(l.getDensidadFinal()) : "—";
            case "ABV (%)"         -> l.getAbv() != null ? l.getAbv() + "%" : "—";
            case "Atenuación (%)"  -> l.getAtenuacionAparente() != null ? l.getAtenuacionAparente() + "%" : "—";
            case "Eficiencia (%)"  -> l.getEficienciaMacerado() != null ? l.getEficienciaMacerado() + "%" : "—";
            case "Litros"          -> l.getLitrosFinales() != null ? l.getLitrosFinales() + " L" : "—";
            case "Costo total"     -> l.getCostoTotal() != null ? "$ " + l.getCostoTotal().setScale(0, java.math.RoundingMode.HALF_UP) : "—";
            case "Costo/litro"     -> l.getCostoPorLitro() != null ? "$ " + l.getCostoPorLitro().setScale(0, java.math.RoundingMode.HALF_UP) : "—";
            default                -> "—";
        };
    }

    // ── PDF Lote ─────────────────────────────────────────────────────

    private void addCabeceraPdf(Document doc, LoteCerveza lote, String brandName) throws DocumentException {
        PdfPTable header = new PdfPTable(new float[]{3, 2});
        header.setWidthPercentage(100);
        header.setSpacingAfter(10);

        PdfPCell izq = new PdfPCell();
        izq.setBackgroundColor(C_VERDE);
        izq.setBorder(0);
        izq.setPaddingTop(12);
        izq.setPaddingBottom(12);
        izq.setPaddingLeft(14);
        izq.setPaddingRight(8);

        Paragraph brand = new Paragraph(brandName.toUpperCase(),
                new Font(Font.HELVETICA, 7, Font.NORMAL, C_DORADO));
        Paragraph titulo = new Paragraph("FICHA DE TRAZABILIDAD",
                new Font(Font.HELVETICA, 13, Font.BOLD, C_CREMA));
        titulo.setSpacingBefore(4);

        String fase = lote.isCompletado() ? "COMPLETADO" : lote.getFaseActual().toUpperCase();
        Paragraph faseP = new Paragraph(fase,
                new Font(Font.HELVETICA, 8, Font.NORMAL, C_VERDE_CLARO));
        faseP.setSpacingBefore(6);

        izq.addElement(brand);
        izq.addElement(titulo);
        izq.addElement(faseP);
        header.addCell(izq);

        PdfPCell der = new PdfPCell();
        der.setBackgroundColor(C_VERDE_OSCURO);
        der.setBorder(0);
        der.setPaddingTop(12);
        der.setPaddingBottom(12);
        der.setPaddingLeft(8);
        der.setPaddingRight(14);

        Paragraph codigo = new Paragraph(lote.getCodigoLote(),
                new Font(Font.HELVETICA, 18, Font.BOLD, C_DORADO));
        codigo.setAlignment(Element.ALIGN_RIGHT);

        Paragraph estilo = new Paragraph(lote.getEstilo(),
                new Font(Font.HELVETICA, 9, Font.NORMAL, C_CREMA));
        estilo.setAlignment(Element.ALIGN_RIGHT);
        estilo.setSpacingBefore(3);

        String fechaStr = lote.getFechaElaboracion() != null
                ? lote.getFechaElaboracion().format(FMT_FECHA) : "—";
        Paragraph fecha = new Paragraph(fechaStr,
                new Font(Font.HELVETICA, 8, Font.NORMAL, C_VERDE_CLARO));
        fecha.setAlignment(Element.ALIGN_RIGHT);
        fecha.setSpacingBefore(4);

        der.addElement(codigo);
        der.addElement(estilo);
        der.addElement(fecha);
        header.addCell(der);

        doc.add(header);
    }

    private void addTituloPdf(Document doc, String titulo) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(10);
        t.setSpacingAfter(4);
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(C_VERDE);
        cell.setBorder(0);
        cell.setPaddingTop(5);
        cell.setPaddingBottom(5);
        cell.setPaddingLeft(8);
        cell.addElement(new Paragraph(titulo,
                new Font(Font.HELVETICA, 8, Font.BOLD, C_CREMA)));
        t.addCell(cell);
        doc.add(t);
    }

    private void addTablaInfoLote(Document doc, LoteCerveza lote) throws DocumentException {
        Font lbl = new Font(Font.HELVETICA, 8, Font.BOLD, C_VERDE);
        Font val = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);

        PdfPTable t = new PdfPTable(new float[]{1.2f, 2, 1.2f, 2});
        t.setWidthPercentage(100);

        par(t, "Código", lote.getCodigoLote(), lbl, val);
        par(t, "Estilo", lote.getEstilo(), lbl, val);
        par(t, "Fecha elaboración",
                lote.getFechaElaboracion() != null ? lote.getFechaElaboracion().format(FMT_FECHA) : "—",
                lbl, val);
        par(t, "Fermentador",
                lote.getEquipoFermentador() != null ? lote.getEquipoFermentador().getNombre() : "—",
                lbl, val);
        par(t, "Receta",
                lote.getReceta() != null ? lote.getReceta().getNombre() : "—",
                lbl, val);
        par(t, "Creado por",
                lote.getCreatedBy() != null ? lote.getCreatedBy() : "—",
                lbl, val);

        doc.add(t);
    }

    private void addTablaMetricas(Document doc, LoteCerveza lote) throws DocumentException {
        Font lbl = new Font(Font.HELVETICA, 7, Font.BOLD, C_VERDE);
        Font val = new Font(Font.HELVETICA, 10, Font.BOLD, C_VERDE_OSCURO);
        Font sub = new Font(Font.HELVETICA, 8, Font.NORMAL, C_GRIS);

        PdfPTable tp = new PdfPTable(new float[]{1.2f, 2, 1.2f, 2});
        tp.setWidthPercentage(100);
        tp.setSpacingAfter(6);

        Font lblP = new Font(Font.HELVETICA, 8, Font.BOLD, C_VERDE);
        Font valP = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);

        par(tp, "Agua utilizada",
                lote.getAguaUtilizada() != null ? lote.getAguaUtilizada() + " L" : "—", lblP, valP);
        par(tp, "pH agua",
                lote.getPhAgua() != null ? lote.getPhAgua().toString() : "—", lblP, valP);
        par(tp, "Vol. final",
                lote.getLitrosFinales() != null ? lote.getLitrosFinales() + " L" : "—", lblP, valP);
        par(tp, "Clarificante",
                notBlank(lote.getClarificante()) ? lote.getClarificante() : "—", lblP, valP);

        doc.add(tp);

        PdfPTable tm = new PdfPTable(6);
        tm.setWidthPercentage(100);

        metricaCell(tm, "OG", lote.getDensidadInicial() != null ? String.valueOf(lote.getDensidadInicial()) : "—", lbl, val, sub, "Densidad inicial");
        metricaCell(tm, "FG", lote.getDensidadFinal() != null ? String.valueOf(lote.getDensidadFinal()) : "Pendiente", lbl, val, sub, "Densidad final");
        metricaCell(tm, "ABV", lote.getAbv() != null ? lote.getAbv() + "%" : "—", lbl, val, sub, "% vol.");
        metricaCell(tm, "Atenuación", lote.getAtenuacionAparente() != null ? lote.getAtenuacionAparente() + "%" : "—", lbl, val, sub, "Aparente");
        metricaCell(tm, "Eficiencia", lote.getEficienciaMacerado() != null ? lote.getEficienciaMacerado() + "%" : "—", lbl, val, sub, "Macerado");
        metricaCell(tm, "Litros", lote.getLitrosFinales() != null ? lote.getLitrosFinales() + " L" : "—", lbl, val, sub, "Vol. final");

        doc.add(tm);
    }

    private void metricaCell(PdfPTable t, String label, String value,
                               Font lbl, Font val, Font sub, String subtitulo) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(C_FONDO);
        cell.setBorderColor(C_BORDE);
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph p1 = new Paragraph(label, lbl);
        p1.setAlignment(Element.ALIGN_CENTER);
        Paragraph p2 = new Paragraph(value, val);
        p2.setAlignment(Element.ALIGN_CENTER);
        p2.setSpacingBefore(3);
        Paragraph p3 = new Paragraph(subtitulo, sub);
        p3.setAlignment(Element.ALIGN_CENTER);

        cell.addElement(p1);
        cell.addElement(p2);
        cell.addElement(p3);
        t.addCell(cell);
    }

    private void addIngredientes(Document doc, LoteCerveza lote) throws DocumentException {
        Font grupoFont = new Font(Font.HELVETICA, 8, Font.BOLD, C_VERDE);
        Font ingFont   = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.DARK_GRAY);

        PdfPTable t = new PdfPTable(new float[]{1, 4});
        t.setWidthPercentage(100);

        addGrupoIngredientes(t, "MALTAS",       lote.getMaltas(),       grupoFont, ingFont);
        addGrupoIngredientes(t, "LÚPULOS",      lote.getLupulos(),      grupoFont, ingFont);
        addGrupoIngredientes(t, "LEVADURAS",    lote.getLevaduras(),    grupoFont, ingFont);
        addGrupoIngredientes(t, "CLARIFICANTES",lote.getClarificantes(),grupoFont, ingFont);

        doc.add(t);
    }

    private void addGrupoIngredientes(PdfPTable t, String grupo, List<Ingrediente> lista,
                                       Font grupoFont, Font ingFont) {
        if (lista.isEmpty()) return;

        PdfPCell cGrupo = new PdfPCell(new Phrase(grupo, grupoFont));
        cGrupo.setBackgroundColor(C_FONDO);
        cGrupo.setBorderColor(C_BORDE);
        cGrupo.setPadding(6);
        cGrupo.setVerticalAlignment(Element.ALIGN_MIDDLE);
        t.addCell(cGrupo);

        String texto = lista.stream()
                .map(i -> i.getNombre() + (notBlank(i.getCantidad()) ? " · " + i.getCantidad() : ""))
                .collect(Collectors.joining("   |   "));
        PdfPCell cVal = new PdfPCell(new Phrase(texto, ingFont));
        cVal.setBorderColor(C_BORDE);
        cVal.setPadding(6);
        t.addCell(cVal);
    }

    private void addTablaFases(Document doc, LoteCerveza lote) throws DocumentException {
        Font thFont  = new Font(Font.HELVETICA, 8, Font.BOLD, C_CREMA);
        Font lblFont = new Font(Font.HELVETICA, 7, Font.BOLD, C_GRIS);
        Font valFont = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.DARK_GRAY);

        PdfPTable t = new PdfPTable(new float[]{1.2f, 1, 1, 1, 1});
        t.setWidthPercentage(100);

        for (String h : new String[]{"", "Fermentación", "Acondic.", "Maduración", "Carbonatación"}) {
            PdfPCell c = new PdfPCell(new Phrase(h, thFont));
            c.setBackgroundColor(C_VERDE);
            c.setBorder(0);
            c.setPadding(5);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            t.addCell(c);
        }

        faseRow(t, "Inicio",
                fmt(lote.getFermFechaInicial()), fmt(lote.getAcondFechaInicial()),
                fmt(lote.getMadurFechaInicial()), fmt(lote.getCarbFechaInicial()), lblFont, valFont);
        faseRow(t, "Fin ideal",
                fmt(lote.getFermFechaFinalIdeal()), fmt(lote.getAcondFechaFinalIdeal()),
                fmt(lote.getMadurFechaFinalIdeal()), fmt(lote.getCarbFechaFinalIdeal()), lblFont, valFont);
        faseRow(t, "Fin real",
                fmt(lote.getFermFechaFinal()), fmt(lote.getAcondFechaFinal()),
                fmt(lote.getMadurFechaFinal()), fmt(lote.getCarbFechaFinal()), lblFont, valFont);
        faseRow(t, "Temperatura",
                temp(lote.getFermTemperatura()), temp(lote.getAcondTemperatura()),
                temp(lote.getMadurTemperatura()), temp(lote.getCarbTemperatura()), lblFont, valFont);

        doc.add(t);
    }

    private void faseRow(PdfPTable t, String label,
                          String f1, String f2, String f3, String f4,
                          Font lblFont, Font valFont) {
        PdfPCell lbl = new PdfPCell(new Phrase(label, lblFont));
        lbl.setBackgroundColor(C_FONDO);
        lbl.setBorderColor(C_BORDE);
        lbl.setPadding(5);
        t.addCell(lbl);
        for (String v : new String[]{f1, f2, f3, f4}) {
            PdfPCell c = new PdfPCell(new Phrase(v != null ? v : "—", valFont));
            c.setBorderColor(C_BORDE);
            c.setPadding(5);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            t.addCell(c);
        }
    }

    private void addCurvaFermentacion(Document doc, LoteCerveza lote,
                                       List<LecturaFermentacion> lecturas) throws Exception {
        addTituloPdf(doc, "CURVA DE FERMENTACIÓN");

        List<LecturaFermentacion> conD = lecturas.stream()
                .filter(l -> l.getDensidad() != null)
                .collect(Collectors.toList());
        List<LecturaFermentacion> conT = lecturas.stream()
                .filter(l -> l.getTemperatura() != null)
                .collect(Collectors.toList());
        boolean hayTemp2 = !conT.isEmpty();

        if (conD.size() >= 2) {
            final int sc = 2;
            final int imgW = 460 * sc, imgH = 120 * sc;
            final int mL = 45 * sc, mB = 22 * sc, mR = (hayTemp2 ? 40 : 8) * sc, mT = 10 * sc;
            final int pW = imgW - mL - mR;
            final int pH = imgH - mB - mT;
            final int plotTop    = mT;
            final int plotBottom = mT + pH;
            final int plotLeft   = mL;
            final int plotRight  = mL + pW;

            int minD = conD.stream().mapToInt(LecturaFermentacion::getDensidad).min().orElse(1010);
            int maxD = conD.stream().mapToInt(LecturaFermentacion::getDensidad).max().orElse(1060);
            if (lote.getDensidadInicial() != null) maxD = Math.max(maxD, lote.getDensidadInicial());
            if (lote.getDensidadFinal()   != null) minD = Math.min(minD, lote.getDensidadFinal());
            int padD = Math.max(3, (maxD - minD) / 8);
            minD = Math.max(990,  minD - padD);
            maxD = Math.min(1150, maxD + padD);
            final int dRange = maxD - minD;

            LocalDate firstDate = lecturas.get(0).getFecha();
            LocalDate lastDate  = lecturas.get(lecturas.size() - 1).getFecha();
            long totalDays = ChronoUnit.DAYS.between(firstDate, lastDate);
            if (totalDays == 0) totalDays = 1;

            double minT = 10, tRange = 20;
            if (hayTemp2) {
                double tMin2 = conT.stream().mapToDouble(l -> l.getTemperatura().doubleValue()).min().orElse(10);
                double tMax2 = conT.stream().mapToDouble(l -> l.getTemperatura().doubleValue()).max().orElse(30);
                double padT = Math.max(0.5, (tMax2 - tMin2) * 0.12);
                minT   = Math.max(0,  tMin2 - padT);
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
            g.setColor(C_FONDO);
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
                int fgJy = plotBottom - (int)((float)(lote.getDensidadFinal() - minD) / dRange * pH);
                if (fgJy >= plotTop && fgJy <= plotBottom) {
                    g.setColor(C_VERDE);
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

            int[] xs = new int[conD.size()];
            int[] ys = new int[conD.size()];
            for (int i = 0; i < conD.size(); i++) {
                long days = ChronoUnit.DAYS.between(firstDate, conD.get(i).getFecha());
                xs[i] = plotLeft + (int)((float) days / totalDays * pW);
                ys[i] = plotBottom - (int)((float)(conD.get(i).getDensidad() - minD) / dRange * pH);
            }

            g.setColor(C_DORADO);
            g.setStroke(new java.awt.BasicStroke(2 * sc, java.awt.BasicStroke.CAP_ROUND,
                        java.awt.BasicStroke.JOIN_ROUND));
            for (int i = 1; i < xs.length; i++) g.drawLine(xs[i-1], ys[i-1], xs[i], ys[i]);

            g.setStroke(new java.awt.BasicStroke(sc));
            int r = 3 * sc;
            for (int i = 0; i < xs.length; i++) g.fillOval(xs[i]-r, ys[i]-r, r*2, r*2);

            if (hayTemp2) {
                java.awt.Color tempColor = new java.awt.Color(2, 136, 209);
                g.setColor(tempColor);
                g.setStroke(new java.awt.BasicStroke(1.5f * sc,
                        java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                int prevTx = -1, prevTy = -1;
                for (LecturaFermentacion lec : conT) {
                    long days = ChronoUnit.DAYS.between(firstDate, lec.getFecha());
                    int tx = plotLeft + (int)((float) days / totalDays * pW);
                    int ty = plotBottom - (int)((lec.getTemperatura().doubleValue() - minT) / tRange * pH);
                    if (prevTx >= 0) g.drawLine(prevTx, prevTy, tx, ty);
                    prevTx = tx; prevTy = ty;
                }
                int rt = 2 * sc;
                g.setStroke(new java.awt.BasicStroke(sc));
                for (LecturaFermentacion lec : conT) {
                    long days = ChronoUnit.DAYS.between(firstDate, lec.getFecha());
                    int tx = plotLeft + (int)((float) days / totalDays * pW);
                    int ty = plotBottom - (int)((lec.getTemperatura().doubleValue() - minT) / tRange * pH);
                    g.fillOval(tx - rt, ty - rt, rt*2, rt*2);
                }
            }

            DateTimeFormatter fmtShort = DateTimeFormatter.ofPattern("dd/MM");
            g.setColor(C_GRIS);
            g.setFont(smallFont);
            g.drawString(firstDate.format(fmtShort), plotLeft, imgH - 2);
            g.drawString(lastDate.format(fmtShort),  plotRight - 16 * sc, imgH - 2);
            if (totalDays > 3) {
                g.drawString(firstDate.plusDays(totalDays / 2).format(fmtShort),
                             plotLeft + pW / 2 - 8 * sc, imgH - 2);
            }

            g.setColor(C_DORADO);
            g.fillOval(plotLeft, 2, r*2, r*2);
            g.setColor(C_GRIS);
            g.drawString("Densidad", plotLeft + r*2 + 3, 10);
            if (hayTemp2) {
                int lx2 = plotLeft + r*2 + 58 * sc;
                g.setColor(new java.awt.Color(2, 136, 209));
                g.fillOval(lx2, 2, r*2, r*2);
                g.setColor(C_GRIS);
                g.drawString("Temp. (°C)", lx2 + r*2 + 3, 10);
            }

            g.dispose();

            ByteArrayOutputStream chartBaos = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(bi, "PNG", chartBaos);
            Image chartImg = Image.getInstance(chartBaos.toByteArray());
            chartImg.scaleToFit(460, 120);
            chartImg.setSpacingBefore(4);
            doc.add(chartImg);
        }

        boolean hayTemp  = lecturas.stream().anyMatch(l -> l.getTemperatura() != null);
        boolean hayNotas = lecturas.stream().anyMatch(l -> notBlank(l.getNotas()));
        Font thF = new Font(Font.HELVETICA, 7, Font.BOLD, C_CREMA);
        Font tdF = new Font(Font.HELVETICA, 7, Font.NORMAL, Color.DARK_GRAY);

        String[] headers = buildHeaders(hayTemp, hayNotas);
        float[]  widths  = buildWidths(hayTemp, hayNotas);
        PdfPTable t = new PdfPTable(widths);
        t.setWidthPercentage(100);
        t.setSpacingBefore(6);

        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, thF));
            c.setBackgroundColor(C_VERDE_OSCURO);
            c.setBorder(0); c.setPadding(4);
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

    private void tableCell(PdfPTable t, String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text != null ? text : "—", font));
        c.setBorderColor(C_BORDE);
        c.setPadding(4);
        t.addCell(c);
    }

    private void addCostos(Document doc, LoteCerveza lote) throws DocumentException {
        Font lbl = new Font(Font.HELVETICA, 8, Font.BOLD, C_VERDE);
        Font val = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);

        PdfPTable resumen = new PdfPTable(new float[]{1.2f, 2, 1.2f, 2});
        resumen.setWidthPercentage(100);
        resumen.setSpacingAfter(6);

        par(resumen, "Costo total",     "$" + fmt2(lote.getCostoTotal()), lbl, val);
        par(resumen, "Costo por litro",
                lote.getCostoPorLitro() != null ? "$" + fmt2(lote.getCostoPorLitro()) : "—", lbl, val);
        par(resumen, "Ítems asignados", String.valueOf(lote.getItemsFactura().size()), lbl, val);
        par(resumen, "", "", lbl, val);

        doc.add(resumen);

        Font thF = new Font(Font.HELVETICA, 7, Font.BOLD, C_CREMA);
        Font tdF = new Font(Font.HELVETICA, 7, Font.NORMAL, Color.DARK_GRAY);

        PdfPTable dt = new PdfPTable(new float[]{1.5f, 1.5f, 2, 1, 1});
        dt.setWidthPercentage(100);

        for (String h : new String[]{"Factura", "Proveedor", "Ítem", "Cantidad", "Valor"}) {
            PdfPCell c = new PdfPCell(new Phrase(h, thF));
            c.setBackgroundColor(C_VERDE_OSCURO);
            c.setBorder(0);
            c.setPadding(4);
            dt.addCell(c);
        }

        for (LoteItemFactura li : lote.getItemsFactura()) {
            String nroFact = li.getItem().getFactura().getNumeroFactura();
            if (nroFact == null || nroFact.isBlank())
                nroFact = "#" + li.getItem().getFactura().getId();

            String cant = li.getCantidadAsignada().doubleValue() == 0.0
                    ? "Total" : li.getCantidadAsignada() + " " + (li.getItem().getUnidad() != null ? li.getItem().getUnidad() : "");

            for (String v : new String[]{
                    nroFact,
                    li.getItem().getFactura().getProveedor(),
                    li.getItem().getNombre(),
                    cant,
                    "$" + fmt2(li.getValorAsignado())
            }) {
                PdfPCell c = new PdfPCell(new Phrase(v != null ? v : "—", tdF));
                c.setBorderColor(C_BORDE);
                c.setPadding(4);
                dt.addCell(c);
            }
        }
        doc.add(dt);
    }

    private void addNotas(Document doc, LoteCerveza lote,
                           boolean hayObs, boolean hayCata) throws DocumentException {
        Font lbl = new Font(Font.HELVETICA, 8, Font.BOLD, C_VERDE);
        Font txt = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);

        if (hayObs) {
            Paragraph p = new Paragraph("Observaciones", lbl);
            p.setSpacingBefore(4);
            doc.add(p);
            doc.add(new Paragraph(lote.getObservaciones(), txt));
        }
        if (hayCata) {
            Paragraph p = new Paragraph("Notas de cata", lbl);
            p.setSpacingBefore(hayObs ? 8 : 4);
            doc.add(p);
            doc.add(new Paragraph(lote.getNotasCata(), txt));
        }
    }

    private void par(PdfPTable t, String label, String value, Font lbl, Font val) {
        PdfPCell cLbl = new PdfPCell(new Phrase(label, lbl));
        cLbl.setBackgroundColor(C_FONDO);
        cLbl.setBorderColor(C_BORDE);
        cLbl.setPadding(6);
        t.addCell(cLbl);

        PdfPCell cVal = new PdfPCell(new Phrase(value != null ? value : "—", val));
        cVal.setBorderColor(C_BORDE);
        cVal.setPadding(6);
        t.addCell(cVal);
    }

    private String fmt(LocalDate d)    { return d != null ? d.format(FMT_FECHA) : null; }
    private String temp(BigDecimal t)  { return t != null ? t + " °C" : null; }
    private boolean notBlank(String s) { return s != null && !s.isBlank(); }
    private String fmt2(BigDecimal n)  {
        return n != null ? String.format("%,.0f", n.doubleValue()) : "—";
    }
}
