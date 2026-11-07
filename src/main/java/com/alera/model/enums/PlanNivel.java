package com.alera.model.enums;

public enum PlanNivel {

    MACERADO("Plan Macerado", "$150.000 COP/mes", 150_000),
    COCCION("Plan Cocción", "$320.000 COP/mes", 320_000),
    FERMENTACION("Plan Fermentación", "$550.000 COP/mes", 550_000);

    private final String displayName;
    private final String precio;
    private final int    precioNumerico;

    PlanNivel(String displayName, String precio, int precioNumerico) {
        this.displayName    = displayName;
        this.precio         = precio;
        this.precioNumerico = precioNumerico;
    }

    public String getDisplayName()   { return displayName; }
    public String getPrecio()        { return precio; }
    public int    getPrecioNumerico(){ return precioNumerico; }

    /** true si este nivel incluye el nivel requerido (FERMENTACION >= COCCION >= MACERADO). */
    public boolean includes(PlanNivel required) {
        return this.ordinal() >= required.ordinal();
    }
}
