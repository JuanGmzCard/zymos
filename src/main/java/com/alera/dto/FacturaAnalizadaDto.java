package com.alera.dto;

import java.util.List;

public class FacturaAnalizadaDto {

    private String proveedor;
    private String numeroFactura;
    private String fechaFactura;
    private String descripcion;
    private Double costoEnvio;
    private List<ItemAnalizadoDto> items;
    private boolean modoMock;
    private String error;

    public static class ItemAnalizadoDto {
        private String nombre;
        private String tipoItem;
        private String tipoInsumo;
        private Double cantidad;
        private String unidad;
        private Double valorUnitario;
        private Double porcentajeIvaItem;

        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public String getTipoItem() { return tipoItem; }
        public void setTipoItem(String tipoItem) { this.tipoItem = tipoItem; }
        public String getTipoInsumo() { return tipoInsumo; }
        public void setTipoInsumo(String tipoInsumo) { this.tipoInsumo = tipoInsumo; }
        public Double getCantidad() { return cantidad; }
        public void setCantidad(Double cantidad) { this.cantidad = cantidad; }
        public String getUnidad() { return unidad; }
        public void setUnidad(String unidad) { this.unidad = unidad; }
        public Double getValorUnitario() { return valorUnitario; }
        public void setValorUnitario(Double valorUnitario) { this.valorUnitario = valorUnitario; }
        public Double getPorcentajeIvaItem() { return porcentajeIvaItem; }
        public void setPorcentajeIvaItem(Double porcentajeIvaItem) { this.porcentajeIvaItem = porcentajeIvaItem; }
    }

    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }
    public String getNumeroFactura() { return numeroFactura; }
    public void setNumeroFactura(String numeroFactura) { this.numeroFactura = numeroFactura; }
    public String getFechaFactura() { return fechaFactura; }
    public void setFechaFactura(String fechaFactura) { this.fechaFactura = fechaFactura; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Double getCostoEnvio() { return costoEnvio; }
    public void setCostoEnvio(Double costoEnvio) { this.costoEnvio = costoEnvio; }
    public List<ItemAnalizadoDto> getItems() { return items; }
    public void setItems(List<ItemAnalizadoDto> items) { this.items = items; }
    public boolean isModoMock() { return modoMock; }
    public void setModoMock(boolean modoMock) { this.modoMock = modoMock; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
