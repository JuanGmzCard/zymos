package com.alera.service;

import com.alera.model.LoteCerveza;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExcelExportService {

    private static final Color C_VERDE        = new Color(54, 67, 24);
    private static final Color C_VERDE_OSCURO = new Color(36, 46, 13);
    private static final Color C_DORADO       = new Color(201, 160, 40);
    private static final Color C_CREMA        = new Color(245, 237, 208);
    private static final Color C_FONDO        = new Color(240, 237, 226);
    private static final Color C_BORDE        = new Color(222, 226, 230);

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] generarExcelReporteProduccion(List<LoteCerveza> lotes,
                                                 List<Object[]> resumen,
                                                 LocalDate desde, LocalDate hasta,
                                                 String brandName) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            construirSheetLotes(wb, lotes, desde, hasta, brandName);
            construirSheetEstilos(wb, resumen, brandName);
            wb.write(baos);
        } catch (Exception e) {
            throw new RuntimeException("Error generando Excel del reporte", e);
        }
        return baos.toByteArray();
    }

    private void construirSheetLotes(XSSFWorkbook wb, List<LoteCerveza> lotes,
                                      LocalDate desde, LocalDate hasta, String brandName) {
        Sheet sheet = wb.createSheet("Reporte de Producción");
        sheet.setDefaultColumnWidth(15);

        XSSFCellStyle stTitulo   = estiloTitulo(wb);
        XSSFCellStyle stHeader   = estiloHeader(wb);
        XSSFCellStyle stDato     = estiloDato(wb, false);
        XSSFCellStyle stDatoAlt  = estiloDato(wb, true);
        XSSFCellStyle stNum      = estiloNumero(wb, false);
        XSSFCellStyle stNumAlt   = estiloNumero(wb, true);

        int r = 0;

        Row fTitulo = sheet.createRow(r++);
        fTitulo.setHeight((short)(20 * 20));
        Cell cTitulo = fTitulo.createCell(0);
        cTitulo.setCellValue(brandName + " — Reporte de Producción");
        cTitulo.setCellStyle(stTitulo);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 13));

        Row fPeriodo = sheet.createRow(r++);
        Cell cPeriodo = fPeriodo.createCell(0);
        cPeriodo.setCellValue("Período: " + desde.format(FMT_FECHA) + " — " + hasta.format(FMT_FECHA));
        cPeriodo.setCellStyle(estiloPeriodo(wb));
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 13));

        r++;
        Row fRes = sheet.createRow(r++);
        long completados = lotes.stream().filter(LoteCerveza::isCompletado).count();
        BigDecimal totalL = lotes.stream()
                .filter(l -> l.getLitrosFinales() != null)
                .map(LoteCerveza::getLitrosFinales)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long estilos = lotes.stream().map(LoteCerveza::getEstilo).distinct().count();

        XSSFCellStyle stResumen = estiloResumen(wb);
        celda(fRes, 0, "Total lotes: " + lotes.size(), stResumen);
        celda(fRes, 2, "Litros producidos: " + totalL + " L", stResumen);
        celda(fRes, 5, "Estilos distintos: " + estilos, stResumen);
        celda(fRes, 8, "Completados: " + completados, stResumen);

        r++;
        Row fHead = sheet.createRow(r++);
        fHead.setHeight((short)(16 * 20));
        String[] headers = {
                "Código", "Estilo", "Receta", "Fecha", "Fase",
                "OG", "FG", "ABV (%)", "Atenuación (%)", "Eficiencia (%)",
                "Litros", "Costo Total", "Costo/Litro", "Creado por"
        };
        for (int i = 0; i < headers.length; i++) {
            celda(fHead, i, headers[i], stHeader);
        }

        int[] widths = {14, 16, 20, 12, 14, 8, 8, 9, 13, 13, 9, 14, 13, 14};
        for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);

        for (int i = 0; i < lotes.size(); i++) {
            LoteCerveza l = lotes.get(i);
            boolean alt = i % 2 != 0;
            Row fila = sheet.createRow(r++);
            XSSFCellStyle sD = alt ? stDatoAlt : stDato;
            XSSFCellStyle sN = alt ? stNumAlt  : stNum;

            celda(fila, 0,  l.getCodigoLote(),                                           sD);
            celda(fila, 1,  l.getEstilo(),                                                sD);
            celda(fila, 2,  l.getReceta() != null ? l.getReceta().getNombre() : "",       sD);
            celda(fila, 3,  l.getFechaElaboracion() != null
                    ? l.getFechaElaboracion().format(FMT_FECHA) : "",                     sD);
            celda(fila, 4,  l.isCompletado() ? "Completado" : l.getFaseActual(),          sD);
            celdaNum(fila, 5,  l.getDensidadInicial() != null
                    ? l.getDensidadInicial().doubleValue() : null,                        sN);
            celdaNum(fila, 6,  l.getDensidadFinal() != null
                    ? l.getDensidadFinal().doubleValue() : null,                          sN);
            celdaNum(fila, 7,  toDouble(l.getAbv()),                                      sN);
            celdaNum(fila, 8,  toDouble(l.getAtenuacionAparente()),                       sN);
            celdaNum(fila, 9,  toDouble(l.getEficienciaMacerado()),                       sN);
            celdaNum(fila, 10, toDouble(l.getLitrosFinales()),                            sN);
            celdaNum(fila, 11, toDouble(l.getCostoTotal()),                               sN);
            celdaNum(fila, 12, toDouble(l.getCostoPorLitro()),                            sN);
            celda(fila, 13, l.getCreatedBy() != null ? l.getCreatedBy() : "",             sD);
        }

        if (!lotes.isEmpty()) {
            sheet.setAutoFilter(new CellRangeAddress(5, r - 1, 0, headers.length - 1));
        }
    }

    private void construirSheetEstilos(XSSFWorkbook wb, List<Object[]> resumen, String brandName) {
        Sheet sheet = wb.createSheet("Por Estilo");
        sheet.setDefaultColumnWidth(18);

        XSSFCellStyle stTitulo  = estiloTitulo(wb);
        XSSFCellStyle stHeader  = estiloHeader(wb);
        XSSFCellStyle stDato    = estiloDato(wb, false);
        XSSFCellStyle stDatoAlt = estiloDato(wb, true);
        XSSFCellStyle stNum     = estiloNumero(wb, false);
        XSSFCellStyle stNumAlt  = estiloNumero(wb, true);

        int r = 0;
        Row fT = sheet.createRow(r++);
        fT.setHeight((short)(20 * 20));
        Cell cT = fT.createCell(0);
        cT.setCellValue(brandName + " — Producción por Estilo");
        cT.setCellStyle(stTitulo);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));

        r++;
        Row fH = sheet.createRow(r++);
        fH.setHeight((short)(16 * 20));
        String[] colHeaders = {"Estilo", "Cantidad de lotes", "Litros totales"};
        for (int i = 0; i < colHeaders.length; i++) {
            celda(fH, i, colHeaders[i], stHeader);
        }

        for (int i = 0; i < resumen.size(); i++) {
            Object[] row = resumen.get(i);
            boolean alt = i % 2 != 0;
            Row fila = sheet.createRow(r++);
            celda(fila, 0, String.valueOf(row[0]),            alt ? stDatoAlt : stDato);
            celdaNum(fila, 1, ((Number)row[1]).doubleValue(), alt ? stNumAlt  : stNum);
            celdaNum(fila, 2, ((Number)row[2]).doubleValue(), alt ? stNumAlt  : stNum);
        }
    }

    private XSSFCellStyle estiloTitulo(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(new java.awt.Color(54, 67, 24), null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.LEFT);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.MEDIUM);
        s.setBottomBorderColor(new XSSFColor(C_DORADO, null));
        XSSFFont f = (XSSFFont) wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short)13);
        f.setColor(new XSSFColor(new java.awt.Color(245, 237, 208), null));
        s.setFont(f);
        return s;
    }

    private XSSFCellStyle estiloHeader(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(new java.awt.Color(36, 46, 13), null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setWrapText(true);
        XSSFFont f = (XSSFFont) wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short)9);
        f.setColor(new XSSFColor(new java.awt.Color(245, 237, 208), null));
        s.setFont(f);
        return s;
    }

    private XSSFCellStyle estiloPeriodo(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(C_DORADO, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.LEFT);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        XSSFFont f = (XSSFFont) wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short)10);
        f.setColor(new XSSFColor(new java.awt.Color(36, 46, 13), null));
        s.setFont(f);
        return s;
    }

    private XSSFCellStyle estiloResumen(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(C_FONDO, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.LEFT);
        XSSFFont f = (XSSFFont) wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short)9);
        f.setColor(new XSSFColor(C_VERDE, null));
        s.setFont(f);
        return s;
    }

    private XSSFCellStyle estiloDato(XSSFWorkbook wb, boolean alt) {
        XSSFCellStyle s = wb.createCellStyle();
        if (alt) {
            s.setFillForegroundColor(new XSSFColor(C_FONDO, null));
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBottomBorderColor(new XSSFColor(C_BORDE, null));
        XSSFFont f = (XSSFFont) wb.createFont();
        f.setFontHeightInPoints((short)9);
        s.setFont(f);
        return s;
    }

    private XSSFCellStyle estiloNumero(XSSFWorkbook wb, boolean alt) {
        XSSFCellStyle s = estiloDato(wb, alt);
        s.setAlignment(HorizontalAlignment.RIGHT);
        s.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        return s;
    }

    private void celda(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value != null ? value : "");
        c.setCellStyle(style);
    }

    private void celdaNum(Row row, int col, Double value, CellStyle style) {
        Cell c = row.createCell(col);
        if (value != null) c.setCellValue(value);
        c.setCellStyle(style);
    }

    private Double toDouble(BigDecimal bd) {
        return bd != null ? bd.doubleValue() : null;
    }
}
