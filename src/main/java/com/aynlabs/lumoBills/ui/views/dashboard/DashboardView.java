package com.aynlabs.lumoBills.ui.views.dashboard;

import com.aynlabs.lumoBills.backend.entity.Product;
import com.aynlabs.lumoBills.backend.service.ProductService;
import com.aynlabs.lumoBills.ui.MainLayout;
import com.vaadin.flow.component.Component;
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

    public DashboardView(ProductService productService) {
        this.productService = productService;
        
        addClassName("dashboard-view");
        setDefaultHorizontalComponentAlignment(Alignment.START);

        add(new H2("Dashboard"));

        FlexLayout statsLayout = new FlexLayout();
        statsLayout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        statsLayout.getStyle().set("gap", "20px");
        
        statsLayout.add(createCard("Total Products", String.valueOf(productService.count())));
        
        long lowStockCount = productService.findAll().stream().filter(Product::isLowStock).count();
        statsLayout.add(createCard("Low Stock Alerts", String.valueOf(lowStockCount), "error-card"));

        add(statsLayout);
        
        add(new H4("Low Stock Items"));
        List<Product> lowStockItems = productService.findAll().stream()
            .filter(Product::isLowStock)
            .limit(5)
            .toList();
            
        for(Product p : lowStockItems) {
            add(new Span(p.getName() + " (" + p.getQuantityInStock() + " left)"));
        }
    }

    private Component createCard(String title, String value) {
        return createCard(title, value, "stats-card");
    }

    private Component createCard(String title, String value, String className) {
        VerticalLayout card = new VerticalLayout();
        card.addClassName(className);
        card.setPadding(true);
        card.setSpacing(false);
        card.setWidth("200px");
        card.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        card.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        card.getStyle().set("background-color", "var(--lumo-base-color)");
        
        Span titleSpan = new Span(title);
        titleSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");
        
        Span valueSpan = new Span(value);
        valueSpan.getStyle().set("font-size", "var(--lumo-font-size-xxl)");
        valueSpan.getStyle().set("font-weight", "bold");
        
        card.add(titleSpan, valueSpan);
        return card;
    }
}

