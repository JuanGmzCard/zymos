package com.alera.model.enums;

public enum PrioridadTarea {
    BAJA("Baja",   "bg-secondary"),
    MEDIA("Media", "bg-primary"),
    ALTA("Alta",   "bg-danger");

    private final String displayName;
    private final String badgeClass;

    PrioridadTarea(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass  = badgeClass;
    }

    public String getDisplayName() { return displayName; }
    public String getBadgeClass()  { return badgeClass; }
}
