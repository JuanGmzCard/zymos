package com.alera.model.enums;

public enum TipoInsumo {
    MALTA("Malta"),
    LUPULO("Lúpulo"),
    LEVADURA("Levadura"),
    CLARIFICANTE("Clarificante"),
    AGENTE_CARBONATACION("Agente de Carbonatación"),
    AGUA("Agua"),
    QUIMICO("Químico"),
    ENVASE("Envase"),
    OTRO("Otro");

    private final String displayName;

    TipoInsumo(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
