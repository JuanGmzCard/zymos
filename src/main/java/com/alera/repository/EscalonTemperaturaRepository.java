package com.alera.repository;

import com.alera.model.EscalonTemperatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EscalonTemperaturaRepository extends JpaRepository<EscalonTemperatura, Long> {

    // Escalones cuya fechaObjetivo es hoy, aún no logrados ni con fecha real
    @Query("SELECT e FROM EscalonTemperatura e JOIN FETCH e.lote l " +
           "WHERE e.fechaObjetivo = :hoy AND e.fechaReal IS NULL AND e.logrado = false " +
           "ORDER BY l.codigoLote ASC, e.orden ASC")
    List<EscalonTemperatura> findPendientesHoy(@Param("hoy") LocalDate hoy);
}
