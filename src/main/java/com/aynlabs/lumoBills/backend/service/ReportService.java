package com.aynlabs.lumoBills.backend.service;

import com.aynlabs.lumoBills.backend.dto.SalesReportDTO;
import com.aynlabs.lumoBills.backend.dto.StockReportDTO;
import com.aynlabs.lumoBills.backend.entity.Invoice;
import com.aynlabs.lumoBills.backend.entity.StockHistory;
import com.aynlabs.lumoBills.backend.entity.StockHistory.TransactionType;
import com.aynlabs.lumoBills.backend.repository.InvoiceRepository;
import com.aynlabs.lumoBills.backend.repository.StockHistoryRepository;
import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final SystemSettingService settingService;
    private final InvoiceRepository invoiceRepository;
    private final StockHistoryRepository stockHistoryRepository;

    public List<SalesReportDTO> getSalesData(LocalDateTime start, LocalDateTime end) {
        return invoiceRepository.findByDateBetween(start, end).stream()
                .map(i -> SalesReportDTO.builder()
                        .invoiceId(i.getId().toString())
                        .date(i.getDate())
                        .customerName(i.getCustomer() != null ? i.getCustomer().getFullName() : "Unknown")
                        .subTotal(i.getSubTotal())
                        .taxAmount(i.getTaxAmount())
                        .discountAmount(i.getDiscountAmount())
                        .totalAmount(i.getTotalAmount())
                        .build())
                .collect(Collectors.toList());
    }

    public List<StockReportDTO> getStockHistoryData(LocalDateTime start, LocalDateTime end, TransactionType type) {
        List<StockHistory> list;
        if (type == null) {
            list = stockHistoryRepository.findByTimestampBetween(start, end);
        } else {
            list = stockHistoryRepository.findByTimestampBetweenAndType(start, end, type);
        }
        return list.stream()
                .map(s -> StockReportDTO.builder()
                        .date(s.getTimestamp())
                        .productName(s.getProduct() != null ? s.getProduct().getName() : "Unknown")
                        .type(s.getType())
                        .changeAmount(s.getChangeAmount())
                        .conductedBy(s.getConductedBy() != null ? s.getConductedBy().getName() : "System")
                        .notes(s.getNotes())
                        .build())
                .collect(Collectors.toList());
    }

    public byte[] exportToExcel(List<?> data, String[] headers, String[] fields) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Report");
            Row headerRow = sheet.createRow(0);

            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            int rowIdx = 1;
            for (Object item : data) {
                Row row = sheet.createRow(rowIdx++);
                for (int i = 0; i < fields.length; i++) {
                    try {
                        Field field = item.getClass().getDeclaredField(fields[i]);
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

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] generateInvoicePdf(Invoice invoice) throws Exception {
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
        parameters.put("CURRENCY", settingService.getValue("CURRENCY", "INR"));
        parameters.put("GST_NO", settingService.getValue("GST_NO", ""));
        parameters.put("COMPANY_PHONE", settingService.getValue("COMPANY_PHONE", ""));
        parameters.put("COMPANY_EMAIL", settingService.getValue("COMPANY_EMAIL", ""));

        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(invoice.getItems());

        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return JasperExportManager.exportReportToPdf(jasperPrint);
    }
}
