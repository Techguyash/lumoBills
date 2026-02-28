package com.aynlabs.lumoBills.ui.views.purchase;

import com.aynlabs.lumoBills.backend.entity.Purchase;
import com.aynlabs.lumoBills.backend.service.PurchaseService;
import com.aynlabs.lumoBills.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@PermitAll
@Route(value = "purchase", layout = MainLayout.class)
@PageTitle("Raw Material Purchase | LumoBills")
public class PurchaseView extends VerticalLayout {

    private final PurchaseService purchaseService;

    private TextField productName = new TextField("Product Name");
    private TextField sellerName = new TextField("Seller Name");
    private IntegerField quantity = new IntegerField("Quantity");
    private BigDecimalField price = new BigDecimalField("Price (Per Unit)");
    private BigDecimalField total = new BigDecimalField("Total");
    private Button submitButton = new Button("Record Purchase");

    private Grid<Purchase> grid = new Grid<>(Purchase.class);
    private Binder<Purchase> binder = new BeanValidationBinder<>(Purchase.class);

    public PurchaseView(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
        setSizeFull();
        setPadding(true);

        add(new H2("Raw Material Purchases"), createForm(), createPurchaseList());

        setupBinder();
        updateList();
    }

    private FormLayout createForm() {
        FormLayout form = new FormLayout();

        total.setReadOnly(true);

        quantity.addValueChangeListener(e -> calculateTotal());
        price.addValueChangeListener(e -> calculateTotal());

        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submitButton.addClickListener(e -> savePurchase());

        form.add(productName, sellerName, quantity, price, total, submitButton);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 3));

        return form;
    }

    private void setupBinder() {
        binder.bindInstanceFields(this);
    }

    private void calculateTotal() {
        Integer qty = quantity.getValue();
        BigDecimal p = price.getValue();
        if (qty != null && p != null) {
            total.setValue(p.multiply(BigDecimal.valueOf(qty)));
        } else {
            total.setValue(BigDecimal.ZERO);
        }
    }

    private VerticalLayout createPurchaseList() {
        grid.setColumns("purchaseDate", "productName", "sellerName", "quantity", "price", "total");
        grid.setSizeFull();

        VerticalLayout layout = new VerticalLayout(new H2("Purchase History"), grid);
        layout.setSizeFull();
        layout.setPadding(false);
        return layout;
    }

    private void savePurchase() {
        Purchase purchase = new Purchase();
        try {
            binder.writeBean(purchase);
            purchase.setPurchaseDate(LocalDateTime.now());
            purchaseService.save(purchase);
            Notification.show("Purchase recorded successfully!");
            clearForm();
            updateList();
        } catch (Exception e) {
            Notification.show("Error saving purchase: " + e.getMessage());
        }
    }

    private void clearForm() {
        productName.clear();
        sellerName.clear();
        quantity.clear();
        price.clear();
        total.clear();
    }

    private void updateList() {
        grid.setItems(purchaseService.findAll());
    }
}
