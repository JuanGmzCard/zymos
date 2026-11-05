package com.alera.repository;

import com.alera.model.EscalonTemperatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EscalonTemperaturaRepository extends JpaRepository<EscalonTemperatura, Long> {
}
