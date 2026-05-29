package com.alera.model.enums;

public enum EstadoVenta {
    PENDIENTE ("Pendiente",  "bg-warning text-dark"),
    DESPACHADO("Despachado", "bg-success"),
    CANCELADO ("Cancelado",  "bg-secondary");

    private final String displayName;
    private final String badgeClass;

    EstadoVenta(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass  = badgeClass;
    }

    public String getDisplayName() { return displayName; }
    public String getBadgeClass()  { return badgeClass; }
}
