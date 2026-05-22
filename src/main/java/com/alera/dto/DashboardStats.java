package com.alera.dto;

import com.alera.model.LoteCerveza;
import java.math.BigDecimal;
import java.util.List;

public class DashboardStats {

    // Lotes
    private long totalLotes;
    private long enProceso;
    private long completados;
    private long estilosDistintos;

    // Inventario
    private long totalInsumos;
    private long bajoStock;
    private long proximosAVencer;

    // Equipos
    private long totalEquipos;
    private long equiposMantenimiento;
    private long mantenimientoPendiente;

    // Financiero
    private long totalFacturas;
    private BigDecimal totalGastado;
    private BigDecimal totalMantenimientos;

    // Listados
    private List<LoteCerveza> ultimosLotes;

    // Builder-style setters encadenados
    public DashboardStats totalLotes(long v)            { this.totalLotes = v; return this; }
    public DashboardStats enProceso(long v)             { this.enProceso = v; return this; }
    public DashboardStats completados(long v)           { this.completados = v; return this; }
    public DashboardStats estilosDistintos(long v)      { this.estilosDistintos = v; return this; }
    public DashboardStats totalInsumos(long v)          { this.totalInsumos = v; return this; }
    public DashboardStats bajoStock(long v)             { this.bajoStock = v; return this; }
    public DashboardStats proximosAVencer(long v)       { this.proximosAVencer = v; return this; }
    public DashboardStats totalEquipos(long v)          { this.totalEquipos = v; return this; }
    public DashboardStats equiposMantenimiento(long v)  { this.equiposMantenimiento = v; return this; }
    public DashboardStats mantenimientoPendiente(long v){ this.mantenimientoPendiente = v; return this; }
    public DashboardStats totalFacturas(long v)         { this.totalFacturas = v; return this; }
    public DashboardStats totalGastado(BigDecimal v)    { this.totalGastado = v; return this; }
    public DashboardStats totalMantenimientos(BigDecimal v){ this.totalMantenimientos = v; return this; }
    public DashboardStats ultimosLotes(List<LoteCerveza> v){ this.ultimosLotes = v; return this; }

    // Getters
    public long getTotalLotes()             { return totalLotes; }
    public long getEnProceso()              { return enProceso; }
    public long getCompletados()            { return completados; }
    public long getEstilosDistintos()       { return estilosDistintos; }
    public long getTotalInsumos()           { return totalInsumos; }
    public long getBajoStock()              { return bajoStock; }
    public long getProximosAVencer()        { return proximosAVencer; }
    public long getTotalEquipos()           { return totalEquipos; }
    public long getEquiposMantenimiento()   { return equiposMantenimiento; }
    public long getMantenimientoPendiente() { return mantenimientoPendiente; }
    public long getTotalFacturas()          { return totalFacturas; }
    public BigDecimal getTotalGastado()     { return totalGastado; }
    public BigDecimal getTotalMantenimientos(){ return totalMantenimientos; }
    public List<LoteCerveza> getUltimosLotes(){ return ultimosLotes; }
}
