package com.aynlabs.lumoBills.backend.repository;

import com.aynlabs.lumoBills.backend.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByTransactionDateBetweenOrderByTransactionDateDesc(LocalDateTime start, LocalDateTime end);
}
