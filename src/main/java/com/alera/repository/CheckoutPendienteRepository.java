package com.alera.repository;

import com.alera.model.CheckoutPendiente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface CheckoutPendienteRepository extends JpaRepository<CheckoutPendiente, String> {

    Optional<CheckoutPendiente> findByMpPaymentId(String mpPaymentId);

    Optional<CheckoutPendiente> findBySubdominioAndEstado(String subdominio, String estado);

    @Transactional
    @Modifying
    @Query("UPDATE CheckoutPendiente c SET c.estado = :estado, c.mpPaymentId = :paymentId, c.procesadoAt = CURRENT_TIMESTAMP WHERE c.id = :id")
    void marcarProcesado(@Param("id") String id,
                         @Param("paymentId") String paymentId,
                         @Param("estado") String estado);
}
