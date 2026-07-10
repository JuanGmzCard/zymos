package com.alera.model;

import jakarta.persistence.*;

@Entity
@Table(name = "roles_modulos_permisos")
public class RolModuloPermiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_id", nullable = false)
    private RolTenant rol;

    @Column(nullable = false, length = 50)
    private String modulo;

    @Column(name = "puede_ver", nullable = false)
    private boolean puedeVer = false;

    @Column(name = "puede_crear", nullable = false)
    private boolean puedeCrear = false;

    @Column(name = "puede_editar", nullable = false)
    private boolean puedeEditar = false;

    @Column(name = "puede_eliminar", nullable = false)
    private boolean puedeEliminar = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public RolTenant getRol() { return rol; }
    public void setRol(RolTenant rol) { this.rol = rol; }
    public String getModulo() { return modulo; }
    public void setModulo(String modulo) { this.modulo = modulo; }
    public boolean isPuedeVer() { return puedeVer; }
    public void setPuedeVer(boolean puedeVer) { this.puedeVer = puedeVer; }
    public boolean isPuedeCrear() { return puedeCrear; }
    public void setPuedeCrear(boolean puedeCrear) { this.puedeCrear = puedeCrear; }
    public boolean isPuedeEditar() { return puedeEditar; }
    public void setPuedeEditar(boolean puedeEditar) { this.puedeEditar = puedeEditar; }
    public boolean isPuedeEliminar() { return puedeEliminar; }
    public void setPuedeEliminar(boolean puedeEliminar) { this.puedeEliminar = puedeEliminar; }

    public boolean tieneAlgunPermiso() {
        return puedeVer || puedeCrear || puedeEditar || puedeEliminar;
    }
}
