package com.alera.repository;

import com.alera.model.MigracionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MigracionLogRepository extends JpaRepository<MigracionLog, Long> {
    List<MigracionLog> findByTenantIdOrderByFechaDesc(String tenantId);
    long countByTenantId(String tenantId);
}
