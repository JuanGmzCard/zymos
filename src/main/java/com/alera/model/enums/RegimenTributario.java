package com.alera.model.enums;

public enum RegimenTributario {
    SIMPLIFICADO   ("Régimen Simplificado"),
    RESPONSABLE_IVA("Responsable de IVA");

    private final String displayName;

    RegimenTributario(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }
}
