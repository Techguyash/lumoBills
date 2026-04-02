package com.aynlabs.lumoBills.ui.util;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

public class GridHelper {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public static <T> void setBasicProperties(Grid<T> grid) {
        grid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_ROW_STRIPES);

        for (Column<T> column : grid.getColumns()) {
            column.setResizable(true);
            column.setSortable(true);
            column.setAutoWidth(true);
        }
    }

    public static <T> Grid.Column<T> addDateTimeColumn(Grid<T> grid,
            com.vaadin.flow.function.ValueProvider<T, LocalDateTime> valueProvider, String header) {
        return grid.addColumn(t -> {
            LocalDateTime ldt = valueProvider.apply(t);
            return ldt != null ? ldt.format(DATE_TIME_FORMATTER) : "";
        }).setHeader(header).setSortable(true).setWidth("180px").setFlexGrow(0);
    }
}
