package com.aynlabs.lumoBills.ui.views.admin;

import com.aynlabs.lumoBills.backend.entity.Role;
import com.aynlabs.lumoBills.backend.entity.User;
import com.aynlabs.lumoBills.backend.service.SystemSettingService;
import com.aynlabs.lumoBills.backend.service.UserService;
import com.aynlabs.lumoBills.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.Collections;

@RolesAllowed("ADMIN")
@Route(value = "admin", layout = MainLayout.class)
@PageTitle("Admin Settings | LumoBills")
public class AdminView extends VerticalLayout {

    private final UserService userService;
    private final SystemSettingService settingService;
    private final com.aynlabs.lumoBills.backend.service.TaxService taxService;
    private final com.aynlabs.lumoBills.backend.service.DiscountService discountService;

    // User Grid
    private Grid<User> userGrid = new Grid<>(User.class);

    // Tax & Discount Grids
    private Grid<com.aynlabs.lumoBills.backend.entity.Tax> taxGrid = new Grid<>(
            com.aynlabs.lumoBills.backend.entity.Tax.class);
    private Grid<com.aynlabs.lumoBills.backend.entity.Discount> discountGrid = new Grid<>(
            com.aynlabs.lumoBills.backend.entity.Discount.class);

    // Settings Fields
    private TextField companyName = new TextField("Company Name");
    private TextArea companyAddress = new TextArea("Company Address");
    private TextField gstNo = new TextField("GST No");
    private TextField companyPhone = new TextField("Phone");
    private TextField companyEmail = new TextField("Email");
    private TextArea terms = new TextArea("Invoice Terms & Conditions");

    // Master Setup
    private com.vaadin.flow.component.combobox.ComboBox<String> currencySelect = new com.vaadin.flow.component.combobox.ComboBox<>(
            "Currency");

    public AdminView(UserService userService, SystemSettingService settingService,
            com.aynlabs.lumoBills.backend.service.TaxService taxService,
            com.aynlabs.lumoBills.backend.service.DiscountService discountService) {
        this.userService = userService;
        this.settingService = settingService;
        this.taxService = taxService;
        this.discountService = discountService;

        addClassName("admin-view");
        setSizeFull();

        Tab usersTab = new Tab("User Management");
        Tab masterSetupTab = new Tab("Master Setup");
        Tab settingsTab = new Tab("Billing Configuration");
        Tab taxDiscountTab = new Tab("Taxes & Discounts");
        Tabs tabs = new Tabs(usersTab, masterSetupTab, settingsTab, taxDiscountTab);

        Div content = new Div();
        content.setSizeFull();

        VerticalLayout usersLayout = createUsersLayout();
        VerticalLayout masterSetupLayout = createMasterSetupLayout();
        VerticalLayout settingsLayout = createSettingsLayout();
        VerticalLayout taxDiscountLayout = createTaxDiscountLayout();

        masterSetupLayout.setVisible(false);
        settingsLayout.setVisible(false);
        taxDiscountLayout.setVisible(false);

        content.add(usersLayout, masterSetupLayout, settingsLayout, taxDiscountLayout);

        tabs.addSelectedChangeListener(event -> {
            usersLayout.setVisible(tabs.getSelectedTab().equals(usersTab));
            masterSetupLayout.setVisible(tabs.getSelectedTab().equals(masterSetupTab));
            settingsLayout.setVisible(tabs.getSelectedTab().equals(settingsTab));
            taxDiscountLayout.setVisible(tabs.getSelectedTab().equals(taxDiscountTab));
        });

        add(new H3("Admin Settings"), tabs, content);
    }

    private VerticalLayout createMasterSetupLayout() {
        VerticalLayout layout = new VerticalLayout();
        layout.setMaxWidth("600px");

        currencySelect.setItems("INR", "USD", "EUR", "GBP", "JPY");
        currencySelect.setValue(settingService.getValue("CURRENCY", "INR"));

        Button saveMaster = new Button("Save Master Settings", e -> {
            settingService.setValue("CURRENCY", currencySelect.getValue());
            Notification.show("Master settings saved");
        });
        saveMaster.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        layout.add(currencySelect, saveMaster);
        return layout;
    }

    private VerticalLayout createTaxDiscountLayout() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setPadding(false);

        // Taxes Section
        H3 taxHeader = new H3("Taxes");
        Button addTaxBtn = new Button("Add Tax", e -> openTaxEditor(new com.aynlabs.lumoBills.backend.entity.Tax()));

