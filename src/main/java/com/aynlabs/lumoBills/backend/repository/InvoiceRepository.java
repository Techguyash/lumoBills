package com.aynlabs.lumoBills.backend.repository;

import com.aynlabs.lumoBills.backend.entity.Invoice;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByDateBetween(LocalDateTime start, LocalDateTime end);
}
