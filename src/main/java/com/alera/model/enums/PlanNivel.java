package com.alera.model.enums;

public enum PlanNivel {

    MACERADO("Plan Macerado", "$150.000 COP/mes", 150_000, 3500),
    COCCION("Plan Cocción", "$320.000 COP/mes", 320_000, 7500),
    FERMENTACION("Plan Fermentación", "$550.000 COP/mes", 550_000, 13000);

    private final String displayName;
    private final String precio;
    private final int    precioNumerico;
    /** Precio en centavos de USD para Stripe (3500 = $35.00 USD). */
    private final int    precioUsd;

    PlanNivel(String displayName, String precio, int precioNumerico, int precioUsd) {
        this.displayName    = displayName;
        this.precio         = precio;
        this.precioNumerico = precioNumerico;
        this.precioUsd      = precioUsd;
    }

    public String getDisplayName()   { return displayName; }
    public String getPrecio()        { return precio; }
    public int    getPrecioNumerico(){ return precioNumerico; }
    public int    getPrecioUsd()     { return precioUsd; }

    /** true si este nivel incluye el nivel requerido (FERMENTACION >= COCCION >= MACERADO). */
    public boolean includes(PlanNivel required) {
        return this.ordinal() >= required.ordinal();
    }
}
