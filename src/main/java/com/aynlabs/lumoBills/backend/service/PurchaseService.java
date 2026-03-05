package com.aynlabs.lumoBills.backend.service;

import com.aynlabs.lumoBills.backend.entity.Purchase;
import com.aynlabs.lumoBills.backend.repository.PurchaseRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PurchaseService {
    private final PurchaseRepository purchaseRepository;
    private final StockService stockService;
    private final com.aynlabs.lumoBills.backend.security.SecurityService securityService;
    private final LedgerService ledgerService;

    @org.springframework.transaction.annotation.Transactional
    public void save(Purchase purchase) {
        boolean isNew = purchase.getId() == null;
        if (purchase.getPurchaseDate() == null) {
            purchase.setPurchaseDate(LocalDateTime.now());
        }
        if (purchase.getTotal() == null && purchase.getPrice() != null && purchase.getQuantity() != null) {
            purchase.setTotal(purchase.getPrice().multiply(BigDecimal.valueOf(purchase.getQuantity())));
        }

        purchaseRepository.save(purchase);

        if (isNew) {
            com.aynlabs.lumoBills.backend.entity.User user = securityService.getAuthenticatedUser();
            stockService.adjustStock(
                    purchase.getProduct(),
                    purchase.getQuantity(),
                    com.aynlabs.lumoBills.backend.entity.StockHistory.TransactionType.PURCHASE,
                    user,
                    "Purchase from " + (purchase.getSellerName() != null ? purchase.getSellerName() : "Vendor"));

            // Record Expense Ledger
            com.aynlabs.lumoBills.backend.entity.LedgerEntry entry = new com.aynlabs.lumoBills.backend.entity.LedgerEntry();
            entry.setTransactionDate(purchase.getPurchaseDate());
            entry.setType(com.aynlabs.lumoBills.backend.entity.LedgerEntry.EntryType.EXPENSE);
            entry.setCategory("Raw Material Purchase");
            entry.setAmount(purchase.getTotal());
            entry.setDescription("Purchase: " + purchase.getProduct().getName() + " from " + purchase.getSellerName());
            entry.setReferenceId(purchase.getId() != null ? purchase.getId().toString() : "PURCHASE");
            entry.setPaymentMode(com.aynlabs.lumoBills.backend.entity.Invoice.PaymentMode.CASH);
            ledgerService.recordEntry(entry);
        }
    }

    public List<Purchase> findAll() {
        return purchaseRepository.findAll();
    }

    public List<Purchase> findByDateBetween(LocalDateTime start, LocalDateTime end) {
        return purchaseRepository.findByPurchaseDateBetween(start, end);
    }

    public BigDecimal getTotalAmountBetween(LocalDateTime start, LocalDateTime end) {
        return findByDateBetween(start, end).stream()
                .map(Purchase::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @org.springframework.transaction.annotation.Transactional
    public void delete(Purchase purchase) {
        // Reverse stock
        com.aynlabs.lumoBills.backend.entity.User user = securityService.getAuthenticatedUser();
        stockService.adjustStock(
                purchase.getProduct(),
                -purchase.getQuantity(),
                com.aynlabs.lumoBills.backend.entity.StockHistory.TransactionType.RETURN,
                user,
                "Deleted Purchase from " + (purchase.getSellerName() != null ? purchase.getSellerName() : "Vendor"));

        // Reverse Ledger by recording an income/refund
        com.aynlabs.lumoBills.backend.entity.LedgerEntry entry = new com.aynlabs.lumoBills.backend.entity.LedgerEntry();
        entry.setTransactionDate(LocalDateTime.now());
        entry.setType(com.aynlabs.lumoBills.backend.entity.LedgerEntry.EntryType.INCOME);
        entry.setCategory("Refund/Purchase Cancel");
        entry.setAmount(purchase.getTotal());
        entry.setDescription(
                "Deleted Purchase: " + purchase.getProduct().getName() + " from " + purchase.getSellerName());
        entry.setReferenceId(purchase.getId() != null ? purchase.getId().toString() : "PURCHASE_DEL");
        entry.setPaymentMode(com.aynlabs.lumoBills.backend.entity.Invoice.PaymentMode.CASH);
        ledgerService.recordEntry(entry);

        purchaseRepository.delete(purchase);
    }

    @org.springframework.transaction.annotation.Transactional
    public void update(Purchase updatedPurchase) {
        Purchase old = purchaseRepository.findById(updatedPurchase.getId()).orElse(null);
        if (old != null) {
            int qtyDiff = updatedPurchase.getQuantity() - old.getQuantity();
            BigDecimal totalDiff = updatedPurchase.getTotal().subtract(old.getTotal());

            if (qtyDiff != 0) {
                com.aynlabs.lumoBills.backend.entity.User user = securityService.getAuthenticatedUser();
                stockService.adjustStock(
                        updatedPurchase.getProduct(),
                        qtyDiff,
                        com.aynlabs.lumoBills.backend.entity.StockHistory.TransactionType.PURCHASE,
                        user,
                        "Updated Purchase from "
                                + (updatedPurchase.getSellerName() != null ? updatedPurchase.getSellerName()
                                        : "Vendor"));
            }
            if (totalDiff.compareTo(BigDecimal.ZERO) != 0) {
                com.aynlabs.lumoBills.backend.entity.LedgerEntry entry = new com.aynlabs.lumoBills.backend.entity.LedgerEntry();
                entry.setTransactionDate(LocalDateTime.now());
                if (totalDiff.compareTo(BigDecimal.ZERO) > 0) {
                    entry.setType(com.aynlabs.lumoBills.backend.entity.LedgerEntry.EntryType.EXPENSE);
                    entry.setDescription("Additional charge for Updated Purchase");
                    entry.setAmount(totalDiff);
                } else {
                    entry.setType(com.aynlabs.lumoBills.backend.entity.LedgerEntry.EntryType.INCOME);
                    entry.setDescription("Refund for Updated Purchase");
                    entry.setAmount(totalDiff.abs());
                }
                entry.setCategory("Raw Material Purchase");
                entry.setReferenceId(updatedPurchase.getId().toString());
                entry.setPaymentMode(com.aynlabs.lumoBills.backend.entity.Invoice.PaymentMode.CASH);
                ledgerService.recordEntry(entry);
            }
        }
        purchaseRepository.save(updatedPurchase);
    }
}
