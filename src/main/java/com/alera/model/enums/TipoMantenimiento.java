package com.alera.model.enums;

public enum TipoMantenimiento {
    PREVENTIVO("Preventivo"),
    CORRECTIVO("Correctivo"),
    CALIBRACION("Calibración"),
    LIMPIEZA("Limpieza");

    private final String displayName;

    TipoMantenimiento(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
