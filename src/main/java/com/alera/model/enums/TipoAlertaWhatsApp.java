package com.alera.model.enums;

public enum TipoAlertaWhatsApp {
    BAJO_STOCK("📦 Bajo stock de insumos", "Inventario"),
    VENCIMIENTO("⏰ Vencimientos próximos", "Inventario"),
    LOTE_FASE("🍻 Cambio de fase de lote", "Producción"),
    MANTENIMIENTO("🔧 Mantenimiento de equipo", "Equipos"),
    BPM_SALUD("🛡️ Síntoma de salud BPM", "BPM"),
    TAREA_ASIGNADA("✅ Tarea asignada", "Tareas"),
    PLAN_VENCIMIENTO("💳 Plan por vencer", "Admin"),
    RESUMEN_MENSUAL("📊 Resumen mensual", "Reportes");

    private final String displayName;
    private final String categoria;

    TipoAlertaWhatsApp(String displayName, String categoria) {
        this.displayName = displayName;
        this.categoria = categoria;
    }

    public String getDisplayName() { return displayName; }
    public String getCategoria()   { return categoria; }
}
