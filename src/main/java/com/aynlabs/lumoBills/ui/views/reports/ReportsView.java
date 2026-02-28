package com.aynlabs.lumoBills.ui.views.reports;

import com.aynlabs.lumoBills.backend.dto.SalesReportDTO;
import com.aynlabs.lumoBills.backend.dto.StockReportDTO;
import com.aynlabs.lumoBills.backend.entity.StockHistory.TransactionType;
import com.aynlabs.lumoBills.backend.service.ReportService;
import com.aynlabs.lumoBills.ui.MainLayout;
import com.aynlabs.lumoBills.ui.util.GridHelper;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import jakarta.annotation.security.PermitAll;
import com.aynlabs.lumoBills.backend.dto.ProductProfitDTO;
import com.vaadin.flow.component.html.Span;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import com.aynlabs.lumoBills.backend.entity.Purchase;

@PermitAll
@Route(value = "reports", layout = MainLayout.class)
@PageTitle("Reports | LumoBills")
public class ReportsView extends VerticalLayout {

    private final ReportService reportService;
    private final com.aynlabs.lumoBills.backend.service.PurchaseService purchaseService;

    private DatePicker startDate = new DatePicker("Start Date");
    private DatePicker endDate = new DatePicker("End Date");
    private ComboBox<String> reportType = new ComboBox<>("Report Type");

    private VerticalLayout gridContainer = new VerticalLayout();
    private Grid<?> currentGrid;
    private List<?> currentData;

    public ReportsView(ReportService reportService,
            com.aynlabs.lumoBills.backend.service.PurchaseService purchaseService) {
        this.reportService = reportService;
        this.purchaseService = purchaseService;
        setSizeFull();

        add(new H2("Business Reports"), createToolbar(), gridContainer);

        // Default dates
        startDate.setValue(LocalDate.now().minusMonths(1));
        endDate.setValue(LocalDate.now());

        reportType.setItems("Sales Report", "Stock History", "Stock Refill (Purchases)", "Product Profitability",
                "Raw Material Purchases");
        reportType.setValue("Sales Report");
    }

    private Component createToolbar() {
        Button showBtn = new Button("Show Report", e -> refreshReport());
        showBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button exportExcelBtn = new Button("Export to Excel");
        Anchor excelAnchor = new Anchor();
        excelAnchor.add(exportExcelBtn);
        excelAnchor.getElement().setAttribute("download", true);
        exportExcelBtn.addClickListener(e -> prepareExcelExport(excelAnchor));

        HorizontalLayout toolbar = new HorizontalLayout(startDate, endDate, reportType, showBtn, excelAnchor);
        toolbar.setVerticalComponentAlignment(Alignment.END, showBtn, excelAnchor);
        return toolbar;
    }

