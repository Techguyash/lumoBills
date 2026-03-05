package com.aynlabs.lumoBills.backend.entity;

import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class LedgerEntry extends AbstractEntity {

    public enum EntryType {
        INCOME, EXPENSE
    }

    @NotNull
    private LocalDateTime transactionDate;

    @NotNull
    private EntryType type;

    @NotEmpty
    private String category; // e.g. "Sales", "Raw Material Purchase"

    @NotNull
    private BigDecimal amount;

    private String description;

    private String referenceId; // e.g. Invoice Number or Purchase ID

    private Invoice.PaymentMode paymentMode;
}
