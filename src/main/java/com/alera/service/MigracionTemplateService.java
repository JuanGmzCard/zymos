package com.alera.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class MigracionTemplateService {

    // ── Public API ────────────────────────────────────────────────────────────

    public byte[] plantillaAlmacen() throws IOException {
        XSSFWorkbook wb = new XSSFWorkbook();
        Estilos es = estilos(wb);
        hojaInstrucciones(wb, es, "Almacén",
            new String[][]{
                {"*", "Campo obligatorio"},
                {"tipo", "MALTA | LUPULO | LEVADURA | CLARIFICANTE | AGENTE_CARBONATACION | AGUA | QUIMICO | ENVASE | OTRO"},
                {"unidad", "gr | kg | mL | L | gal | und"},
                {"fecha_vencimiento", "Formato: YYYY-MM-DD  (ej: 2025-12-31)"},
                {"cantidad / stock_minimo", "Número decimal (ej: 5000  o  2.5)"}
            });
        hojaInsumos(wb, es);
        return bytes(wb);
    }

    public byte[] plantillaEquipos() throws IOException {
        XSSFWorkbook wb = new XSSFWorkbook();
        Estilos es = estilos(wb);
        hojaInstrucciones(wb, es, "Equipos",
            new String[][]{
                {"*", "Campo obligatorio"},
                {"tipo", "FERMENTADOR | OLLA_MACERADO | OLLA_HERVOR | ENFRIADOR | BOMBA | FILTRO | MEDIDOR_PH | DENSIMETRO | BASCULA | COMPRESOR | OTRO"},
                {"estado", "OPERATIVO | MANTENIMIENTO | INACTIVO  (default: OPERATIVO)"},
                {"fecha_adquisicion / fecha_proximo_mant", "Formato: YYYY-MM-DD"},
                {"capacidad", "Número decimal en la unidad indicada (ej: 300  para 300 L)"}
            });
        hojaEquipos(wb, es);
        return bytes(wb);
    }

    public byte[] plantillaComercial() throws IOException {
        XSSFWorkbook wb = new XSSFWorkbook();
        Estilos es = estilos(wb);
        hojaInstrucciones(wb, es, "Comercial",
            new String[][]{
                {"*", "Campo obligatorio"},
                {"Orden de llenado", "1) Proveedores  →  2) Facturas  →  3) Factura_Items"},
                {"proveedor_nombre (Facturas)", "Debe coincidir exactamente con el nombre en la hoja Proveedores"},
                {"numero_factura (Factura_Items)", "Debe coincidir con el numero_factura de la hoja Facturas"},
                {"tipo_item", "INSUMO | EQUIPO"},
                {"estado (Facturas)", "RECIBIDA | VERIFICADA | PAGADA  (default: RECIBIDA)"},
                {"fecha_factura", "Formato: YYYY-MM-DD"},
                {"valores monetarios", "Número decimal sin separadores de miles (ej: 85000.50)"},
                {"iva_pct / descuento_pct", "Porcentaje como número (ej: 19 para 19%)"}
            });
        hojaProveedores(wb, es);
        hojaFacturas(wb, es);
        hojaFacturaItems(wb, es);
        return bytes(wb);
    }

    public byte[] plantillaProduccion() throws IOException {
        XSSFWorkbook wb = new XSSFWorkbook();
        Estilos es = estilos(wb);
        hojaInstrucciones(wb, es, "Producción / Trazabilidad",
            new String[][]{
                {"*", "Campo obligatorio"},
                {"Orden de llenado", "1) Recetas  →  2) Receta_Ingredientes  →  3) Receta_Escalones  →  4) Receta_Adiciones  →  5) Lotes  →  6) Lote_Ingredientes"},
                {"nombre_receta (hojas 2-4)", "Debe coincidir exactamente con el nombre en la hoja Recetas"},
                {"codigo_lote (hoja 6)", "Debe coincidir con el codigo_lote de la hoja Lotes"},
                {"tipo (ingredientes)", "MALTA | LUPULO | LEVADURA | CLARIFICANTE"},
                {"cantidad_con_unidad", "Número + espacio + unidad  (ej: 5000 gr  |  4.5 kg  |  11.5 gr)"},
                {"og_objetivo / fg_objetivo", "Densidad en formato XXXX  (ej: 1058 para 1.058)"},
                {"densidad_inicial / densidad_final", "Densidad en formato XXXX  (ej: 1056 para 1.056)"},
                {"fecha_elaboracion", "Formato: YYYY-MM-DD"},
                {"activa (Recetas)", "TRUE o FALSE  (default: TRUE)"},
                {"minutos_restantes (Adiciones Hervor)", "0 = flameout/apagado"},
                {"carb_metodo", "NATURAL (priming con azúcar) | FORZADA (inyección CO₂) | vacío = sin registrar"},
                {"carb_azucar_tipo", "dextrosa | sacarosa | extracto | miel  (solo si carb_metodo = NATURAL)"},
                {"carb_tecnica", "PIEDRA | PRESION_FIJA  (solo si carb_metodo = FORZADA)"},
                {"carb_validacion", "ADECUADA | RETENCION_CORRECTA | SOBRECARBONATADA | BAJA_CARBONATACION"},
                {"carb_co2_objetivo / carb_co2_real", "Volúmenes de CO₂ en formato decimal  (ej: 2.5)"}
            });
        hojaRecetas(wb, es);
        hojaRecetaIngredientes(wb, es);
        hojaRecetaEscalones(wb, es);
        hojaRecetaAdicionesHervor(wb, es);
        hojaLotes(wb, es);
        hojaLoteIngredientes(wb, es);
        return bytes(wb);
    }

    // ── Sheet builders ────────────────────────────────────────────────────────

    private void hojaInsumos(XSSFWorkbook wb, Estilos es) {
        XSSFSheet sh = wb.createSheet("Insumos");
        wb.setSheetOrder("Insumos", 1);
        String[][] cols = {
            {"nombre",            "req"},
            {"tipo",              "req"},
            {"cantidad",          "opt"},
            {"unidad",            "opt"},
            {"stock_minimo",      "opt"},
            {"proveedor",         "opt"},
            {"fecha_vencimiento", "opt"},
            {"observaciones",     "opt"}
        };
        cabecera(sh, es, cols);
        ejemplo(sh, es, new Object[]{"Pale Ale 2-Row", "MALTA", 5000, "gr", 500, "MaltaCo SA", "2025-12-31", "Malta base clara"});
        dropdown(sh, 1, 9999, 1, "MALTA","LUPULO","LEVADURA","CLARIFICANTE","AGENTE_CARBONATACION","AGUA","QUIMICO","ENVASE","OTRO");
        dropdown(sh, 1, 9999, 3, "gr","kg","mL","L","gal","und");
        anchos(sh, 220, 160, 120, 100, 130, 200, 170, 300);
    }

    private void hojaEquipos(XSSFWorkbook wb, Estilos es) {
        XSSFSheet sh = wb.createSheet("Equipos");
        wb.setSheetOrder("Equipos", 1);
        String[][] cols = {
            {"nombre",                "req"},
            {"tipo",                  "req"},
            {"estado",                "opt"},
            {"capacidad",             "opt"},
            {"unidad_capacidad",      "opt"},
            {"fecha_adquisicion",     "opt"},
            {"fecha_proximo_mant",    "opt"},
            {"observaciones",         "opt"}
        };
        cabecera(sh, es, cols);
        ejemplo(sh, es, new Object[]{"Fermentador 1","FERMENTADOR","OPERATIVO",300,"L","2023-01-15","","Estado ideal"});
        dropdown(sh, 1, 9999, 1, "FERMENTADOR","OLLA_MACERADO","OLLA_HERVOR","ENFRIADOR","BOMBA","FILTRO","MEDIDOR_PH","DENSIMETRO","BASCULA","COMPRESOR","OTRO");
        dropdown(sh, 1, 9999, 2, "OPERATIVO","MANTENIMIENTO","INACTIVO");
        dropdown(sh, 1, 9999, 4, "L","mL","gal","und");
        anchos(sh, 220, 180, 150, 120, 160, 170, 180, 300);
    }

    private void hojaProveedores(XSSFWorkbook wb, Estilos es) {
        XSSFSheet sh = wb.createSheet("Proveedores");
        String[][] cols = {
            {"nombre",    "req"},
            {"nit",       "opt"},
            {"telefono",  "opt"},
            {"email",     "opt"},
            {"direccion", "opt"}
        };
        cabecera(sh, es, cols);
        ejemplo(sh, es, new Object[]{"MaltaCo SA","900123456-1","3001234567","ventas@maltaco.com","Calle 123 #45-67, Bogotá"});
        anchos(sh, 220, 160, 150, 250, 300);
    }

    private void hojaFacturas(XSSFWorkbook wb, Estilos es) {
        XSSFSheet sh = wb.createSheet("Facturas");
        String[][] cols = {
            {"numero_factura",  "opt"},
            {"proveedor_nombre","req"},
            {"fecha_factura",   "req"},
            {"descripcion",     "opt"},
            {"costo_envio",     "opt"},
            {"estado",          "opt"}
        };
        cabecera(sh, es, cols);
        ejemplo(sh, es, new Object[]{"FAC-2024-001","MaltaCo SA","2024-01-15","Compra insumos enero",15000,"PAGADA"});
        dropdown(sh, 1, 9999, 5, "RECIBIDA","VERIFICADA","PAGADA");
        anchos(sh, 180, 220, 160, 300, 140, 130);
    }

    private void hojaFacturaItems(XSSFWorkbook wb, Estilos es) {
        XSSFSheet sh = wb.createSheet("Factura_Items");
        String[][] cols = {
            {"numero_factura",  "req"},
            {"tipo_item",       "req"},
            {"nombre",          "req"},
            {"tipo_insumo",     "opt"},
            {"tipo_equipo",     "opt"},
            {"cantidad",        "req"},
            {"unidad",          "opt"},
            {"valor_unitario",  "req"},
            {"descuento_pct",   "opt"},
            {"iva_pct",         "opt"}
        };
        cabecera(sh, es, cols);
        ejemplo(sh, es, new Object[]{"FAC-2024-001","INSUMO","Pale Ale 2-Row","MALTA","",25,"kg",8500,0,19});
        dropdown(sh, 1, 9999, 1, "INSUMO","EQUIPO");
        dropdown(sh, 1, 9999, 3, "MALTA","LUPULO","LEVADURA","CLARIFICANTE","AGUA","QUIMICO","ENVASE","OTRO");
        dropdown(sh, 1, 9999, 4, "FERMENTADOR","OLLA_MACERADO","OLLA_HERVOR","ENFRIADOR","BOMBA","FILTRO","MEDIDOR_PH","DENSIMETRO","BASCULA","COMPRESOR","OTRO");
        dropdown(sh, 1, 9999, 6, "gr","kg","mL","L","gal","und");
        anchos(sh, 180, 130, 220, 160, 180, 110, 100, 150, 130, 110);
    }

    private void hojaRecetas(XSSFWorkbook wb, Estilos es) {
        XSSFSheet sh = wb.createSheet("Recetas");
        String[][] cols = {
            {"nombre",             "req"},
            {"estilo",             "req"},
            {"descripcion",        "opt"},
            {"activa",             "opt"},
            {"volumen_base_L",     "opt"},
            {"hervor_minutos",     "opt"},
            {"og_objetivo",        "opt"},
            {"fg_objetivo",        "opt"},
            {"agua_macerado",      "opt"},
            {"unidad_agua_mac",    "opt"},
            {"agua_sparge",        "opt"},
            {"unidad_agua_sp",     "opt"},
            {"ph_agua",            "opt"},
            {"notas",              "opt"}
        };
        cabecera(sh, es, cols);
        ejemplo(sh, es, new Object[]{"IPA Clásica","IPA","Receta base IPA americana","TRUE",20,60,1058,1012,18,"L",8,"L",5.2,"Receta optimizada"});
        dropdown(sh, 1, 9999, 3, "TRUE","FALSE");
        dropdown(sh, 1, 9999, 9, "L","mL","gal");
        dropdown(sh, 1, 9999, 11, "L","mL","gal");
        anchos(sh, 200, 150, 280, 90, 130, 130, 130, 130, 140, 140, 140, 140, 110, 280);
    }

    private void hojaRecetaIngredientes(XSSFWorkbook wb, Estilos es) {
        XSSFSheet sh = wb.createSheet("Receta_Ingredientes");
        String[][] cols = {
            {"nombre_receta",       "req"},
            {"tipo",                "req"},
            {"nombre_ingrediente",  "req"},
            {"cantidad_con_unidad", "req"}
        };
        cabecera(sh, es, cols);
        ejemplo(sh, es, new Object[]{"IPA Clásica","MALTA","Pale Ale 2-Row","5000 gr"});
        Row ej2 = sh.createRow(3);
        fila(ej2, es.example(), new Object[]{"IPA Clásica","LUPULO","Cascade","50 gr"});
        Row ej3 = sh.createRow(4);
        fila(ej3, es.example(), new Object[]{"IPA Clásica","LEVADURA","US-05","11.5 gr"});
        dropdown(sh, 1, 9999, 1, "MALTA","LUPULO","LEVADURA","CLARIFICANTE");
        anchos(sh, 200, 140, 220, 180);
    }

    private void hojaRecetaEscalones(XSSFWorkbook wb, Estilos es) {
        XSSFSheet sh = wb.createSheet("Receta_Escalones");
        String[][] cols = {
            {"nombre_receta",    "req"},
            {"nombre_escalon",   "req"},
            {"temperatura_c",    "req"},
            {"duracion_minutos", "req"},
            {"orden",            "opt"}
        };
        cabecera(sh, es, cols);
        ejemplo(sh, es, new Object[]{"IPA Clásica","Sacarificación",67.5,60,1});
        Row ej2 = sh.createRow(3);
        fila(ej2, es.example(), new Object[]{"IPA Clásica","Mash-out",76,10,2});
        anchos(sh, 200, 200, 150, 170, 90);
    }

    private void hojaRecetaAdicionesHervor(XSSFWorkbook wb, Estilos es) {
        XSSFSheet sh = wb.createSheet("Receta_Adiciones");
        String[][] cols = {
            {"nombre_receta",      "req"},
            {"nombre",             "req"},
            {"minutos_restantes",  "req"},
            {"cantidad",           "req"},
            {"unidad",             "req"},
            {"orden",              "opt"}
        };
        cabecera(sh, es, cols);
        ejemplo(sh, es, new Object[]{"IPA Clásica","Cascade",60,30,"gr",1});
        Row ej2 = sh.createRow(3);
        fila(ej2, es.example(), new Object[]{"IPA Clásica","Cascade",0,10,"gr",2});
        dropdown(sh, 1, 9999, 4, "gr","kg","mL","L","und");
        anchos(sh, 200, 180, 170, 110, 100, 90);
    }

    private void hojaLotes(XSSFWorkbook wb, Estilos es) {
        XSSFSheet sh = wb.createSheet("Lotes");
        String[][] cols = {
            {"codigo_lote",        "req"},
            {"estilo",             "req"},
            {"fecha_elaboracion",  "req"},
            {"litros_finales",     "opt"},
            {"densidad_inicial",   "opt"},
            {"densidad_final",     "opt"},
            {"agua_utilizada",     "opt"},
            {"ph_agua",            "opt"},
            {"clarificante",       "opt"},
            {"observaciones",      "opt"},
            {"notas_cata",         "opt"},
            {"nombre_receta",      "opt"},
            {"carb_metodo",        "opt"},
            {"carb_co2_objetivo",  "opt"},
            {"carb_co2_real",      "opt"},
            {"carb_azucar_tipo",   "opt"},
            {"carb_azucar_gramos", "opt"},
            {"carb_presion_psi",   "opt"},
            {"carb_tiempo_horas",  "opt"},
            {"carb_tecnica",       "opt"},
            {"carb_validacion",    "opt"},
            {"carb_destino",       "opt"}
        };
        cabecera(sh, es, cols);
        ejemplo(sh, es, new Object[]{
            "IPA-001","IPA","2024-01-20",19.5,1058,1012,25,5.3,"","","","IPA Clásica",
            "NATURAL",2.5,2.4,"dextrosa",120.5,"","","","ADECUADA","Botella 330mL"});
        dropdown(sh, 1, 9999, 12, "NATURAL", "FORZADA");
        dropdown(sh, 1, 9999, 15, "dextrosa", "sacarosa", "extracto", "miel");
        dropdown(sh, 1, 9999, 19, "PIEDRA", "PRESION_FIJA");
        dropdown(sh, 1, 9999, 20, "ADECUADA", "RETENCION_CORRECTA", "SOBRECARBONATADA", "BAJA_CARBONATACION");
        anchos(sh, 140, 140, 170, 140, 160, 150, 140, 110, 180, 280, 280, 200,
               150, 160, 120, 160, 170, 140, 160, 150, 200, 280);
    }

    private void hojaLoteIngredientes(XSSFWorkbook wb, Estilos es) {
        XSSFSheet sh = wb.createSheet("Lote_Ingredientes");
        String[][] cols = {
            {"codigo_lote",         "req"},
            {"tipo",                "req"},
            {"nombre",              "req"},
            {"cantidad_con_unidad", "req"}
        };
        cabecera(sh, es, cols);
        ejemplo(sh, es, new Object[]{"IPA-001","MALTA","Pale Ale 2-Row","5000 gr"});
        Row ej2 = sh.createRow(3);
        fila(ej2, es.example(), new Object[]{"IPA-001","LUPULO","Cascade","50 gr"});
        dropdown(sh, 1, 9999, 1, "MALTA","LUPULO","LEVADURA","CLARIFICANTE");
        anchos(sh, 160, 140, 220, 180);
    }

    private void hojaInstrucciones(XSSFWorkbook wb, Estilos es, String modulo, String[][] reglas) {
        XSSFSheet sh = wb.createSheet("Instrucciones");
        wb.setSheetOrder("Instrucciones", 0);

        Row titulo = sh.createRow(0);
        Cell t = titulo.createCell(0);
        t.setCellValue("Plantilla de Migración — Módulo: " + modulo);
        t.setCellStyle(es.instrTitle());
        titulo.setHeightInPoints(24);
        sh.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 3));

        Row subtitulo = sh.createRow(2);
        Cell sub = subtitulo.createCell(0);
        sub.setCellValue("Reglas de llenado:");
        sub.setCellStyle(es.req());

        for (int i = 0; i < reglas.length; i++) {
            Row r = sh.createRow(3 + i);
            Cell clave = r.createCell(0);
            clave.setCellValue(reglas[i][0]);
            clave.setCellStyle(es.opt());
            Cell valor = r.createCell(1);
            valor.setCellValue(reglas[i][1]);
            valor.setCellStyle(es.instrBody());
        }
        sh.setColumnWidth(0, 60 * 256);
        sh.setColumnWidth(1, 80 * 256);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private record Estilos(
        XSSFCellStyle req,
        XSSFCellStyle opt,
        XSSFCellStyle example,
        XSSFCellStyle data,
        XSSFCellStyle instrTitle,
        XSSFCellStyle instrBody
    ) {}

    private Estilos estilos(XSSFWorkbook wb) {
        // Required header: verde oscuro + texto blanco + negrita
        XSSFFont fntWhiteBold = (XSSFFont) wb.createFont();
        fntWhiteBold.setBold(true);
        fntWhiteBold.setColor(IndexedColors.WHITE.getIndex());

        XSSFFont fntDark = (XSSFFont) wb.createFont();
        fntDark.setColor(IndexedColors.BLACK1.getIndex());

        XSSFFont fntGray = (XSSFFont) wb.createFont();
        fntGray.setItalic(true);
        fntGray.setColor(IndexedColors.GREY_50_PERCENT.getIndex());

        XSSFFont fntTitleBold = (XSSFFont) wb.createFont();
        fntTitleBold.setBold(true);
        fntTitleBold.setFontHeightInPoints((short) 13);
        fntTitleBold.setColor(IndexedColors.WHITE.getIndex());

        XSSFCellStyle req = wb.createCellStyle();
        req.setFillForegroundColor(new XSSFColor(new byte[]{(byte)54,(byte)67,(byte)24}, null));
        req.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        req.setFont(fntWhiteBold);
        req.setAlignment(HorizontalAlignment.CENTER);
        req.setBorderBottom(BorderStyle.THIN);
        req.setBottomBorderColor(IndexedColors.WHITE.getIndex());
        req.setWrapText(false);

        XSSFCellStyle opt = wb.createCellStyle();
        opt.setFillForegroundColor(new XSSFColor(new byte[]{(byte)108,(byte)117,(byte)125}, null));
        opt.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        opt.setFont(fntWhiteBold);
        opt.setAlignment(HorizontalAlignment.CENTER);
        opt.setBorderBottom(BorderStyle.THIN);
        opt.setBottomBorderColor(IndexedColors.WHITE.getIndex());
        opt.setWrapText(false);

        XSSFCellStyle example = wb.createCellStyle();
        example.setFillForegroundColor(new XSSFColor(new byte[]{(byte)248,(byte)249,(byte)250}, null));
        example.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        example.setFont(fntGray);
        example.setBorderBottom(BorderStyle.THIN);
        example.setBottomBorderColor(new XSSFColor(new byte[]{(byte)222,(byte)226,(byte)230}, null));

        XSSFCellStyle data = wb.createCellStyle();
        data.setFont(fntDark);
        data.setBorderBottom(BorderStyle.THIN);
        data.setBottomBorderColor(new XSSFColor(new byte[]{(byte)222,(byte)226,(byte)230}, null));

        XSSFCellStyle instrTitle = wb.createCellStyle();
        instrTitle.setFillForegroundColor(new XSSFColor(new byte[]{(byte)54,(byte)67,(byte)24}, null));
        instrTitle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        instrTitle.setFont(fntTitleBold);
        instrTitle.setAlignment(HorizontalAlignment.CENTER);
        instrTitle.setVerticalAlignment(VerticalAlignment.CENTER);

        XSSFCellStyle instrBody = wb.createCellStyle();
        instrBody.setFont(fntDark);
        instrBody.setWrapText(true);

        return new Estilos(req, opt, example, data, instrTitle, instrBody);
    }

    private void cabecera(XSSFSheet sh, Estilos es, String[][] cols) {
        Row hdr = sh.createRow(0);
        hdr.setHeightInPoints(20);
        for (int i = 0; i < cols.length; i++) {
            Cell c = hdr.createCell(i);
            boolean requerido = "req".equals(cols[i][1]);
            c.setCellValue(requerido ? cols[i][0] + " *" : cols[i][0]);
            c.setCellStyle(requerido ? es.req() : es.opt());
        }

        // Fila de leyenda sobre los colores
        Row leyenda = sh.createRow(1);
        leyenda.setHeightInPoints(14);
        XSSFWorkbook wb = sh.getWorkbook();
        XSSFCellStyle esLeyenda = wb.createCellStyle();
        XSSFFont fnt = (XSSFFont) wb.createFont();
        fnt.setItalic(true);
        fnt.setFontHeightInPoints((short) 9);
        fnt.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        esLeyenda.setFont(fnt);
        Cell lbl = leyenda.createCell(0);
        lbl.setCellValue("Verde * = obligatorio   |   Gris = opcional   |   Fila 3 = ejemplo (puedes borrarla)");
        lbl.setCellStyle(esLeyenda);
        sh.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 0, Math.max(cols.length - 1, 1)));
    }

    private void ejemplo(XSSFSheet sh, Estilos es, Object[] valores) {
        Row r = sh.createRow(2);
        r.setHeightInPoints(16);
        fila(r, es.example(), valores);
    }

    private void fila(Row r, XSSFCellStyle estilo, Object[] valores) {
        for (int i = 0; i < valores.length; i++) {
            Cell c = r.createCell(i);
            c.setCellStyle(estilo);
            if (valores[i] == null || "".equals(valores[i])) continue;
            if (valores[i] instanceof Number n) c.setCellValue(n.doubleValue());
            else c.setCellValue(valores[i].toString());
        }
    }

    private void dropdown(XSSFSheet sh, int primeraDato, int ultimaFila, int col, String... opciones) {
        XSSFDataValidationHelper dvh = new XSSFDataValidationHelper(sh);
        DataValidationConstraint dvc = dvh.createExplicitListConstraint(opciones);
        CellRangeAddressList rango = new CellRangeAddressList(primeraDato + 1, ultimaFila, col, col);
        DataValidation dv = dvh.createValidation(dvc, rango);
        dv.setSuppressDropDownArrow(false);
        dv.setShowErrorBox(true);
        dv.setErrorStyle(DataValidation.ErrorStyle.WARNING);
        dv.createErrorBox("Valor no reconocido", "El sistema intentará mapear el valor. Verifique los valores válidos en la hoja Instrucciones.");
        sh.addValidationData(dv);
    }

    private void anchos(XSSFSheet sh, int... chars) {
        for (int i = 0; i < chars.length; i++) {
            sh.setColumnWidth(i, chars[i] * 37);
        }
    }

    private byte[] bytes(XSSFWorkbook wb) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();
        return out.toByteArray();
    }
}
