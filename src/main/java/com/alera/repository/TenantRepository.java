package com.alera.repository;

import com.alera.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, String> {
    Optional<Tenant> findBySubdomainAndActiveTrue(String subdomain);
}
