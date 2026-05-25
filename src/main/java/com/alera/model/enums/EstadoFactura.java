package com.alera.model.enums;

public enum EstadoFactura {
    RECIBIDA("Recibida",   "bg-secondary"),
    VERIFICADA("Verificada", "bg-warning text-dark"),
    PAGADA("Pagada",       "bg-success");

    private final String displayName;
    private final String badgeClass;

    EstadoFactura(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass  = badgeClass;
    }

    public String getDisplayName() { return displayName; }
    public String getBadgeClass()  { return badgeClass; }
}
