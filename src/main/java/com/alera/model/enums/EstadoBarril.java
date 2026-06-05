package com.alera.model.enums;

public enum EstadoBarril {
    DISPONIBLE("Disponible",   "bg-success"),
    LLENO     ("Lleno",        "bg-primary"),
    DESPACHADO("Despachado",   "bg-warning text-dark"),
    VACIO     ("Vacío",        "bg-secondary"),
    LIMPIEZA  ("En limpieza",  "bg-info text-dark"),
    BAJA      ("Dado de baja", "bg-danger");

    private final String displayName;
    private final String badgeClass;

    EstadoBarril(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass  = badgeClass;
    }

    public String getDisplayName() { return displayName; }
    public String getBadgeClass()  { return badgeClass; }
}
