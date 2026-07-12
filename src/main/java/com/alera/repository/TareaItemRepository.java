package com.alera.repository;

import com.alera.model.TareaItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TareaItemRepository extends JpaRepository<TareaItem, Long> {

    List<TareaItem> findByTareaIdOrderByOrdenItemAscIdAsc(Long tareaId);
}
