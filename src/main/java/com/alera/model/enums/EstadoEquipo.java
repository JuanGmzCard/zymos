package com.alera.model.enums;

public enum EstadoEquipo {
    OPERATIVO("Operativo"),
    MANTENIMIENTO("En Mantenimiento"),
    INACTIVO("Inactivo");

    private final String displayName;

    EstadoEquipo(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
