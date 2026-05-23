package com.alera.model.enums;

public enum TipoNotificacion {
    BAJO_STOCK("bi-box-seam",       "text-warning"),
    VENCIMIENTO("bi-calendar-x",    "text-warning"),
    MANTENIMIENTO("bi-tools",       "text-info"),
    SISTEMA("bi-info-circle-fill",  "text-primary");

    private final String icono;
    private final String colorClase;

    TipoNotificacion(String icono, String colorClase) {
        this.icono      = icono;
        this.colorClase = colorClase;
    }

    public String getIcono()      { return icono; }
    public String getColorClase() { return colorClase; }
}
