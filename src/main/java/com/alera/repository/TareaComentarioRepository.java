package com.alera.repository;

import com.alera.model.TareaComentario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TareaComentarioRepository extends JpaRepository<TareaComentario, Long> {
    List<TareaComentario> findByTareaIdOrderByFechaHoraAsc(Long tareaId);
}
