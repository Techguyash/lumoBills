package com.aynlabs.lumoBills.ui.util;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;

public class GridHelper {

    public static <T> void setBasicProperties(Grid<T> grid) {
        grid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_ROW_STRIPES);

        for (Column<T> column : grid.getColumns()) {
            column.setResizable(true);
            column.setSortable(true);
            column.setAutoWidth(true);
        }
    }
}
