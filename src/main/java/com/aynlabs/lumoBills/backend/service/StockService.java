package com.aynlabs.lumoBills.backend.service;

import com.aynlabs.lumoBills.backend.entity.Product;
import com.aynlabs.lumoBills.backend.entity.StockHistory;
import com.aynlabs.lumoBills.backend.entity.StockHistory.TransactionType;
import com.aynlabs.lumoBills.backend.entity.User;
import com.aynlabs.lumoBills.backend.repository.ProductRepository;
import com.aynlabs.lumoBills.backend.repository.StockHistoryRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockHistoryRepository stockHistoryRepository;
    private final ProductRepository productRepository;

    @Transactional
    public void adjustStock(Product product, int amount, TransactionType type, User user, String notes) {
        adjustStock(product, amount, null, type, user, notes);
    }

    @Transactional
    public void adjustStock(Product product, int amount, java.math.BigDecimal purchasePrice, TransactionType type,
            User user, String notes) {
        // Update product stock
        int currentStock = product.getQuantityInStock() != null ? product.getQuantityInStock() : 0;
        product.setQuantityInStock(currentStock + amount);

        // Update product's last buying price if it's a purchase
        if (type == TransactionType.PURCHASE && purchasePrice != null) {
            product.setBuyingPrice(purchasePrice);
        }

        productRepository.save(product);

        // Record history
        StockHistory history = new StockHistory();
        history.setProduct(product);
        history.setChangeAmount(amount);
        history.setPurchasePrice(purchasePrice);

        if (purchasePrice != null) {
            history.setTotalAmount(purchasePrice.multiply(java.math.BigDecimal.valueOf(Math.abs(amount))));
        }

        history.setType(type);
        history.setTimestamp(LocalDateTime.now());
        history.setConductedBy(user);
        history.setNotes(notes);

        stockHistoryRepository.save(history);
    }

    public List<StockHistory> findRecentActivity(int limit) {
        return stockHistoryRepository.findAll(org.springframework.data.domain.PageRequest.of(0, limit,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,
                        "timestamp")))
                .getContent();
    }

    public java.math.BigDecimal getTotalPurchaseAmount() {
        return stockHistoryRepository.findByType(TransactionType.PURCHASE).stream()
                .map(sh -> sh.getTotalAmount() != null ? sh.getTotalAmount() : java.math.BigDecimal.ZERO)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }

    public java.util.List<StockHistory> findAll() {
        return stockHistoryRepository.findAll();
    }
}
