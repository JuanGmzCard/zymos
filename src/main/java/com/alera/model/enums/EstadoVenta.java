package com.alera.model.enums;

public enum EstadoVenta {
    COTIZACION("Cotización", "bg-info text-dark"),
    PENDIENTE ("Pendiente",  "bg-warning text-dark"),
    DESPACHADO("Despachado", "bg-success"),
    CANCELADO ("Cancelado",  "bg-secondary"),
    EXPIRADO  ("Expirado",   "bg-dark");

    private final String displayName;
    private final String badgeClass;

    EstadoVenta(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass  = badgeClass;
    }

    public String getDisplayName() { return displayName; }
    public String getBadgeClass()  { return badgeClass; }
}
