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

    @Transactional
    public void createInvoice(Invoice invoice, User creator) {
        // Deduct stock for each item
        for (InvoiceItem item : invoice.getItems()) {
            stockService.adjustStock(
                    item.getProduct(),
                    -item.getQuantity(),
                    TransactionType.SALE,
                    creator,
                    "Invoice #" + (invoice.getId() != null ? invoice.getId() : "NEW"));
        }

        invoice.setCreatedBy(creator);
        // Note: JPA Cascade will save items, but we might need to ensure they have the
        // invoice ref if bidirectional
        invoice.getItems().forEach(item -> item.setInvoice(invoice));

        invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice saveInvoice(Invoice invoice) {
        // Basic save without stock deduction logic for updates/drafts
        // Make sure items reference the invoice
        if (invoice.getItems() != null) {
            invoice.getItems().forEach(item -> item.setInvoice(invoice));
        }
        return invoiceRepository.save(invoice);
    }

    public java.math.BigDecimal getTotalSalesAmount() {
        return invoiceRepository.findAll().stream()
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
