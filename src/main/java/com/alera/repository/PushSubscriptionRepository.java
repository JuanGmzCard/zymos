package com.alera.repository;

import com.alera.model.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {
    Optional<PushSubscription> findByEndpoint(String endpoint);
    List<PushSubscription> findByUsernameAndTenantId(String username, String tenantId);
    List<PushSubscription> findByTenantId(String tenantId);
    boolean existsByUsernameAndTenantId(String username, String tenantId);
    @Transactional
    void deleteByEndpoint(String endpoint);
}
