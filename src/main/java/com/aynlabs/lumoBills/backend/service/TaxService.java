package com.aynlabs.lumoBills.backend.service;

import com.aynlabs.lumoBills.backend.entity.Tax;
import com.aynlabs.lumoBills.backend.repository.TaxRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaxService {
    private final TaxRepository repository;

    public List<Tax> findAll() {
        return repository.findAll();
    }

    public List<Tax> findActive() {
        return repository.findByActiveTrue();
    }

    public void save(Tax tax) {
        repository.save(tax);
    }

    public void delete(Tax tax) {
        repository.delete(tax);
    }
}
