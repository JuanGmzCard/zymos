package com.alera.dto;

/**
 * Promedios de lotes del mismo estilo para mostrar en la sección comparativa de detalle.html.
 * Los campos Double son null cuando no hay datos suficientes para calcular el promedio.
 */
public record ComparativaEstiloDto(
        int     totalLotes,
        Double  ogPromedio,
        Double  fgPromedio,
        Double  abvPromedio,
        Double  litrosPromedio
) {
    public boolean hayDatos() { return totalLotes > 0; }
}
