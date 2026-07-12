package com.alera.model.enums;

public enum EstadoTarea {
    PENDIENTE("Pendiente",     "bg-warning text-dark"),
    EN_PROGRESO("En progreso", "bg-info text-dark"),
    COMPLETADA("Completada",   "bg-success");

    private final String displayName;
    private final String badgeClass;

    EstadoTarea(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass  = badgeClass;
    }

    public String getDisplayName() { return displayName; }
    public String getBadgeClass()  { return badgeClass; }
}
