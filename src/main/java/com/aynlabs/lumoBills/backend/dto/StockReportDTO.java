package com.aynlabs.lumoBills.backend.dto;

import com.aynlabs.lumoBills.backend.entity.StockHistory.TransactionType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StockReportDTO {
    private LocalDateTime date;
    private String productName;
    private TransactionType type;
    private Integer changeAmount;
    private java.math.BigDecimal purchasePrice;
    private java.math.BigDecimal totalAmount;
    private String conductedBy;
    private String notes;
}
