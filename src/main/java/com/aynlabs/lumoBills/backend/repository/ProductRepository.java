package com.aynlabs.lumoBills.backend.repository;

import com.aynlabs.lumoBills.backend.entity.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {
    
    @Query("select p from Product p " +
           "where lower(p.name) like lower(concat('%', :searchTerm, '%'))")
    List<Product> search(@Param("searchTerm") String searchTerm);
}
