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
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@PermitAll
@Route(value = "reports", layout = MainLayout.class)
@PageTitle("Reports | LumoBills")
public class ReportsView extends VerticalLayout {

    private final ReportService reportService;

    private DatePicker startDate = new DatePicker("Start Date");
    private DatePicker endDate = new DatePicker("End Date");
    private ComboBox<String> reportType = new ComboBox<>("Report Type");

    private VerticalLayout gridContainer = new VerticalLayout();
    private Grid<?> currentGrid;
    private List<?> currentData;

    public ReportsView(ReportService reportService) {
        this.reportService = reportService;
        setSizeFull();

        add(new H2("Business Reports"), createToolbar(), gridContainer);

        // Default dates
        startDate.setValue(LocalDate.now().minusMonths(1));
        endDate.setValue(LocalDate.now());

        reportType.setItems("Sales Report", "Stock History", "Stock Refill (Purchases)");
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
            GridHelper.setBasicProperties(grid);
            gridContainer.add(grid);
            currentGrid = grid;
            currentData = data;
        } else if ("Stock Refill (Purchases)".equals(type)) {
            Grid<StockReportDTO> grid = new Grid<>(StockReportDTO.class);
            List<StockReportDTO> data = reportService.getStockHistoryData(start, end, TransactionType.PURCHASE);
            grid.setItems(data);
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
        } else {
            headers = new String[] { "Date", "Product", "Type", "Change", "Conducted By", "Notes" };
            fields = new String[] { "date", "productName", "type", "changeAmount", "conductedBy", "notes" };
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
