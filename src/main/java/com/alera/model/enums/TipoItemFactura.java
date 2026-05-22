package com.alera.model.enums;

public enum TipoItemFactura {
    INSUMO("Insumo"),
    EQUIPO("Equipo");

    private final String displayName;

    TipoItemFactura(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
