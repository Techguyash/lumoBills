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
import java.util.List;

@PermitAll
@Route(value = "dashboard", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@PageTitle("Dashboard | LumoBills")
public class DashboardView extends VerticalLayout {

        private final ProductService productService;
        private final InvoiceService invoiceService;
        private final StockService stockService;
        private final SystemSettingService settingService;

        public DashboardView(ProductService productService,
                        InvoiceService invoiceService,
                        StockService stockService,
                        SystemSettingService settingService) {
                this.productService = productService;
                this.invoiceService = invoiceService;
                this.stockService = stockService;
                this.settingService = settingService;

                addClassName("dashboard-view");
                setSizeFull();
                setPadding(true);

                add(new H2("Dashboard Summary"));

                String currencyCode = settingService.getValue("CURRENCY", "INR");
                String currencySymbol = getCurrencySymbol(currencyCode);

                // Stats Cards with distinct colors
                FlexLayout statsLayout = new FlexLayout();
                statsLayout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
                statsLayout.getStyle().set("gap", "20px");
                statsLayout.setWidthFull();

                // Blue gradient for Total Products
                statsLayout.add(createCard("Total Products", String.valueOf(productService.count()),
                                "stats-card", "#4F46E5", "#818CF8"));

                // Green gradient for Total Invoices
                statsLayout.add(createCard("Total Invoices", String.valueOf(invoiceService.count()),
                                "stats-card", "#059669", "#34D399"));

                // Purple gradient for Total Sales
                statsLayout.add(createCard("Total Sales",
                                currencySymbol + " " + invoiceService.getTotalSalesAmount().toPlainString(),
                                "stats-card", "#7C3AED", "#A78BFA"));

                long lowStockCount = productService.findAll().stream().filter(Product::isLowStock).count();
                // Red gradient for Low Stock Alerts (error card)
                statsLayout.add(createCard("Low Stock Alerts", String.valueOf(lowStockCount), "error-card"));

                // Main Content Area (Charts and Activity)
                com.vaadin.flow.component.orderedlayout.HorizontalLayout mainContent = new com.vaadin.flow.component.orderedlayout.HorizontalLayout();
                mainContent.setSizeFull();
                mainContent.setSpacing(true);

                // Chart Section
                VerticalLayout chartSection = new VerticalLayout();
                chartSection.setPadding(false);
                chartSection.setSpacing(true);
                chartSection.setWidth("65%");

                H4 chartTitle = new H4("Sales Overview");
                com.vaadin.flow.component.html.Div chartContainer = new com.vaadin.flow.component.html.Div();
                chartContainer.setId("sales-chart-container");
                chartContainer.getStyle().set("width", "100%");
                chartContainer.getStyle().set("height", "400px");
                chartContainer.add(new Html("<canvas id='salesChart'></canvas>"));

                chartSection.add(chartTitle, chartContainer);

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

        private void initChart() {
                // Simple Chart.js integration via executeJs
                getElement().executeJs(
                                "const script = document.createElement('script');" +
                                                "script.src = 'https://cdn.jsdelivr.net/npm/chart.js';" +
                                                "script.onload = () => {" +
                                                "  const ctx = document.getElementById('salesChart').getContext('2d');"
                                                +
                                                "  new Chart(ctx, {" +
                                                "    type: 'line'," +
                                                "    data: {" +
                                                "      labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']," +
                                                "      datasets: [{" +
                                                "        label: 'Daily Revenue'," +
                                                "        data: [12, 19, 3, 5, 2, 3, 7]," +
                                                "        borderColor: 'rgb(75, 192, 192)'," +
                                                "        tension: 0.1" +
                                                "      }]" +
                                                "    }" +
                                                "  });" +
                                                "};" +
                                                "document.head.appendChild(script);");
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