        taxGrid.setColumns("name", "percentage", "active");
        taxGrid.addComponentColumn(tax -> {
            Button delete = new Button(
                    new com.vaadin.flow.component.icon.Icon(com.vaadin.flow.component.icon.VaadinIcon.TRASH));
            delete.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            delete.addClickListener(e -> {
                taxService.delete(tax);
                updateTaxList();
                Notification.show("Tax deleted");
            });
            return delete;
        });

        com.aynlabs.lumoBills.ui.util.GridHelper.setBasicProperties(taxGrid);

        taxGrid.asSingleSelect().addValueChangeListener(e -> {
            if (e.getValue() != null)
                openTaxEditor(e.getValue());
        });

        // Discounts Section
        H3 discountHeader = new H3("Discounts");
        Button addDiscountBtn = new Button("Add Discount",
                e -> openDiscountEditor(new com.aynlabs.lumoBills.backend.entity.Discount()));

        discountGrid.setColumns("name", "discountValue", "type", "active");
        discountGrid.addComponentColumn(d -> {
            Button delete = new Button(
                    new com.vaadin.flow.component.icon.Icon(com.vaadin.flow.component.icon.VaadinIcon.TRASH));
            delete.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            delete.addClickListener(e -> {
                discountService.delete(d);
                updateDiscountList();
                Notification.show("Discount deleted");
            });
            return delete;
        });

        com.aynlabs.lumoBills.ui.util.GridHelper.setBasicProperties(discountGrid);

        discountGrid.asSingleSelect().addValueChangeListener(e -> {
            if (e.getValue() != null)
                openDiscountEditor(e.getValue());
        });

        VerticalLayout taxSection = new VerticalLayout(taxHeader, addTaxBtn, taxGrid);
        VerticalLayout discountSection = new VerticalLayout(discountHeader, addDiscountBtn, discountGrid);

        com.vaadin.flow.component.orderedlayout.HorizontalLayout mainLayout = new com.vaadin.flow.component.orderedlayout.HorizontalLayout(
                taxSection, discountSection);
        mainLayout.setSizeFull();

        updateTaxList();
        updateDiscountList();

