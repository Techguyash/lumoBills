package com.aynlabs.lumoBills.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SalesReportDTO {
    private String invoiceId;
    private LocalDateTime date;
    private String customerName;
    private BigDecimal subTotal;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
}
