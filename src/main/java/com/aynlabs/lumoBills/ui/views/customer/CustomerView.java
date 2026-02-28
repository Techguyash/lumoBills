package com.aynlabs.lumoBills.ui.views.customer;

import com.aynlabs.lumoBills.backend.entity.Customer;
import com.aynlabs.lumoBills.backend.service.CustomerService;
import com.aynlabs.lumoBills.ui.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@PermitAll
@Route(value = "customers", layout = MainLayout.class)
@PageTitle("Customers | LumoBills")
public class CustomerView extends VerticalLayout {

    private final CustomerService customerService;
    private Grid<Customer> grid = new Grid<>(Customer.class);
    private CustomerForm form;

    public CustomerView(CustomerService customerService) {
        this.customerService = customerService;
        addClassName("customer-view");
        setSizeFull();

        configureGrid();
        configureForm();

        add(new H2("Customer Management"), getToolbar(), getContent());
        updateList();
        closeEditor();
    }

    private void configureGrid() {
        grid.addClassNames("customer-grid");
        grid.setSizeFull();
        grid.setColumns("firstName", "lastName", "email", "phone", "address");
        grid.getColumns().forEach(col -> col.setAutoWidth(true));

        grid.asSingleSelect().addValueChangeListener(event -> editCustomer(event.getValue()));
    }

    private void configureForm() {
        form = new CustomerForm();
        form.setWidth("25em");
        form.addSaveListener(this::saveCustomer);
        form.addDeleteListener(this::deleteCustomer);
        form.addCloseListener(e -> closeEditor());
    }

    private HorizontalLayout getToolbar() {
        Button addCustomerButton = new Button("Add Customer");
        addCustomerButton.addClickListener(click -> addCustomer());

        HorizontalLayout toolbar = new HorizontalLayout(addCustomerButton);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private Component getContent() {
        HorizontalLayout content = new HorizontalLayout(grid, form);
        content.setFlexGrow(2, grid);
        content.setFlexGrow(1, form);
        content.addClassNames("content");
        content.setSizeFull();
        return content;
    }

    private void saveCustomer(CustomerForm.SaveEvent event) {
        customerService.save(event.getCustomer());
        updateList();
        closeEditor();
        Notification.show("Customer saved successfully");
    }

    private void deleteCustomer(CustomerForm.DeleteEvent event) {
        customerService.delete(event.getCustomer());
        updateList();
        closeEditor();
        Notification.show("Customer deleted successfully");
    }

    public void editCustomer(Customer customer) {
        if (customer == null) {
            closeEditor();
        } else {
            form.setCustomer(customer);
            form.setVisible(true);
            addClassName("editing");
        }
    }

    private void addCustomer() {
        grid.asSingleSelect().clear();
        editCustomer(new Customer());
    }

    private void closeEditor() {
        form.setCustomer(null);
        form.setVisible(false);
        removeClassName("editing");
    }

    private void updateList() {
        grid.setItems(customerService.findAll());
    }

    // Inner Form Class
    public static class CustomerForm extends FormLayout {
        TextField firstName = new TextField("First Name");
        TextField lastName = new TextField("Last Name");
        TextField email = new TextField("Email");
        TextField phone = new TextField("Phone");
        TextField address = new TextField("Address");

        Button save = new Button("Save");
        Button delete = new Button("Delete");
        Button close = new Button("Cancel");

        Binder<Customer> binder = new BeanValidationBinder<>(Customer.class);
        private Customer customer;

        public CustomerForm() {
            addClassName("customer-form");
            binder.bindInstanceFields(this);

            add(firstName, lastName, email, phone, address, createButtonsLayout());
        }

        private Component createButtonsLayout() {
            save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
            close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

            save.addClickListener(event -> validateAndSave());
            delete.addClickListener(event -> fireEvent(new DeleteEvent(this, customer)));
            close.addClickListener(event -> fireEvent(new CloseEvent(this)));

            binder.addStatusChangeListener(e -> save.setEnabled(binder.isValid()));
            return new HorizontalLayout(save, delete, close);
        }

        private void validateAndSave() {
            try {
                binder.writeBean(customer);
                fireEvent(new SaveEvent(this, customer));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void setCustomer(Customer customer) {
            this.customer = customer;
            binder.readBean(customer);
        }

        // Events
        public static abstract class CustomerFormEvent extends com.vaadin.flow.component.ComponentEvent<CustomerForm> {
            private Customer customer;

            protected CustomerFormEvent(CustomerForm source, Customer customer) {
                super(source, false);
                this.customer = customer;
            }

            public Customer getCustomer() {
                return customer;
            }
        }

        public static class SaveEvent extends CustomerFormEvent {
            SaveEvent(CustomerForm source, Customer customer) {
                super(source, customer);
            }
        }

        public static class DeleteEvent extends CustomerFormEvent {
            DeleteEvent(CustomerForm source, Customer customer) {
                super(source, customer);
            }
        }

        public static class CloseEvent extends CustomerFormEvent {
            CloseEvent(CustomerForm source) {
                super(source, null);
            }
        }

        public com.vaadin.flow.shared.Registration addSaveListener(
                com.vaadin.flow.component.ComponentEventListener<SaveEvent> listener) {
            return addListener(SaveEvent.class, listener);
        }

        public com.vaadin.flow.shared.Registration addDeleteListener(
                com.vaadin.flow.component.ComponentEventListener<DeleteEvent> listener) {
            return addListener(DeleteEvent.class, listener);
        }

        public com.vaadin.flow.shared.Registration addCloseListener(
                com.vaadin.flow.component.ComponentEventListener<CloseEvent> listener) {
            return addListener(CloseEvent.class, listener);
        }
    }
}
