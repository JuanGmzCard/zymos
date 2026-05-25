package com.alera.repository;

import com.alera.model.SuperAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SuperAdminRepository extends JpaRepository<SuperAdmin, Long> {
    Optional<SuperAdmin> findByUsernameAndActivoTrue(String username);
    boolean existsByUsername(String username);
}
