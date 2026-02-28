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

    public void save(Purchase purchase) {
        if (purchase.getPurchaseDate() == null) {
            purchase.setPurchaseDate(LocalDateTime.now());
        }
        if (purchase.getTotal() == null && purchase.getPrice() != null && purchase.getQuantity() != null) {
            purchase.setTotal(purchase.getPrice().multiply(BigDecimal.valueOf(purchase.getQuantity())));
        }
        purchaseRepository.save(purchase);
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
}
