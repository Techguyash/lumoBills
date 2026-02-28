package com.aynlabs.lumoBills.ui.views.stock;

import com.aynlabs.lumoBills.backend.entity.Category;
import com.aynlabs.lumoBills.backend.service.CategoryService;
import com.aynlabs.lumoBills.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@PermitAll
@Route(value = "categories", layout = MainLayout.class)
@PageTitle("Categories | LumoBills")
public class CategoryView extends VerticalLayout {

    private final CategoryService categoryService;
    private Grid<Category> grid = new Grid<>(Category.class);
    private TextField filterText = new TextField();

    public CategoryView(CategoryService categoryService) {
        this.categoryService = categoryService;
        addClassName("category-view");
        setSizeFull();

        configureGrid();

        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(com.vaadin.flow.data.value.ValueChangeMode.LAZY);
        filterText.addValueChangeListener(e -> updateList());

        Button addBtn = new Button("Add Category");
        addBtn.addClickListener(e -> openEditor(new Category()));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout toolbar = new HorizontalLayout(filterText, addBtn);
        toolbar.addClassName("toolbar");

        add(toolbar, grid);
        updateList();
    }

    private void configureGrid() {
        grid.setColumns("name");
        com.aynlabs.lumoBills.ui.util.GridHelper.setBasicProperties(grid);

        grid.asSingleSelect().addValueChangeListener(e -> {
            if (e.getValue() != null)
                openEditor(e.getValue());
        });
    }

    private void updateList() {
        java.util.List<Category> allCategories = categoryService.findAll();
        if (filterText.getValue() == null || filterText.getValue().isEmpty()) {
            grid.setItems(allCategories);
        } else {
            String filter = filterText.getValue().toLowerCase();
            grid.setItems(allCategories.stream()
                    .filter(c -> c.getName().toLowerCase().contains(filter))
                    .collect(java.util.stream.Collectors.toList()));
        }
    }

    private void openEditor(Category category) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(category.getId() == null ? "New Category" : "Edit Category");

        FormLayout form = new FormLayout();
        TextField name = new TextField("Name");

        Binder<Category> binder = new BeanValidationBinder<>(Category.class);
        binder.bind(name, "name");
        binder.readBean(category);

        Button save = new Button("Save", e -> {
            try {
                binder.writeBean(category);
                categoryService.save(category);
                updateList();
                dialog.close();
            } catch (Exception ex) {
                // handle error
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button delete = new Button("Delete", e -> {
            if (category.getId() != null) {
                try {
                    categoryService.delete(category);
                    updateList();
                    dialog.close();
                } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                    Notification.show(
                            "Cannot delete category as products are tagged to it.",
                            5000,
                            Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                } catch (Exception ex) {
                    Notification.show(
                            "Error deleting category: " + ex.getMessage(),
                            5000,
                            Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            } else {
                dialog.close();
            }
        });
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);

        Button cancel = new Button("Cancel", e -> dialog.close());

        dialog.add(form);
        form.add(name);

        HorizontalLayout footer = new HorizontalLayout(save, delete, cancel);
        dialog.getFooter().add(footer);

        dialog.open();
    }
}
