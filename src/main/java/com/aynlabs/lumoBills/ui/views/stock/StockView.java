package com.aynlabs.lumoBills.ui.views.stock;

import com.aynlabs.lumoBills.backend.entity.Product;
import com.aynlabs.lumoBills.backend.service.ProductService;
import com.aynlabs.lumoBills.ui.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@PermitAll
@Route(value = "stock", layout = MainLayout.class)
@PageTitle("Stock Management | LumoBills")
public class StockView extends VerticalLayout {

    Grid<Product> grid = new Grid<>(Product.class);
    TextField filterText = new TextField();

    private final ProductService productService;
    private final com.aynlabs.lumoBills.backend.service.CategoryService categoryService;

    private ProductForm form;

    public StockView(ProductService productService,
            com.aynlabs.lumoBills.backend.service.CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
        addClassName("stock-view");
        setSizeFull();
        configureGrid();
        configureForm();

        add(getToolbar(), getContent());
        updateList();
        closeEditor();
    }

    private Component getContent() {
        HorizontalLayout content = new HorizontalLayout(grid, form);
        content.setFlexGrow(2, grid);
        content.setFlexGrow(1, form);
        content.addClassNames("content");
        content.setSizeFull();
        return content;
    }

    private void configureForm() {
        form = new ProductForm(categoryService.findAll());
        form.setWidth("25em");
        form.addSaveListener(this::saveProduct);
        form.addDeleteListener(this::deleteProduct);
        form.addCloseListener(e -> closeEditor());
    }

    private void saveProduct(ProductForm.SaveEvent event) {
        productService.save(event.getProduct());
        updateList();
        closeEditor();
    }

    private void deleteProduct(ProductForm.DeleteEvent event) {
        productService.delete(event.getProduct());
        updateList();
        closeEditor();
    }

    private void closeEditor() {
        form.setProduct(null);
        form.setVisible(false);
        removeClassName("editing");
    }

    private void editProduct(Product product) {
        if (product == null) {
            closeEditor();
        } else {
            form.setProduct(product);
            form.setVisible(true);
            addClassName("editing");
        }
    }

    private void configureGrid() {
        grid.addClassNames("contact-grid");
        grid.setSizeFull();
        grid.setColumns("name", "category", "unitPrice", "quantityInStock", "description");

        grid.getColumnByKey("unitPrice").setHeader("Selling Price");

        com.aynlabs.lumoBills.ui.util.GridHelper.setBasicProperties(grid);

        grid.asSingleSelect().addValueChangeListener(event -> editProduct(event.getValue()));
    }

    private HorizontalLayout getToolbar() {
        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(e -> updateList());

        Button addProductButton = new Button("Add Product");
        addProductButton.addClickListener(click -> addProduct());

        HorizontalLayout toolbar = new HorizontalLayout(filterText, addProductButton);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private void addProduct() {
        grid.asSingleSelect().clear();
        editProduct(new Product());
    }

    private void updateList() {
        grid.setItems(productService.findAll(filterText.getValue()));
    }
}
