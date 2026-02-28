package com.aynlabs.lumoBills.ui.views.billing;

import com.aynlabs.lumoBills.backend.entity.Invoice;
import com.aynlabs.lumoBills.backend.entity.InvoiceItem;
import com.aynlabs.lumoBills.backend.service.InvoiceService;
import com.aynlabs.lumoBills.backend.service.ReportService;
import com.aynlabs.lumoBills.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import jakarta.annotation.security.PermitAll;
import java.io.ByteArrayInputStream;
import com.vaadin.flow.component.html.Anchor;

@PermitAll
@Route(value = "invoices", layout = MainLayout.class)
@PageTitle("Invoices | LumoBills")
public class InvoiceListView extends VerticalLayout {

    private final InvoiceService invoiceService;
    private final ReportService reportService;
    private final com.aynlabs.lumoBills.backend.security.SecurityService securityService;
    private final com.aynlabs.lumoBills.backend.service.SystemSettingService settingService;
    private Grid<Invoice> grid = new Grid<>(Invoice.class);
    private Grid<InvoiceItem> detailsGrid = new Grid<>(InvoiceItem.class);
    private TextField filterText = new TextField();
    private SplitLayout splitLayout;
    private String currencySymbol = "$";

    public InvoiceListView(InvoiceService invoiceService, ReportService reportService,
            com.aynlabs.lumoBills.backend.security.SecurityService securityService,
            com.aynlabs.lumoBills.backend.service.SystemSettingService settingService) {
        this.invoiceService = invoiceService;
        this.reportService = reportService;
        this.securityService = securityService;
        this.settingService = settingService;

        // Load currency symbol
        String currencyCode = settingService.getValue("CURRENCY", "INR");
        this.currencySymbol = getCurrencySymbol(currencyCode);

        addClassName("invoice-list-view");
        setSizeFull();

        configureGrid();
        configureDetailsGrid();

        splitLayout = new SplitLayout();
        splitLayout.setSizeFull();

        VerticalLayout listLayout = new VerticalLayout(getToolbar(), grid);
        listLayout.setSizeFull();
        splitLayout.addToPrimary(listLayout);

        VerticalLayout detailsLayout = new VerticalLayout();
        detailsLayout.add(new com.vaadin.flow.component.html.H3("Invoice Items"), detailsGrid);
        detailsLayout.setSizeFull();
        detailsLayout.setVisible(false); // Hidden by default
        splitLayout.addToSecondary(detailsLayout);

        add(splitLayout);
        updateList();
    }

    private String getCurrencySymbol(String currencyCode) {
        return switch (currencyCode) {
            case "INR" -> "₹";
            case "USD" -> "$";
            case "EUR" -> "€";
            case "GBP" -> "£";
            case "JPY" -> "¥";
            default -> currencyCode + " ";
        };
    }

