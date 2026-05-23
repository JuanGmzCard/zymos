package com.alera.repository;

import com.alera.model.HistorialTenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HistorialTenantRepository extends JpaRepository<HistorialTenant, Long> {
    List<HistorialTenant> findBySubdomainOrderByFechaDesc(String subdomain);
    Page<HistorialTenant> findBySubdomainOrderByFechaDesc(String subdomain, Pageable pageable);
}
