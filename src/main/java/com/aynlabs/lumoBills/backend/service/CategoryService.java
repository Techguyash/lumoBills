package com.aynlabs.lumoBills.backend.service;

import com.aynlabs.lumoBills.backend.entity.Category;
import com.aynlabs.lumoBills.backend.repository.CategoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository repository;

    public List<Category> findAll() {
        return repository.findAll();
    }

    public Category save(Category category) {
        return repository.save(category);
    }

    public void delete(Category category) {
        repository.delete(category);
    }
}
