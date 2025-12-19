package com.aynlabs.lumoBills.backend.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Invoice extends AbstractEntity {

    @ManyToOne
    private Customer customer;

    private LocalDateTime date;

    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;

    private BigDecimal subTotal;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    // totalAmount is already there, acts as grandTotal

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<InvoiceItem> items = new ArrayList<>();

    @ManyToOne
    private User createdBy;

    public enum InvoiceStatus {
        PENDING, PAID, CANCELLED
    }
}
