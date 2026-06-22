package com.alera.model.enums;

public enum TipoMovimiento {
    ENTRADA("Entrada manual", "success"),
    SALIDA("Salida manual", "danger"),
    AJUSTE("Ajuste de stock", "warning"),
    DESCUENTO_LOTE("Descuento por lote", "secondary"),
    RESTAURACION_LOTE("Restauración de lote", "info"),
    INGRESO_FACTURA("Ingreso por factura", "primary"),
    REVERSION_FACTURA("Reversión de factura", "warning");

    private final String displayName;
    private final String badgeClass;

    TipoMovimiento(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass  = badgeClass;
    }

    public String getDisplayName() { return displayName; }
    public String getBadgeClass()  { return badgeClass; }
}
