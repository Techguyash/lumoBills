package com.aynlabs.lumoBills.ui.views.stock;

import com.aynlabs.lumoBills.backend.entity.Product;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.shared.Registration;

import com.aynlabs.lumoBills.backend.entity.Category;
import com.vaadin.flow.component.combobox.ComboBox;
import java.util.List;

public class ProductForm extends FormLayout {

    TextField name = new TextField("Product Name");
    ComboBox<Category> category = new ComboBox<>("Category");
    BigDecimalField unitPrice = new BigDecimalField("Unit Price");
    IntegerField quantityInStock = new IntegerField("Initial Qty (Adjust in audit)");
    IntegerField reorderLevel = new IntegerField("Reorder Level");
    TextField description = new TextField("Description");

    Button save = new Button("Save");
    Button delete = new Button("Delete");
    Button close = new Button("Cancel");

    Binder<Product> binder = new BeanValidationBinder<>(Product.class);
    private Product product;

    public ProductForm(List<Category> categories) {
        addClassName("contact-form");

        binder.bindInstanceFields(this);

        category.setItems(categories);
        category.setItemLabelGenerator(Category::getName);

        add(name,
                category,
                unitPrice,
                quantityInStock,
                reorderLevel,
                description,
                createButtonsLayout());
    }

    private Component createButtonsLayout() {
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        save.addClickShortcut(Key.ENTER);
        close.addClickShortcut(Key.ESCAPE);

        save.addClickListener(event -> validateAndSave());
        delete.addClickListener(event -> fireEvent(new DeleteEvent(this, product)));
        close.addClickListener(event -> fireEvent(new CloseEvent(this)));

        binder.addStatusChangeListener(e -> save.setEnabled(binder.isValid()));

        return new HorizontalLayout(save, delete, close);
    }

    private void validateAndSave() {
        try {
            binder.writeBean(product);
            fireEvent(new SaveEvent(this, product));
        } catch (ValidationException e) {
            e.printStackTrace();
        }
    }

    public void setProduct(Product product) {
        this.product = product;
        binder.readBean(product);
    }

    // Events
    public static abstract class ProductFormEvent extends ComponentEvent<ProductForm> {
        private Product product;

        protected ProductFormEvent(ProductForm source, Product product) {
            super(source, false);
            this.product = product;
        }

        public Product getProduct() {
            return product;
        }
    }

    public static class SaveEvent extends ProductFormEvent {
        SaveEvent(ProductForm source, Product product) {
            super(source, product);
        }
    }

    public static class DeleteEvent extends ProductFormEvent {
        DeleteEvent(ProductForm source, Product product) {
            super(source, product);
        }
    }

    public static class CloseEvent extends ProductFormEvent {
        CloseEvent(ProductForm source) {
            super(source, null);
        }
    }

    public Registration addSaveListener(ComponentEventListener<SaveEvent> listener) {
        return addListener(SaveEvent.class, listener);
    }

    public Registration addDeleteListener(ComponentEventListener<DeleteEvent> listener) {
        return addListener(DeleteEvent.class, listener);
    }

    public Registration addCloseListener(ComponentEventListener<CloseEvent> listener) {
        return addListener(CloseEvent.class, listener);
    }
}
