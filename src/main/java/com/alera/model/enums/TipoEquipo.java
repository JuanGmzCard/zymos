package com.alera.model.enums;

public enum TipoEquipo {
    FERMENTADOR("Fermentador"),
    OLLA_MACERADO("Olla de Macerado"),
    OLLA_HERVOR("Olla de Hervor"),
    ENFRIADOR("Enfriador"),
    BOMBA("Bomba"),
    FILTRO("Filtro"),
    MEDIDOR_PH("Medidor de pH"),
    DENSIMETRO("Densímetro"),
    BASCULA("Báscula"),
    COMPRESOR("Compresor"),
    OTRO("Otro");

    private final String displayName;

    TipoEquipo(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
