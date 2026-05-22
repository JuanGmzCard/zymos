package com.alera.repository;

import com.alera.model.HistorialLote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HistorialLoteRepository extends JpaRepository<HistorialLote, Long> {
    List<HistorialLote> findByLoteIdOrderByFechaDesc(Long loteId);
}
