package com.alera.model.enums;

public enum EstadoOrdenCompra {
    BORRADOR("Borrador", "bg-secondary"),
    ENVIADA("Enviada", "bg-info text-dark"),
    RECIBIDA_PARCIAL("Recibida parcial", "bg-warning text-dark"),
    RECIBIDA("Recibida", "bg-success"),
    CANCELADA("Cancelada", "bg-dark");

    private final String displayName;
    private final String badgeClass;

    EstadoOrdenCompra(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass  = badgeClass;
    }

    public String getDisplayName() { return displayName; }
    public String getBadgeClass()  { return badgeClass; }
}
