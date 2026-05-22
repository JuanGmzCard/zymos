package com.alera.model.enums;

public enum TipoIngrediente {
    MALTA("Malta"),
    LUPULO("Lúpulo"),
    LEVADURA("Levadura"),
    CLARIFICANTE("Clarificante");

    private final String displayName;

    TipoIngrediente(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
