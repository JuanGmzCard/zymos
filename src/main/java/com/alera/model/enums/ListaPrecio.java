package com.alera.model.enums;

public enum ListaPrecio {
    VENTA_DIRECTA("Venta Directa"),
    DISTRIBUIDOR ("Distribuidor"),
    BAR          ("Bar / Restaurante"),
    MAYORISTA    ("Mayorista"),
    EXPORTACION  ("Exportación"),
    EMPLEADO     ("Empleado / Interno");

    private final String displayName;

    ListaPrecio(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }
}
