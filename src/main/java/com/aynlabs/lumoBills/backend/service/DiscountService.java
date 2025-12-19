package com.aynlabs.lumoBills.backend.service;

import com.aynlabs.lumoBills.backend.entity.Discount;
import com.aynlabs.lumoBills.backend.repository.DiscountRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DiscountService {
    private final DiscountRepository repository;

    public List<Discount> findAll() {
        return repository.findAll();
    }

    public List<Discount> findActive() {
        return repository.findByActiveTrue();
    }

    public void save(Discount discount) {
        repository.save(discount);
    }

    public void delete(Discount discount) {
        repository.delete(discount);
    }
}
