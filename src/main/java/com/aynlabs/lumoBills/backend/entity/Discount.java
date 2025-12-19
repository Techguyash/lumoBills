package com.aynlabs.lumoBills.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Discount extends AbstractEntity {
    private String name;
    private BigDecimal discountValue;

    @Enumerated(EnumType.STRING)
    private DiscountType type; // PERCENT or FIXED

    private boolean active = true;

    public enum DiscountType {
        PERCENT, FIXED
    }
}
