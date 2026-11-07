package com.alera.repository;

import com.alera.model.ContactoWhatsApp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContactoWhatsAppRepository extends JpaRepository<ContactoWhatsApp, Long> {

    List<ContactoWhatsApp> findAllByActivoTrueOrderByNombreAsc();

    List<ContactoWhatsApp> findAllByOrderByNombreAsc();

    // Native para bypassar el filtro @TenantId de Hibernate — necesario para acceso cross-tenant
    @Query(value = """
            SELECT cwa.* FROM contactos_whatsapp cwa
            JOIN contactos_whatsapp_suscripciones cwas ON cwas.contacto_id = cwa.id
            WHERE cwa.tenant_id = :tenantId
              AND cwas.tipo = :tipo
              AND cwa.activo = TRUE
            """, nativeQuery = true)
    List<ContactoWhatsApp> findActivosByTenantIdAndTipo(@Param("tenantId") String tenantId,
                                                         @Param("tipo") String tipo);
}
