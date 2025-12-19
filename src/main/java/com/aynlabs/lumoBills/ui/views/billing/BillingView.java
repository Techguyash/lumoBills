package com.aynlabs.lumoBills.ui.views.billing;

import com.aynlabs.lumoBills.backend.entity.Customer;
import com.aynlabs.lumoBills.backend.entity.Invoice;
import com.aynlabs.lumoBills.backend.entity.InvoiceItem;
import com.aynlabs.lumoBills.backend.entity.Product;
import com.aynlabs.lumoBills.backend.entity.User;
import com.aynlabs.lumoBills.backend.security.SecurityService;
import com.aynlabs.lumoBills.backend.service.CustomerService;
import com.aynlabs.lumoBills.backend.service.DiscountService;
import com.aynlabs.lumoBills.backend.service.InvoiceService;
import com.aynlabs.lumoBills.backend.service.ProductService;
import com.aynlabs.lumoBills.backend.service.ReportService;
import com.aynlabs.lumoBills.backend.service.TaxService;
import com.aynlabs.lumoBills.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.component.html.Anchor;
import jakarta.annotation.security.PermitAll;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@PermitAll
@Route(value = "billing", layout = MainLayout.class)
@PageTitle("Billing | LumoBills")
public class BillingView extends VerticalLayout {

    private final ProductService productService;
    private final CustomerService customerService;
    private final InvoiceService invoiceService;
    private final SecurityService securityService;
    private final ReportService reportService;
    private final TaxService taxService;
    private final DiscountService discountService;

    // UI Components
    private ComboBox<Customer> customerSelect = new ComboBox<>("Customer");
    private ComboBox<Product> productSelect = new ComboBox<>("Add Product");
    private IntegerField quantity = new IntegerField("Quantity");
    private Button addButton = new Button("Add Item");

    private Grid<InvoiceItem> itemGrid = new Grid<>(InvoiceItem.class);

    // Summary Fields
    private Span subTotalSpan = new Span("Subtotal: $0.00");
    private Span discountSpan = new Span("Discount: -$0.00");
    private Span taxSpan = new Span("Tax: +$0.00");
    private Span totalAmount = new Span("Total: $0.00");

    private Button saveInvoiceButton = new Button("Save Invoice");
    private Anchor downloadLink = new Anchor();

    // State
    private List<InvoiceItem> currentItems = new ArrayList<>();
    private BigDecimal currentSubTotal = BigDecimal.ZERO;
    private BigDecimal currentTax = BigDecimal.ZERO;
    private BigDecimal currentDiscount = BigDecimal.ZERO;
    private BigDecimal currentTotal = BigDecimal.ZERO;
    private Customer selectedCustomer;

    // Recent Invoices
    private Grid<Invoice> recentGrid = new Grid<>(Invoice.class);

    private final com.aynlabs.lumoBills.backend.service.SystemSettingService settingService;
    private String currencySymbol = "$";

    public BillingView(ProductService productService, CustomerService customerService,
            InvoiceService invoiceService, SecurityService securityService,
            ReportService reportService, TaxService taxService, DiscountService discountService,
            com.aynlabs.lumoBills.backend.service.SystemSettingService settingService) {
        this.productService = productService;
        this.customerService = customerService;
        this.invoiceService = invoiceService;
        this.securityService = securityService;
        this.reportService = reportService;
        this.taxService = taxService;
        this.discountService = discountService;
        this.settingService = settingService;

        // Load currency symbol
        String currencyCode = settingService.getValue("CURRENCY", "INR");
        this.currencySymbol = getCurrencySymbol(currencyCode);

        addClassName("billing-view");
        setSizeFull();

        configureComponents();
        configureRecentGrid();

        // Update initial labels
        subTotalSpan.setText("Subtotal: " + this.currencySymbol + "0.00");
        discountSpan.setText("Discount: -" + this.currencySymbol + "0.00");
        taxSpan.setText("Tax: +" + this.currencySymbol + "0.00");
        totalAmount.setText("Total: " + this.currencySymbol + "0.00");

        SplitLayout splitLayout = new SplitLayout();
        splitLayout.setSizeFull();
        splitLayout.addToPrimary(createInvoiceForm());
        splitLayout.addToSecondary(createInvoiceHistory());
        splitLayout.setSplitterPosition(70);

        add(splitLayout);
        updateRecentInvoices();
    }

    /**
     * Convert currency code to symbol
     */
    private String getCurrencySymbol(String currencyCode) {
        return switch (currencyCode) {
            case "INR" -> "₹";
            case "USD" -> "$";
            case "EUR" -> "€";
            case "GBP" -> "£";
            case "JPY" -> "¥";
            default -> currencyCode + " "; // Fallback to code if unknown
        };
    }

