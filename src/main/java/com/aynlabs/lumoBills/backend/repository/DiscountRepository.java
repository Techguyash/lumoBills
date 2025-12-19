package com.aynlabs.lumoBills.backend.repository;

import com.aynlabs.lumoBills.backend.entity.Discount;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscountRepository extends JpaRepository<Discount, Long> {
    List<Discount> findByActiveTrue();
}
