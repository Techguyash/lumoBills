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
import com.vaadin.flow.component.orderedlayout.FlexComponent;
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
    private final com.aynlabs.lumoBills.backend.service.EmailService emailService;
    private Grid<Invoice> grid = new Grid<>(Invoice.class);
    private TextField filterText = new TextField();
    private com.vaadin.flow.component.datepicker.DatePicker startDate = new com.vaadin.flow.component.datepicker.DatePicker(
            "From Date");
    private com.vaadin.flow.component.datepicker.DatePicker endDate = new com.vaadin.flow.component.datepicker.DatePicker(
            "To Date");
    private com.vaadin.flow.component.combobox.ComboBox<Invoice.InvoiceStatus> statusFilter = new com.vaadin.flow.component.combobox.ComboBox<>(
            "Status");
    private String currencySymbol = "$";

    public InvoiceListView(InvoiceService invoiceService, ReportService reportService,
            com.aynlabs.lumoBills.backend.security.SecurityService securityService,
            com.aynlabs.lumoBills.backend.service.SystemSettingService settingService,
            com.aynlabs.lumoBills.backend.service.EmailService emailService) {
        this.invoiceService = invoiceService;
        this.reportService = reportService;
        this.securityService = securityService;
        this.settingService = settingService;
        this.emailService = emailService;

        // Load currency symbol
        String currencyCode = settingService.getValue("CURRENCY", "INR");
        this.currencySymbol = com.aynlabs.lumoBills.ui.util.CurrencyUtility.getCurrencySymbol(currencyCode);

        addClassName("invoice-list-view");
        setSizeFull();

        configureGrid();

        add(getToolbar(), grid);
        updateList();
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
            } else if (invoice.getStatus() == Invoice.InvoiceStatus.PARTIAL) {
                badge.getElement().getThemeList().add("primary");
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
        grid.addColumn(invoice -> this.currencySymbol
                + (invoice.getAmountPaid() != null ? invoice.getAmountPaid() : java.math.BigDecimal.ZERO))
                .setHeader("Paid");
        grid.addColumn(invoice -> this.currencySymbol
                + (invoice.getAmountPending() != null ? invoice.getAmountPending() : java.math.BigDecimal.ZERO))
                .setHeader("Pending");

        grid.setSelectionMode(Grid.SelectionMode.NONE);
        grid.addItemClickListener(e -> {
            if (e.getItem() != null) {
                openInvoiceDialog(e.getItem());
            }
        });

        com.aynlabs.lumoBills.ui.util.GridHelper.setBasicProperties(grid);
    }

    private void openInvoiceDialog(Invoice invoice) {
        com.vaadin.flow.component.dialog.Dialog detailsDialog = new com.vaadin.flow.component.dialog.Dialog();
        detailsDialog.setHeaderTitle("Invoice Details - #"
                + (invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : invoice.getId()));
        detailsDialog.setWidth("500px");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);

        layout.add(new com.vaadin.flow.component.html.Span(
                "Customer: " + (invoice.getCustomer() != null ? invoice.getCustomer().getFullName() : "N/A")));
        layout.add(new com.vaadin.flow.component.html.Span("Date: " + invoice.getDate().toString()));
        layout.add(
                new com.vaadin.flow.component.html.Span("Total Amount: " + currencySymbol + invoice.getTotalAmount()));
        layout.add(new com.vaadin.flow.component.html.Span("Amount Paid: " + currencySymbol + invoice.getAmountPaid()));
        layout.add(new com.vaadin.flow.component.html.Span(
                "Amount Pending: " + currencySymbol + invoice.getAmountPending()));

        // Add Items Grid to Dialog
        Grid<InvoiceItem> itemGrid = new Grid<>(InvoiceItem.class);
        itemGrid.setItems(invoice.getItems());
        itemGrid.setColumns();
        itemGrid.addColumn(item -> item.getProduct() != null ? item.getProduct().getName() : "Unknown")
                .setHeader("Product");
        itemGrid.addColumn(InvoiceItem::getQuantity).setHeader("Qty");
        itemGrid.addColumn(InvoiceItem::getUnitPrice).setHeader("Price");
        itemGrid.addColumn(item -> item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())))
                .setHeader("Subtotal");
        itemGrid.setAllRowsVisible(true);
        com.aynlabs.lumoBills.ui.util.GridHelper.setBasicProperties(itemGrid);

        layout.add(new com.vaadin.flow.component.html.H4("Items"), itemGrid);

        HorizontalLayout footerActions = new HorizontalLayout();
        footerActions.setWidthFull();
        footerActions.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        // Print Button
        Button printBtn = new Button("Print Invoice", new Icon(VaadinIcon.PRINT));
        printBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        StreamResource resource = new StreamResource("invoice_" + invoice.getId() + ".pdf", () -> {
            try {
                return new ByteArrayInputStream(reportService.generateInvoicePdf(invoice));
            } catch (Exception ex) {
                ex.printStackTrace();
                return new ByteArrayInputStream(new byte[0]);
            }
        });
        Anchor printLink = new Anchor(resource, "");
        printLink.getElement().setAttribute("download", true);
        printLink.add(printBtn);

        // Cancel Button
        Button cancelInvoiceBtn = new Button("Cancel Invoice", new Icon(VaadinIcon.BAN));
        cancelInvoiceBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        cancelInvoiceBtn.setEnabled(invoice.getStatus() != Invoice.InvoiceStatus.CANCELLED);
        cancelInvoiceBtn.addClickListener(e -> {
            com.vaadin.flow.component.dialog.Dialog confirmCancel = new com.vaadin.flow.component.dialog.Dialog();
            confirmCancel.setHeaderTitle("Cancel Invoice?");
            confirmCancel.add("Are you sure you want to cancel this invoice?");
            Button yesBtn = new Button("Yes, Cancel", ev -> {
                invoiceService.cancelInvoice(invoice, securityService.getAuthenticatedUser());
                updateList();
                confirmCancel.close();
                detailsDialog.close();
                Notification.show("Invoice Cancelled");
            });
            yesBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            confirmCancel.getFooter().add(new Button("No", ev -> confirmCancel.close()), yesBtn);
            confirmCancel.open();
        });

        layout.add(new com.vaadin.flow.component.html.Hr());

        // Update Payment Section
        if (invoice.getStatus() == Invoice.InvoiceStatus.PENDING
                || invoice.getStatus() == Invoice.InvoiceStatus.PARTIAL) {
            com.vaadin.flow.component.html.H4 payTitle = new com.vaadin.flow.component.html.H4("Add Payment");
            com.vaadin.flow.component.textfield.BigDecimalField amountField = new com.vaadin.flow.component.textfield.BigDecimalField(
                    "Amount");
            amountField.setValue(invoice.getAmountPending());
            amountField.setWidthFull();

            com.vaadin.flow.component.combobox.ComboBox<Invoice.PaymentMode> modeField = new com.vaadin.flow.component.combobox.ComboBox<>(
                    "Mode");
            modeField.setItems(Invoice.PaymentMode.values());
            modeField.setValue(Invoice.PaymentMode.CASH);
            modeField.setWidthFull();

            Button recordPayBtn = new Button("Record Payment", ev -> {
                if (amountField.getValue() != null && amountField.getValue().compareTo(java.math.BigDecimal.ZERO) > 0) {
                    try {
                        invoiceService.addPayment(invoice, amountField.getValue(), modeField.getValue(), "");
                        updateList();
                        detailsDialog.close();
                        Notification.show("Payment added successfully");
                    } catch (Exception ex) {
                        Notification.show("Error: " + ex.getMessage());
                    }
                }
            });
            recordPayBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
            recordPayBtn.setWidthFull();

            layout.add(payTitle, amountField, modeField, recordPayBtn);
        }

        detailsDialog.add(layout);

        // Email button in footer if email exists
        if (invoice.getCustomer() != null && invoice.getCustomer().getEmail() != null
                && !invoice.getCustomer().getEmail().isEmpty()) {
            Button emailBtn = new Button("Email", new Icon(VaadinIcon.ENVELOPE), e -> {
                try {
                    String companyName = settingService.getValue("COMPANY_NAME", "LumoBills");
                    String subject = "Invoice #" + invoice.getInvoiceNumber() + " from " + companyName;
                    String body = "Dear " + invoice.getCustomer().getFirstName()
                            + ",\n\nPlease find your invoice details attached.";
                    emailService.sendEmail(invoice.getCustomer().getEmail(), subject, body);
                    Notification.show("Email sent successfully");
                } catch (Exception ex) {
                    Notification.show("Failed to send email");
                }
            });
            detailsDialog.getFooter().add(emailBtn);
        }

        detailsDialog.getFooter().add(new Button("Close", e -> detailsDialog.close()), cancelInvoiceBtn, printLink);
        detailsDialog.open();
    }

    private HorizontalLayout getToolbar() {
        filterText.setPlaceholder("Filter by customer...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);

        statusFilter.setItems(Invoice.InvoiceStatus.values());
        statusFilter.setClearButtonVisible(true);

        startDate.setClearButtonVisible(true);
        endDate.setClearButtonVisible(true);

        Button searchBtn = new Button("Filters", e -> updateList());
        searchBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout toolbar = new HorizontalLayout(filterText, startDate, endDate, statusFilter, searchBtn);
        toolbar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
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

        if (statusFilter.getValue() != null) {
            all = all.stream().filter(i -> i.getStatus() == statusFilter.getValue()).toList();
        }

        if (startDate.getValue() != null) {
            java.time.LocalDateTime start = startDate.getValue().atStartOfDay();
            all = all.stream().filter(i -> !i.getDate().isBefore(start)).toList();
        }

        if (endDate.getValue() != null) {
            java.time.LocalDateTime end = endDate.getValue().atTime(java.time.LocalTime.MAX);
            all = all.stream().filter(i -> !i.getDate().isAfter(end)).toList();
        }

        all = new java.util.ArrayList<>(all);
        all.sort((i1, i2) -> i2.getId().compareTo(i1.getId()));

        grid.setItems(all);
    }
}
