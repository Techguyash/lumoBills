package com.aynlabs.lumoBills.backend.repository;

import com.aynlabs.lumoBills.backend.entity.Customer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    
    @Query("select c from Customer c " +
           "where lower(c.firstName) like lower(concat('%', :searchTerm, '%')) " +
           "or lower(c.lastName) like lower(concat('%', :searchTerm, '%'))")
    List<Customer> search(@Param("searchTerm") String searchTerm);
}
