package com.aynlabs.lumoBills.backend.util;

import com.aynlabs.lumoBills.backend.entity.Category;
import com.aynlabs.lumoBills.backend.entity.Customer;
import com.aynlabs.lumoBills.backend.entity.Invoice;
import com.aynlabs.lumoBills.backend.entity.InvoiceItem;
import com.aynlabs.lumoBills.backend.entity.Product;
import com.aynlabs.lumoBills.backend.entity.Role;
import com.aynlabs.lumoBills.backend.entity.User;
import com.aynlabs.lumoBills.backend.repository.CategoryRepository;
import com.aynlabs.lumoBills.backend.repository.CustomerRepository;
import com.aynlabs.lumoBills.backend.repository.ProductRepository;
import com.aynlabs.lumoBills.backend.repository.UserRepository;
import com.aynlabs.lumoBills.backend.service.InvoiceService;
import com.vaadin.flow.spring.annotation.SpringComponent;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringComponent
@Profile("local")
public class DataGenerator {

    @Bean
    public CommandLineRunner loadData(UserRepository userRepository,
            ProductRepository productRepository,
            CustomerRepository customerRepository,
            CategoryRepository categoryRepository,
            InvoiceService invoiceService,
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
            Category grains = createCategory(categoryRepository, "Grains");
            Category fiber = createCategory(categoryRepository, "Fiber");
            Category commodity = createCategory(categoryRepository, "Commodity");
            Category oil = createCategory(categoryRepository, "Oil");

            // Generate Products
            createProduct(productRepository, "Premium Wheat", grains, new java.math.BigDecimal("400.00"),
                    new java.math.BigDecimal("450.00"), 500,
                    "High quality whole wheat");
            createProduct(productRepository, "Basmati Rice", grains, new java.math.BigDecimal("1000.00"),
                    new java.math.BigDecimal("1200.00"), 200,
                    "Aged extra long grain");
            createProduct(productRepository, "Raw Cotton", fiber, new java.math.BigDecimal("3000.00"),
                    new java.math.BigDecimal("3500.00"), 100,
                    "Grade A raw cotton bales");
            createProduct(productRepository, "Sugar", commodity, new java.math.BigDecimal("38.00"),
                    new java.math.BigDecimal("42.00"), 1000,
                    "Refined white sugar");
            createProduct(productRepository, "Soybean Oil", oil, new java.math.BigDecimal("130.00"),
                    new java.math.BigDecimal("150.00"), 300,
                    "Edible oil tin");

            // Generate Customers
            createCustomer(customerRepository, "Alice", "Smith", "alice@test.com");
            createCustomer(customerRepository, "Bob", "Jones", "bob@test.com");
            createCustomer(customerRepository, "Charlie", "Brown", "charlie@test.com");
            createCustomer(customerRepository, "Diana", "Prince", "diana@test.com");

            // Generate Invoices
            java.util.List<Product> products = productRepository.findAll();
            java.util.List<Customer> customers = customerRepository.findAll();

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

    private Category createCategory(
            CategoryRepository repo, String name) {
        Category c = new Category();
        c.setName(name);
        return repo.save(c);
    }

    private void createProduct(ProductRepository repo, String name,
            Category category, java.math.BigDecimal buyPrice,
            BigDecimal sellPrice, int qty, String desc) {
        Product p = new Product();
        p.setName(name);
        p.setCategory(category);
        p.setBuyingPrice(buyPrice);
        p.setUnitPrice(sellPrice);
        p.setQuantityInStock(qty);
        p.setDescription(desc);
        repo.save(p);
    }

    private void createCustomer(CustomerRepository repo, String first,
            String last, String email) {
        Customer c = new Customer();
        c.setFirstName(first);
        c.setLastName(last);
        c.setEmail(email);
        repo.save(c);
    }

    private void createInvoice(InvoiceService service,
            Customer customer, java.time.LocalDate date,
            java.util.Map<Product, Integer> items) {

        Invoice invoice = new Invoice();
        invoice.setCustomer(customer);
        invoice.setDate(date.atStartOfDay());
        invoice.setStatus(Invoice.InvoiceStatus.PAID);

        java.util.List<InvoiceItem> invoiceItems = new java.util.ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (java.util.Map.Entry<Product, Integer> entry : items.entrySet()) {
            Product p = entry.getKey();
            Integer qty = entry.getValue();

            InvoiceItem item = new InvoiceItem();
            item.setProduct(p);
            item.setQuantity(qty);
            item.setUnitPrice(p.getUnitPrice());
            item.setInvoice(invoice);

            total = total.add(p.getUnitPrice().multiply(BigDecimal.valueOf(qty)));

            invoiceItems.add(item);
        }

        invoice.setItems(invoiceItems);
        invoice.setSubTotal(total);
        invoice.setDiscountAmount(BigDecimal.ZERO);
        invoice.setTaxAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(total);
        service.saveInvoice(invoice);
    }
}
