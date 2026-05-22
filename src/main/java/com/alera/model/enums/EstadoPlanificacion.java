package com.alera.model.enums;

public enum EstadoPlanificacion {
    PLANIFICADA("Planificada"),
    EN_PROCESO("En proceso"),
    COMPLETADA("Completada"),
    CANCELADA("Cancelada");

    private final String displayName;

    EstadoPlanificacion(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }

    public String getColor() {
        return switch (this) {
            case PLANIFICADA -> "#C9A028";
            case EN_PROCESO  -> "#0288D1";
            case COMPLETADA  -> "#198754";
            case CANCELADA   -> "#6c757d";
        };
    }

    public String getColorTexto() {
        return switch (this) {
            case PLANIFICADA -> "#242E0D";
            default          -> "#ffffff";
        };
    }
}