        layout.add(mainLayout);
        return layout;
    }

    private void updateTaxList() {
        taxGrid.setItems(taxService.findAll());
    }

    private void updateDiscountList() {
        discountGrid.setItems(discountService.findAll());
    }

    private void openTaxEditor(com.aynlabs.lumoBills.backend.entity.Tax tax) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(tax.getId() == null ? "New Tax" : "Edit Tax");

        FormLayout form = new FormLayout();
        TextField name = new TextField("Name");
        com.vaadin.flow.component.textfield.BigDecimalField percentage = new com.vaadin.flow.component.textfield.BigDecimalField(
                "Percentage (%)");
        com.vaadin.flow.component.checkbox.Checkbox active = new com.vaadin.flow.component.checkbox.Checkbox("Active");

        Binder<com.aynlabs.lumoBills.backend.entity.Tax> binder = new BeanValidationBinder<>(
                com.aynlabs.lumoBills.backend.entity.Tax.class);
        // REMOVED: binder.bindInstanceFields(this);
        binder.bind(name, "name");
        binder.bind(percentage, "percentage");
        binder.bind(active, "active");
        binder.setBean(tax);

        Button save = new Button("Save", e -> {
            if (binder.validate().isOk()) {
                taxService.save(tax);
                updateTaxList();
                dialog.close();
                Notification.show("Tax saved");
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button("Cancel", e -> dialog.close());

        form.add(name, percentage, active);
        dialog.add(form);
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void openDiscountEditor(com.aynlabs.lumoBills.backend.entity.Discount discount) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(discount.getId() == null ? "New Discount" : "Edit Discount");

        FormLayout form = new FormLayout();
        TextField name = new TextField("Name");
        com.vaadin.flow.component.textfield.BigDecimalField value = new com.vaadin.flow.component.textfield.BigDecimalField(
                "Value");
        com.vaadin.flow.component.combobox.ComboBox<com.aynlabs.lumoBills.backend.entity.Discount.DiscountType> type = new com.vaadin.flow.component.combobox.ComboBox<>(
                "Type");
        type.setItems(com.aynlabs.lumoBills.backend.entity.Discount.DiscountType.values());
        com.vaadin.flow.component.checkbox.Checkbox active = new com.vaadin.flow.component.checkbox.Checkbox("Active");

        Binder<com.aynlabs.lumoBills.backend.entity.Discount> binder = new BeanValidationBinder<>(
                com.aynlabs.lumoBills.backend.entity.Discount.class);
        binder.bind(name, "name");
        // UPDATED: "value" -> "discountValue"
        binder.bind(value, "discountValue");
        binder.bind(type, "type");
        binder.bind(active, "active");
        binder.setBean(discount);

        Button save = new Button("Save", e -> {
            if (binder.validate().isOk()) {
                discountService.save(discount);
                updateDiscountList();
                dialog.close();
                Notification.show("Discount saved");
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button("Cancel", e -> dialog.close());

        form.add(name, value, type, active);
        dialog.add(form);
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private VerticalLayout createUsersLayout() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setPadding(false);

        userGrid.setColumns("username", "name", "active");
        userGrid.addColumn(user -> user.getRoles()).setHeader("Roles");
        // Duplicate columns removed

        com.aynlabs.lumoBills.ui.util.GridHelper.setBasicProperties(userGrid);

        userGrid.asSingleSelect().addValueChangeListener(e -> {
            if (e.getValue() != null)
                openUserEditor(e.getValue());
        });

        Button addUserBtn = new Button("Add User", e -> openUserEditor(new User()));
        addUserBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        layout.add(addUserBtn, userGrid);
        updateUserList();
        return layout;
    }

    private VerticalLayout createSettingsLayout() {
        VerticalLayout layout = new VerticalLayout();
        layout.setMaxWidth("600px");

        loadSettings();

        companyName.setRequired(true);
        companyName.setRequiredIndicatorVisible(true);

        Button saveSettings = new Button("Save Configuration", e -> {
            if (companyName.isEmpty()) {
                Notification.show("Company Name is mandatory", 5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            saveSettings();
        });
        saveSettings.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        FormLayout form = new FormLayout();
        form.add(companyName, companyAddress, gstNo, companyPhone, companyEmail, terms);
        form.setColspan(companyAddress, 2);
        form.setColspan(terms, 2);

        layout.add(form, saveSettings);
        return layout;
    }

    private void loadSettings() {
        companyName.setValue(settingService.getValue("COMPANY_NAME", "LumoBills Corp"));
        companyAddress.setValue(settingService.getValue("COMPANY_ADDRESS", "123 Business St, City"));
        gstNo.setValue(settingService.getValue("GST_NO", ""));
        companyPhone.setValue(settingService.getValue("COMPANY_PHONE", ""));
        companyEmail.setValue(settingService.getValue("COMPANY_EMAIL", ""));
        terms.setValue(settingService.getValue("INVOICE_TERMS", "Payment due within 30 days."));
    }

    private void saveSettings() {
        settingService.setValue("COMPANY_NAME", companyName.getValue());
        settingService.setValue("COMPANY_ADDRESS", companyAddress.getValue());
        settingService.setValue("GST_NO", gstNo.getValue());
        settingService.setValue("COMPANY_PHONE", companyPhone.getValue());
        settingService.setValue("COMPANY_EMAIL", companyEmail.getValue());
        settingService.setValue("INVOICE_TERMS", terms.getValue());
        Notification.show("Settings saved successfully");
    }

    private void updateUserList() {
        userGrid.setItems(userService.findAll());
    }

    private void openUserEditor(User user) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(user.getId() == null ? "New User" : "Edit User");

        FormLayout form = new FormLayout();
        TextField username = new TextField("Username");
        TextField name = new TextField("Full Name");
        PasswordField password = new PasswordField("Password (leave empty to keep)");
        CheckboxGroup<Role> roles = new CheckboxGroup<>("Roles");
        roles.setItems(Role.values());

        CheckboxGroup<String> accessiblePages = new CheckboxGroup<>("Allowed Pages");
        accessiblePages.setItems("Dashboard", "Stock", "Categories", "Billing", "Invoices");

        Binder<User> binder = new BeanValidationBinder<>(User.class);
        binder.bind(username, "username");
        binder.bind(name, "name");
        binder.bind(roles, "roles");
        binder.bind(accessiblePages, "accessibleViews");

        binder.readBean(user);
        if (user.getRoles() != null) {
            roles.setValue(user.getRoles());
        } else {
            roles.setValue(Collections.singleton(Role.USER));
        }

        if (user.getAccessibleViews() != null) {
            accessiblePages.setValue(user.getAccessibleViews());
        }

        Button save = new Button("Save", e -> {
            try {
                binder.writeBean(user);
                user.setRoles(roles.getValue());
                user.setAccessibleViews(accessiblePages.getValue());

                if (!password.isEmpty()) {
                    userService.registerUser(user, password.getValue());
                } else {
                    userService.save(user);
                }
                updateUserList();
                dialog.close();
                Notification.show("User saved");
            } catch (Exception ex) {
                Notification.show("Error saving: " + ex.getMessage());
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Cancel", e -> dialog.close());

        dialog.add(form);
        form.add(username, name, password, roles, accessiblePages);
        dialog.getFooter().add(cancel, save);

        dialog.open();
    }
}
