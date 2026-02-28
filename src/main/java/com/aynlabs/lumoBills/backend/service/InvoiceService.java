package com.aynlabs.lumoBills.backend.service;

import com.aynlabs.lumoBills.backend.entity.Invoice;
import com.aynlabs.lumoBills.backend.entity.InvoiceItem;
import com.aynlabs.lumoBills.backend.entity.StockHistory.TransactionType;
import com.aynlabs.lumoBills.backend.entity.User;
import com.aynlabs.lumoBills.backend.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final StockService stockService;

    private void ensureInvoiceNumber(Invoice invoice) {
        if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().trim().isEmpty()) {
            String prefix = "INV-"
                    + java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd").format(java.time.LocalDate.now()) + "-";
            String random = java.util.UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            invoice.setInvoiceNumber(prefix + random);
        }
    }

    @Transactional
    public void createInvoice(Invoice invoice, User creator) {
        ensureInvoiceNumber(invoice);

        if (invoice.getStatus() == Invoice.InvoiceStatus.PAID) {
            // Deduct stock for each item
            for (InvoiceItem item : invoice.getItems()) {
                stockService.adjustStock(
                        item.getProduct(),
                        -item.getQuantity(),
                        TransactionType.SALE,
                        creator,
                        "Invoice #" + invoice.getInvoiceNumber());
            }
        }

        invoice.setCreatedBy(creator);
        // Note: JPA Cascade will save items, but we might need to ensure they have the
        // invoice ref if bidirectional
        if (invoice.getItems() != null) {
            invoice.getItems().forEach(item -> item.setInvoice(invoice));
        }

        invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice saveInvoice(Invoice invoice) {
        ensureInvoiceNumber(invoice);
        // Basic save without stock deduction logic for updates/drafts
        // Make sure items reference the invoice
        if (invoice.getItems() != null) {
            invoice.getItems().forEach(item -> item.setInvoice(invoice));
        }
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public void cancelInvoice(Invoice invoice, User user) {
        if (invoice.getStatus() == Invoice.InvoiceStatus.CANCELLED) {
            return; // Already cancelled
        }

        // If it was PAID, we restore stock. (Pending would not have deducted stock)
        if (invoice.getStatus() == Invoice.InvoiceStatus.PAID) {
            for (InvoiceItem item : invoice.getItems()) {
                stockService.adjustStock(
                        item.getProduct(),
                        item.getQuantity(), // Positive to add back stock
                        TransactionType.RETURN,
                        user,
                        "Cancelled Invoice #" + invoice.getInvoiceNumber());
            }
        }

        invoice.setStatus(Invoice.InvoiceStatus.CANCELLED);
        invoiceRepository.save(invoice);
    }

    public java.math.BigDecimal getTotalSalesAmount() {
        return getTotalSalesAmountBetween(java.time.LocalDateTime.MIN, java.time.LocalDateTime.MAX);
    }

    public java.math.BigDecimal getTotalSalesAmountBetween(java.time.LocalDateTime start, java.time.LocalDateTime end) {
        return invoiceRepository.findByDateBetween(start, end).stream()
                .filter(i -> i.getStatus() == Invoice.InvoiceStatus.PAID)
                .map(Invoice::getTotalAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }

    public long count() {
        return invoiceRepository.count();
    }

    public java.util.List<Invoice> findAll() {
        return invoiceRepository.findAll();
    }
}