    private VerticalLayout createInvoiceForm() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);

        HorizontalLayout toolbar = new HorizontalLayout(customerSelect, productSelect, quantity, addButton);
        toolbar.setAlignItems(Alignment.BASELINE);

        addButton.addClickListener(e -> addItem());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        saveInvoiceButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        saveInvoiceButton.addClickListener(e -> saveInvoice());
        saveInvoiceButton.setEnabled(false);

        downloadLink.getElement().setAttribute("download", true);
        downloadLink.setVisible(false);
        add(downloadLink);

        // Styling summary
        totalAmount.getStyle().set("font-size", "1.5em").set("font-weight", "bold");
        subTotalSpan.getStyle().set("font-size", "0.9em");
        discountSpan.getStyle().set("font-size", "0.9em").set("color", "green");
        taxSpan.getStyle().set("font-size", "0.9em").set("color", "red");

        VerticalLayout summaryLayout = new VerticalLayout(subTotalSpan, discountSpan, taxSpan, totalAmount);
        summaryLayout.setSpacing(false);
        summaryLayout.setPadding(false);
        summaryLayout.setAlignItems(Alignment.END);

        HorizontalLayout footer = new HorizontalLayout(summaryLayout, saveInvoiceButton);
        footer.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        footer.setWidthFull();
        footer.setJustifyContentMode(JustifyContentMode.BETWEEN);

        layout.add(new H3("New Invoice"), toolbar, itemGrid, footer);
        return layout;
    }

    private VerticalLayout createInvoiceHistory() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.add(new H3("Recent Invoices"), recentGrid);
        return layout;
    }

    private void configureRecentGrid() {
        recentGrid.setSizeFull();
        recentGrid.setColumns("id", "date", "totalAmount");
        recentGrid.addColumn(invoice -> invoice.getCustomer() != null ? invoice.getCustomer().getFullName() : "-")
                .setHeader("Customer");
        recentGrid.addColumn(invoice -> invoice.getCustomer() != null ? invoice.getCustomer().getFullName() : "-")
                .setHeader("Customer");
        // recentGrid.getColumns().forEach(col -> col.setAutoWidth(true));
        com.aynlabs.lumoBills.ui.util.GridHelper.setBasicProperties(recentGrid);
    }

    private void updateRecentInvoices() {
        // Simple implementation: all invoices, could be optimized to top 10
        List<Invoice> all = invoiceService.findAll();
        // Sort descending by id or date to show newest first
        all.sort((i1, i2) -> i2.getId().compareTo(i1.getId()));
        recentGrid.setItems(all.stream().limit(10).toList());
    }

    private void configureComponents() {
        customerSelect.setItems(customerService.findAll());
        customerSelect.setItemLabelGenerator(Customer::getFullName);
        customerSelect.addValueChangeListener(e -> {
            selectedCustomer = e.getValue();
            updateSaveButtonState();
        });

        productSelect.setItems(productService.findAll());
        productSelect.setItemLabelGenerator(Product::getName);

        quantity.setValue(1);
        quantity.setMin(1);

        itemGrid.removeAllColumns();
        itemGrid.addColumn(item -> item.getProduct().getName()).setHeader("Product");
        itemGrid.addColumn(InvoiceItem::getQuantity).setHeader("Quantity");
        itemGrid.addColumn(InvoiceItem::getUnitPrice).setHeader("Unit Price");
        itemGrid.addColumn(InvoiceItem::getSubTotal).setHeader("Subtotal");

        itemGrid.addComponentColumn(item -> {
            Button removeBtn = new Button(
                    new com.vaadin.flow.component.icon.Icon(com.vaadin.flow.component.icon.VaadinIcon.TRASH));
            removeBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            removeBtn.addClickListener(e -> {
                currentItems.remove(item);
                refreshGrid();
                Notification.show("Item removed", 2000, Notification.Position.BOTTOM_END);
            });
            return removeBtn;
        }).setHeader("Actions");

        com.aynlabs.lumoBills.ui.util.GridHelper.setBasicProperties(itemGrid);
    }

    private void addItem() {
        Product product = productSelect.getValue();
        Integer qty = quantity.getValue();

        if (product != null && qty != null && qty > 0) {
            // Check total quantity needed including what's already in the grid
            int currentQtyInGrid = currentItems.stream()
                    .filter(item -> item.getProduct().equals(product))
                    .mapToInt(InvoiceItem::getQuantity)
                    .sum();

            if (product.getQuantityInStock() < (qty + currentQtyInGrid)) {
                Notification.show("Insufficient Stock! Available: " + product.getQuantityInStock(),
                        3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            // Check if product already exists in currentItems
            java.util.Optional<InvoiceItem> existingItem = currentItems.stream()
                    .filter(item -> item.getProduct().equals(product))
                    .findFirst();

            if (existingItem.isPresent()) {
                InvoiceItem item = existingItem.get();
                item.setQuantity(item.getQuantity() + qty);
            } else {
                InvoiceItem item = new InvoiceItem();
                item.setProduct(product);
                item.setQuantity(qty);
                item.setUnitPrice(product.getUnitPrice());
                currentItems.add(item);
            }

            refreshGrid();
            quantity.setValue(1);
        }
    }

    private void refreshGrid() {
        itemGrid.setItems(currentItems);
        calculateTotal();
        updateSaveButtonState();
    }

    private void calculateTotal() {
        // 1. Subtotal
        currentSubTotal = currentItems.stream()
                .map(InvoiceItem::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Discounts
        List<com.aynlabs.lumoBills.backend.entity.Discount> activeDiscounts = discountService.findActive();
        currentDiscount = BigDecimal.ZERO;

        for (com.aynlabs.lumoBills.backend.entity.Discount d : activeDiscounts) {
            if (d.getType() == com.aynlabs.lumoBills.backend.entity.Discount.DiscountType.FIXED) {
                currentDiscount = currentDiscount.add(d.getDiscountValue());
            } else {
                // Percent
                BigDecimal discountVal = currentSubTotal.multiply(d.getDiscountValue().divide(BigDecimal.valueOf(100)));
                currentDiscount = currentDiscount.add(discountVal);
            }
        }

        // Ensure discount doesn't exceed subtotal
        if (currentDiscount.compareTo(currentSubTotal) > 0) {
            currentDiscount = currentSubTotal;
        }

        BigDecimal taxableAmount = currentSubTotal.subtract(currentDiscount);

        // 3. Taxes
        List<com.aynlabs.lumoBills.backend.entity.Tax> activeTaxes = taxService.findActive();
        currentTax = BigDecimal.ZERO;

        for (com.aynlabs.lumoBills.backend.entity.Tax t : activeTaxes) {
            BigDecimal taxVal = taxableAmount.multiply(t.getPercentage().divide(BigDecimal.valueOf(100)));
            currentTax = currentTax.add(taxVal);
        }

        // 4. Grand Total
        currentTotal = taxableAmount.add(currentTax);

        // Update UI
        subTotalSpan.setText("Subtotal: " + this.currencySymbol + String.format("%.2f", currentSubTotal));
        discountSpan.setText("Discount: -" + this.currencySymbol + String.format("%.2f", currentDiscount));
        taxSpan.setText("Tax: +" + this.currencySymbol + String.format("%.2f", currentTax));
        totalAmount.setText("Total: " + this.currencySymbol + String.format("%.2f", currentTotal));
    }

    private void updateSaveButtonState() {
        saveInvoiceButton.setEnabled(selectedCustomer != null && !currentItems.isEmpty());
    }

    private void saveInvoice() {
        try {
            User currentUser = securityService.getAuthenticatedUser();

            Invoice invoice = new Invoice();
            invoice.setCustomer(selectedCustomer);
            invoice.setDate(LocalDateTime.now());
            invoice.setItems(new ArrayList<>(currentItems)); // Copy list

            // Set calculated values
            invoice.setSubTotal(currentSubTotal);
            invoice.setDiscountAmount(currentDiscount);
            invoice.setTaxAmount(currentTax);
            invoice.setTotalAmount(currentTotal);

            invoice.setStatus(Invoice.InvoiceStatus.PAID);

            invoiceService.createInvoice(invoice, currentUser);

            // Show Popup Dialog instead of simple notification
            com.vaadin.flow.component.dialog.Dialog dialog = new com.vaadin.flow.component.dialog.Dialog();
            dialog.setHeaderTitle("Invoice Saved Details");

            VerticalLayout dialogLayout = new VerticalLayout();
            dialogLayout.add(new Span("Invoice #" + invoice.getId() + " saved successfully!"));

            Button downloadBtn = new Button("Download PDF",
                    new com.vaadin.flow.component.icon.Icon(com.vaadin.flow.component.icon.VaadinIcon.DOWNLOAD));
            downloadBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

            StreamResource resource = new StreamResource("invoice_" + invoice.getId() + ".pdf", () -> {
                try {
                    return new ByteArrayInputStream(reportService.generateInvoicePdf(invoice));
                } catch (Exception e) {
                    e.printStackTrace();
                    return new ByteArrayInputStream(new byte[0]);
                }
            });

            Anchor link = new Anchor(resource, "");
            link.getElement().setAttribute("download", true);
            link.add(downloadBtn);

            dialogLayout.add(link);
            dialog.add(dialogLayout);

            Button closeBtn = new Button("Close", event -> {
                dialog.close();
                // Reset UI only after closing
                resetInvoice();
                updateRecentInvoices();
            });
            dialog.getFooter().add(closeBtn);

            dialog.open();

        } catch (Exception e) {
            e.printStackTrace();
            Notification.show("Error saving invoice: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    // Removed separate generatePdf method as logic is now in saveInvoice dialog

    private void resetInvoice() {
        currentItems.clear();
        refreshGrid();
        customerSelect.clear();
        productSelect.clear();
        selectedCustomer = null;
    }
}
