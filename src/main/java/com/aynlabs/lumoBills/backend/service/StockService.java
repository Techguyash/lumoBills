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
        // Update product stock
        int currentStock = product.getQuantityInStock() != null ? product.getQuantityInStock() : 0;
        product.setQuantityInStock(currentStock + amount);
        productRepository.save(product);

        // Record history
        StockHistory history = new StockHistory();
        history.setProduct(product);
        history.setChangeAmount(amount);
        history.setType(type);
        history.setTimestamp(LocalDateTime.now());
        history.setConductedBy(user);
        history.setNotes(notes);

        stockHistoryRepository.save(history);
    }
    
    public List<StockHistory> findAll() {
        return stockHistoryRepository.findAll();
    }
}
