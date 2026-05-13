package com.handbagstore.utils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Tiện ích cho JTable: Căn lề, định dạng cột.
 */
public class TableUtils {

    public static void alignColumn(JTable table, int column, int alignment) {
        if (column < 0 || column >= table.getColumnCount())
            return;
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setHorizontalAlignment(alignment);
        table.getColumnModel().getColumn(column).setCellRenderer(renderer);
    }

    public static void alignRight(JTable table, int... columns) {
        for (int col : columns)
            alignColumn(table, col, JLabel.RIGHT);
    }

    public static void alignCenter(JTable table, int... columns) {
        for (int col : columns)
            alignColumn(table, col, JLabel.CENTER);
    }

    public static void alignLeft(JTable table, int... columns) {
        for (int col : columns)
            alignColumn(table, col, JLabel.LEFT);
    }
}
