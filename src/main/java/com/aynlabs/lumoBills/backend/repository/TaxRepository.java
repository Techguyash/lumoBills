package com.aynlabs.lumoBills.backend.repository;

import com.aynlabs.lumoBills.backend.entity.Tax;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxRepository extends JpaRepository<Tax, Long> {
    List<Tax> findByActiveTrue();
}
