package com.alera.repository;

import com.alera.model.CarbSplit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CarbSplitRepository extends JpaRepository<CarbSplit, Long> {
    List<CarbSplit> findByLoteIdOrderByOrdenAsc(Long loteId);
}
