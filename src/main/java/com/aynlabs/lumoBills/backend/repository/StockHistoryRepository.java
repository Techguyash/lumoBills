package com.aynlabs.lumoBills.backend.repository;

import com.aynlabs.lumoBills.backend.entity.StockHistory;
import com.aynlabs.lumoBills.backend.entity.StockHistory.TransactionType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockHistoryRepository extends JpaRepository<StockHistory, Long> {
    List<StockHistory> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    List<StockHistory> findByTimestampBetweenAndType(LocalDateTime start, LocalDateTime end, TransactionType type);

    List<StockHistory> findByType(TransactionType type);
}
