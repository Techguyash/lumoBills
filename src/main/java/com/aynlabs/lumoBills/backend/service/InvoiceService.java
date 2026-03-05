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
    private final LedgerService ledgerService;
    private final com.aynlabs.lumoBills.backend.repository.PaymentRepository paymentRepository;
    private final com.aynlabs.lumoBills.backend.service.CustomerService customerService;

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
            invoice.setAmountPaid(invoice.getTotalAmount());
            invoice.setAmountPending(java.math.BigDecimal.ZERO);

            // Deduct stock for each item
            for (InvoiceItem item : invoice.getItems()) {
                stockService.adjustStock(
                        item.getProduct(),
                        -item.getQuantity(),
                        TransactionType.SALE,
                        creator,
                        "Invoice #" + invoice.getInvoiceNumber());
            }

            // Record Income Ledger
            com.aynlabs.lumoBills.backend.entity.LedgerEntry entry = new com.aynlabs.lumoBills.backend.entity.LedgerEntry();
            entry.setTransactionDate(java.time.LocalDateTime.now());
            entry.setType(com.aynlabs.lumoBills.backend.entity.LedgerEntry.EntryType.INCOME);
            entry.setCategory("Sales");
            entry.setAmount(invoice.getTotalAmount());
            entry.setDescription("Invoice #" + invoice.getInvoiceNumber() + " Paid");
            entry.setReferenceId(invoice.getInvoiceNumber());
            entry.setPaymentMode(invoice.getPaymentMode());
            ledgerService.recordEntry(entry);

        } else if (invoice.getStatus() == Invoice.InvoiceStatus.PENDING) {
            invoice.setAmountPaid(java.math.BigDecimal.ZERO);
            invoice.setAmountPending(invoice.getTotalAmount());

            // Update customer outstanding balance
            if (invoice.getCustomer() != null) {
                com.aynlabs.lumoBills.backend.entity.Customer c = invoice.getCustomer();
                c.setOutstandingBalance(c.getOutstandingBalance().add(invoice.getTotalAmount()));
                customerService.save(c);
            }
        } else if (invoice.getStatus() == Invoice.InvoiceStatus.PARTIAL) {
            if (invoice.getAmountPaid() == null) {
                invoice.setAmountPaid(java.math.BigDecimal.ZERO);
            }
            invoice.setAmountPending(invoice.getTotalAmount().subtract(invoice.getAmountPaid()));

            // Deduct stock for each item
            for (InvoiceItem item : invoice.getItems()) {
                stockService.adjustStock(
                        item.getProduct(),
                        -item.getQuantity(),
                        TransactionType.SALE,
                        creator,
                        "Invoice #" + invoice.getInvoiceNumber());
            }

            // Record Income Ledger for the received amount
            if (invoice.getAmountPaid().compareTo(java.math.BigDecimal.ZERO) > 0) {
                com.aynlabs.lumoBills.backend.entity.LedgerEntry entry = new com.aynlabs.lumoBills.backend.entity.LedgerEntry();
                entry.setTransactionDate(java.time.LocalDateTime.now());
                entry.setType(com.aynlabs.lumoBills.backend.entity.LedgerEntry.EntryType.INCOME);
                entry.setCategory("Sales");
                entry.setAmount(invoice.getAmountPaid());
                entry.setDescription("Invoice #" + invoice.getInvoiceNumber() + " Partially Paid");
                entry.setReferenceId(invoice.getInvoiceNumber());
                entry.setPaymentMode(invoice.getPaymentMode());
                ledgerService.recordEntry(entry);
            }

            // Update customer outstanding balance with pending amount
            if (invoice.getCustomer() != null && invoice.getAmountPending().compareTo(java.math.BigDecimal.ZERO) > 0) {
                com.aynlabs.lumoBills.backend.entity.Customer c = invoice.getCustomer();
                c.setOutstandingBalance(c.getOutstandingBalance().add(invoice.getAmountPending()));
                customerService.save(c);
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
    public void finalizeDraft(Invoice invoice, User user) {
        if (invoice.getStatus() == Invoice.InvoiceStatus.PENDING
                || invoice.getStatus() == Invoice.InvoiceStatus.PARTIAL) {

            // Track amount to update customer balance
            java.math.BigDecimal amountRemaining = invoice.getAmountPending();

            invoice.setStatus(Invoice.InvoiceStatus.PAID);
            invoice.setAmountPaid(invoice.getTotalAmount());
            invoice.setAmountPending(java.math.BigDecimal.ZERO);

            for (InvoiceItem item : invoice.getItems()) {
                stockService.adjustStock(
                        item.getProduct(),
                        -item.getQuantity(),
                        TransactionType.SALE,
                        user,
                        "Invoice #" + invoice.getInvoiceNumber());
            }

            // Record Income Ledger
            com.aynlabs.lumoBills.backend.entity.LedgerEntry entry = new com.aynlabs.lumoBills.backend.entity.LedgerEntry();
            entry.setTransactionDate(java.time.LocalDateTime.now());
            entry.setType(com.aynlabs.lumoBills.backend.entity.LedgerEntry.EntryType.INCOME);
            entry.setCategory("Sales");
            entry.setAmount(amountRemaining);
            entry.setDescription("Invoice #" + invoice.getInvoiceNumber() + " Finalized");
            entry.setReferenceId(invoice.getInvoiceNumber());
            entry.setPaymentMode(invoice.getPaymentMode());
            ledgerService.recordEntry(entry);

            // Update customer outstanding balance
            if (invoice.getCustomer() != null) {
                com.aynlabs.lumoBills.backend.entity.Customer c = invoice.getCustomer();
                // Decrease outstanding balance by the remaining amount just paid
                c.setOutstandingBalance(c.getOutstandingBalance().subtract(amountRemaining));
                customerService.save(c);
            }

            invoiceRepository.save(invoice);
        }
    }

    @Transactional
    public void cancelInvoice(Invoice invoice, User user) {
        if (invoice.getStatus() == Invoice.InvoiceStatus.CANCELLED) {
            return; // Already cancelled
        }

        // If it was PAID or PARTIAL, we restore stock and handle refunds
        if (invoice.getStatus() == Invoice.InvoiceStatus.PAID || invoice.getStatus() == Invoice.InvoiceStatus.PARTIAL) {
            for (InvoiceItem item : invoice.getItems()) {
                stockService.adjustStock(
                        item.getProduct(),
                        item.getQuantity(), // Positive to add back stock
                        TransactionType.RETURN,
                        user,
                        "Cancelled Invoice #" + invoice.getInvoiceNumber());
            }
        }

        // If money was paid, record an expense (refund)
        if (invoice.getAmountPaid().compareTo(java.math.BigDecimal.ZERO) > 0) {
            com.aynlabs.lumoBills.backend.entity.LedgerEntry refund = new com.aynlabs.lumoBills.backend.entity.LedgerEntry();
            refund.setTransactionDate(java.time.LocalDateTime.now());
            refund.setType(com.aynlabs.lumoBills.backend.entity.LedgerEntry.EntryType.EXPENSE);
            refund.setCategory("Refund");
            refund.setAmount(invoice.getAmountPaid());
            refund.setDescription("Refund for Cancelled Invoice #" + invoice.getInvoiceNumber());
            refund.setReferenceId(invoice.getInvoiceNumber());
            refund.setPaymentMode(invoice.getPaymentMode());
            ledgerService.recordEntry(refund);
        }

        // If they owed money, reduce their outstanding balance
        if (invoice.getCustomer() != null && invoice.getAmountPending().compareTo(java.math.BigDecimal.ZERO) > 0) {
            com.aynlabs.lumoBills.backend.entity.Customer c = invoice.getCustomer();
            c.setOutstandingBalance(c.getOutstandingBalance().subtract(invoice.getAmountPending()));
            customerService.save(c);
        }

        invoice.setStatus(Invoice.InvoiceStatus.CANCELLED);
        invoice.setAmountPaid(java.math.BigDecimal.ZERO);
        invoice.setAmountPending(java.math.BigDecimal.ZERO);
        invoiceRepository.save(invoice);
    }

    @Transactional
    public void addPayment(Invoice invoice, java.math.BigDecimal amount, Invoice.PaymentMode mode, String refNumber) {
        if (invoice.getStatus() == Invoice.InvoiceStatus.CANCELLED
                || invoice.getStatus() == Invoice.InvoiceStatus.PAID) {
            throw new IllegalStateException("Cannot add payment to a PAID or CANCELLED invoice.");
        }

        if (amount.compareTo(invoice.getAmountPending()) > 0) {
            throw new IllegalArgumentException("Payment amount cannot exceed pending amount.");
        }

        // Create Payment
        com.aynlabs.lumoBills.backend.entity.Payment payment = new com.aynlabs.lumoBills.backend.entity.Payment();
        payment.setInvoice(invoice);
        payment.setAmount(amount);
        payment.setPaymentDate(java.time.LocalDateTime.now());
        payment.setMode(mode);
        payment.setReferenceNumber(refNumber);
        paymentRepository.save(payment);

        // Update Invoice
        invoice.setAmountPaid(invoice.getAmountPaid().add(amount));
        invoice.setAmountPending(invoice.getAmountPending().subtract(amount));

        if (invoice.getAmountPending().compareTo(java.math.BigDecimal.ZERO) == 0) {
            invoice.setStatus(Invoice.InvoiceStatus.PAID);
        } else {
            invoice.setStatus(Invoice.InvoiceStatus.PARTIAL);
        }
        invoiceRepository.save(invoice);

        // Update Customer Balance
        if (invoice.getCustomer() != null) {
            com.aynlabs.lumoBills.backend.entity.Customer c = invoice.getCustomer();
            c.setOutstandingBalance(c.getOutstandingBalance().subtract(amount));
            customerService.save(c);
        }

        // Record Income
        com.aynlabs.lumoBills.backend.entity.LedgerEntry entry = new com.aynlabs.lumoBills.backend.entity.LedgerEntry();
        entry.setTransactionDate(java.time.LocalDateTime.now());
        entry.setType(com.aynlabs.lumoBills.backend.entity.LedgerEntry.EntryType.INCOME);
        entry.setCategory("Sales");
        entry.setAmount(amount);
        entry.setDescription("Payment towards Invoice #" + invoice.getInvoiceNumber());
        entry.setReferenceId(invoice.getInvoiceNumber());
        entry.setPaymentMode(mode);
        ledgerService.recordEntry(entry);
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

    public java.math.BigDecimal getPendingSalesAmount() {
        return invoiceRepository.findAll().stream()
                .filter(i -> i.getStatus() == Invoice.InvoiceStatus.PENDING
                        || i.getStatus() == Invoice.InvoiceStatus.PARTIAL)
                .map(i -> i.getAmountPending() != null ? i.getAmountPending() : java.math.BigDecimal.ZERO)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }

    public long count() {
        return invoiceRepository.count();
    }

    public java.util.List<Invoice> findAll() {
        return invoiceRepository.findAll();
    }
}
