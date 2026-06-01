package com.alera.model;

import com.alera.model.enums.ListaPrecio;
import com.alera.model.enums.RegimenTributario;
import jakarta.persistence.*;

@Entity
@Table(name = "clientes")
public class Cliente extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(name = "razon_social", length = 200)
    private String razonSocial;

    @Column(length = 50)
    private String nit;

    @Enumerated(EnumType.STRING)
    @Column(name = "regimen_tributario", length = 30)
    private RegimenTributario regimenTributario;

    @Column(length = 200)
    private String email;

    @Column(length = 50)
    private String telefono;

    @Column(name = "direccion_despacho", length = 300)
    private String direccionDespacho;

    @Column(length = 100)
    private String ciudad;

    @Column(length = 100)
    private String departamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "lista_precio", length = 30)
    private ListaPrecio listaPrecio;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(length = 500)
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
