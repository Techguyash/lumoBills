package com.aynlabs.lumoBills.backend.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductProfitDTO {
    private String productName;
    private BigDecimal buyingPrice;
    private BigDecimal sellingPrice;
    private BigDecimal profitPerUnit;
    private String status; // Profit or Loss
}