    private void configureDetailsGrid() {
        detailsGrid.setSizeFull();
        detailsGrid.setColumns(); // Clear default
        detailsGrid.addColumn(item -> item.getProduct() != null ? item.getProduct().getName() : "Unknown")
                .setHeader("Product");
        detailsGrid.addColumn(InvoiceItem::getQuantity).setHeader("Qty");
        detailsGrid.addColumn(InvoiceItem::getUnitPrice).setHeader("Price");
        detailsGrid.addColumn(item -> item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())))
                .setHeader("Subtotal");

        com.aynlabs.lumoBills.ui.util.GridHelper.setBasicProperties(detailsGrid);
    }

    private void configureGrid() {
        grid.addClassName("invoice-grid");
        grid.setSizeFull();
        grid.setColumns("date");
        grid.addColumn(invoice -> invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : invoice.getId())
                .setHeader("Invoice #").setSortable(true);
        grid.addColumn(invoice -> invoice.getCustomer() != null ? invoice.getCustomer().getFullName() : "N/A")
                .setHeader("Customer");
        grid.addComponentColumn(invoice -> {
            com.vaadin.flow.component.html.Span badge = new com.vaadin.flow.component.html.Span(
                    invoice.getStatus() != null ? invoice.getStatus().name() : "UNKNOWN");
            badge.getElement().getThemeList().add("badge");
            if (invoice.getStatus() == Invoice.InvoiceStatus.PAID) {
                badge.getElement().getThemeList().add("success");
            } else if (invoice.getStatus() == Invoice.InvoiceStatus.PENDING) {
                badge.getElement().getThemeList().add("contrast");
            } else if (invoice.getStatus() == Invoice.InvoiceStatus.CANCELLED) {
                badge.getElement().getThemeList().add("error");
            }
            return badge;
        }).setHeader("Status").setSortable(true);
        grid.addColumn(invoice -> this.currencySymbol
                + (invoice.getTotalAmount() != null ? invoice.getTotalAmount() : java.math.BigDecimal.ZERO))
                .setHeader("Total Amount");

        grid.addComponentColumn(invoice -> {
            HorizontalLayout actions = new HorizontalLayout();

            Button printBtn = new Button(new Icon(VaadinIcon.PRINT));
            printBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

            // Create a StreamResource for the PDF
            StreamResource resource = new StreamResource("invoice_" + invoice.getId() + ".pdf", () -> {
                try {
                    byte[] pdfBytes = reportService.generateInvoicePdf(invoice);
                    if (pdfBytes == null) {
                        throw new RuntimeException("PDF generation returned null bytes");
                    }
                    return new ByteArrayInputStream(pdfBytes);
                } catch (Exception e) {
                    e.printStackTrace(); // Log error to console
                    Notification.show("Error generating PDF: " + e.getMessage());
                    // Return empty stream to avoid confusing the response handler
                    return new ByteArrayInputStream(new byte[0]);
                }
            });

            Anchor link = new Anchor(resource, "");
            link.getElement().setAttribute("download", true);
            link.add(printBtn);
            actions.add(link);

            if (invoice.getStatus() != Invoice.InvoiceStatus.CANCELLED) {
                Button cancelBtn = new Button(new Icon(VaadinIcon.BAN));
                cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
                cancelBtn.addClickListener(e -> {
                    com.vaadin.flow.component.dialog.Dialog confirmDialog = new com.vaadin.flow.component.dialog.Dialog();
                    confirmDialog.setHeaderTitle("Cancel Invoice?");
                    confirmDialog.add("Are you sure you want to cancel Invoice #"
                            + (invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : invoice.getId())
                            + "?");

                    Button confirm = new Button("Yes, Cancel", ev -> {
                        com.aynlabs.lumoBills.backend.entity.User user = securityService.getAuthenticatedUser();
                        invoiceService.cancelInvoice(invoice, user);
                        updateList();
                        confirmDialog.close();
                        Notification.show("Invoice Cancelled");
                    });
                    confirm.addThemeVariants(ButtonVariant.LUMO_ERROR);

                    Button cancel = new Button("Nevermind", ev -> confirmDialog.close());

                    confirmDialog.getFooter().add(cancel, confirm);
                    confirmDialog.open();
                });
                actions.add(cancelBtn);
            }

            return actions;
        }).setHeader("Actions");

        // grid.getColumns().forEach(col -> col.setAutoWidth(true)); // Handled by
        // GridHelper now
        com.aynlabs.lumoBills.ui.util.GridHelper.setBasicProperties(grid);

        // Add row click listener to show details
        grid.asSingleSelect().addValueChangeListener(event ->

        {
            if (event.getValue() != null) {
                showDetails(event.getValue());
            } else {
                splitLayout.getSecondaryComponent().setVisible(false);
            }
        });
    }

    private void showDetails(Invoice invoice) {
        detailsGrid.setItems(invoice.getItems());

        // Remove previous summary if exists
        splitLayout.getSecondaryComponent().getChildren()
                .filter(c -> c instanceof VerticalLayout && ((VerticalLayout) c).getId().orElse("").equals("summary"))
                .findFirst().ifPresent(c -> ((VerticalLayout) splitLayout.getSecondaryComponent()).remove(c));

        VerticalLayout detailsLayout = (VerticalLayout) splitLayout.getSecondaryComponent();

        // Check if we already added a summary block to the layout (fragile check,
        // better to rebuild or struct)
        // Simplest: clear detailsLayout and rebuild
        detailsLayout.removeAll();
        detailsLayout.add(new com.vaadin.flow.component.html.H3("Invoice Items"), detailsGrid);

        VerticalLayout summary = new VerticalLayout();
        summary.setPadding(false);
        summary.setSpacing(false);
        summary.add(new com.vaadin.flow.component.html.Span(
                "Subtotal: " + this.currencySymbol + (invoice.getSubTotal() != null ? invoice.getSubTotal() : "-")));
        summary.add(new com.vaadin.flow.component.html.Span(
                "Discount: -" + this.currencySymbol
                        + (invoice.getDiscountAmount() != null ? invoice.getDiscountAmount() : "-")));
        summary.add(new com.vaadin.flow.component.html.Span(
                "Tax: +" + this.currencySymbol + (invoice.getTaxAmount() != null ? invoice.getTaxAmount() : "-")));
        com.vaadin.flow.component.html.Span total = new com.vaadin.flow.component.html.Span(
                "Total: " + this.currencySymbol + invoice.getTotalAmount());
        total.getStyle().set("font-weight", "bold");
        summary.add(total);

        detailsLayout.add(summary);

        splitLayout.getSecondaryComponent().setVisible(true);
        splitLayout.setSplitterPosition(60);
    }

    private HorizontalLayout getToolbar() {
        filterText.setPlaceholder("Filter by customer...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(e -> updateList());

        HorizontalLayout toolbar = new HorizontalLayout(filterText);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private void updateList() {
        java.util.List<Invoice> all = invoiceService.findAll();

        if (filterText.getValue() != null && !filterText.getValue().isEmpty()) {
            String term = filterText.getValue().toLowerCase();
            all = all.stream().filter(invoice -> (invoice.getCustomer() != null
                    && invoice.getCustomer().getFullName().toLowerCase().contains(term)) ||
                    (invoice.getInvoiceNumber() != null && invoice.getInvoiceNumber().toLowerCase().contains(term)))
                    .toList();
        }

        all = new java.util.ArrayList<>(all);
        all.sort((i1, i2) -> i2.getId().compareTo(i1.getId()));

        grid.setItems(all);
    }
}
