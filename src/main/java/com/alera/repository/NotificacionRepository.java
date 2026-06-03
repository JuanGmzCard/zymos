package com.alera.repository;

import com.alera.model.Notificacion;
import com.alera.model.enums.TipoNotificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findTop5ByLeidaFalseOrderByCreatedAtDesc();

    long countByLeidaFalse();

    @Query("SELECT n FROM Notificacion n ORDER BY n.leida ASC, n.createdAt DESC")
    Page<Notificacion> findAllOrdenadas(Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE Notificacion n SET n.leida = true WHERE n.leida = false")
    void marcarTodasLeidas();

    @Query("SELECT COUNT(n) > 0 FROM Notificacion n WHERE n.tipo = :tipo AND n.createdAt >= :desde AND n.createdAt < :hasta")
    boolean existeEnPeriodo(@Param("tipo") TipoNotificacion tipo,
                             @Param("desde") LocalDateTime desde,
                             @Param("hasta") LocalDateTime hasta);
}
