package com.alera.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StockLoteDto(
        Long loteId,
        String codigoLote,
        String estilo,
        LocalDate fechaCompletado,
        BigDecimal producido,
        String carbDestino,
        BigDecimal vendido,
        BigDecimal ajustado,
        BigDecimal disponible,
        String unidad,
        BigDecimal despachado,
        BigDecimal reservado
) {
    public boolean agotado() {
        return disponible == null || disponible.compareTo(BigDecimal.ZERO) <= 0;
    }

    public boolean conStock() {
        return !agotado();
    }
}
