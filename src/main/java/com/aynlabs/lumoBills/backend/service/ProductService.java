package com.aynlabs.lumoBills.backend.service;

import com.aynlabs.lumoBills.backend.entity.Product;
import com.aynlabs.lumoBills.backend.repository.ProductRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public List<Product> findAll(String stringFilter) {
        if (stringFilter == null || stringFilter.isEmpty()) {
            return productRepository.findAll();
        } else {
            return productRepository.search(stringFilter);
        }
    }

    public long count() {
        return productRepository.count();
    }

    public void delete(Product product) {
        productRepository.delete(product);
    }

    public void save(Product product) {
        if (product == null) {
            System.err.println("Product is null. Are you sure you have connected your form to the application?");
            return;
        }
        productRepository.save(product);
    }
}
