package com.alera.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.EntityListeners;
import org.hibernate.annotations.TenantId;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

/**
 * Superclase con los 4 campos de auditoría JPA.
 * Extienden esta clase: LoteCerveza, Receta, Equipo,
 * InsumoInventario, FacturaProveedor, Proveedor.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {

    @TenantId
    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 100)
    private String createdBy;

    @LastModifiedDate
    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;

    @LastModifiedBy
    @Column(name = "last_modified_by", length = 100)
    private String lastModifiedBy;

    public String        getTenantId()        { return tenantId; }
    public LocalDateTime getCreatedAt()      { return createdAt; }
    public String        getCreatedBy()      { return createdBy; }
    public LocalDateTime getLastModifiedAt() { return lastModifiedAt; }
    public String        getLastModifiedBy() { return lastModifiedBy; }
}
