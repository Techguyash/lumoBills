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
public class Purchase extends AbstractEntity {

    @NotEmpty
    private String productName;

    private String sellerName;

    @NotNull
    private Integer quantity;

    @NotNull
    private BigDecimal price;

    @NotNull
    private BigDecimal total;

    @NotNull
    private LocalDateTime purchaseDate;
}
