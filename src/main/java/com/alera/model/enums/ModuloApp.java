package com.alera.model.enums;

public enum ModuloApp {
    TRAZABILIDAD("Trazabilidad"),
    RECETAS("Recetas"),
    INVENTARIO("Inventario"),
    FACTURACION("Facturación"),
    COMERCIAL("Comercial"),
    EQUIPOS("Equipos"),
    REPORTES("Reportes"),
    PLANIFICACION("Planificación"),
    BPM("BPM"),
    BARRILES("Barriles"),
    TAREAS("Tareas");

    private final String displayName;

    ModuloApp(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }
}
