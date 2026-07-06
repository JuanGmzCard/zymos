package com.alera.model.enums;

public enum TipoResiduo {
    ORGANICO("Orgánico"),
    APROVECHABLE("Aprovechable"),
    NO_APROVECHABLE("No aprovechable");

    private final String display;
    TipoResiduo(String display) { this.display = display; }
    public String getDisplayName() { return display; }
}
