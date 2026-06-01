package com.alera.dto;

import com.alera.model.enums.ListaPrecio;
import com.alera.model.enums.RegimenTributario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ClienteFormDto {

    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 200)
    private String nombre;

    @Size(max = 200)
    private String razonSocial;

    @Size(max = 50)
    private String nit;

    private RegimenTributario regimenTributario;

    @Email(message = "Email inválido")
    @Size(max = 200)
    private String email;

    @Size(max = 50)
    private String telefono;

    @Size(max = 300)
    private String direccionDespacho;

    @Size(max = 100)
    private String ciudad;

    @Size(max = 100)
    private String departamento;

    private ListaPrecio listaPrecio;

    private boolean activo = true;

    @Size(max = 500)
    private String notas;

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }
    public String getNit() { return nit; }
    public void setNit(String nit) { this.nit = nit; }
    public RegimenTributario getRegimenTributario() { return regimenTributario; }
    public void setRegimenTributario(RegimenTributario regimenTributario) { this.regimenTributario = regimenTributario; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getDireccionDespacho() { return direccionDespacho; }
    public void setDireccionDespacho(String direccionDespacho) { this.direccionDespacho = direccionDespacho; }
    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }
    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }
    public ListaPrecio getListaPrecio() { return listaPrecio; }
    public void setListaPrecio(ListaPrecio listaPrecio) { this.listaPrecio = listaPrecio; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
}
