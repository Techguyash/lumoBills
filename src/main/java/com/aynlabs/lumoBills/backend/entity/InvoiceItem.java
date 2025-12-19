package com.aynlabs.lumoBills.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class InvoiceItem extends AbstractEntity {

    @ManyToOne
    private Invoice invoice;

    @ManyToOne
    private Product product;

    private Integer quantity;

    private BigDecimal unitPrice;

    public BigDecimal getSubTotal() {
        if (unitPrice == null || quantity == null) return BigDecimal.ZERO;
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
