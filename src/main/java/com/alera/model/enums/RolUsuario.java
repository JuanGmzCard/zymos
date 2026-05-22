package com.alera.model.enums;

public enum RolUsuario {
    ADMIN("Administrador"),
    INVENTARIO("Inventario"),
    FACTURACION("Facturación"),
    EQUIPOS("Equipos");

    private final String displayName;

    RolUsuario(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }
}