package com.aynlabs.lumoBills.backend.dto;

import com.aynlabs.lumoBills.backend.entity.StockHistory.TransactionType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReportDTO {
    private LocalDateTime date;
    private String productName;
    private TransactionType type;
    private Integer changeAmount;
    private String conductedBy;
    private String notes;
}
