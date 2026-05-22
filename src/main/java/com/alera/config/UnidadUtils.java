package com.alera.config;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utilitario centralizado para conversión de unidades.
 * Evita duplicación entre TrazabilidadService y FacturaProveedorService.
 */
public final class UnidadUtils {

    private UnidadUtils() {}

    // ── Peso ─────────────────────────────────────────────────────────
    // Unidad base: gramos (gr)
    // kg → gr ×1000

    // ── Volumen ──────────────────────────────────────────────────────
    // Unidad base: mililitros (mL)
    // L/lt → mL ×1000
    // gal  → mL ×3785.41

    /**
     * Convierte un valor numérico a la unidad base de almacenamiento.
     * Pesos   → gramos  | Volúmenes → mililitros
     *
     * @return BigDecimal en unidad base, o el valor original si la unidad no requiere conversión
     */
    public static BigDecimal convertirAUnidadBase(BigDecimal valor, String unidad) {
        if (valor == null || unidad == null) return valor != null ? valor : BigDecimal.ZERO;
        return switch (unidad.trim().toLowerCase()) {
            case "kg"                       -> valor.multiply(BigDecimal.valueOf(1000));
            case "l", "lt", "litro", "litros" -> valor.multiply(BigDecimal.valueOf(1000));
            case "gal", "galon", "galón"    ->
                    valor.multiply(BigDecimal.valueOf(3785.41)).setScale(2, RoundingMode.HALF_UP);
            default -> valor;
        };
    }

    /**
     * Retorna la unidad base correspondiente a la unidad de entrada.
     * kg → gr | L/lt/gal → mL | resto sin cambio
     */
    public static String unidadBase(String unidad) {
        if (unidad == null) return "gr";
        return switch (unidad.trim().toLowerCase()) {
            case "kg"                         -> "gr";
            case "l", "lt", "litro", "litros" -> "mL";
            case "gal", "galon", "galón"      -> "mL";
            default -> unidad;
        };
    }

    /**
     * Convierte un texto como "5000 gr" o "2 kg" a valor en unidad base.
     */
    public static BigDecimal parsearYConvertir(String cantidadTexto) {
        if (cantidadTexto == null || cantidadTexto.isBlank()) return BigDecimal.ZERO;
        String[] partes = cantidadTexto.trim().split("\\s+", 2);
        try {
            BigDecimal valor = new BigDecimal(partes[0].replace(",", "."));
            String unidad = partes.length > 1 ? partes[1] : "gr";
            return convertirAUnidadBase(valor, unidad);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Formatea valor + unidad como texto normalizado para almacenar en Ingrediente.cantidad.
     * Ej: "5000", "kg" → "5000000 gr"  |  "2", "L" → "2000 mL"
     */
    public static String normalizarParaAlmacenamiento(String cantidadStr, String unidad) {
        if (cantidadStr == null || cantidadStr.isBlank()) return "0 gr";
        try {
            BigDecimal valor = new BigDecimal(cantidadStr.trim().replace(",", "."));
            BigDecimal convertido = convertirAUnidadBase(valor, unidad);
            return convertido.toPlainString() + " " + unidadBase(unidad);
        } catch (NumberFormatException e) {
            return cantidadStr + " " + (unidad != null ? unidad : "gr");
        }
    }
}
