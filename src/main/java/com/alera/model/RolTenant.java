package com.alera.model;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "roles_tenant")
public class RolTenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "es_sistema", nullable = false)
    private boolean esSistema = false;

    @Column(name = "es_admin", nullable = false)
    private boolean esAdmin = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "rol", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("modulo ASC")
    private List<RolModuloPermiso> permisos = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public boolean isEsSistema() { return esSistema; }
    public void setEsSistema(boolean esSistema) { this.esSistema = esSistema; }
    public boolean isEsAdmin() { return esAdmin; }
    public void setEsAdmin(boolean esAdmin) { this.esAdmin = esAdmin; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<RolModuloPermiso> getPermisos() { return permisos; }
    public void setPermisos(List<RolModuloPermiso> permisos) { this.permisos = permisos; }
}
