package com.alera.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RentabilidadLoteDto(
        Long loteId,
        String codigoLote,
        String estilo,
        LocalDate fechaCompletado,
        BigDecimal litrosFinales,
        BigDecimal costo,
        BigDecimal ingresos,
        BigDecimal margen,
        BigDecimal margenPct,
        BigDecimal costoPorLitro,
        BigDecimal ingresoPorLitro
) {
    public boolean rentable()  { return margen != null && margen.compareTo(BigDecimal.ZERO) > 0; }
    public boolean enRojo()    { return margen != null && margen.compareTo(BigDecimal.ZERO) < 0; }
    public boolean sinCosto()  { return costo == null || costo.compareTo(BigDecimal.ZERO) == 0; }
    public boolean sinVentas() { return ingresos == null || ingresos.compareTo(BigDecimal.ZERO) == 0; }
}
