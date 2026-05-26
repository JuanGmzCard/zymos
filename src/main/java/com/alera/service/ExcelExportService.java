package com.alera.service;

import com.alera.config.ExportBranding;
import com.alera.model.FacturaProveedor;
import com.alera.model.InsumoInventario;
import com.alera.model.LoteCerveza;
import com.alera.model.enums.EstadoFactura;
import com.alera.model.enums.TipoInsumo;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExcelExportService {

    // Colores neutros fijos (no son parte del branding del tenant)
    private static final Color C_BORDE = new Color(222, 226, 230);

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Paleta calculada por request a partir del branding del tenant. */
    private record Pal(Color verde, Color verdeOscuro, Color dorado, Color crema, Color fondo) {
        static Pal of(ExportBranding b) {
            return new Pal(b.primary(), b.primaryDark(), b.accent(), b.cream(), b.background());
        }
    }

    // ── Excel Inventario ─────────────────────────────────────────────

    public byte[] generarExcelInventario(List<InsumoInventario> insumos, ExportBranding branding) {
        Pal pal = Pal.of(branding);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            construirSheetInventario(wb, insumos, branding.name(), pal);
            construirSheetInventarioTipos(wb, insumos, branding.name(), pal);
            wb.write(baos);
        } catch (Exception e) {
            throw new RuntimeException("Error generando Excel de inventario", e);
        }
        return baos.toByteArray();
    }

    private void construirSheetInventario(XSSFWorkbook wb, List<InsumoInventario> insumos,
                                           String brandName, Pal pal) {
        Sheet sheet = wb.createSheet("Inventario");
        sheet.setDefaultColumnWidth(15);

        XSSFCellStyle stTitulo  = estiloTitulo(wb, pal);
        XSSFCellStyle stResumen = estiloResumen(wb, pal);
        XSSFCellStyle stHeader  = estiloHeader(wb, pal);
        XSSFCellStyle stDato    = estiloDato(wb, pal, false);
        XSSFCellStyle stDatoAlt = estiloDato(wb, pal, true);
        XSSFCellStyle stNum     = estiloNumero(wb, pal, false);
        XSSFCellStyle stNumAlt  = estiloNumero(wb, pal, true);

        int r = 0;
        Row fT = sheet.createRow(r++);
        fT.setHeight((short) (20 * 20));
        Cell cT = fT.createCell(0);
        cT.setCellValue(brandName + " — Inventario de Insumos");
        cT.setCellStyle(stTitulo);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));

        r++;
        long totalBajoStock = insumos.stream().filter(InsumoInventario::isBajoStock).count();
        Row fRes = sheet.createRow(r++);
        celda(fRes, 0, "Total: " + insumos.size() + " insumos", stResumen);
        celda(fRes, 3, "Bajo stock: " + totalBajoStock, stResumen);

        r++;
        Row fHead = sheet.createRow(r++);
        fHead.setHeight((short) (16 * 20));
        String[] headers = {"Nombre", "Tipo", "Cantidad", "Unidad", "Stock Mínimo", "Estado", "Vencimiento", "Proveedor"};
        for (int i = 0; i < headers.length; i++) celda(fHead, i, headers[i], stHeader);

        int[] widths = {28, 14, 12, 10, 13, 10, 14, 20};
        for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);

        for (int i = 0; i < insumos.size(); i++) {
            InsumoInventario ins = insumos.get(i);
            boolean alt = i % 2 != 0;
            Row fila = sheet.createRow(r++);
            XSSFCellStyle sD = alt ? stDatoAlt : stDato;
            XSSFCellStyle sN = alt ? stNumAlt  : stNum;

            celda(fila, 0, ins.getNombre(), sD);
            celda(fila, 1, ins.getTipo() != null ? ins.getTipo().getDisplayName() : "", sD);
            celdaNum(fila, 2, ins.getCantidad() != null ? ins.getCantidad().doubleValue() : null, sN);
            celda(fila, 3, ins.getUnidad() != null ? ins.getUnidad() : "", sD);
            celdaNum(fila, 4, ins.getStockMinimo() != null ? ins.getStockMinimo().doubleValue() : null, sN);
            celda(fila, 5, ins.isBajoStock() ? "Bajo stock" : "OK", sD);
            celda(fila, 6, ins.getFechaVencimiento() != null
                    ? ins.getFechaVencimiento().format(FMT_FECHA) : "", sD);
            celda(fila, 7, ins.getProveedor() != null ? ins.getProveedor() : "", sD);
        }

        if (!insumos.isEmpty()) {
            sheet.setAutoFilter(new CellRangeAddress(4, r - 1, 0, headers.length - 1));
        }
    }

    private void construirSheetInventarioTipos(XSSFWorkbook wb, List<InsumoInventario> insumos,
                                                String brandName, Pal pal) {
        Sheet sheet = wb.createSheet("Por Tipo");
        sheet.setDefaultColumnWidth(18);

        XSSFCellStyle stTitulo  = estiloTitulo(wb, pal);
        XSSFCellStyle stHeader  = estiloHeader(wb, pal);
        XSSFCellStyle stDato    = estiloDato(wb, pal, false);
        XSSFCellStyle stDatoAlt = estiloDato(wb, pal, true);
        XSSFCellStyle stNum     = estiloNumero(wb, pal, false);
        XSSFCellStyle stNumAlt  = estiloNumero(wb, pal, true);

        int r = 0;
        Row fT = sheet.createRow(r++);
        fT.setHeight((short) (20 * 20));
        Cell cT = fT.createCell(0);
        cT.setCellValue(brandName + " — Inventario por Tipo");
        cT.setCellStyle(stTitulo);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

        r++;
        Row fHead = sheet.createRow(r++);
        fHead.setHeight((short) (16 * 20));
        String[] headers = {"Tipo", "Cantidad de items", "Items bajo stock", "% bajo stock"};
        for (int i = 0; i < headers.length; i++) celda(fHead, i, headers[i], stHeader);

        java.util.EnumMap<TipoInsumo, long[]> grouped = new java.util.EnumMap<>(TipoInsumo.class);
        for (TipoInsumo t : TipoInsumo.values()) grouped.put(t, new long[]{0, 0});
        for (InsumoInventario ins : insumos) {
            if (ins.getTipo() != null) {
                grouped.get(ins.getTipo())[0]++;
                if (ins.isBajoStock()) grouped.get(ins.getTipo())[1]++;
            }
        }

        int idx = 0;
        for (Map.Entry<TipoInsumo, long[]> entry : grouped.entrySet()) {
            if (entry.getValue()[0] == 0) continue;
            boolean alt = idx % 2 != 0;
            Row fila = sheet.createRow(r++);
            long total = entry.getValue()[0], bajo = entry.getValue()[1];
            celda(fila, 0, entry.getKey().getDisplayName(), alt ? stDatoAlt : stDato);
            celdaNum(fila, 1, (double) total, alt ? stNumAlt : stNum);
            celdaNum(fila, 2, (double) bajo,  alt ? stNumAlt : stNum);
            celdaNum(fila, 3, total > 0 ? (bajo * 100.0 / total) : 0.0, alt ? stNumAlt : stNum);
            idx++;
        }
    }

    // ── Excel Facturas ───────────────────────────────────────────────

    public byte[] generarExcelFacturas(List<FacturaProveedor> facturas,
                                        EstadoFactura estadoFiltro,
                                        LocalDate desde, LocalDate hasta,
                                        ExportBranding branding) {
        Pal pal = Pal.of(branding);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            construirSheetFacturas(wb, facturas, estadoFiltro, desde, hasta, branding.name(), pal);
            construirSheetProveedores(wb, facturas, branding.name(), pal);
            wb.write(baos);
        } catch (Exception e) {
            throw new RuntimeException("Error generando Excel de facturas", e);
        }
        return baos.toByteArray();
    }

    private void construirSheetFacturas(XSSFWorkbook wb, List<FacturaProveedor> facturas,
                                         EstadoFactura estadoFiltro,
                                         LocalDate desde, LocalDate hasta,
                                         String brandName, Pal pal) {
        Sheet sheet = wb.createSheet("Facturas");
        sheet.setDefaultColumnWidth(15);

        XSSFCellStyle stTitulo  = estiloTitulo(wb, pal);
        XSSFCellStyle stPeriodo = estiloPeriodo(wb, pal);
        XSSFCellStyle stResumen = estiloResumen(wb, pal);
        XSSFCellStyle stHeader  = estiloHeader(wb, pal);
        XSSFCellStyle stDato    = estiloDato(wb, pal, false);
        XSSFCellStyle stDatoAlt = estiloDato(wb, pal, true);
        XSSFCellStyle stNum     = estiloNumero(wb, pal, false);
        XSSFCellStyle stNumAlt  = estiloNumero(wb, pal, true);

        int r = 0;

        Row fTitulo = sheet.createRow(r++);
        fTitulo.setHeight((short) (20 * 20));
        Cell cTitulo = fTitulo.createCell(0);
        cTitulo.setCellValue(brandName + " — Facturas de Proveedores");
        cTitulo.setCellStyle(stTitulo);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 10));

        Row fFiltro = sheet.createRow(r++);
        Cell cFiltro = fFiltro.createCell(0);
        String desdeStr  = desde != null ? desde.format(FMT_FECHA) : "—";
        String hastaStr  = hasta != null ? hasta.format(FMT_FECHA) : "—";
        String estadoStr = estadoFiltro != null ? estadoFiltro.getDisplayName() : "Todas";
        cFiltro.setCellValue("Estado: " + estadoStr + "   |   Período: " + desdeStr + " — " + hastaStr);
        cFiltro.setCellStyle(stPeriodo);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 10));

        r++;
        Row fRes = sheet.createRow(r++);
        BigDecimal totalSubtotal = facturas.stream()
                .map(f -> f.getSubtotal() != null ? f.getSubtotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalIva = facturas.stream()
                .map(f -> f.getValorIva() != null ? f.getValorIva() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalGeneral = facturas.stream()
                .map(f -> f.getValorTotal() != null ? f.getValorTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        celda(fRes, 0, "Facturas: " + facturas.size(), stResumen);
        celda(fRes, 2, "Subtotal: $" + totalSubtotal.toPlainString(), stResumen);
        celda(fRes, 5, "IVA: $" + totalIva.toPlainString(), stResumen);
        celda(fRes, 7, "Total: $" + totalGeneral.toPlainString(), stResumen);

        r++;
        Row fHead = sheet.createRow(r++);
        fHead.setHeight((short) (16 * 20));
        String[] headers = {
                "N° Factura", "Proveedor", "Fecha", "Estado",
                "Ítems", "Subtotal", "IVA", "Envío", "Total", "Descripción", "Creado por"
        };
        for (int i = 0; i < headers.length; i++) celda(fHead, i, headers[i], stHeader);

        int[] widths = {14, 22, 12, 12, 7, 14, 14, 14, 14, 24, 14};
        for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);

        for (int i = 0; i < facturas.size(); i++) {
            FacturaProveedor f = facturas.get(i);
            boolean alt = i % 2 != 0;
            Row fila = sheet.createRow(r++);
            XSSFCellStyle sD = alt ? stDatoAlt : stDato;
            XSSFCellStyle sN = alt ? stNumAlt  : stNum;

            celda(fila, 0, f.getNumeroFactura() != null ? f.getNumeroFactura() : "#" + f.getId(), sD);
            celda(fila, 1, f.getProveedor() != null ? f.getProveedor() : "", sD);
            celda(fila, 2, f.getFechaFactura() != null ? f.getFechaFactura().format(FMT_FECHA) : "", sD);
            celda(fila, 3, f.getEstado() != null ? f.getEstado().getDisplayName() : "", sD);
            celdaNum(fila, 4, (double) f.getItems().size(), sN);
            celdaNum(fila, 5, toDouble(f.getSubtotal()),   sN);
            celdaNum(fila, 6, toDouble(f.getValorIva()),   sN);
            celdaNum(fila, 7, toDouble(f.getCostoEnvio()), sN);
            celdaNum(fila, 8, toDouble(f.getValorTotal()), sN);
            celda(fila, 9,  f.getDescripcion() != null ? f.getDescripcion() : "", sD);
            celda(fila, 10, f.getCreatedBy()  != null ? f.getCreatedBy()  : "", sD);
        }

        if (!facturas.isEmpty()) {
            sheet.setAutoFilter(new CellRangeAddress(5, r - 1, 0, headers.length - 1));
        }
    }

    private void construirSheetProveedores(XSSFWorkbook wb, List<FacturaProveedor> facturas,
                                            String brandName, Pal pal) {
        Sheet sheet = wb.createSheet("Por Proveedor");
        sheet.setDefaultColumnWidth(20);

        XSSFCellStyle stTitulo  = estiloTitulo(wb, pal);
        XSSFCellStyle stHeader  = estiloHeader(wb, pal);
        XSSFCellStyle stDato    = estiloDato(wb, pal, false);
        XSSFCellStyle stDatoAlt = estiloDato(wb, pal, true);
        XSSFCellStyle stNum     = estiloNumero(wb, pal, false);
        XSSFCellStyle stNumAlt  = estiloNumero(wb, pal, true);

        int r = 0;
        Row fT = sheet.createRow(r++);
        fT.setHeight((short) (20 * 20));
        Cell cT = fT.createCell(0);
        cT.setCellValue(brandName + " — Resumen por Proveedor");
        cT.setCellStyle(stTitulo);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));

        r++;
        Row fH = sheet.createRow(r++);
        fH.setHeight((short) (16 * 20));
        celda(fH, 0, "Proveedor", stHeader);
        celda(fH, 1, "Facturas",  stHeader);
        celda(fH, 2, "Total ($)", stHeader);

        Map<String, long[]> resumen = new LinkedHashMap<>();
        for (FacturaProveedor f : facturas) {
            String prov = f.getProveedor() != null ? f.getProveedor() : "Sin proveedor";
            resumen.computeIfAbsent(prov, k -> new long[]{0, 0});
            resumen.get(prov)[0]++;
            if (f.getValorTotal() != null) resumen.get(prov)[1] += f.getValorTotal().longValue();
        }

        int i = 0;
        for (Map.Entry<String, long[]> e : resumen.entrySet()) {
            boolean alt = i++ % 2 != 0;
            Row fila = sheet.createRow(r++);
            celda(fila, 0, e.getKey(),              alt ? stDatoAlt : stDato);
            celdaNum(fila, 1, (double) e.getValue()[0], alt ? stNumAlt : stNum);
            celdaNum(fila, 2, (double) e.getValue()[1], alt ? stNumAlt : stNum);
        }

        sheet.setColumnWidth(0, 30 * 256);
        sheet.setColumnWidth(1, 12 * 256);
        sheet.setColumnWidth(2, 18 * 256);
    }

    // ── Excel Reporte Producción ──────────────────────────────────────

    public byte[] generarExcelReporteProduccion(List<LoteCerveza> lotes,
                                                  List<Object[]> resumen,
                                                  LocalDate desde, LocalDate hasta,
                                                  ExportBranding branding) {
        Pal pal = Pal.of(branding);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            construirSheetLotes(wb, lotes, desde, hasta, branding.name(), pal);
            construirSheetEstilos(wb, resumen, branding.name(), pal);
            wb.write(baos);
        } catch (Exception e) {
            throw new RuntimeException("Error generando Excel del reporte", e);
        }
        return baos.toByteArray();
    }

    private void construirSheetLotes(XSSFWorkbook wb, List<LoteCerveza> lotes,
                                      LocalDate desde, LocalDate hasta,
                                      String brandName, Pal pal) {
        Sheet sheet = wb.createSheet("Reporte de Producción");
        sheet.setDefaultColumnWidth(15);

        XSSFCellStyle stTitulo  = estiloTitulo(wb, pal);
        XSSFCellStyle stHeader  = estiloHeader(wb, pal);
        XSSFCellStyle stDato    = estiloDato(wb, pal, false);
        XSSFCellStyle stDatoAlt = estiloDato(wb, pal, true);
        XSSFCellStyle stNum     = estiloNumero(wb, pal, false);
        XSSFCellStyle stNumAlt  = estiloNumero(wb, pal, true);

        int r = 0;

        Row fTitulo = sheet.createRow(r++);
        fTitulo.setHeight((short) (20 * 20));
        Cell cTitulo = fTitulo.createCell(0);
        cTitulo.setCellValue(brandName + " — Reporte de Producción");
        cTitulo.setCellStyle(stTitulo);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 13));

        Row fPeriodo = sheet.createRow(r++);
        Cell cPeriodo = fPeriodo.createCell(0);
        String desdeStr = desde != null ? desde.format(FMT_FECHA) : "—";
        String hastaStr = hasta != null ? hasta.format(FMT_FECHA) : "—";
        cPeriodo.setCellValue("Período: " + desdeStr + " — " + hastaStr);
        cPeriodo.setCellStyle(estiloPeriodo(wb, pal));
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 13));

        r++;
        Row fRes = sheet.createRow(r++);
        long completados = lotes.stream().filter(LoteCerveza::isCompletado).count();
        BigDecimal totalL = lotes.stream()
                .filter(l -> l.getLitrosFinales() != null)
                .map(LoteCerveza::getLitrosFinales)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long estilos = lotes.stream().map(LoteCerveza::getEstilo).distinct().count();

        XSSFCellStyle stResumen = estiloResumen(wb, pal);
        celda(fRes, 0, "Total lotes: " + lotes.size(),        stResumen);
        celda(fRes, 2, "Litros producidos: " + totalL + " L", stResumen);
        celda(fRes, 5, "Estilos distintos: " + estilos,       stResumen);
        celda(fRes, 8, "Completados: " + completados,         stResumen);

        r++;
        Row fHead = sheet.createRow(r++);
        fHead.setHeight((short) (16 * 20));
        String[] headers = {
                "Código", "Estilo", "Receta", "Fecha", "Fase",
                "OG", "FG", "ABV (%)", "Atenuación (%)", "Eficiencia (%)",
                "Litros", "Costo Total", "Costo/Litro", "Creado por"
        };
        for (int i = 0; i < headers.length; i++) celda(fHead, i, headers[i], stHeader);

        int[] widths = {14, 16, 20, 12, 14, 8, 8, 9, 13, 13, 9, 14, 13, 14};
        for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);

        for (int i = 0; i < lotes.size(); i++) {
            LoteCerveza l = lotes.get(i);
            boolean alt = i % 2 != 0;
            Row fila = sheet.createRow(r++);
            XSSFCellStyle sD = alt ? stDatoAlt : stDato;
            XSSFCellStyle sN = alt ? stNumAlt  : stNum;

            celda(fila, 0,  l.getCodigoLote(), sD);
            celda(fila, 1,  l.getEstilo(), sD);
            celda(fila, 2,  l.getReceta() != null ? l.getReceta().getNombre() : "", sD);
            celda(fila, 3,  l.getFechaElaboracion() != null
                    ? l.getFechaElaboracion().format(FMT_FECHA) : "", sD);
            celda(fila, 4,  l.isCompletado() ? "Completado" : l.getFaseActual(), sD);
            celdaNum(fila, 5,  l.getDensidadInicial() != null ? l.getDensidadInicial().doubleValue() : null, sN);
            celdaNum(fila, 6,  l.getDensidadFinal()   != null ? l.getDensidadFinal().doubleValue()   : null, sN);
            celdaNum(fila, 7,  toDouble(l.getAbv()),                  sN);
            celdaNum(fila, 8,  toDouble(l.getAtenuacionAparente()),   sN);
            celdaNum(fila, 9,  toDouble(l.getEficienciaMacerado()),   sN);
            celdaNum(fila, 10, toDouble(l.getLitrosFinales()),        sN);
            celdaNum(fila, 11, toDouble(l.getCostoTotal()),           sN);
            celdaNum(fila, 12, toDouble(l.getCostoPorLitro()),        sN);
            celda(fila, 13, l.getCreatedBy() != null ? l.getCreatedBy() : "", sD);
        }

        if (!lotes.isEmpty()) {
            sheet.setAutoFilter(new CellRangeAddress(5, r - 1, 0, headers.length - 1));
        }
    }

    private void construirSheetEstilos(XSSFWorkbook wb, List<Object[]> resumen,
                                        String brandName, Pal pal) {
        Sheet sheet = wb.createSheet("Por Estilo");
        sheet.setDefaultColumnWidth(18);

        XSSFCellStyle stTitulo  = estiloTitulo(wb, pal);
        XSSFCellStyle stHeader  = estiloHeader(wb, pal);
        XSSFCellStyle stDato    = estiloDato(wb, pal, false);
        XSSFCellStyle stDatoAlt = estiloDato(wb, pal, true);
        XSSFCellStyle stNum     = estiloNumero(wb, pal, false);
        XSSFCellStyle stNumAlt  = estiloNumero(wb, pal, true);

        int r = 0;
        Row fT = sheet.createRow(r++);
        fT.setHeight((short) (20 * 20));
        Cell cT = fT.createCell(0);
        cT.setCellValue(brandName + " — Producción por Estilo");
        cT.setCellStyle(stTitulo);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));

        r++;
        Row fH = sheet.createRow(r++);
        fH.setHeight((short) (16 * 20));
        String[] colHeaders = {"Estilo", "Cantidad de lotes", "Litros totales"};
        for (int i = 0; i < colHeaders.length; i++) celda(fH, i, colHeaders[i], stHeader);

        for (int i = 0; i < resumen.size(); i++) {
            Object[] row = resumen.get(i);
            boolean alt = i % 2 != 0;
            Row fila = sheet.createRow(r++);
            celda(fila, 0, String.valueOf(row[0]),            alt ? stDatoAlt : stDato);
            celdaNum(fila, 1, ((Number) row[1]).doubleValue(), alt ? stNumAlt  : stNum);
            celdaNum(fila, 2, ((Number) row[2]).doubleValue(), alt ? stNumAlt  : stNum);
        }
    }

    // ── Estilos de celda ─────────────────────────────────────────────

    private XSSFCellStyle estiloTitulo(XSSFWorkbook wb, Pal pal) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(pal.verde(), null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.LEFT);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.MEDIUM);
        s.setBottomBorderColor(new XSSFColor(pal.dorado(), null));
        XSSFFont f = (XSSFFont) wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 13);
        f.setColor(new XSSFColor(pal.crema(), null));
        s.setFont(f);
        return s;
    }

    private XSSFCellStyle estiloHeader(XSSFWorkbook wb, Pal pal) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(pal.verdeOscuro(), null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setWrapText(true);
        XSSFFont f = (XSSFFont) wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 9);
        f.setColor(new XSSFColor(pal.crema(), null));
        s.setFont(f);
        return s;
    }

    private XSSFCellStyle estiloPeriodo(XSSFWorkbook wb, Pal pal) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(pal.dorado(), null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.LEFT);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        XSSFFont f = (XSSFFont) wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 10);
        f.setColor(new XSSFColor(pal.verdeOscuro(), null));
        s.setFont(f);
        return s;
    }

    private XSSFCellStyle estiloResumen(XSSFWorkbook wb, Pal pal) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(pal.fondo(), null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.LEFT);
        XSSFFont f = (XSSFFont) wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 9);
        f.setColor(new XSSFColor(pal.verde(), null));
        s.setFont(f);
        return s;
    }

    private XSSFCellStyle estiloDato(XSSFWorkbook wb, Pal pal, boolean alt) {
        XSSFCellStyle s = wb.createCellStyle();
        if (alt) {
            s.setFillForegroundColor(new XSSFColor(pal.fondo(), null));
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBottomBorderColor(new XSSFColor(C_BORDE, null));
        XSSFFont f = (XSSFFont) wb.createFont();
        f.setFontHeightInPoints((short) 9);
        s.setFont(f);
        return s;
    }

    private XSSFCellStyle estiloNumero(XSSFWorkbook wb, Pal pal, boolean alt) {
        XSSFCellStyle s = estiloDato(wb, pal, alt);
        s.setAlignment(HorizontalAlignment.RIGHT);
        s.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        return s;
    }

    // ── Helpers ──────────────────────────────────────────────────────

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
