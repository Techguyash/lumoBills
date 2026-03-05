package com.aynlabs.lumoBills.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Payment extends AbstractEntity {

    @ManyToOne
    @JoinColumn(name = "invoice_id")
    @NotNull
    private Invoice invoice;

    @NotNull
    private BigDecimal amount;

    @NotNull
    private LocalDateTime paymentDate;

    private Invoice.PaymentMode mode;

    private String referenceNumber;
}
