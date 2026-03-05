package com.aynlabs.lumoBills.backend.service;

import com.aynlabs.lumoBills.backend.entity.LedgerEntry;
import com.aynlabs.lumoBills.backend.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;

    public void recordEntry(LedgerEntry entry) {
        if (entry.getTransactionDate() == null) {
            entry.setTransactionDate(LocalDateTime.now());
        }
        ledgerEntryRepository.save(entry);
    }

    public List<LedgerEntry> getEntriesBetween(LocalDateTime start, LocalDateTime end) {
        return ledgerEntryRepository.findByTransactionDateBetweenOrderByTransactionDateDesc(start, end);
    }

    public BigDecimal getTotalIncome(LocalDateTime start, LocalDateTime end) {
        return getEntriesBetween(start, end).stream()
                .filter(e -> e.getType() == LedgerEntry.EntryType.INCOME)
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalExpense(LocalDateTime start, LocalDateTime end) {
        return getEntriesBetween(start, end).stream()
                .filter(e -> e.getType() == LedgerEntry.EntryType.EXPENSE)
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
