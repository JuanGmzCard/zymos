package com.alera.repository;

import com.alera.model.WebhookEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WebhookEndpointRepository extends JpaRepository<WebhookEndpoint, Long> {

    List<WebhookEndpoint> findAllByOrderByNombreAsc();

    // Bypass Hibernate @TenantId filter — necesario en WebhookService que corre
    // desde schedulers y threads asíncronos donde TenantContext puede no estar activo.
    @Query(value = "SELECT * FROM webhook_endpoints WHERE tenant_id = :tenantId AND activo = TRUE",
           nativeQuery = true)
    List<WebhookEndpoint> findActivosByTenantId(@Param("tenantId") String tenantId);
}
