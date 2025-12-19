package com.aynlabs.lumoBills.backend.service;

import com.aynlabs.lumoBills.backend.entity.Invoice;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final SystemSettingService settingService;
    private final com.aynlabs.lumoBills.backend.repository.InvoiceRepository invoiceRepository;
    private final com.aynlabs.lumoBills.backend.repository.StockHistoryRepository stockHistoryRepository;

    public java.util.List<com.aynlabs.lumoBills.backend.dto.SalesReportDTO> getSalesData(java.time.LocalDateTime start,
            java.time.LocalDateTime end) {
        return invoiceRepository.findByDateBetween(start, end).stream()
                .map(i -> com.aynlabs.lumoBills.backend.dto.SalesReportDTO.builder()
                        .invoiceId(i.getId().toString())
                        .date(i.getDate())
                        .customerName(i.getCustomer() != null ? i.getCustomer().getFullName() : "Unknown")
                        .subTotal(i.getSubTotal())
                        .taxAmount(i.getTaxAmount())
                        .discountAmount(i.getDiscountAmount())
                        .totalAmount(i.getTotalAmount())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    public java.util.List<com.aynlabs.lumoBills.backend.dto.StockReportDTO> getStockHistoryData(
            java.time.LocalDateTime start, java.time.LocalDateTime end,
            com.aynlabs.lumoBills.backend.entity.StockHistory.TransactionType type) {
        java.util.List<com.aynlabs.lumoBills.backend.entity.StockHistory> list;
        if (type == null) {
            list = stockHistoryRepository.findByTimestampBetween(start, end);
        } else {
            list = stockHistoryRepository.findByTimestampBetweenAndType(start, end, type);
        }
        return list.stream()
                .map(s -> com.aynlabs.lumoBills.backend.dto.StockReportDTO.builder()
                        .date(s.getTimestamp())
                        .productName(s.getProduct() != null ? s.getProduct().getName() : "Unknown")
                        .type(s.getType())
                        .changeAmount(s.getChangeAmount())
                        .conductedBy(s.getConductedBy() != null ? s.getConductedBy().getName() : "System")
                        .notes(s.getNotes())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    public byte[] exportToExcel(java.util.List<?> data, String[] headers, String[] fields) throws java.io.IOException {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Report");
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);

            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            int rowIdx = 1;
            for (Object item : data) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIdx++);
                for (int i = 0; i < fields.length; i++) {
                    try {
                        java.lang.reflect.Field field = item.getClass().getDeclaredField(fields[i]);
                        field.setAccessible(true);
                        Object value = field.get(item);
                        if (value != null) {
                            row.createCell(i).setCellValue(value.toString());
                        }
                    } catch (Exception e) {
                        row.createCell(i).setCellValue("");
                    }
                }
            }

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] generateInvoicePdf(Invoice invoice) throws Exception {
        // ... (existing logic)
        InputStream template = getClass().getResourceAsStream("/reports/invoice.jrxml");
        if (template == null) {
            throw new RuntimeException("Invoice template not found!");
        }

        JasperReport jasperReport = JasperCompileManager.compileReport(template);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("INVOICE_ID", invoice.getId());
        parameters.put("CUSTOMER_NAME", invoice.getCustomer().getFullName());
        parameters.put("TOTAL_AMOUNT", invoice.getTotalAmount());
        parameters.put("SUBTOTAL", invoice.getSubTotal());
        parameters.put("TAX", invoice.getTaxAmount());
        parameters.put("DISCOUNT", invoice.getDiscountAmount());

        parameters.put("COMPANY_NAME", settingService.getValue("COMPANY_NAME", "LumoBills Corp"));
        parameters.put("COMPANY_ADDRESS", settingService.getValue("COMPANY_ADDRESS", "123 Business St"));
        parameters.put("INVOICE_TERMS", settingService.getValue("INVOICE_TERMS", "Payment due within 30 days"));
        parameters.put("CURRENCY", settingService.getValue("CURRENCY", "$"));
        parameters.put("GST_NO", settingService.getValue("GST_NO", ""));
        parameters.put("COMPANY_PHONE", settingService.getValue("COMPANY_PHONE", ""));
        parameters.put("COMPANY_EMAIL", settingService.getValue("COMPANY_EMAIL", ""));

        net.sf.jasperreports.engine.data.JRBeanCollectionDataSource dataSource = new net.sf.jasperreports.engine.data.JRBeanCollectionDataSource(
                invoice.getItems());

        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return JasperExportManager.exportReportToPdf(jasperPrint);
    }

}
