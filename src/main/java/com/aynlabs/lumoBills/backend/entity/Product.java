package com.aynlabs.lumoBills.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Product extends AbstractEntity {

    @NotEmpty
    private String name;

    @ManyToOne
    @JoinColumn(name = "category_id")
    @NotNull
    private Category category;

    @NotNull
    private BigDecimal buyingPrice;

    @NotNull
    private BigDecimal unitPrice; // Selling price

    @NotNull
    private Integer quantityInStock = 0;

    private Integer reorderLevel = 10;

    private String description;

    // Helper to check stock
    public boolean isLowStock() {
        return quantityInStock != null && quantityInStock <= reorderLevel;
    }
}
