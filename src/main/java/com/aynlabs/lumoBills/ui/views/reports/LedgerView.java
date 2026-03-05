package com.aynlabs.lumoBills.ui.views.reports;

import com.aynlabs.lumoBills.backend.entity.LedgerEntry;
import com.aynlabs.lumoBills.backend.service.LedgerService;
import com.aynlabs.lumoBills.backend.service.SystemSettingService;
import com.aynlabs.lumoBills.ui.MainLayout;
import com.aynlabs.lumoBills.ui.util.CurrencyUtility;
import com.aynlabs.lumoBills.ui.util.GridHelper;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@PermitAll
@Route(value = "ledger", layout = MainLayout.class)
@PageTitle("Ledger Statement | LumoBills")
public class LedgerView extends VerticalLayout {

    private final LedgerService ledgerService;
    private final String currencySymbol;

    private Grid<LedgerEntry> grid = new Grid<>(LedgerEntry.class);

    private DatePicker startDate = new DatePicker("Start Date");
    private DatePicker endDate = new DatePicker("End Date");

    private Span incomeSpan = new Span();
    private Span expenseSpan = new Span();
    private Span netSpan = new Span();

    public LedgerView(LedgerService ledgerService, SystemSettingService settingService) {
        this.ledgerService = ledgerService;
        String cur = settingService.getValue("CURRENCY", "INR");
        this.currencySymbol = CurrencyUtility.getCurrencySymbol(cur);

        setSizeFull();
        setPadding(true);

        // Defaults to current month
        startDate.setValue(LocalDate.now().withDayOfMonth(1));
        endDate.setValue(LocalDate.now());

        add(new H2("Financial Ledger"));
        add(createToolbar());
        add(createSummaryBoard());

        configureGrid();
        add(grid);

        updateView();
    }

    private HorizontalLayout createToolbar() {
        Button searchBtn = new Button("Filter", new Icon(VaadinIcon.SEARCH));
        searchBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        searchBtn.addClickListener(e -> updateView());

        HorizontalLayout layout = new HorizontalLayout(startDate, endDate, searchBtn);
        layout.setDefaultVerticalComponentAlignment(Alignment.BASELINE);
        return layout;
    }

    private HorizontalLayout createSummaryBoard() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setJustifyContentMode(JustifyContentMode.AROUND);
        layout.setPadding(true);
        layout.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        layout.getStyle().set("border-radius", "8px");
        layout.getStyle().set("margin-bottom", "1em");

        incomeSpan.getStyle().set("color", "var(--lumo-success-text-color)");
        incomeSpan.getStyle().set("font-size", "1.5em");
        incomeSpan.getStyle().set("font-weight", "bold");

        expenseSpan.getStyle().set("color", "var(--lumo-error-text-color)");
        expenseSpan.getStyle().set("font-size", "1.5em");
        expenseSpan.getStyle().set("font-weight", "bold");

        netSpan.getStyle().set("font-size", "1.5em");
        netSpan.getStyle().set("font-weight", "bold");

        layout.add(
                new VerticalLayout(new H4("Total Income"), incomeSpan),
                new VerticalLayout(new H4("Total Expense"), expenseSpan),
                new VerticalLayout(new H4("Net Balance"), netSpan));

        return layout;
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.setColumns("transactionDate", "category", "description", "referenceId", "paymentMode");

        grid.addComponentColumn(entry -> {
            Span badge = new Span(entry.getType().name());
            badge.getElement().getThemeList().add("badge");
            if (entry.getType() == LedgerEntry.EntryType.INCOME) {
                badge.getElement().getThemeList().add("success");
            } else {
                badge.getElement().getThemeList().add("error");
            }
            return badge;
        }).setHeader("Type").setSortable(true);

        grid.addColumn(entry -> currencySymbol + entry.getAmount()).setHeader("Amount");

        GridHelper.setBasicProperties(grid);
    }

    private void updateView() {
        LocalDateTime start = startDate.getValue() != null ? startDate.getValue().atStartOfDay() : LocalDateTime.MIN;
        LocalDateTime end = endDate.getValue() != null ? endDate.getValue().atTime(LocalTime.MAX) : LocalDateTime.MAX;

        List<LedgerEntry> entries = ledgerService.getEntriesBetween(start, end);
        grid.setItems(entries);

        BigDecimal income = ledgerService.getTotalIncome(start, end);
        BigDecimal expense = ledgerService.getTotalExpense(start, end);
        BigDecimal net = income.subtract(expense);

        incomeSpan.setText(currencySymbol + income.toString());
        expenseSpan.setText(currencySymbol + expense.toString());
        netSpan.setText(currencySymbol + net.toString());

        if (net.compareTo(BigDecimal.ZERO) >= 0) {
            netSpan.getStyle().set("color", "var(--lumo-success-text-color)");
        } else {
            netSpan.getStyle().set("color", "var(--lumo-error-text-color)");
        }
    }
}
