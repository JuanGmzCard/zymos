package com.alera.config;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class TempUtils {

    private TempUtils() {}

    /** Convierte un valor en la unidad del tenant a Celsius para almacenar en BD. */
    public static BigDecimal toStorage(BigDecimal value, String unit) {
        if (value == null) return null;
        if ("F".equals(unit))
            return value.subtract(BigDecimal.valueOf(32))
                    .multiply(BigDecimal.valueOf(5))
                    .divide(BigDecimal.valueOf(9), 2, RoundingMode.HALF_UP);
        return value;
    }

    /** Convierte un valor en Celsius (BD) a la unidad del tenant para mostrar/editar. */
    public static BigDecimal toDisplay(BigDecimal celsius, String unit) {
        if (celsius == null) return null;
        if ("F".equals(unit))
            return celsius.multiply(BigDecimal.valueOf(9))
                    .divide(BigDecimal.valueOf(5), 2, RoundingMode.HALF_UP)
                    .add(BigDecimal.valueOf(32));
        return celsius;
    }

    /** Etiqueta de unidad: "°C" o "°F". */
    public static String label(String unit) {
        return "F".equals(unit) ? "°F" : "°C";
    }

    /** Min del input HTML según unidad. */
    public static int inputMin(String unit) {
        return "F".equals(unit) ? 14 : -10;
    }

    /** Max del input HTML según unidad. */
    public static int inputMax(String unit) {
        return "F".equals(unit) ? 140 : 60;
    }
}