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
    public CommandLineRunner loadData(
            UserRepository userRepository,
            ProductRepository productRepository,
            CustomerRepository customerRepository,
            CategoryRepository categoryRepository,
            com.aynlabs.lumoBills.backend.repository.InvoiceRepository invoiceRepository,
            com.aynlabs.lumoBills.backend.repository.PurchaseRepository purchaseRepository,
            com.aynlabs.lumoBills.backend.repository.LedgerEntryRepository ledgerEntryRepository,
            com.aynlabs.lumoBills.backend.repository.StockHistoryRepository stockHistoryRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() == 0L) {
                User admin = new User();
                admin.setName("Administrator");
                admin.setUsername("admin");
                admin.setHashedPassword(passwordEncoder.encode("admin"));
                admin.setRoles(Set.of(Role.ADMIN, Role.USER));
                userRepository.save(admin);

                User user = new User();
                user.setName("John User");
                user.setUsername("user");
                user.setHashedPassword(passwordEncoder.encode("user"));
                user.setRoles(Collections.singleton(Role.USER));
                userRepository.save(user);
            }

            if (productRepository.count() != 0L) {
                return;
            }

            User admin = userRepository.findByUsername("admin");

            // Categories
            Category breads = createCategory(categoryRepository, "Breads");
            Category pastries = createCategory(categoryRepository, "Pastries");
            Category cakes = createCategory(categoryRepository, "Cakes");
            Category rawMaterials = createCategory(categoryRepository, "Raw Materials");

            // Products
            Product flour = createProduct(productRepository, "Artisan Flour", rawMaterials, new BigDecimal("40.00"),
                    new BigDecimal("50.00"), "High quality baking flour");
            Product sugar = createProduct(productRepository, "Caster Sugar", rawMaterials, new BigDecimal("30.00"),
                    new BigDecimal("40.00"), "Fine baking sugar");
            Product butter = createProduct(productRepository, "French Butter", rawMaterials, new BigDecimal("150.00"),
                    new BigDecimal("180.00"), "Premium cultured butter");

            Product sourdough = createProduct(productRepository, "Sourdough Loaf", breads, new BigDecimal("50.00"),
                    new BigDecimal("120.00"), "Freshly baked sourdough");
            Product croissant = createProduct(productRepository, "Butter Croissant", pastries, new BigDecimal("30.00"),
                    new BigDecimal("80.00"), "Flaky layered pastry");
            Product chocCake = createProduct(productRepository, "Truffle Cake", cakes, new BigDecimal("300.00"),
                    new BigDecimal("800.00"), "Rich chocolate cake");
            Product muffin = createProduct(productRepository, "Blueberry Muffin", pastries, new BigDecimal("20.00"),
                    new BigDecimal("60.00"), "Soft crumb muffin");

            // Customers
            Customer alice = createCustomer(customerRepository, "Alice", "Wonderland", "alice@bakery.com");
            Customer bob = createCustomer(customerRepository, "Bob", "Builder", "bob@bakery.com");
            Customer charlie = createCustomer(customerRepository, "Charlie", "Chocolate", "charlie@bakery.com");

            // Purchases (Expense & Stock In)
            createPurchase(purchaseRepository, ledgerEntryRepository, stockHistoryRepository, productRepository,
                    flour, "Millers Co.", 100, java.time.LocalDateTime.now().minusDays(10), admin);
            createPurchase(purchaseRepository, ledgerEntryRepository, stockHistoryRepository, productRepository,
                    sugar, "Sweet Suppliers", 50, java.time.LocalDateTime.now().minusDays(9), admin);
            createPurchase(purchaseRepository, ledgerEntryRepository, stockHistoryRepository, productRepository,
                    butter, "Dairy Farms", 30, java.time.LocalDateTime.now().minusDays(8), admin);
            createPurchase(purchaseRepository, ledgerEntryRepository, stockHistoryRepository, productRepository,
                    sourdough, "In-house Bakery", 40, java.time.LocalDateTime.now().minusDays(2), admin);
            createPurchase(purchaseRepository, ledgerEntryRepository, stockHistoryRepository, productRepository,
                    croissant, "In-house Bakery", 50, java.time.LocalDateTime.now().minusDays(1), admin);
            createPurchase(purchaseRepository, ledgerEntryRepository, stockHistoryRepository, productRepository,
                    chocCake, "In-house Bakery", 10, java.time.LocalDateTime.now().minusDays(1), admin);
            createPurchase(purchaseRepository, ledgerEntryRepository, stockHistoryRepository, productRepository,
                    muffin, "In-house Bakery", 30, java.time.LocalDateTime.now().minusDays(1), admin);

            // Invoices (Income & Stock Out)
            createSalesInvoice(invoiceRepository, ledgerEntryRepository, stockHistoryRepository, productRepository,
                    alice, java.time.LocalDateTime.now().minusDays(1), java.util.Map.of(sourdough, 2, croissant, 4),
                    admin);

            createSalesInvoice(invoiceRepository, ledgerEntryRepository, stockHistoryRepository, productRepository,
                    bob, java.time.LocalDateTime.now(), java.util.Map.of(chocCake, 1, muffin, 5), admin);

            createSalesInvoice(invoiceRepository, ledgerEntryRepository, stockHistoryRepository, productRepository,
                    charlie, java.time.LocalDateTime.now().minusHours(5),
                    java.util.Map.of(croissant, 2, sourdough, 1, butter, 1), admin);

            System.out.println("Generated Bakery Demo Data");
        };
    }

    private Category createCategory(CategoryRepository repo, String name) {
        Category c = new Category();
        c.setName(name);
        return repo.save(c);
    }

    private Product createProduct(ProductRepository repo, String name, Category category, BigDecimal buy,
            BigDecimal sell, String desc) {
        Product p = new Product();
        p.setName(name);
        p.setCategory(category);
        p.setBuyingPrice(buy);
        p.setUnitPrice(sell);
        p.setQuantityInStock(0); // Starts at 0, filled by Purchase
        p.setDescription(desc);
        return repo.save(p);
    }

    private Customer createCustomer(CustomerRepository repo, String first, String last, String email) {
        Customer c = new Customer();
        c.setFirstName(first);
        c.setLastName(last);
        c.setEmail(email);
        c.setOutstandingBalance(BigDecimal.ZERO);
        return repo.save(c);
    }

    private void createPurchase(com.aynlabs.lumoBills.backend.repository.PurchaseRepository purchaseRepo,
            com.aynlabs.lumoBills.backend.repository.LedgerEntryRepository ledgerRepo,
            com.aynlabs.lumoBills.backend.repository.StockHistoryRepository stockRepo,
            ProductRepository prodRepo,
            Product productRef, String seller, int qty, java.time.LocalDateTime date, User user) {
        Product product = prodRepo.findById(productRef.getId()).orElse(productRef);
        BigDecimal total = product.getBuyingPrice().multiply(BigDecimal.valueOf(qty));

        // 1. Save Purchase
        com.aynlabs.lumoBills.backend.entity.Purchase p = new com.aynlabs.lumoBills.backend.entity.Purchase();
        p.setProduct(product);
        p.setSellerName(seller);
        p.setQuantity(qty);
        p.setPrice(product.getBuyingPrice());
        p.setTotal(total);
        p.setPurchaseDate(date);
        purchaseRepo.save(p);

        // 2. Adjust Stock
        product.setQuantityInStock(product.getQuantityInStock() + qty);
        prodRepo.save(product);

        // 3. Stock History
        com.aynlabs.lumoBills.backend.entity.StockHistory sh = new com.aynlabs.lumoBills.backend.entity.StockHistory();
        sh.setProduct(product);
        sh.setChangeAmount(qty);
        sh.setPurchasePrice(product.getBuyingPrice());
        sh.setTotalAmount(total);
        sh.setType(com.aynlabs.lumoBills.backend.entity.StockHistory.TransactionType.PURCHASE);
        sh.setTimestamp(date);
        sh.setConductedBy(user);
        sh.setNotes("Purchase from " + seller);
        stockRepo.save(sh);

        // 4. Ledger Entry (Expense)
        com.aynlabs.lumoBills.backend.entity.LedgerEntry le = new com.aynlabs.lumoBills.backend.entity.LedgerEntry();
        le.setTransactionDate(date);
        le.setType(com.aynlabs.lumoBills.backend.entity.LedgerEntry.EntryType.EXPENSE);
        le.setCategory("Purchases");
        le.setAmount(total);
        le.setDescription("Purchased " + qty + " of " + product.getName());
        le.setReferenceId(p.getId() != null ? p.getId().toString()
                : "PUR-" + java.util.UUID.randomUUID().toString().substring(0, 4));
        le.setPaymentMode(Invoice.PaymentMode.CASH);
        ledgerRepo.save(le);
    }

    private void createSalesInvoice(com.aynlabs.lumoBills.backend.repository.InvoiceRepository invoiceRepo,
            com.aynlabs.lumoBills.backend.repository.LedgerEntryRepository ledgerRepo,
            com.aynlabs.lumoBills.backend.repository.StockHistoryRepository stockRepo,
            ProductRepository prodRepo,
            Customer customer, java.time.LocalDateTime date, java.util.Map<Product, Integer> items, User user) {
        // 1. Setup Invoice
        Invoice invoice = new Invoice();
        invoice.setCustomer(customer);
        invoice.setDate(date);
        invoice.setStatus(Invoice.InvoiceStatus.PAID);
        invoice.setPaymentMode(Invoice.PaymentMode.CASH);
        invoice.setCreatedBy(user);
        invoice.setInvoiceNumber("INV-" + date.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "-"
                + java.util.UUID.randomUUID().toString().substring(0, 4).toUpperCase());

        java.util.List<InvoiceItem> invoiceItems = new java.util.ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (java.util.Map.Entry<Product, Integer> entry : items.entrySet()) {
            Product p = prodRepo.findById(entry.getKey().getId()).orElseThrow();
            Integer qty = entry.getValue();

            InvoiceItem it = new InvoiceItem();
            it.setProduct(p);
            it.setQuantity(qty);
            it.setUnitPrice(p.getUnitPrice());
            it.setInvoice(invoice);

            BigDecimal itemTotal = p.getUnitPrice().multiply(BigDecimal.valueOf(qty));
            total = total.add(itemTotal);
            invoiceItems.add(it);

            // 2. Adjust Stock
            p.setQuantityInStock(p.getQuantityInStock() - qty);
            prodRepo.save(p);

            // 3. Stock History
            com.aynlabs.lumoBills.backend.entity.StockHistory sh = new com.aynlabs.lumoBills.backend.entity.StockHistory();
            sh.setProduct(p);
            sh.setChangeAmount(-qty);
            sh.setTotalAmount(itemTotal);
            sh.setType(com.aynlabs.lumoBills.backend.entity.StockHistory.TransactionType.SALE);
            sh.setTimestamp(date);
            sh.setConductedBy(user);
            sh.setNotes("Sale via " + invoice.getInvoiceNumber());
            stockRepo.save(sh);
        }

        invoice.setItems(invoiceItems);
        invoice.setSubTotal(total);
        invoice.setTaxAmount(BigDecimal.ZERO);
        invoice.setDiscountAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(total);
        invoice.setAmountPaid(total);
        invoice.setAmountPending(BigDecimal.ZERO);
        invoiceRepo.save(invoice);

        // 4. Ledger Entry (Income)
        com.aynlabs.lumoBills.backend.entity.LedgerEntry le = new com.aynlabs.lumoBills.backend.entity.LedgerEntry();
        le.setTransactionDate(date);
        le.setType(com.aynlabs.lumoBills.backend.entity.LedgerEntry.EntryType.INCOME);
        le.setCategory("Sales");
        le.setAmount(total);
        le.setDescription("Sales from " + invoice.getInvoiceNumber());
        le.setReferenceId(invoice.getInvoiceNumber());
        le.setPaymentMode(Invoice.PaymentMode.CASH);
        ledgerRepo.save(le);
    }
}
