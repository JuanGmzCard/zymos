package com.alera.repository;

import com.alera.model.TareaItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TareaItemRepository extends JpaRepository<TareaItem, Long> {

    List<TareaItem> findByTareaIdOrderByOrdenItemAscIdAsc(Long tareaId);

    Optional<TareaItem> findByIdAndTareaId(Long id, Long tareaId);
}
