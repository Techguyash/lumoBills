package com.aynlabs.lumoBills.backend.service;

import com.aynlabs.lumoBills.backend.entity.Customer;
import com.aynlabs.lumoBills.backend.repository.CustomerRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    public List<Customer> findAll(String stringFilter) {
        if (stringFilter == null || stringFilter.isEmpty()) {
            return customerRepository.findAll();
        } else {
            return customerRepository.search(stringFilter);
        }
    }

    public void delete(Customer customer) {
        customerRepository.delete(customer);
    }

    public void save(Customer customer) {
        if (customer == null) {
            System.err.println("Customer is null.");
            return;
        }
        customerRepository.save(customer);
    }
}
