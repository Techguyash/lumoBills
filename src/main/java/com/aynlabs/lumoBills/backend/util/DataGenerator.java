package com.aynlabs.lumoBills.backend.util;

import com.aynlabs.lumoBills.backend.entity.Role;
import com.aynlabs.lumoBills.backend.entity.User;
import com.aynlabs.lumoBills.backend.repository.UserRepository;
import com.vaadin.flow.spring.annotation.SpringComponent;
import java.util.Collections;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringComponent
public class DataGenerator {

    @Bean
    public CommandLineRunner loadData(UserRepository userRepository,
            com.aynlabs.lumoBills.backend.repository.ProductRepository productRepository,
            com.aynlabs.lumoBills.backend.repository.CustomerRepository customerRepository,
            com.aynlabs.lumoBills.backend.repository.CategoryRepository categoryRepository,
            com.aynlabs.lumoBills.backend.service.InvoiceService invoiceService,
            PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() != 0L) {
                return;
            }
            // Create Admin
            User admin = new User();
            admin.setName("Administrator");
            admin.setUsername("admin");
            admin.setHashedPassword(passwordEncoder.encode("admin"));
            admin.setRoles(Set.of(Role.ADMIN, Role.USER));
            userRepository.save(admin);

            // Create User
            User user = new User();
            user.setName("John User");
            user.setUsername("user");
            user.setHashedPassword(passwordEncoder.encode("user"));
            user.setRoles(Collections.singleton(Role.USER));
            userRepository.save(user);

            // Generate Categories
            com.aynlabs.lumoBills.backend.entity.Category grains = createCategory(categoryRepository, "Grains");
            com.aynlabs.lumoBills.backend.entity.Category fiber = createCategory(categoryRepository, "Fiber");
            com.aynlabs.lumoBills.backend.entity.Category commodity = createCategory(categoryRepository, "Commodity");
            com.aynlabs.lumoBills.backend.entity.Category oil = createCategory(categoryRepository, "Oil");

            // Generate Products
            createProduct(productRepository, "Premium Wheat", grains, new java.math.BigDecimal("450.00"), 500,
                    "High quality whole wheat");
            createProduct(productRepository, "Basmati Rice", grains, new java.math.BigDecimal("1200.00"), 200,
                    "Aged extra long grain");
            createProduct(productRepository, "Raw Cotton", fiber, new java.math.BigDecimal("3500.00"), 100,
                    "Grade A raw cotton bales");
            createProduct(productRepository, "Sugar", commodity, new java.math.BigDecimal("42.00"), 1000,
                    "Refined white sugar");
            createProduct(productRepository, "Soybean Oil", oil, new java.math.BigDecimal("150.00"), 300,
                    "Edible oil tin");

            // Generate Customers
            createCustomer(customerRepository, "Alice", "Smith", "alice@test.com");
            createCustomer(customerRepository, "Bob", "Jones", "bob@test.com");
            createCustomer(customerRepository, "Charlie", "Brown", "charlie@test.com");
            createCustomer(customerRepository, "Diana", "Prince", "diana@test.com");

            // Generate Invoices
            java.util.List<com.aynlabs.lumoBills.backend.entity.Product> products = productRepository.findAll();
            java.util.List<com.aynlabs.lumoBills.backend.entity.Customer> customers = customerRepository.findAll();

            if (!products.isEmpty() && !customers.isEmpty()) {
                createInvoice(invoiceService, customers.get(0), java.time.LocalDate.now().minusDays(5),
                        java.util.Map.of(products.get(0), 10, products.get(1), 5));

                if (products.size() > 2 && customers.size() > 1) {
                    createInvoice(invoiceService, customers.get(1), java.time.LocalDate.now().minusDays(2),
                            java.util.Map.of(products.get(2), 20));
                }
            }

            System.out.println("Generated demo data");
        };
    }

    private com.aynlabs.lumoBills.backend.entity.Category createCategory(
            com.aynlabs.lumoBills.backend.repository.CategoryRepository repo, String name) {
        com.aynlabs.lumoBills.backend.entity.Category c = new com.aynlabs.lumoBills.backend.entity.Category();
        c.setName(name);
        return repo.save(c);
    }

    private void createProduct(com.aynlabs.lumoBills.backend.repository.ProductRepository repo, String name,
            com.aynlabs.lumoBills.backend.entity.Category category, java.math.BigDecimal price, int qty, String desc) {
        com.aynlabs.lumoBills.backend.entity.Product p = new com.aynlabs.lumoBills.backend.entity.Product();
        p.setName(name);
        p.setCategory(category);
        p.setUnitPrice(price);
        p.setQuantityInStock(qty);
        p.setDescription(desc);
        repo.save(p);
    }

    private void createCustomer(com.aynlabs.lumoBills.backend.repository.CustomerRepository repo, String first,
            String last, String email) {
        com.aynlabs.lumoBills.backend.entity.Customer c = new com.aynlabs.lumoBills.backend.entity.Customer();
        c.setFirstName(first);
        c.setLastName(last);
        c.setEmail(email);
        repo.save(c);
    }

    private void createInvoice(com.aynlabs.lumoBills.backend.service.InvoiceService service,
            com.aynlabs.lumoBills.backend.entity.Customer customer, java.time.LocalDate date,
            java.util.Map<com.aynlabs.lumoBills.backend.entity.Product, Integer> items) {

        com.aynlabs.lumoBills.backend.entity.Invoice invoice = new com.aynlabs.lumoBills.backend.entity.Invoice();
        invoice.setCustomer(customer);
        invoice.setDate(date.atStartOfDay());

        java.util.List<com.aynlabs.lumoBills.backend.entity.InvoiceItem> invoiceItems = new java.util.ArrayList<>();

        for (java.util.Map.Entry<com.aynlabs.lumoBills.backend.entity.Product, Integer> entry : items.entrySet()) {
            com.aynlabs.lumoBills.backend.entity.Product p = entry.getKey();
            Integer qty = entry.getValue();

            com.aynlabs.lumoBills.backend.entity.InvoiceItem item = new com.aynlabs.lumoBills.backend.entity.InvoiceItem();
            item.setProduct(p);
            item.setQuantity(qty);
            item.setUnitPrice(p.getUnitPrice());
            item.setInvoice(invoice);

            invoiceItems.add(item);
        }

        invoice.setItems(invoiceItems);
        service.saveInvoice(invoice);
    }
}
