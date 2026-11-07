package com.alera.dto;

import java.util.List;

public class OptimizacionOcDto {

    private List<ItemCompraDto> itemsSugeridos;
    private String resumen;
    private List<String> alertas;
    private int elaboracionesAnalizadas;
    private boolean modoMock;
    private String error;

    public static class ItemCompraDto {
        private String nombre;
        private String tipo;
        private double cantidadNecesaria;
        private double cantidadEnStock;
        private double cantidadAComprar;
        private String unidad;
        private String precioUnitarioReferencia;
        private String costoEstimado;
        private String proveedorSugerido;
        private List<String> elaboraciones;

        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public String getTipo() { return tipo; }
        public void setTipo(String tipo) { this.tipo = tipo; }
        public double getCantidadNecesaria() { return cantidadNecesaria; }
        public void setCantidadNecesaria(double cantidadNecesaria) { this.cantidadNecesaria = cantidadNecesaria; }
        public double getCantidadEnStock() { return cantidadEnStock; }
        public void setCantidadEnStock(double cantidadEnStock) { this.cantidadEnStock = cantidadEnStock; }
        public double getCantidadAComprar() { return cantidadAComprar; }
        public void setCantidadAComprar(double cantidadAComprar) { this.cantidadAComprar = cantidadAComprar; }
        public String getUnidad() { return unidad; }
        public void setUnidad(String unidad) { this.unidad = unidad; }
        public String getPrecioUnitarioReferencia() { return precioUnitarioReferencia; }
        public void setPrecioUnitarioReferencia(String precioUnitarioReferencia) { this.precioUnitarioReferencia = precioUnitarioReferencia; }
        public String getCostoEstimado() { return costoEstimado; }
        public void setCostoEstimado(String costoEstimado) { this.costoEstimado = costoEstimado; }
        public String getProveedorSugerido() { return proveedorSugerido; }
        public void setProveedorSugerido(String proveedorSugerido) { this.proveedorSugerido = proveedorSugerido; }
        public List<String> getElaboraciones() { return elaboraciones; }
        public void setElaboraciones(List<String> elaboraciones) { this.elaboraciones = elaboraciones; }
    }

    public List<ItemCompraDto> getItemsSugeridos() { return itemsSugeridos; }
    public void setItemsSugeridos(List<ItemCompraDto> itemsSugeridos) { this.itemsSugeridos = itemsSugeridos; }
    public String getResumen() { return resumen; }
    public void setResumen(String resumen) { this.resumen = resumen; }
    public List<String> getAlertas() { return alertas; }
    public void setAlertas(List<String> alertas) { this.alertas = alertas; }
    public int getElaboracionesAnalizadas() { return elaboracionesAnalizadas; }
    public void setElaboracionesAnalizadas(int elaboracionesAnalizadas) { this.elaboracionesAnalizadas = elaboracionesAnalizadas; }
    public boolean isModoMock() { return modoMock; }
    public void setModoMock(boolean modoMock) { this.modoMock = modoMock; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