    private void refreshReport() {
        gridContainer.removeAll();
        LocalDateTime start = startDate.getValue().atStartOfDay();
        LocalDateTime end = endDate.getValue().atTime(LocalTime.MAX);

        String type = reportType.getValue();
        if ("Sales Report".equals(type)) {
            Grid<SalesReportDTO> grid = new Grid<>(SalesReportDTO.class);
            List<SalesReportDTO> data = reportService.getSalesData(start, end);
            grid.setItems(data);
            GridHelper.setBasicProperties(grid);
            gridContainer.add(grid);
            currentGrid = grid;
            currentData = data;
        } else if ("Stock History".equals(type)) {
            Grid<StockReportDTO> grid = new Grid<>(StockReportDTO.class);
            List<StockReportDTO> data = reportService.getStockHistoryData(start, end, null);
            grid.setItems(data);
            grid.setColumns("date", "productName", "type", "changeAmount", "purchasePrice", "totalAmount",
                    "conductedBy", "notes");
            grid.getColumnByKey("purchasePrice").setHeader("Price Rate");
            grid.getColumnByKey("totalAmount").setHeader("Total Financial");
            GridHelper.setBasicProperties(grid);
            gridContainer.add(grid);
            currentGrid = grid;
            currentData = data;
        } else if ("Stock Refill (Purchases)".equals(type)) {
            Grid<StockReportDTO> grid = new Grid<>(StockReportDTO.class);
            List<StockReportDTO> data = reportService.getStockHistoryData(start, end, TransactionType.PURCHASE);
            grid.setItems(data);
            grid.setColumns("date", "productName", "changeAmount", "purchasePrice", "totalAmount", "conductedBy",
                    "notes");
            grid.getColumnByKey("purchasePrice").setHeader("Buying Price");
            grid.getColumnByKey("totalAmount").setHeader("Total Cost");
            GridHelper.setBasicProperties(grid);
            gridContainer.add(grid);
            currentGrid = grid;
            currentData = data;
        } else if ("Product Profitability".equals(type)) {
            Grid<ProductProfitDTO> grid = new Grid<>(ProductProfitDTO.class);
            List<ProductProfitDTO> data = reportService.getProductProfitData();
            grid.setItems(data);

            grid.removeAllColumns();
            grid.addColumn(ProductProfitDTO::getProductName).setHeader("Product Name").setKey("productName");
            grid.addColumn(ProductProfitDTO::getBuyingPrice).setHeader("Buying Price").setKey("buyingPrice");
            grid.addColumn(ProductProfitDTO::getSellingPrice).setHeader("Selling Price").setKey("sellingPrice");

            grid.addComponentColumn(p -> {
                BigDecimal profit = p.getProfitPerUnit();
                Span span = new Span();
                if (profit.compareTo(BigDecimal.ZERO) >= 0) {
                    span.setText("↑ " + profit + " (Profit)");
                    span.getStyle().set("color", "green");
                    span.getStyle().set("font-weight", "bold");
                } else {
                    span.setText("↓ " + profit.abs() + " (Loss)");
                    span.getStyle().set("color", "red");
                    span.getStyle().set("font-weight", "bold");
                }
                return span;
            }).setHeader("Profit/Loss").setSortable(true).setKey("profitHistory");

            GridHelper.setBasicProperties(grid);
            gridContainer.add(grid);
            currentGrid = grid;
            currentData = data;
        } else if ("Raw Material Purchases".equals(type)) {
            Grid<Purchase> grid = new Grid<>(Purchase.class);
            List<Purchase> data = purchaseService.findByDateBetween(start, end);
            grid.setItems(data);
            grid.setColumns("purchaseDate", "productName", "sellerName", "quantity", "price", "total");
            GridHelper.setBasicProperties(grid);
            gridContainer.add(grid);
            currentGrid = grid;
            currentData = data;
        }
    }

    private void prepareExcelExport(Anchor anchor) {
        if (currentData == null || currentData.isEmpty())
            return;

        String type = reportType.getValue();
        String[] headers;
        String[] fields;

        if ("Sales Report".equals(type)) {
            headers = new String[] { "Invoice ID", "Date", "Customer", "Subtotal", "Tax", "Discount", "Total" };
            fields = new String[] { "invoiceId", "date", "customerName", "subTotal", "taxAmount", "discountAmount",
                    "totalAmount" };
        } else if ("Raw Material Purchases".equals(type)) {
            headers = new String[] { "Date", "Item Name", "Seller", "Qty", "Rate", "Total" };
            fields = new String[] { "purchaseDate", "productName", "sellerName", "quantity", "price", "total" };
        } else if ("Product Profitability".equals(type)) {
            headers = new String[] { "Product", "Buying Price", "Selling Price", "Profit/Loss Status" };
            fields = new String[] { "productName", "buyingPrice", "sellingPrice", "status" };
        } else if ("Stock Refill (Purchases)".equals(type)) {
            headers = new String[] { "Date", "Product", "Quantity", "Buying Price", "Total Cost", "Conducted By",
                    "Notes" };
            fields = new String[] { "date", "productName", "changeAmount", "purchasePrice", "totalAmount",
                    "conductedBy", "notes" };
        } else {
            headers = new String[] { "Date", "Product", "Type", "Change", "Rate", "Total Financial", "Conducted By",
                    "Notes" };
            fields = new String[] { "date", "productName", "type", "changeAmount", "purchasePrice", "totalAmount",
                    "conductedBy", "notes" };
        }

        StreamResource resource = new StreamResource("report.xlsx", () -> {
            try {
                return new ByteArrayInputStream(reportService.exportToExcel(currentData, headers, fields));
            } catch (Exception e) {
                return new ByteArrayInputStream(new byte[0]);
            }
        });
        anchor.setHref(resource);
    }
}
