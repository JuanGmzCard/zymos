package com.alera.dto;

public class AlertaContadores {

    private final int bajoStock;
    private final int vencimientos;
    private final int mantenimiento;

    public AlertaContadores(int bajoStock, int vencimientos, int mantenimiento) {
        this.bajoStock    = bajoStock;
        this.vencimientos = vencimientos;
        this.mantenimiento = mantenimiento;
    }

    public int getBajoStock()     { return bajoStock; }
    public int getVencimientos()  { return vencimientos; }
    public int getMantenimiento() { return mantenimiento; }
    public int getTotal()         { return bajoStock + vencimientos + mantenimiento; }
}
