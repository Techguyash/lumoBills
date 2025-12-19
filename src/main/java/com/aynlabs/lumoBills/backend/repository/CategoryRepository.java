package com.aynlabs.lumoBills.backend.repository;

import com.aynlabs.lumoBills.backend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
