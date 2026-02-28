package com.aynlabs.lumoBills.ui.views.dashboard;

import com.aynlabs.lumoBills.backend.entity.Product;
import com.aynlabs.lumoBills.backend.service.ProductService;
import com.aynlabs.lumoBills.ui.MainLayout;
import com.aynlabs.lumoBills.backend.service.InvoiceService;
import com.aynlabs.lumoBills.backend.service.StockService;
import com.aynlabs.lumoBills.backend.service.SystemSettingService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import jakarta.annotation.security.PermitAll;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

@PermitAll
@Route(value = "dashboard", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@PageTitle("Dashboard | LumoBills")
public class DashboardView extends VerticalLayout {

        private final ProductService productService;
        private final InvoiceService invoiceService;
        private final StockService stockService;
        private final SystemSettingService settingService;
        private final com.aynlabs.lumoBills.backend.service.PurchaseService purchaseService;

        private DatePicker startDate = new DatePicker("From Date");
        private DatePicker endDate = new DatePicker("To Date");
        private FlexLayout statsLayout = new FlexLayout();
        private String currencySymbol = "$";
        private com.vaadin.flow.component.html.Div chartContainer = new com.vaadin.flow.component.html.Div();

        public DashboardView(ProductService productService,
                        InvoiceService invoiceService,
                        StockService stockService,
                        SystemSettingService settingService,
                        com.aynlabs.lumoBills.backend.service.PurchaseService purchaseService) {
                this.productService = productService;
                this.invoiceService = invoiceService;
                this.stockService = stockService;
                this.settingService = settingService;
                this.purchaseService = purchaseService;

                addClassName("dashboard-view");
                setSizeFull();
                setPadding(true);

                add(new H2("Dashboard Summary"));

                String currencyCode = settingService.getValue("CURRENCY", "INR");
                this.currencySymbol = getCurrencySymbol(currencyCode);

                // Date Filters
                startDate.setValue(LocalDate.now().minusMonths(1));
                endDate.setValue(LocalDate.now());

                Button refreshBtn = new Button("Apply Filters", e -> refreshDashboard());
                refreshBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

                HorizontalLayout filterLayout = new HorizontalLayout(startDate, endDate, refreshBtn);
                filterLayout.setAlignItems(Alignment.BASELINE);
                add(filterLayout);

                // Stats Cards Container
                statsLayout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
                statsLayout.getStyle().set("gap", "20px");
                statsLayout.setWidthFull();

                add(statsLayout);

                // Main Content Area (Charts and Activity)
                com.vaadin.flow.component.orderedlayout.HorizontalLayout mainContent = new com.vaadin.flow.component.orderedlayout.HorizontalLayout();
                mainContent.setSizeFull();
                mainContent.setSpacing(true);

                // Chart Section
                VerticalLayout chartSection = new VerticalLayout();
                chartSection.setPadding(false);
                chartSection.setSpacing(true);
                chartSection.setWidth("65%");

                H4 chartTitle = new H4("Sales vs Purchases Performance");
                chartContainer.setId("sales-chart-container");
                chartContainer.getStyle().set("width", "100%");
                chartContainer.getStyle().set("height", "400px");
                chartContainer.add(new Html("<canvas id='salesChart'></canvas>"));

                chartSection.add(chartTitle, chartContainer);

                refreshDashboard();

                // Activity Feed (Notification Column)
                VerticalLayout activityFeed = new VerticalLayout();
                activityFeed.setWidth("35%");
                activityFeed.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
                activityFeed.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
                activityFeed.setPadding(true);
                activityFeed.setSpacing(false);

                activityFeed.add(new H4("Activity & Notifications"));

                // Add Low Stock Alerts first
                List<Product> lowStockProducts = productService.findAll().stream()
                                .filter(Product::isLowStock)
                                .limit(5)
                                .toList();

                if (!lowStockProducts.isEmpty()) {
                        Span alertHeader = new Span("⚠️ Low Stock Alerts");
                        alertHeader.getStyle()
                                        .set("font-weight", "600")
                                        .set("color", "#DC2626")
                                        .set("font-size", "var(--lumo-font-size-s)")
                                        .set("padding", "8px 0")
                                        .set("display", "block");
                        activityFeed.add(alertHeader);

                        for (Product product : lowStockProducts) {
                                String text = String.format("%s - Only %d left (Reorder at %d)",
                                                product.getName(),
                                                product.getQuantityInStock(),
                                                product.getReorderLevel());
                                Span span = new Span(text);
                                span.getStyle()
                                                .set("font-size", "var(--lumo-font-size-s)")
                                                .set("color", "#DC2626")
                                                .set("background-color", "#FEE2E2")
                                                .set("padding", "6px 8px")
                                                .set("margin", "4px 0")
                                                .set("border-radius", "4px")
                                                .set("border-left", "3px solid #DC2626")
                                                .set("display", "block");
                                activityFeed.add(span);
                        }
                }

                // Add Recent Activity
                Span activityHeader = new Span("📊 Recent Activity");
                activityHeader.getStyle()
                                .set("font-weight", "600")
                                .set("font-size", "var(--lumo-font-size-s)")
                                .set("padding", "12px 0 8px 0")
                                .set("display", "block")
                                .set("margin-top", "12px");
                activityFeed.add(activityHeader);

                List<com.aynlabs.lumoBills.backend.entity.StockHistory> recentActivity = stockService
                                .findRecentActivity(8);
                for (com.aynlabs.lumoBills.backend.entity.StockHistory activity : recentActivity) {
                        String text = String.format("%s: %s %d for %s",
                                        activity.getTimestamp().toLocalTime().toString(),
                                        activity.getType(),
                                        activity.getChangeAmount(),
                                        activity.getProduct().getName());
                        Span span = new Span(text);
                        span.getStyle()
                                        .set("font-size", "var(--lumo-font-size-s)")
                                        .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
                                        .set("padding", "6px 0")
                                        .set("display", "block");
                        activityFeed.add(span);
                }

                mainContent.add(chartSection, activityFeed);

                add(statsLayout, mainContent);

                initChart();
        }

        private void refreshDashboard() {
                statsLayout.removeAll();
                LocalDateTime start = startDate.getValue().atStartOfDay();
                LocalDateTime end = endDate.getValue().atTime(java.time.LocalTime.MAX);

                BigDecimal income = invoiceService.getTotalSalesAmountBetween(start, end);
                BigDecimal expense = purchaseService.getTotalAmountBetween(start, end);
                BigDecimal profit = income.subtract(expense);

                statsLayout.add(createCard("Total Income", currencySymbol + " " + income, "stats-card", "#059669",
                                "#34D399"));
                statsLayout.add(createCard("Total Expense", currencySymbol + " " + expense, "stats-card", "#EA580C",
                                "#FB923C"));

                String profitColor = profit.compareTo(BigDecimal.ZERO) >= 0 ? "#4F46E5" : "#DC2626";
                String profitEnd = profit.compareTo(BigDecimal.ZERO) >= 0 ? "#818CF8" : "#F87171";
                statsLayout.add(createCard("Net Profit/Loss", currencySymbol + " " + profit, "stats-card", profitColor,
                                profitEnd));

                long lowStockCount = productService.findAll().stream().filter(Product::isLowStock).count();
                statsLayout.add(createCard("Low Stock Alerts", String.valueOf(lowStockCount), "error-card"));

                updateChartData(start, end);
        }

        private void updateChartData(LocalDateTime start, LocalDateTime end) {
                // Fetch and aggregate data
                Map<LocalDate, BigDecimal> salesByDay = invoiceService.findAll().stream()
                                .filter(i -> i.getStatus() == com.aynlabs.lumoBills.backend.entity.Invoice.InvoiceStatus.PAID)
                                .filter(i -> i.getDate().isAfter(start) && i.getDate().isBefore(end))
                                .collect(Collectors.groupingBy(i -> i.getDate().toLocalDate(),
                                                Collectors.reducing(BigDecimal.ZERO,
                                                                com.aynlabs.lumoBills.backend.entity.Invoice::getTotalAmount,
                                                                BigDecimal::add)));

                Map<LocalDate, BigDecimal> purchaseByDay = purchaseService.findByDateBetween(start, end).stream()
                                .collect(Collectors.groupingBy(p -> p.getPurchaseDate().toLocalDate(),
                                                Collectors.reducing(BigDecimal.ZERO,
                                                                com.aynlabs.lumoBills.backend.entity.Purchase::getTotal,
                                                                BigDecimal::add)));

                // Get combined sorted dates
                List<LocalDate> allDates = new ArrayList<>();
                allDates.addAll(salesByDay.keySet());
                allDates.addAll(purchaseByDay.keySet());
                List<LocalDate> sortedDates = allDates.stream().distinct().sorted().collect(Collectors.toList());

                if (sortedDates.isEmpty()) {
                        sortedDates.add(LocalDate.now());
                }

                List<String> labels = sortedDates.stream().map(LocalDate::toString).collect(Collectors.toList());
                List<BigDecimal> salesData = sortedDates.stream().map(d -> salesByDay.getOrDefault(d, BigDecimal.ZERO))
                                .collect(Collectors.toList());
                List<BigDecimal> purchaseData = sortedDates.stream()
                                .map(d -> purchaseByDay.getOrDefault(d, BigDecimal.ZERO)).collect(Collectors.toList());

                String labelsJson = "['" + String.join("','", labels) + "']";
                String salesJson = salesData.toString();
                String purchaseJson = purchaseData.toString();

                getElement().executeJs(
                                "if (window.myDashboardChart) { window.myDashboardChart.destroy(); }" +
                                                "const ctx = document.getElementById('salesChart').getContext('2d');" +
                                                "window.myDashboardChart = new Chart(ctx, {" +
                                                "  type: 'line'," +
                                                "  data: {" +
                                                "    labels: " + labelsJson + "," +
                                                "    datasets: [" +
                                                "      { label: 'Sales Income', data: " + salesJson
                                                + ", borderColor: '#059669', tension: 0.3, fill: false }," +
                                                "      { label: 'Purchase Expense', data: " + purchaseJson
                                                + ", borderColor: '#EA580C', tension: 0.3, fill: false }" +
                                                "    ]" +
                                                "  }," +
                                                "  options: { responsive: true, maintainAspectRatio: false }" +
                                                "});");
        }

        private void initChart() {
                // Ensure Chart.js is loaded
                getElement().executeJs(
                                "if (!window.Chart) {" +
                                                "  const script = document.createElement('script');" +
                                                "  script.src = 'https://cdn.jsdelivr.net/npm/chart.js';" +
                                                "  script.onload = () => { console.log('Chart.js loaded'); };" +
                                                "  document.head.appendChild(script);" +
                                                "}");
        }

        private Component createCard(String title, String value) {
                return createCard(title, value, "stats-card", "#4F46E5", "#818CF8");
        }

        private Component createCard(String title, String value, String className) {
                // Default gradient colors for error cards (red theme)
                return createCard(title, value, className, "#DC2626", "#F87171");
        }

        private Component createCard(String title, String value, String className, String gradientStart,
                        String gradientEnd) {
                VerticalLayout card = new VerticalLayout();
                card.addClassName(className);
                card.setPadding(true);
                card.setSpacing(false);

                // Build complete style string with gradient and all sizing properties
                String gradient = String.format("linear-gradient(135deg, %s 0%%, %s 100%%)", gradientStart,
                                gradientEnd);
                String styleString = String.format(
                                "background: %s !important; " +
                                                "border-radius: 12px !important; " +
                                                "box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06) !important; "
                                                +
                                                "transition: transform 0.2s, box-shadow 0.2s !important; " +
                                                "cursor: default !important; " +
                                                "flex: 1 1 calc(25%% - 20px) !important; " +
                                                "min-width: 220px !important; " +
                                                "max-width: 280px !important; " +
                                                "height: 120px !important;",
                                gradient);

                // Set style attribute directly - this has highest priority
                card.getElement().setAttribute("style", styleString);

                // Add JavaScript to ensure styles are applied (fallback)
                card.getElement().executeJs(
                                "this.style.background = '" + gradient + "'; " +
                                                "this.style.borderRadius = '12px'; " +
                                                "this.style.boxShadow = '0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)'; "
                                                +
                                                "this.style.transition = 'transform 0.2s, box-shadow 0.2s'; " +
                                                "this.style.cursor = 'default'; " +
                                                "this.style.flex = '1 1 calc(25% - 20px)'; " +
                                                "this.style.minWidth = '220px'; " +
                                                "this.style.maxWidth = '280px'; " +
                                                "this.style.height = '120px'; " +
                                                // Add hover effects
                                                "this.addEventListener('mouseenter', function() {" +
                                                "  this.style.transform = 'translateY(-4px)';" +
                                                "  this.style.boxShadow = '0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05)';"
                                                +
                                                "});" +
                                                "this.addEventListener('mouseleave', function() {" +
                                                "  this.style.transform = 'translateY(0)';" +
                                                "  this.style.boxShadow = '0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)';"
                                                +
                                                "});");

                Span titleSpan = new Span(title);
                titleSpan.getStyle()
                                .set("color", "rgba(255, 255, 255, 0.9)")
                                .set("font-size", "0.875rem")
                                .set("font-weight", "500")
                                .set("text-transform", "uppercase")
                                .set("letter-spacing", "0.05em");

                Span valueSpan = new Span(value);
                valueSpan.getStyle()
                                .set("font-size", "2rem")
                                .set("font-weight", "700")
                                .set("color", "white")
                                .set("margin-top", "0.5rem");

                card.add(titleSpan, valueSpan);
                return card;
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
                        default -> currencyCode; // Fallback to code if unknown
                };
        }
}
