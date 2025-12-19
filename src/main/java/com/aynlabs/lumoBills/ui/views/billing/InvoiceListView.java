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
    private Grid<Invoice> grid = new Grid<>(Invoice.class);
    private Grid<InvoiceItem> detailsGrid = new Grid<>(InvoiceItem.class);
    private TextField filterText = new TextField();
    private SplitLayout splitLayout;

    public InvoiceListView(InvoiceService invoiceService, ReportService reportService) {
        this.invoiceService = invoiceService;
        this.reportService = reportService;

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
        grid.setColumns("id", "date", "totalAmount");
        grid.addColumn(invoice -> invoice.getCustomer() != null ? invoice.getCustomer().getFullName() : "N/A")
                .setHeader("Customer");

        grid.addComponentColumn(invoice -> {
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

            return link;
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
                "Subtotal: $" + (invoice.getSubTotal() != null ? invoice.getSubTotal() : "-")));
        summary.add(new com.vaadin.flow.component.html.Span(
                "Discount: -$" + (invoice.getDiscountAmount() != null ? invoice.getDiscountAmount() : "-")));
        summary.add(new com.vaadin.flow.component.html.Span(
                "Tax: +$" + (invoice.getTaxAmount() != null ? invoice.getTaxAmount() : "-")));
        com.vaadin.flow.component.html.Span total = new com.vaadin.flow.component.html.Span(
                "Total: $" + invoice.getTotalAmount());
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
        // Implement searching if needed, currently findAll
        grid.setItems(invoiceService.findAll());
    }
}
