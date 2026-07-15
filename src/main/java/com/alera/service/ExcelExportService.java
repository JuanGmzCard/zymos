package com.alera.service;

import com.alera.config.ExportBranding;
import com.alera.config.UnidadUtils;
import com.alera.dto.RentabilidadLoteDto;
import com.alera.model.FacturaItem;
import com.alera.model.FacturaProveedor;
import com.alera.model.Ingrediente;
import com.alera.model.InsumoInventario;
import com.alera.model.LoteCerveza;
import com.alera.model.Venta;
import com.alera.model.VentaItem;
import com.alera.model.enums.EstadoFactura;
import com.alera.model.enums.EstadoVenta;
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
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class ExcelExportService {

    private final MessageSource messageSource;

    public ExcelExportService(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    private static final Color C_BORDE = new Color(222, 226, 230);
    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
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

    /** Paleta calculada por request a partir del branding del tenant. */
    private record Pal(Color verde, Color verdeOscuro, Color dorado, Color crema, Color fondo) {
        static Pal of(ExportBranding b) {
            return new Pal(b.primary(), b.primaryDark(), b.accent(), b.cream(), b.background());
        }
    }

    // ── Excel Inventario ─────────────────────────────────────────────

    public byte[] generarExcelInventario(List<InsumoInventario> insumos, ExportBranding branding, Locale locale) {
        LOCALE_HOLDER.set(locale);
        try {
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
        } finally {
            LOCALE_HOLDER.remove();
        }
    }

    private void construirSheetInventario(XSSFWorkbook wb, List<InsumoInventario> insumos,
                                           String brandName, Pal pal) {
        Sheet sheet = wb.createSheet(t("xls.sheet.inventario"));
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
        cT.setCellValue(tf("xls.titulo.inventario", brandName));
        cT.setCellStyle(stTitulo);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));

        r++;
        long totalBajoStock = insumos.stream().filter(InsumoInventario::isBajoStock).count();
        Row fRes = sheet.createRow(r++);
        celda(fRes, 0, tf("xls.resumen.total_insumos", insumos.size()), stResumen);
        celda(fRes, 3, tf("xls.resumen.bajo_stock", totalBajoStock), stResumen);

        r++;
        Row fHead = sheet.createRow(r++);
        fHead.setHeight((short) (16 * 20));
        String[] headers = {
            t("xls.header.nombre"), t("xls.header.tipo"), t("xls.header.cantidad"),
            t("xls.header.unidad"), t("xls.header.stock_minimo"), t("xls.header.estado"),
            t("xls.header.vencimiento"), t("xls.header.proveedor")
        };
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
            celda(fila, 1, ins.getTipo() != null ? ins.getTipo() : "", sD);
            celdaNum(fila, 2, ins.getCantidadDisplay() != null ? ins.getCantidadDisplay().doubleValue() : null, sN);
            celda(fila, 3, ins.getUnidadDisplay() != null ? ins.getUnidadDisplay() : "", sD);
            celdaNum(fila, 4, ins.getStockMinimoDisplay() != null ? ins.getStockMinimoDisplay().doubleValue() : null, sN);
            celda(fila, 5, ins.isBajoStock() ? t("xls.text.bajo_stock") : t("xls.text.ok"), sD);
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
        Sheet sheet = wb.createSheet(t("xls.sheet.por_tipo"));
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
        cT.setCellValue(tf("xls.titulo.por_tipo", brandName));
        cT.setCellStyle(stTitulo);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

        r++;
        Row fHead = sheet.createRow(r++);
        fHead.setHeight((short) (16 * 20));
        String[] headers = {
            t("xls.header.tipo"), t("xls.header.cantidad_items"),
            t("xls.header.items_bajo_stock"), t("xls.header.pct_bajo_stock")
        };
        for (int i = 0; i < headers.length; i++) celda(fHead, i, headers[i], stHeader);

        java.util.LinkedHashMap<String, long[]> grouped = new java.util.LinkedHashMap<>();
        for (InsumoInventario ins : insumos) {
            String tp = ins.getTipo() != null ? ins.getTipo() : t("xls.text.otro");
            grouped.computeIfAbsent(tp, k -> new long[]{0, 0});
            grouped.get(tp)[0]++;
            if (ins.isBajoStock()) grouped.get(tp)[1]++;
        }

        int idx = 0;
        for (Map.Entry<String, long[]> entry : grouped.entrySet()) {
            boolean alt = idx % 2 != 0;
            Row fila = sheet.createRow(r++);
            long total = entry.getValue()[0], bajo = entry.getValue()[1];
            celda(fila, 0, entry.getKey(), alt ? stDatoAlt : stDato);
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
                                        ExportBranding branding, Locale locale) {
        LOCALE_HOLDER.set(locale);
        try {
            Pal pal = Pal.of(branding);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (XSSFWorkbook wb = new XSSFWorkbook()) {
                construirSheetFacturas(wb, facturas, estadoFiltro, desde, hasta, branding.name(), pal);
                construirSheetProveedores(wb, facturas, branding.name(), pal);
                construirSheetItems(wb, facturas, branding.name(), pal);
                wb.write(baos);
            } catch (Exception e) {
                throw new RuntimeException("Error generando Excel de facturas", e);
            }
            return baos.toByteArray();
        } finally {
            LOCALE_HOLDER.remove();
        }
    }

    private void construirSheetFacturas(XSSFWorkbook wb, List<FacturaProveedor> facturas,
                                         EstadoFactura estadoFiltro,
                                         LocalDate desde, LocalDate hasta,
                                         String brandName, Pal pal) {
        Sheet sheet = wb.createSheet(t("xls.sheet.facturas"));
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
        cTitulo.setCellValue(tf("xls.titulo.facturas", brandName));
        cTitulo.setCellStyle(stTitulo);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 11));

        String desdeStr  = desde != null ? desde.format(FMT_FECHA) : "—";
        String hastaStr  = hasta != null ? hasta.format(FMT_FECHA) : "—";
        String estadoStr = estadoFiltro != null ? estadoFiltro.getDisplayName() : t("xls.filtro.todas");
        Row fFiltro = sheet.createRow(r++);
        Cell cFiltro = fFiltro.createCell(0);
        cFiltro.setCellValue(tf("xls.filtro.estado_periodo", estadoStr, desdeStr, hastaStr));
        cFiltro.setCellStyle(stPeriodo);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 11));

        r++;
        BigDecimal totalSubtotal = facturas.stream()
                .map(f -> f.getSubtotal() != null ? f.getSubtotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalIva = facturas.stream()
                .map(f -> f.getValorIva() != null ? f.getValorIva() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalGeneral = facturas.stream()
                .map(f -> f.getValorTotal() != null ? f.getValorTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Row fRes = sheet.createRow(r++);
        celda(fRes, 0, tf("xls.resumen.total_facturas", facturas.size()), stResumen);
        celda(fRes, 2, tf("xls.resumen.subtotal",       totalSubtotal.toPlainString()), stResumen);
        celda(fRes, 5, tf("xls.resumen.iva",            totalIva.toPlainString()), stResumen);
        celda(fRes, 7, tf("xls.resumen.total",          totalGeneral.toPlainString()), stResumen);

        r++;
        Row fHead = sheet.createRow(r++);
        fHead.setHeight((short) (16 * 20));
        String[] headers = {
            t("xls.header.num_factura"), t("xls.header.proveedor"), t("xls.header.fecha"),
            t("xls.header.estado"), t("xls.header.items"), t("xls.header.subtotal"),
            t("xls.header.iva"), t("xls.header.envio"), t("xls.header.total"),
            t("xls.header.iva_incluido"), t("xls.header.descripcion"), t("xls.header.creado_por")
        };
        for (int i = 0; i < headers.length; i++) celda(fHead, i, headers[i], stHeader);

        int[] widths = {14, 22, 12, 12, 7, 14, 14, 14, 14, 12, 24, 14};
        for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);

        for (int i = 0; i < facturas.size(); i++) {
            FacturaProveedor f = facturas.get(i);
            boolean alt = i % 2 != 0;
            Row fila = sheet.createRow(r++);
            XSSFCellStyle sD = alt ? stDatoAlt : stDato;
            XSSFCellStyle sN = alt ? stNumAlt  : stNum;

            celda(fila, 0,  f.getNumeroFactura() != null ? f.getNumeroFactura() : "#" + f.getId(), sD);
            celda(fila, 1,  f.getProveedor() != null ? f.getProveedor() : "", sD);
            celda(fila, 2,  f.getFechaFactura() != null ? f.getFechaFactura().format(FMT_FECHA) : "", sD);
            celda(fila, 3,  f.getEstado() != null ? f.getEstado().getDisplayName() : "", sD);
            celdaNum(fila, 4,  (double) f.getItems().size(), sN);
            celdaNum(fila, 5,  toDouble(f.getSubtotal()),    sN);
            celdaNum(fila, 6,  toDouble(f.getValorIva()),    sN);
            celdaNum(fila, 7,  toDouble(f.getCostoEnvio()),  sN);
            celdaNum(fila, 8,  toDouble(f.getValorTotal()),  sN);
            celda(fila, 9,  f.isIvaIncluido() ? t("xls.text.si") : t("xls.text.no"), sD);
            celda(fila, 10, f.getDescripcion() != null ? f.getDescripcion() : "", sD);
            celda(fila, 11, f.getCreatedBy()  != null ? f.getCreatedBy()  : "", sD);
        }

        if (!facturas.isEmpty()) {
            sheet.setAutoFilter(new CellRangeAddress(5, r - 1, 0, headers.length - 1));
        }
    }

    private void construirSheetProveedores(XSSFWorkbook wb, List<FacturaProveedor> facturas,
                                            String brandName, Pal pal) {
        Sheet sheet = wb.createSheet(t("xls.sheet.por_proveedor"));
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
        cT.setCellValue(tf("xls.titulo.por_proveedor", brandName));
        cT.setCellStyle(stTitulo);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));

        r++;
        Row fH = sheet.createRow(r++);
        fH.setHeight((short) (16 * 20));
        celda(fH, 0, t("xls.header.proveedor"),  stHeader);
        celda(fH, 1, t("xls.header.facturas"),   stHeader);
        celda(fH, 2, t("xls.header.total_pesos"), stHeader);

        Map<String, long[]> resumen = new LinkedHashMap<>();
        for (FacturaProveedor f : facturas) {
            String prov = f.getProveedor() != null ? f.getProveedor() : t("xls.text.sin_proveedor");
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

    private void construirSheetItems(XSSFWorkbook wb, List<FacturaProveedor> facturas,
                                      String brandName, Pal pal) {
        Sheet sheet = wb.createSheet(t("xls.sheet.items_factura"));
        sheet.setDefaultColumnWidth(15);

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
        cT.setCellValue(tf("xls.titulo.items_factura", brandName));
        cT.setCellStyle(stTitulo);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 12));

        r++;
        Row fH = sheet.createRow(r++);
        fH.setHeight((short) (16 * 20));
        String[] headers = {
            t("xls.header.num_factura"), t("xls.header.proveedor"), t("xls.header.fecha"),
            t("xls.header.tipo"), t("xls.header.nombre"), t("xls.header.cantidad"),
            t("xls.header.unidad"), t("xls.header.v_unitario"),
            t("xls.header.desc_pct"), t("xls.header.iva_pct"),
            t("xls.header.valor_iva"), t("xls.header.imp_consumo"), t("xls.header.total_linea")
        };
        for (int i = 0; i < headers.length; i++) celda(fH, i, headers[i], stHeader);

        int[] widths = {14, 22, 12, 12, 28, 10, 9, 14, 9, 9, 14, 14, 14};
        for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);

        int rowIdx = 0;
        for (FacturaProveedor f : facturas) {
            if (f.getItems() == null || f.getItems().isEmpty()) continue;
            String numFact = f.getNumeroFactura() != null ? f.getNumeroFactura() : "#" + f.getId();
            String prov    = f.getProveedor() != null ? f.getProveedor() : "";
            String fecha   = f.getFechaFactura() != null ? f.getFechaFactura().format(FMT_FECHA) : "";
            for (FacturaItem item : f.getItems()) {
                boolean alt = rowIdx % 2 != 0;
                Row fila = sheet.createRow(r++);
                XSSFCellStyle sD = alt ? stDatoAlt : stDato;
                XSSFCellStyle sN = alt ? stNumAlt  : stNum;

                celda(fila, 0, numFact, sD);
                celda(fila, 1, prov, sD);
                celda(fila, 2, fecha, sD);
                celda(fila, 3, item.getTipoItem() != null ? item.getTipoItem().getDisplayName() : "", sD);
                celda(fila, 4, item.getNombre() != null ? item.getNombre() : "", sD);
                celdaNum(fila, 5, toDouble(item.getCantidadDisplay()), sN);
                celda(fila, 6, item.getUnidadDisplay() != null ? item.getUnidadDisplay() : "", sD);
                celdaNum(fila, 7, toDouble(item.getValorUnitario()), sN);
                celdaNum(fila, 8, item.getPorcentajeDescuento() != null
                        ? item.getPorcentajeDescuento().doubleValue() : null, sN);
                celdaNum(fila, 9, item.getPorcentajeIvaItem() != null
                        ? item.getPorcentajeIvaItem().doubleValue() : null, sN);
                celdaNum(fila, 10, toDouble(item.getValorIvaItem()), sN);
                celdaNum(fila, 11, toDouble(item.getImpuestoConsumo()), sN);
                celdaNum(fila, 12, toDouble(item.getValorLinea()), sN);
                rowIdx++;
            }
        }

        if (rowIdx > 0) {
            sheet.setAutoFilter(new CellRangeAddress(2, r - 1, 0, headers.length - 1));
        }
    }

    // ── Excel Reporte Producción ──────────────────────────────────────

    public byte[] generarExcelReporteProduccion(List<LoteCerveza> lotes,
                                                  List<Object[]> resumen,
                                                  LocalDate desde, LocalDate hasta,
                                                  ExportBranding branding, Locale locale) {
        LOCALE_HOLDER.set(locale);
        try {
            Pal pal = Pal.of(branding);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (XSSFWorkbook wb = new XSSFWorkbook()) {
                construirSheetLotes(wb, lotes, desde, hasta, branding.name(), pal);
                construirSheetEstilos(wb, lotes, branding.name(), pal);
                construirSheetTendencia(wb, lotes, branding.name(), pal);
                wb.write(baos);
            } catch (Exception e) {
                throw new RuntimeException("Error generando Excel del reporte", e);
            }
            return baos.toByteArray();
        } finally {
            LOCALE_HOLDER.remove();
        }
    }

    private void construirSheetLotes(XSSFWorkbook wb, List<LoteCerveza> lotes,
                                      LocalDate desde, LocalDate hasta,
                                      String brandName, Pal pal) {
        Sheet sheet = wb.createSheet(t("xls.sheet.reporte_produccion"));
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
        cTitulo.setCellValue(tf("xls.titulo.reporte_produccion", brandName));
        cTitulo.setCellStyle(stTitulo);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 19));

        String desdeStr = desde != null ? desde.format(FMT_FECHA) : "—";
        String hastaStr = hasta != null ? hasta.format(FMT_FECHA) : "—";
        Row fPeriodo = sheet.createRow(r++);
        Cell cPeriodo = fPeriodo.createCell(0);
        cPeriodo.setCellValue(tf("xls.filtro.periodo", desdeStr, hastaStr));
        cPeriodo.setCellStyle(estiloPeriodo(wb, pal));
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 19));

        r++;
        long completados = lotes.stream().filter(LoteCerveza::isCompletado).count();
        BigDecimal totalL = lotes.stream()
                .filter(l -> l.getLitrosFinales() != null)
                .map(LoteCerveza::getLitrosFinales)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long estilos  = lotes.stream().map(LoteCerveza::getEstilo).distinct().count();
        long pctComp  = lotes.isEmpty() ? 0L : Math.round(completados * 100.0 / lotes.size());
        OptionalDouble avgAbv  = lotes.stream().filter(l -> l.getAbv() != null)
                .mapToDouble(l -> l.getAbv().doubleValue()).average();
        OptionalDouble avgEfic = lotes.stream().filter(l -> l.getEficienciaMacerado() != null)
                .mapToDouble(l -> l.getEficienciaMacerado().doubleValue()).average();
        BigDecimal costoTotal  = lotes.stream().map(LoteCerveza::getCostoTotal)
                .filter(c -> c != null && c.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String avgLitrosStr = lotes.isEmpty() ? "—"
                : String.format("%.1f", totalL.doubleValue() / lotes.size()) + " L";

        XSSFCellStyle stResumen = estiloResumen(wb, pal);
        Row fRes = sheet.createRow(r++);
        celda(fRes, 0, tf("xls.resumen.total_lotes",  lotes.size()),                stResumen);
        celda(fRes, 2, tf("xls.resumen.litros",        totalL),                      stResumen);
        celda(fRes, 5, tf("xls.resumen.estilos",       estilos),                    stResumen);
        celda(fRes, 8, tf("xls.resumen.completados",   completados, pctComp),       stResumen);

        Row fRes2 = sheet.createRow(r++);
        celda(fRes2, 0, tf("xls.resumen.prom_lote", avgLitrosStr), stResumen);
        if (avgAbv.isPresent())
            celda(fRes2, 2, tf("xls.resumen.abv_prom",
                    String.format("%.2f", avgAbv.getAsDouble())), stResumen);
        if (avgEfic.isPresent())
            celda(fRes2, 5, tf("xls.resumen.efic_prom",
                    String.format("%.1f", avgEfic.getAsDouble())), stResumen);
        if (costoTotal.compareTo(BigDecimal.ZERO) > 0)
            celda(fRes2, 8, tf("xls.resumen.costo_total", costoTotal.toPlainString()), stResumen);

        r++;
        Row fHead = sheet.createRow(r++);
        fHead.setHeight((short) (16 * 20));
        String[] headers = {
            t("xls.header.codigo"),      t("xls.header.estilo"),       t("xls.header.receta"),
            t("xls.header.fecha"),       t("xls.header.fase"),         t("xls.header.og"),
            t("xls.header.fg"),          t("xls.header.abv_pct"),      t("xls.header.atenuacion_pct"),
            t("xls.header.eficiencia_pct"), t("xls.header.litros"),   t("xls.header.costo_total"),
            t("xls.header.costo_litro"), t("xls.header.creado_por"),   t("xls.header.metodo_carb"),
            t("xls.header.co2_obj"),     t("xls.header.co2_real"),     t("xls.header.destino_empaque"),
            t("xls.header.hora_inicio"), t("xls.header.hora_fin")
        };
        for (int i = 0; i < headers.length; i++) celda(fHead, i, headers[i], stHeader);

        int[] widths = {14, 16, 20, 12, 14, 8, 8, 9, 13, 13, 9, 14, 13, 14, 16, 14, 14, 24, 12, 12};
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
            celda(fila, 4,  l.isCompletado() ? t("xls.text.completado") : l.getFaseActual(), sD);
            celdaNum(fila, 5,  l.getDensidadInicial() != null ? l.getDensidadInicial().doubleValue() : null, sN);
            celdaNum(fila, 6,  l.getDensidadFinal()   != null ? l.getDensidadFinal().doubleValue()   : null, sN);
            celdaNum(fila, 7,  toDouble(l.getAbv()),                 sN);
            celdaNum(fila, 8,  toDouble(l.getAtenuacionAparente()),  sN);
            celdaNum(fila, 9,  toDouble(l.getEficienciaMacerado()),  sN);
            celdaNum(fila, 10, toDouble(l.getLitrosFinales()),       sN);
            celdaNum(fila, 11, toDouble(l.getCostoTotal()),          sN);
            celdaNum(fila, 12, toDouble(l.getCostoPorLitro()),       sN);
            celda(fila, 13, l.getCreatedBy() != null ? l.getCreatedBy() : "", sD);
            String metodoCarb = l.getCarbMetodo() == null ? ""
                    : "NATURAL".equals(l.getCarbMetodo()) ? t("xls.text.carb_natural") : t("xls.text.carb_forzada");
            celda(fila, 14, metodoCarb, sD);
            celdaNum(fila, 15, l.getCarbCo2Objetivo() != null ? l.getCarbCo2Objetivo().doubleValue() : null, sN);
            celdaNum(fila, 16, l.getCarbCo2Real()     != null ? l.getCarbCo2Real().doubleValue()     : null, sN);
            celda(fila, 17, l.getCarbDestino() != null ? l.getCarbDestino() : "", sD);
            celda(fila, 18, l.getHoraInicioPrimeraElaboracion() != null ? l.getHoraInicioPrimeraElaboracion().format(FMT_HORA) : "", sD);
            celda(fila, 19, l.getHoraFinPrimeraElaboracion()    != null ? l.getHoraFinPrimeraElaboracion().format(FMT_HORA)    : "", sD);
        }

        if (!lotes.isEmpty()) {
            sheet.setAutoFilter(new CellRangeAddress(6, r - 1, 0, headers.length - 1));
        }
    }

    private void construirSheetEstilos(XSSFWorkbook wb, List<LoteCerveza> lotes,
                                        String brandName, Pal pal) {
        // [0]=count, [1]=litros, [2]=abvSum, [3]=abvCount, [4]=eficSum, [5]=eficCount, [6]=costoSum
        Map<String, double[]> styleData = new LinkedHashMap<>();
        for (LoteCerveza l : lotes) {
            String est = l.getEstilo() != null ? l.getEstilo() : "—";
            double[] d = styleData.computeIfAbsent(est, k -> new double[7]);
            d[0]++;
            if (l.getLitrosFinales() != null) d[1] += l.getLitrosFinales().doubleValue();
            if (l.getAbv() != null)              { d[2] += l.getAbv().doubleValue();               d[3]++; }
            if (l.getEficienciaMacerado() != null){ d[4] += l.getEficienciaMacerado().doubleValue(); d[5]++; }
            if (l.getCostoTotal() != null && l.getCostoTotal().compareTo(BigDecimal.ZERO) > 0)
                d[6] += l.getCostoTotal().doubleValue();
        }
        List<Map.Entry<String, double[]>> sorted = styleData.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue()[1], a.getValue()[1]))
                .collect(Collectors.toList());

        Sheet sheet = wb.createSheet(t("xls.sheet.por_estilo"));
        int[] colWidths = {22, 14, 16, 14, 14, 16};
        for (int i = 0; i < colWidths.length; i++) sheet.setColumnWidth(i, colWidths[i] * 256);

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
        cT.setCellValue(tf("xls.titulo.por_estilo", brandName));
        cT.setCellStyle(stTitulo);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

        r++;
        Row fH = sheet.createRow(r++);
        fH.setHeight((short) (16 * 20));
        String[] colHeaders = {
            t("xls.header.estilo"), t("xls.header.cantidad_lotes"), t("xls.header.litros_totales"),
            t("xls.header.abv_prom"), t("xls.header.efic_prom"), t("xls.header.costo_total")
        };
        for (int i = 0; i < colHeaders.length; i++) celda(fH, i, colHeaders[i], stHeader);

        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<String, double[]> e = sorted.get(i);
            boolean alt = i % 2 != 0;
            double[] d = e.getValue();
            Row fila = sheet.createRow(r++);
            celda   (fila, 0, e.getKey(),                alt ? stDatoAlt : stDato);
            celdaNum(fila, 1, d[0],                      alt ? stNumAlt  : stNum);
            celdaNum(fila, 2, d[1],                      alt ? stNumAlt  : stNum);
            celdaNum(fila, 3, d[3] > 0 ? d[2] / d[3] : null, alt ? stNumAlt : stNum);
            celdaNum(fila, 4, d[5] > 0 ? d[4] / d[5] : null, alt ? stNumAlt : stNum);
            celdaNum(fila, 5, d[6] > 0 ? d[6] : null,   alt ? stNumAlt  : stNum);
        }
    }

    private void construirSheetTendencia(XSSFWorkbook wb, List<LoteCerveza> lotes,
                                          String brandName, Pal pal) {
        // Group by year-month of fechaElaboracion, sorted chronologically
        // [0]=count, [1]=litros, [2]=abvSum, [3]=abvCount
        TreeMap<String, double[]> mensual = new TreeMap<>();
        for (LoteCerveza l : lotes) {
            if (l.getFechaElaboracion() == null) continue;
            String mes = String.format("%04d/%02d",
                    l.getFechaElaboracion().getYear(), l.getFechaElaboracion().getMonthValue());
            double[] d = mensual.computeIfAbsent(mes, k -> new double[4]);
            d[0]++;
            if (l.getLitrosFinales() != null) d[1] += l.getLitrosFinales().doubleValue();
            if (l.getAbv() != null)           { d[2] += l.getAbv().doubleValue(); d[3]++; }
        }
        if (mensual.isEmpty()) return;

        Sheet sheet = wb.createSheet(t("xls.sheet.tendencia_mensual"));
        int[] colWidths = {14, 16, 16, 14};
        for (int i = 0; i < colWidths.length; i++) sheet.setColumnWidth(i, colWidths[i] * 256);

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
        cT.setCellValue(tf("xls.titulo.tendencia_mensual", brandName));
        cT.setCellStyle(stTitulo);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

        r++;
        Row fH = sheet.createRow(r++);
        fH.setHeight((short) (16 * 20));
        String[] colHeaders = {
            t("xls.header.mes"), t("xls.header.lotes_elaborados"),
            t("xls.header.litros_totales"), t("xls.header.abv_prom")
        };
        for (int i = 0; i < colHeaders.length; i++) celda(fH, i, colHeaders[i], stHeader);

        List<Map.Entry<String, double[]>> entries = new ArrayList<>(mensual.entrySet());
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<String, double[]> e = entries.get(i);
            boolean alt = i % 2 != 0;
            double[] d = e.getValue();
            Row fila = sheet.createRow(r++);
            celda   (fila, 0, e.getKey(),                       alt ? stDatoAlt : stDato);
            celdaNum(fila, 1, d[0],                              alt ? stNumAlt  : stNum);
            celdaNum(fila, 2, d[1],                              alt ? stNumAlt  : stNum);
            celdaNum(fila, 3, d[3] > 0 ? d[2] / d[3] : null,  alt ? stNumAlt  : stNum);
        }
    }

    // ── Excel Ventas ─────────────────────────────────────────────────

    public byte[] generarExcelVentas(List<Venta> ventas,
                                      EstadoVenta estadoFiltro,
                                      LocalDate desde, LocalDate hasta,
                                      ExportBranding branding, Locale locale) {
        LOCALE_HOLDER.set(locale);
        try {
            Pal pal = Pal.of(branding);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (XSSFWorkbook wb = new XSSFWorkbook()) {
                construirSheetVentas(wb, ventas, estadoFiltro, desde, hasta, branding.name(), pal);
                construirSheetVentasItems(wb, ventas, branding.name(), pal);
                construirSheetVentasPorCliente(wb, ventas, branding.name(), pal);
                construirSheetVentasPorEstado(wb, ventas, branding.name(), pal);
                construirSheetVentasTendencia(wb, ventas, branding.name(), pal);
                wb.write(baos);
            } catch (Exception e) {
                throw new RuntimeException("Error generando Excel de ventas", e);
            }
            return baos.toByteArray();
        } finally {
            LOCALE_HOLDER.remove();
        }
    }

    private void construirSheetVentas(XSSFWorkbook wb, List<Venta> ventas,
                                       EstadoVenta estadoFiltro,
                                       LocalDate desde, LocalDate hasta,
                                       String brandName, Pal pal) {
        Sheet sheet = wb.createSheet(t("xls.sheet.ventas"));
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
        Row fT = sheet.createRow(r++);
        fT.setHeight((short) (20 * 20));
        Cell cT = fT.createCell(0);
        cT.setCellValue(tf("xls.titulo.ventas", brandName));
        cT.setCellStyle(stTitulo);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));

        String periodoStr = tf("xls.filtro.periodo",
                desde != null ? desde.format(FMT_FECHA) : "—",
                hasta != null ? hasta.format(FMT_FECHA) : "—");
        if (estadoFiltro != null) periodoStr += tf("xls.filtro.estado", estadoFiltro.getDisplayName());
        Row fP = sheet.createRow(r++);
        Cell cP = fP.createCell(0);
        cP.setCellValue(periodoStr);
        cP.setCellStyle(stPeriodo);
        sheet.addMergedRegion(new CellRangeAddress(r - 1, r - 1, 0, 7));

        long totalVentas = ventas.size();
        long totalDespachadas = ventas.stream().filter(v -> v.getEstado() == EstadoVenta.DESPACHADO).count();
        BigDecimal totalIngresos = ventas.stream()
                .filter(v -> v.getEstado() == EstadoVenta.DESPACHADO)
                .map(v -> v.getValorTotal() != null ? v.getValorTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Row fRes = sheet.createRow(r++);
        celda(fRes, 0, tf("xls.resumen.total_ventas",         totalVentas),      stResumen);
        celda(fRes, 3, tf("xls.resumen.despachadas",           totalDespachadas), stResumen);
        celda(fRes, 6, tf("xls.resumen.ingresos_despachados",
                String.format("%,.0f", totalIngresos)), stResumen);

        r++;
        sheet.createRow(r++);

        Row fHead = sheet.createRow(r++);
        fHead.setHeight((short) (16 * 20));
        String[] headers = {
            t("xls.header.cliente"),        t("xls.header.primer_lote"),
            t("xls.header.remision"),        t("xls.header.fecha_despacho"),
            t("xls.header.estado"),          t("xls.header.valor_total"),
            t("xls.header.notas"),           t("xls.header.creado_por")
        };
        for (int i = 0; i < headers.length; i++) celda(fHead, i, headers[i], stHeader);

        int[] widths = {30, 16, 14, 14, 14, 16, 30, 18};
        for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);

        for (int i = 0; i < ventas.size(); i++) {
            Venta v = ventas.get(i);
            boolean alt = i % 2 != 0;
            Row fila = sheet.createRow(r++);
            XSSFCellStyle sD = alt ? stDatoAlt : stDato;
            XSSFCellStyle sN = alt ? stNumAlt  : stNum;

            celda   (fila, 0, v.getCliente(), sD);
            celda   (fila, 1, v.getPrimerCodigoLote()  != null ? v.getPrimerCodigoLote()  : "", sD);
            celda   (fila, 2, v.getRemisionNumero()    != null ? v.getRemisionNumero()    : "", sD);
            celda   (fila, 3, v.getFechaDespacho()     != null ? v.getFechaDespacho().format(FMT_FECHA) : "", sD);
            celda   (fila, 4, v.getEstado()            != null ? v.getEstado().getDisplayName() : "", sD);
            celdaNum(fila, 5, toDouble(v.getValorTotal()), sN);
            celda   (fila, 6, v.getNotas()             != null ? v.getNotas()             : "", sD);
            celda   (fila, 7, v.getCreatedBy()         != null ? v.getCreatedBy()         : "", sD);
        }

        if (!ventas.isEmpty()) {
            sheet.setAutoFilter(new CellRangeAddress(r - ventas.size() - 1, r - 1, 0, headers.length - 1));
        }
    }

    private void construirSheetVentasItems(XSSFWorkbook wb, List<Venta> ventas,
                                            String brandName, Pal pal) {
        Sheet sheet = wb.createSheet(t("xls.sheet.items_venta"));
        sheet.setDefaultColumnWidth(14);

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
        cT.setCellValue(tf("xls.titulo.items_venta", brandName));
        cT.setCellStyle(stTitulo);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 10));

        r++;
        Row fHead = sheet.createRow(r++);
        fHead.setHeight((short) (16 * 20));
        String[] headers = {
            t("xls.header.venta_id"),   t("xls.header.cliente"),
            t("xls.header.fecha_despacho"), t("xls.header.estado_venta"),
            t("xls.header.lote"),       t("xls.header.descripcion"),
            t("xls.header.cantidad"),   t("xls.header.unidad"),
            t("xls.header.precio_unit"), t("xls.header.desc_pct_v"),
            t("xls.header.total_linea")
        };
        for (int i = 0; i < headers.length; i++) celda(fHead, i, headers[i], stHeader);
        int[] widths = {10, 26, 14, 13, 14, 22, 12, 10, 15, 10, 15};
        for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);

        int rowCount = 0;
        for (Venta v : ventas) {
            for (VentaItem item : v.getItems()) {
                boolean alt = rowCount % 2 != 0;
                Row fila = sheet.createRow(r++);
                XSSFCellStyle sD = alt ? stDatoAlt : stDato;
                XSSFCellStyle sN = alt ? stNumAlt  : stNum;

                celda(fila, 0, String.valueOf(v.getId()), sD);
                celda(fila, 1, v.getCliente(), sD);
                celda(fila, 2, v.getFechaDespacho() != null ? v.getFechaDespacho().format(FMT_FECHA) : "", sD);
                celda(fila, 3, v.getEstado() != null ? v.getEstado().getDisplayName() : "", sD);
                celda(fila, 4, item.getCodigoLote() != null ? item.getCodigoLote() : "", sD);
                celda(fila, 5, item.getDescripcion() != null ? item.getDescripcion() : "", sD);
                celdaNum(fila, 6, toDouble(item.getCantidadDisplay()), sN);
                celda(fila, 7, item.getUnidadDisplay() != null ? item.getUnidadDisplay() : "", sD);
                celdaNum(fila, 8, toDouble(item.getPrecioUnitario()), sN);
                celdaNum(fila, 9, toDouble(item.getDescuentoPct()), sN);
                celdaNum(fila, 10, toDouble(item.getValorLinea()), sN);
                rowCount++;
            }
        }

        if (rowCount > 0) {
            sheet.setAutoFilter(new CellRangeAddress(2, r - 1, 0, headers.length - 1));
        }
    }

    private void construirSheetVentasPorCliente(XSSFWorkbook wb, List<Venta> ventas,
                                                 String brandName, Pal pal) {
        Sheet sheet = wb.createSheet(t("xls.sheet.por_cliente"));

        XSSFCellStyle stTitulo  = estiloTitulo(wb, pal);
        XSSFCellStyle stHeader  = estiloHeader(wb, pal);
        XSSFCellStyle stDato    = estiloDato(wb, pal, false);
        XSSFCellStyle stDatoAlt = estiloDato(wb, pal, true);
        XSSFCellStyle stNum     = estiloNumero(wb, pal, false);
        XSSFCellStyle stNumAlt  = estiloNumero(wb, pal, true);

        int r = 0;
        Row fT = sheet.createRow(r++);
        fT.setHeight((short) (18 * 20));
        Cell cT = fT.createCell(0);
        cT.setCellValue(tf("xls.titulo.por_cliente", brandName));
        cT.setCellStyle(stTitulo);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

        r++;
        Row fHead = sheet.createRow(r++);
        fHead.setHeight((short) (14 * 20));
        celda(fHead, 0, t("xls.header.cliente"),     stHeader);
        celda(fHead, 1, t("xls.header.ventas"),      stHeader);
        celda(fHead, 2, t("xls.header.total_dolar"), stHeader);
        celda(fHead, 3, t("xls.header.pct_total"),   stHeader);

        sheet.setColumnWidth(0, 35 * 256);
        sheet.setColumnWidth(1, 12 * 256);
        sheet.setColumnWidth(2, 18 * 256);
        sheet.setColumnWidth(3, 14 * 256);

        Map<String, double[]> agrupado = new LinkedHashMap<>();
        for (Venta v : ventas) {
            String cliente = v.getCliente() != null ? v.getCliente() : t("xls.text.sin_cliente");
            agrupado.computeIfAbsent(cliente, k -> new double[]{0, 0});
            agrupado.get(cliente)[0]++;
            if (v.getValorTotal() != null) agrupado.get(cliente)[1] += v.getValorTotal().doubleValue();
        }

        double totalGlobal = agrupado.values().stream().mapToDouble(d -> d[1]).sum();
        List<Map.Entry<String, double[]>> sorted = agrupado.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue()[1], a.getValue()[1]))
                .collect(Collectors.toList());

        int i = 0;
        for (Map.Entry<String, double[]> entry : sorted) {
            boolean alt = i % 2 != 0;
            Row fila = sheet.createRow(r++);
            celda   (fila, 0, entry.getKey(),     alt ? stDatoAlt : stDato);
            celdaNum(fila, 1, entry.getValue()[0], alt ? stNumAlt : stNum);
            celdaNum(fila, 2, entry.getValue()[1], alt ? stNumAlt : stNum);
            celdaNum(fila, 3, totalGlobal > 0 ? entry.getValue()[1] * 100.0 / totalGlobal : 0,
                     alt ? stNumAlt : stNum);
            i++;
        }
    }

    private void construirSheetVentasPorEstado(XSSFWorkbook wb, List<Venta> ventas,
                                                String brandName, Pal pal) {
        Sheet sheet = wb.createSheet(t("xls.sheet.por_estado"));

        XSSFCellStyle stTitulo  = estiloTitulo(wb, pal);
        XSSFCellStyle stHeader  = estiloHeader(wb, pal);
        XSSFCellStyle stDato    = estiloDato(wb, pal, false);
        XSSFCellStyle stDatoAlt = estiloDato(wb, pal, true);
        XSSFCellStyle stNum     = estiloNumero(wb, pal, false);
        XSSFCellStyle stNumAlt  = estiloNumero(wb, pal, true);

        int r = 0;
        Row fT = sheet.createRow(r++);
        fT.setHeight((short) (18 * 20));
        Cell cT = fT.createCell(0);
        cT.setCellValue(tf("xls.titulo.por_estado", brandName));
        cT.setCellStyle(stTitulo);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

        r++;
        Row fHead = sheet.createRow(r++);
        fHead.setHeight((short) (14 * 20));
        celda(fHead, 0, t("xls.header.estado"),      stHeader);
        celda(fHead, 1, t("xls.header.ventas"),      stHeader);
        celda(fHead, 2, t("xls.header.total_dolar"), stHeader);
        celda(fHead, 3, t("xls.header.pct_total"),   stHeader);

        sheet.setColumnWidth(0, 18 * 256);
        sheet.setColumnWidth(1, 12 * 256);
        sheet.setColumnWidth(2, 18 * 256);
        sheet.setColumnWidth(3, 14 * 256);

        Map<String, double[]> agrupado = new LinkedHashMap<>();
        for (Venta v : ventas) {
            String estado = v.getEstado() != null ? v.getEstado().getDisplayName() : t("xls.text.sin_estado");
            agrupado.computeIfAbsent(estado, k -> new double[]{0, 0});
            agrupado.get(estado)[0]++;
            if (v.getValorTotal() != null) agrupado.get(estado)[1] += v.getValorTotal().doubleValue();
        }

        double totalGlobal = agrupado.values().stream().mapToDouble(d -> d[1]).sum();
        List<Map.Entry<String, double[]>> sorted = agrupado.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue()[1], a.getValue()[1]))
                .collect(Collectors.toList());

        int i = 0;
        for (Map.Entry<String, double[]> entry : sorted) {
            boolean alt = i % 2 != 0;
            Row fila = sheet.createRow(r++);
            celda   (fila, 0, entry.getKey(),     alt ? stDatoAlt : stDato);
            celdaNum(fila, 1, entry.getValue()[0], alt ? stNumAlt : stNum);
            celdaNum(fila, 2, entry.getValue()[1], alt ? stNumAlt : stNum);
            celdaNum(fila, 3, totalGlobal > 0 ? entry.getValue()[1] * 100.0 / totalGlobal : 0,
                     alt ? stNumAlt : stNum);
            i++;
        }
    }

    private void construirSheetVentasTendencia(XSSFWorkbook wb, List<Venta> ventas,
                                                String brandName, Pal pal) {
        // Group by year-month of createdAt — [0]=total ventas, [1]=despachadas, [2]=ingresos
        TreeMap<String, double[]> mensual = new TreeMap<>();
        for (Venta v : ventas) {
            if (v.getCreatedAt() == null) continue;
            String mes = String.format("%04d/%02d",
                    v.getCreatedAt().getYear(), v.getCreatedAt().getMonthValue());
            double[] d = mensual.computeIfAbsent(mes, k -> new double[3]);
            d[0]++;
            if (v.getEstado() == EstadoVenta.DESPACHADO) {
                d[1]++;
                if (v.getValorTotal() != null) d[2] += v.getValorTotal().doubleValue();
            }
        }
        if (mensual.isEmpty()) return;

        Sheet sheet = wb.createSheet(t("xls.sheet.tendencia_ventas"));
        int[] colWidths = {14, 14, 14, 18};
        for (int i = 0; i < colWidths.length; i++) sheet.setColumnWidth(i, colWidths[i] * 256);

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
        cT.setCellValue(tf("xls.titulo.tendencia_ventas", brandName));
        cT.setCellStyle(stTitulo);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

        r++;
        Row fH = sheet.createRow(r++);
        fH.setHeight((short) (16 * 20));
        celda(fH, 0, t("xls.header.mes"),          stHeader);
        celda(fH, 1, t("xls.header.ventas"),        stHeader);
        celda(fH, 2, t("xls.header.despachadas"),   stHeader);
        celda(fH, 3, t("xls.header.ingresos_desp"), stHeader);

        List<Map.Entry<String, double[]>> entries = new ArrayList<>(mensual.entrySet());
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<String, double[]> e = entries.get(i);
            boolean alt = i % 2 != 0;
            double[] d = e.getValue();
            Row fila = sheet.createRow(r++);
            celda   (fila, 0, e.getKey(), alt ? stDatoAlt : stDato);
            celdaNum(fila, 1, d[0],        alt ? stNumAlt  : stNum);
            celdaNum(fila, 2, d[1],        alt ? stNumAlt  : stNum);
            celdaNum(fila, 3, d[2],        alt ? stNumAlt  : stNum);
        }
    }

    // ── Excel Reporte Rentabilidad ────────────────────────────────────

    public byte[] generarExcelReporteRentabilidad(List<RentabilidadLoteDto> filas,
                                                   String estilo,
                                                   LocalDate desde, LocalDate hasta,
                                                   ExportBranding branding, Locale locale) {
        LOCALE_HOLDER.set(locale);
        try {
            Pal pal = Pal.of(branding);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (XSSFWorkbook wb = new XSSFWorkbook()) {
                construirSheetRentabilidad(wb, filas, estilo, desde, hasta, branding.name(), pal);
                construirSheetRentabilidadPorEstilo(wb, filas, branding.name(), pal);
                construirSheetRentabilidadTendencia(wb, filas, branding.name(), pal);
                wb.write(baos);
            } catch (Exception e) {
                throw new RuntimeException("Error generando Excel de rentabilidad", e);
            }
            return baos.toByteArray();
        } finally {
            LOCALE_HOLDER.remove();
        }
    }

    private void construirSheetRentabilidad(XSSFWorkbook wb, List<RentabilidadLoteDto> filas,
                                             String estilo,
                                             LocalDate desde, LocalDate hasta,
                                             String brandName, Pal pal) {
        Sheet sheet = wb.createSheet(t("xls.sheet.rentabilidad"));
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
        // Título
        Row fT = sheet.createRow(r++);
        fT.setHeight((short) (20 * 20));
        Cell cT = fT.createCell(0);
        cT.setCellValue(tf("xls.titulo.rentabilidad", brandName));
        cT.setCellStyle(stTitulo);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 10));

        // Período / filtro
        String desdeStr = desde != null ? desde.format(FMT_FECHA) : "—";
        String hastaStr = hasta != null ? hasta.format(FMT_FECHA) : "—";
        String filtro   = tf("xls.filtro.periodo", desdeStr, hastaStr);
        if (estilo != null && !estilo.isBlank()) filtro += tf("xls.filtro.estado", estilo);
        Row fP = sheet.createRow(r++);
        Cell cP = fP.createCell(0);
        cP.setCellValue(filtro);
        cP.setCellStyle(stPeriodo);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 10));

        // Resumen
        r++;
        BigDecimal totalCosto    = filas.stream().filter(f -> f.costo() != null)
                .map(RentabilidadLoteDto::costo).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalIngresos = filas.stream().filter(f -> f.ingresos() != null)
                .map(RentabilidadLoteDto::ingresos).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalMargen   = totalIngresos.subtract(totalCosto)
                .setScale(0, java.math.RoundingMode.HALF_UP);
        long rentables = filas.stream().filter(RentabilidadLoteDto::rentable).count();
        long enRojo    = filas.stream().filter(RentabilidadLoteDto::enRojo).count();

        Row fRes = sheet.createRow(r++);
        celda(fRes, 0, tf("xls.resumen.total_lotes", filas.size()), stResumen);
        celda(fRes, 2, tf("xls.resumen.ingresos",    totalIngresos.toPlainString()), stResumen);
        celda(fRes, 5, tf("xls.resumen.margen",      totalMargen.toPlainString()), stResumen);
        celda(fRes, 8, tf("xls.resumen.completados", rentables, enRojo), stResumen);

        r++;
        // Encabezados
        Row fH = sheet.createRow(r++);
        fH.setHeight((short) (16 * 20));
        String[] headers = {
            t("xls.header.codigo"),       t("xls.header.estilo"),
            t("xls.header.completado"),   t("xls.header.litros"),
            t("xls.header.costo_total"),  t("xls.header.ingresos"),
            t("xls.header.margen"),       t("xls.header.margen_pct"),
            t("xls.header.costo_litro"),  t("xls.header.ingreso_litro"),
            t("xls.header.estado")
        };
        for (int i = 0; i < headers.length; i++) celda(fH, i, headers[i], stHeader);

        int[] widths = {14, 18, 14, 10, 16, 16, 16, 12, 13, 13, 14};
        for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);

        for (int i = 0; i < filas.size(); i++) {
            RentabilidadLoteDto f = filas.get(i);
            boolean alt = i % 2 != 0;
            Row fila = sheet.createRow(r++);
            XSSFCellStyle sD = alt ? stDatoAlt : stDato;
            XSSFCellStyle sN = alt ? stNumAlt  : stNum;

            celda   (fila, 0,  f.codigoLote(), sD);
            celda   (fila, 1,  f.estilo() != null ? f.estilo() : "", sD);
            celda   (fila, 2,  f.fechaCompletado() != null ? f.fechaCompletado().format(FMT_FECHA) : "", sD);
            celdaNum(fila, 3,  toDouble(f.litrosFinales()), sN);
            celdaNum(fila, 4,  toDouble(f.costo()), sN);
            celdaNum(fila, 5,  toDouble(f.ingresos()), sN);
            celdaNum(fila, 6,  toDouble(f.margen()), sN);
            celdaNum(fila, 7,  toDouble(f.margenPct()), sN);
            celdaNum(fila, 8,  toDouble(f.costoPorLitro()), sN);
            celdaNum(fila, 9,  toDouble(f.ingresoPorLitro()), sN);

            String estado;
            if (f.sinVentas())                       estado = t("xls.text.sin_ventas");
            else if (f.sinCosto())                   estado = t("xls.text.sin_costo");
            else if (f.rentable())                   estado = t("xls.text.rentable");
            else                                     estado = t("xls.text.en_rojo");
            celda(fila, 10, estado, sD);
        }

        if (!filas.isEmpty()) {
            sheet.setAutoFilter(new CellRangeAddress(r - filas.size() - 1, r - 1, 0, headers.length - 1));
        }
    }

    private void construirSheetRentabilidadPorEstilo(XSSFWorkbook wb, List<RentabilidadLoteDto> filas,
                                                      String brandName, Pal pal) {
        // [0]=count, [1]=litros, [2]=costo, [3]=ingresos, [4]=margen
        Map<String, double[]> styleData = new LinkedHashMap<>();
        for (RentabilidadLoteDto f : filas) {
            String est = f.estilo() != null ? f.estilo() : "—";
            double[] d = styleData.computeIfAbsent(est, k -> new double[5]);
            d[0]++;
            if (f.litrosFinales() != null) d[1] += f.litrosFinales().doubleValue();
            if (f.costo()         != null) d[2] += f.costo().doubleValue();
            if (f.ingresos()      != null) d[3] += f.ingresos().doubleValue();
            if (f.margen()        != null) d[4] += f.margen().doubleValue();
        }
        List<Map.Entry<String, double[]>> sorted = styleData.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue()[3], a.getValue()[3]))
                .collect(Collectors.toList());

        Sheet sheet = wb.createSheet(t("xls.sheet.por_estilo"));

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
        cT.setCellValue(tf("xls.titulo.rent.por_estilo", brandName));
        cT.setCellStyle(stTitulo);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

        r++;
        Row fH = sheet.createRow(r++);
        fH.setHeight((short) (16 * 20));
        String[] colHeaders = {
            t("xls.header.estilo"),       t("xls.header.cantidad_lotes"),
            t("xls.header.litros_totales"), t("xls.header.costo_total"),
            t("xls.header.ingresos"),     t("xls.header.margen"),
            t("xls.header.margen_pct")
        };
        for (int i = 0; i < colHeaders.length; i++) celda(fH, i, colHeaders[i], stHeader);

        int[] colWidths = {22, 14, 16, 16, 16, 16, 14};
        for (int i = 0; i < colWidths.length; i++) sheet.setColumnWidth(i, colWidths[i] * 256);

        double totalIngresos = sorted.stream().mapToDouble(e -> e.getValue()[3]).sum();

        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<String, double[]> e = sorted.get(i);
            boolean alt = i % 2 != 0;
            double[] d = e.getValue();
            Row fila = sheet.createRow(r++);
            double margenPct = d[3] > 0 ? d[4] * 100.0 / d[3] : 0;
            celda   (fila, 0, e.getKey(),         alt ? stDatoAlt : stDato);
            celdaNum(fila, 1, d[0],               alt ? stNumAlt  : stNum);
            celdaNum(fila, 2, d[1] > 0 ? d[1] : null, alt ? stNumAlt : stNum);
            celdaNum(fila, 3, d[2] > 0 ? d[2] : null, alt ? stNumAlt : stNum);
            celdaNum(fila, 4, d[3] > 0 ? d[3] : null, alt ? stNumAlt : stNum);
            celdaNum(fila, 5, d[4] != 0 ? d[4] : null, alt ? stNumAlt : stNum);
            celdaNum(fila, 6, d[3] > 0 ? margenPct : null, alt ? stNumAlt : stNum);
        }
    }

    private void construirSheetRentabilidadTendencia(XSSFWorkbook wb, List<RentabilidadLoteDto> filas,
                                                      String brandName, Pal pal) {
        // Group by year-month of fechaCompletado — [0]=lotes, [1]=ingresos, [2]=margen
        TreeMap<String, double[]> mensual = new TreeMap<>();
        for (RentabilidadLoteDto f : filas) {
            if (f.fechaCompletado() == null) continue;
            String mes = String.format("%04d/%02d",
                    f.fechaCompletado().getYear(), f.fechaCompletado().getMonthValue());
            double[] d = mensual.computeIfAbsent(mes, k -> new double[3]);
            d[0]++;
            if (f.ingresos() != null) d[1] += f.ingresos().doubleValue();
            if (f.margen()   != null) d[2] += f.margen().doubleValue();
        }
        if (mensual.isEmpty()) return;

        Sheet sheet = wb.createSheet(t("xls.sheet.tendencia_mensual"));
        int[] colWidths = {14, 14, 18, 16};
        for (int i = 0; i < colWidths.length; i++) sheet.setColumnWidth(i, colWidths[i] * 256);

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
        cT.setCellValue(tf("xls.titulo.rent.tendencia", brandName));
        cT.setCellStyle(stTitulo);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

        r++;
        Row fH = sheet.createRow(r++);
        fH.setHeight((short) (16 * 20));
        celda(fH, 0, t("xls.header.mes"),             stHeader);
        celda(fH, 1, t("xls.header.cantidad_lotes"),  stHeader);
        celda(fH, 2, t("xls.header.ingresos"),        stHeader);
        celda(fH, 3, t("xls.header.margen"),          stHeader);

        List<Map.Entry<String, double[]>> entries = new ArrayList<>(mensual.entrySet());
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<String, double[]> e = entries.get(i);
            boolean alt = i % 2 != 0;
            double[] d = e.getValue();
            Row fila = sheet.createRow(r++);
            celda   (fila, 0, e.getKey(),                  alt ? stDatoAlt : stDato);
            celdaNum(fila, 1, d[0],                        alt ? stNumAlt  : stNum);
            celdaNum(fila, 2, d[1] > 0 ? d[1] : null,    alt ? stNumAlt  : stNum);
            celdaNum(fila, 3, d[2] != 0 ? d[2] : null,   alt ? stNumAlt  : stNum);
        }
    }

    // ── Excel Trazabilidad de Lotes ──────────────────────────────────

    public byte[] generarExcelLotes(List<LoteCerveza> lotes,
                                     String estilo, String fase,
                                     LocalDate desde, LocalDate hasta,
                                     ExportBranding branding, Locale locale) {
        LOCALE_HOLDER.set(locale);
        try {
            Pal pal = Pal.of(branding);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (XSSFWorkbook wb = new XSSFWorkbook()) {
                construirSheetLotesGeneral(wb, lotes, estilo, fase, desde, hasta, branding.name(), pal);
                construirSheetLotesFase(wb, lotes, branding.name(), pal,
                        t("xls.sheet.lotes.ferm"), tf("xls.titulo.lotes", branding.name()),
                        "FERM");
                construirSheetLotesFase(wb, lotes, branding.name(), pal,
                        t("xls.sheet.lotes.acond"), tf("xls.titulo.lotes", branding.name()),
                        "ACOND");
                construirSheetLotesFase(wb, lotes, branding.name(), pal,
                        t("xls.sheet.lotes.madur"), tf("xls.titulo.lotes", branding.name()),
                        "MADUR");
                construirSheetLotesCarbon(wb, lotes, branding.name(), pal);
                construirSheetLotesDensidad(wb, lotes, branding.name(), pal);
                wb.write(baos);
            } catch (Exception e) {
                throw new RuntimeException("Error generando Excel de lotes", e);
            }
            return baos.toByteArray();
        } finally {
            LOCALE_HOLDER.remove();
        }
    }

    private void construirSheetLotesGeneral(XSSFWorkbook wb, List<LoteCerveza> lotes,
                                             String estilo, String fase,
                                             LocalDate desde, LocalDate hasta,
                                             String brandName, Pal pal) {
        Sheet sheet = wb.createSheet(t("xls.sheet.lotes"));

        XSSFCellStyle stTitulo  = estiloTitulo(wb, pal);
        XSSFCellStyle stPeriodo = estiloPeriodo(wb, pal);
        XSSFCellStyle stResumen = estiloResumen(wb, pal);
        XSSFCellStyle stHeader  = estiloHeader(wb, pal);
        XSSFCellStyle stDato    = estiloDato(wb, pal, false);
        XSSFCellStyle stDatoAlt = estiloDato(wb, pal, true);
        XSSFCellStyle stNum     = estiloNumero(wb, pal, false);
        XSSFCellStyle stNumAlt  = estiloNumero(wb, pal, true);

        int[] widths = {12, 18, 12, 8, 20, 18, 14, 30, 28, 22, 22, 10, 8, 12, 14, 12};
        for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);

        int r = 0;
        Row fT = sheet.createRow(r++);
        fT.setHeight((short) (20 * 20));
        Cell cT = fT.createCell(0);
        cT.setCellValue(tf("xls.titulo.lotes", brandName));
        cT.setCellStyle(stTitulo);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, widths.length - 1));

        String desdeStr = desde != null ? desde.format(FMT_FECHA) : "—";
        String hastaStr = hasta != null ? hasta.format(FMT_FECHA) : "—";
        StringBuilder filtroSb = new StringBuilder(tf("xls.filtro.periodo", desdeStr, hastaStr));
        if (estilo != null && !estilo.isBlank()) filtroSb.append("   ").append(t("xls.header.estilo")).append(": ").append(estilo);
        if (fase   != null && !fase.isBlank())   filtroSb.append("   ").append(tf("xls.filtro.fase", fase));
        Row fF = sheet.createRow(r++);
        Cell cF = fF.createCell(0);
        cF.setCellValue(filtroSb.toString());
        cF.setCellStyle(stPeriodo);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, widths.length - 1));

        r++;
        long completados = lotes.stream().filter(LoteCerveza::isCompletado).count();
        Row fRes = sheet.createRow(r++);
        celda(fRes, 0, tf("xls.resumen.total_lotes", lotes.size()), stResumen);
        celda(fRes, 3, tf("xls.resumen.completados", completados,
                lotes.isEmpty() ? 0 : completados * 100 / lotes.size()), stResumen);
        r++;

        Row fH = sheet.createRow(r++);
        fH.setHeight((short) (16 * 20));
        String[] headers = {
            t("xls.header.codigo"),     t("xls.header.estilo"),
            t("xls.header.elaboracion"),t("xls.header.sesiones"),
            t("xls.header.receta"),     t("xls.header.fermentador"),
            t("xls.header.fase"),       t("xls.header.maltas"),
            t("xls.header.lupulos"),    t("xls.header.levadura"),
            t("xls.header.clarificante"),t("xls.header.agua"),
            t("xls.header.ph"),         t("xls.header.litros"),
            t("xls.header.costo_total"),t("xls.header.costo_litro")
        };
        for (int i = 0; i < headers.length; i++) celda(fH, i, headers[i], stHeader);

        for (int i = 0; i < lotes.size(); i++) {
            LoteCerveza l = lotes.get(i);
            boolean alt = i % 2 != 0;
            Row fila = sheet.createRow(r++);
            XSSFCellStyle sD = alt ? stDatoAlt : stDato;
            XSSFCellStyle sN = alt ? stNumAlt  : stNum;

            BigDecimal aguaTotal = l.getTotalAguaElaboraciones();

            celda   (fila, 0,  l.getCodigoLote(), sD);
            celda   (fila, 1,  l.getEstilo() != null ? l.getEstilo() : "", sD);
            celda   (fila, 2,  l.getFechaElaboracion() != null ? l.getFechaElaboracion().format(FMT_FECHA) : "", sD);
            celdaNum(fila, 3,  l.getNumeroElaboraciones() != null ? l.getNumeroElaboraciones().doubleValue() : 1.0, sN);
            celda   (fila, 4,  l.getReceta() != null ? l.getReceta().getNombre() : "", sD);
            celda   (fila, 5,  l.getEquipoFermentador() != null ? l.getEquipoFermentador().getNombre() : "", sD);
            celda   (fila, 6,  l.isCompletado() ? t("xls.text.completado") : l.getFaseActual(), sD);
            celda   (fila, 7,  joinIngredientes(l.getMaltas()), sD);
            celda   (fila, 8,  joinIngredientes(l.getLupulos()), sD);
            celda   (fila, 9,  joinIngredientes(l.getLevaduras()), sD);
            celda   (fila, 10, joinIngredientes(l.getClarificantes()), sD);
            celdaNum(fila, 11, aguaTotal.compareTo(BigDecimal.ZERO) > 0 ? toDouble(aguaTotal) : null, sN);
            celdaNum(fila, 12, toDouble(l.getPhAgua()), sN);
            celdaNum(fila, 13, toDouble(l.getLitrosFinales()), sN);
            celdaNum(fila, 14, toDouble(l.getCostoTotal()), sN);
            celdaNum(fila, 15, toDouble(l.getCostoPorLitro()), sN);
        }

        if (!lotes.isEmpty()) {
            sheet.setAutoFilter(new CellRangeAddress(5, r - 1, 0, headers.length - 1));
        }
    }

    private void construirSheetLotesFase(XSSFWorkbook wb, List<LoteCerveza> lotes,
                                          String brandName, Pal pal,
                                          String sheetName, String titulo, String fase) {
        Sheet sheet = wb.createSheet(sheetName);

        XSSFCellStyle stTitulo  = estiloTitulo(wb, pal);
        XSSFCellStyle stHeader  = estiloHeader(wb, pal);
        XSSFCellStyle stDato    = estiloDato(wb, pal, false);
        XSSFCellStyle stDatoAlt = estiloDato(wb, pal, true);
        XSSFCellStyle stNum     = estiloNumero(wb, pal, false);
        XSSFCellStyle stNumAlt  = estiloNumero(wb, pal, true);

        int[] widths = {12, 18, 12, 12, 12, 14, 12};
        for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);

        int r = 0;
        Row fT = sheet.createRow(r++);
        fT.setHeight((short) (20 * 20));
        Cell cT = fT.createCell(0);
        cT.setCellValue(titulo);
        cT.setCellStyle(stTitulo);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, widths.length - 1));
        r++;

        Row fH = sheet.createRow(r++);
        fH.setHeight((short) (16 * 20));
        celda(fH, 0, t("xls.header.codigo"),      stHeader);
        celda(fH, 1, t("xls.header.estilo"),       stHeader);
        celda(fH, 2, t("xls.header.estado"),       stHeader);
        celda(fH, 3, t("xls.header.fecha_inicio"), stHeader);
        celda(fH, 4, t("xls.header.fin_ideal"),    stHeader);
        celda(fH, 5, t("xls.header.temperatura"),  stHeader);
        celda(fH, 6, t("xls.header.fin_real"),     stHeader);

        for (int i = 0; i < lotes.size(); i++) {
            LoteCerveza l = lotes.get(i);
            boolean alt = i % 2 != 0;
            Row fila = sheet.createRow(r++);
            XSSFCellStyle sD = alt ? stDatoAlt : stDato;
            XSSFCellStyle sN = alt ? stNumAlt  : stNum;

            LocalDate inicio, finIdeal, finReal;
            BigDecimal temp;
            switch (fase) {
                case "FERM"  -> { inicio = l.getFermFechaInicial();  finIdeal = l.getFermFechaFinalIdeal();  temp = l.getFermTemperatura();  finReal = l.getFermFechaFinal();  }
                case "ACOND" -> { inicio = l.getAcondFechaInicial(); finIdeal = l.getAcondFechaFinalIdeal(); temp = l.getAcondTemperatura(); finReal = l.getAcondFechaFinal(); }
                default      -> { inicio = l.getMadurFechaInicial(); finIdeal = l.getMadurFechaFinalIdeal(); temp = l.getMadurTemperatura(); finReal = l.getMadurFechaFinal(); }
            }

            celda   (fila, 0, l.getCodigoLote(), sD);
            celda   (fila, 1, l.getEstilo() != null ? l.getEstilo() : "", sD);
            celda   (fila, 2, faseEstado(inicio, finReal), sD);
            celda   (fila, 3, inicio  != null ? inicio.format(FMT_FECHA)  : "", sD);
            celda   (fila, 4, finIdeal != null ? finIdeal.format(FMT_FECHA) : "", sD);
            celdaNum(fila, 5, toDouble(temp), sN);
            celda   (fila, 6, finReal != null ? finReal.format(FMT_FECHA) : "", sD);
        }

        if (!lotes.isEmpty()) {
            sheet.setAutoFilter(new CellRangeAddress(2, r - 1, 0, 6));
        }
    }

    private void construirSheetLotesCarbon(XSSFWorkbook wb, List<LoteCerveza> lotes,
                                            String brandName, Pal pal) {
        Sheet sheet = wb.createSheet(t("xls.sheet.lotes.carbon"));

        XSSFCellStyle stTitulo  = estiloTitulo(wb, pal);
        XSSFCellStyle stHeader  = estiloHeader(wb, pal);
        XSSFCellStyle stDato    = estiloDato(wb, pal, false);
        XSSFCellStyle stDatoAlt = estiloDato(wb, pal, true);
        XSSFCellStyle stNum     = estiloNumero(wb, pal, false);
        XSSFCellStyle stNumAlt  = estiloNumero(wb, pal, true);

        int[] widths = {12, 18, 12, 12, 12, 14, 12, 14, 12, 12, 14, 12, 12, 12, 14, 16, 20};
        for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);

        int r = 0;
        Row fT = sheet.createRow(r++);
        fT.setHeight((short) (20 * 20));
        Cell cT = fT.createCell(0);
        cT.setCellValue(tf("xls.titulo.lotes", brandName));
        cT.setCellStyle(stTitulo);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, widths.length - 1));
        r++;

        Row fH = sheet.createRow(r++);
        fH.setHeight((short) (16 * 20));
        String[] headers = {
            t("xls.header.codigo"),       t("xls.header.estilo"),
            t("xls.header.estado"),       t("xls.header.fecha_inicio"),
            t("xls.header.fin_ideal"),    t("xls.header.temperatura"),
            t("xls.header.fin_real"),     t("xls.header.metodo_carb"),
            t("xls.header.co2_obj"),      t("xls.header.co2_real"),
            t("xls.header.azucar_tipo"),  t("xls.header.azucar_gr"),
            t("xls.header.presion_psi"),  t("xls.header.tiempo_horas"),
            t("xls.header.tecnica"),      t("xls.header.validacion_carb"),
            t("xls.header.destino_empaque")
        };
        for (int i = 0; i < headers.length; i++) celda(fH, i, headers[i], stHeader);

        for (int i = 0; i < lotes.size(); i++) {
            LoteCerveza l = lotes.get(i);
            boolean alt = i % 2 != 0;
            Row fila = sheet.createRow(r++);
            XSSFCellStyle sD = alt ? stDatoAlt : stDato;
            XSSFCellStyle sN = alt ? stNumAlt  : stNum;

            celda   (fila, 0,  l.getCodigoLote(), sD);
            celda   (fila, 1,  l.getEstilo() != null ? l.getEstilo() : "", sD);
            celda   (fila, 2,  faseEstado(l.getCarbFechaInicial(), l.getCarbFechaFinal()), sD);
            celda   (fila, 3,  l.getCarbFechaInicial()  != null ? l.getCarbFechaInicial().format(FMT_FECHA)  : "", sD);
            celda   (fila, 4,  l.getCarbFechaFinalIdeal() != null ? l.getCarbFechaFinalIdeal().format(FMT_FECHA) : "", sD);
            celdaNum(fila, 5,  toDouble(l.getCarbTemperatura()), sN);
            celda   (fila, 6,  l.getCarbFechaFinal() != null ? l.getCarbFechaFinal().format(FMT_FECHA) : "", sD);
            celda   (fila, 7,  formatCarbMetodo(l.getCarbMetodo()), sD);
            celdaNum(fila, 8,  toDouble(l.getCarbCo2Objetivo()), sN);
            celdaNum(fila, 9,  toDouble(l.getCarbCo2Real()), sN);
            celda   (fila, 10, l.getCarbAzucarTipo() != null ? l.getCarbAzucarTipo() : "", sD);
            celdaNum(fila, 11, toDouble(l.getCarbAzucarGramos()), sN);
            celdaNum(fila, 12, toDouble(l.getCarbPresionPsi()), sN);
            celdaNum(fila, 13, l.getCarbTiempoHoras() != null ? l.getCarbTiempoHoras().doubleValue() : null, sN);
            celda   (fila, 14, l.getCarbTecnica() != null ? l.getCarbTecnica() : "", sD);
            celda   (fila, 15, l.getCarbValidacion() != null ? l.getCarbValidacion() : "", sD);
            celda   (fila, 16, l.getCarbDestino() != null ? l.getCarbDestino() : "", sD);
        }

        if (!lotes.isEmpty()) {
            sheet.setAutoFilter(new CellRangeAddress(2, r - 1, 0, headers.length - 1));
        }
    }

    private void construirSheetLotesDensidad(XSSFWorkbook wb, List<LoteCerveza> lotes,
                                              String brandName, Pal pal) {
        Sheet sheet = wb.createSheet(t("xls.sheet.lotes.density"));

        XSSFCellStyle stTitulo  = estiloTitulo(wb, pal);
        XSSFCellStyle stHeader  = estiloHeader(wb, pal);
        XSSFCellStyle stDato    = estiloDato(wb, pal, false);
        XSSFCellStyle stDatoAlt = estiloDato(wb, pal, true);
        XSSFCellStyle stNum     = estiloNumero(wb, pal, false);
        XSSFCellStyle stNumAlt  = estiloNumero(wb, pal, true);

        int[] widths = {12, 18, 10, 10, 10, 14, 14, 14, 35, 35};
        for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);

        int r = 0;
        Row fT = sheet.createRow(r++);
        fT.setHeight((short) (20 * 20));
        Cell cT = fT.createCell(0);
        cT.setCellValue(tf("xls.titulo.lotes", brandName));
        cT.setCellStyle(stTitulo);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, widths.length - 1));
        r++;

        Row fH = sheet.createRow(r++);
        fH.setHeight((short) (16 * 20));
        String[] headers = {
            t("xls.header.codigo"),         t("xls.header.estilo"),
            t("xls.header.og"),             t("xls.header.fg"),
            t("xls.header.abv_pct"),        t("xls.header.atenuacion_pct"),
            t("xls.header.eficiencia_pct"), t("xls.header.fecha"),
            t("xls.header.observaciones"),  t("xls.header.notas_cata")
        };
        for (int i = 0; i < headers.length; i++) celda(fH, i, headers[i], stHeader);

        for (int i = 0; i < lotes.size(); i++) {
            LoteCerveza l = lotes.get(i);
            boolean alt = i % 2 != 0;
            Row fila = sheet.createRow(r++);
            XSSFCellStyle sD = alt ? stDatoAlt : stDato;
            XSSFCellStyle sN = alt ? stNumAlt  : stNum;

            celda   (fila, 0, l.getCodigoLote(), sD);
            celda   (fila, 1, l.getEstilo() != null ? l.getEstilo() : "", sD);
            celdaNum(fila, 2, l.getDensidadInicial() != null ? l.getDensidadInicial().doubleValue() : null, sN);
            celdaNum(fila, 3, l.getDensidadFinal() != null ? l.getDensidadFinal().doubleValue() : null, sN);
            celdaNum(fila, 4, toDouble(l.getAbv()), sN);
            celdaNum(fila, 5, toDouble(l.getAtenuacionAparente()), sN);
            celdaNum(fila, 6, toDouble(l.getEficienciaMacerado()), sN);
            celda   (fila, 7, l.getDensidadFinalFecha() != null ? l.getDensidadFinalFecha().format(FMT_FECHA) : "", sD);
            celda   (fila, 8, l.getObservaciones() != null ? l.getObservaciones() : "", sD);
            celda   (fila, 9, l.getNotasCata() != null ? l.getNotasCata() : "", sD);
        }

        if (!lotes.isEmpty()) {
            sheet.setAutoFilter(new CellRangeAddress(2, r - 1, 0, headers.length - 1));
        }
    }

    private String joinIngredientes(List<Ingrediente> items) {
        if (items == null || items.isEmpty()) return "";
        return items.stream()
                .map(i -> {
                    String disp = displayIngrediente(i.getCantidad());
                    return disp.isEmpty() ? i.getNombre() : disp + " " + i.getNombre();
                })
                .collect(Collectors.joining(", "));
    }

    private String displayIngrediente(String cantidadRaw) {
        if (cantidadRaw == null || cantidadRaw.isBlank()) return "";
        String[] parts = cantidadRaw.trim().split("\\s+", 2);
        if (parts.length < 2) return cantidadRaw;
        try {
            BigDecimal val = new BigDecimal(parts[0]);
            return UnidadUtils.displayTexto(val, parts[1]);
        } catch (NumberFormatException e) {
            return cantidadRaw;
        }
    }

    private String faseEstado(LocalDate inicio, LocalDate fin) {
        if (inicio == null) return t("xls.text.pendiente");
        if (fin   == null) return t("xls.text.en_proceso");
        return t("xls.text.completado");
    }

    private String formatCarbMetodo(String metodo) {
        if (metodo == null) return "";
        return switch (metodo) {
            case "NATURAL"  -> t("xls.text.carb_natural");
            case "FORZADA"  -> t("xls.text.carb_forzada");
            default         -> metodo;
        };
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
