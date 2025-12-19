package com.aynlabs.lumoBills.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class StockHistory extends AbstractEntity {

    @ManyToOne
    private Product product;

    private Integer changeAmount; // Positive for add, negative for remove

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    private LocalDateTime timestamp;

    @ManyToOne
    private User conductedBy;
    
    private String notes;

    public enum TransactionType {
        PURCHASE, SALE, ADJUSTMENT, RETURN
    }
}
