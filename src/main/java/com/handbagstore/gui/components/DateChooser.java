package com.handbagstore.gui.components;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Một bộ chọn ngày đơn giản sử dụng JDialog.
 */
public class DateChooser extends JDialog {
    private LocalDate selectedDate;
    private LocalDate displayDate;
    private final JPanel daysPanel = new JPanel(new GridLayout(0, 7));
    private final JComboBox<String> cmbMonth;
    private final JComboBox<Integer> cmbYear;
    private boolean isUpdating = false;

    public DateChooser(Frame parent, LocalDate initialDate) {
        super(parent, "Chọn ngày", true);
        this.selectedDate = initialDate != null ? initialDate : LocalDate.now();
        this.displayDate = selectedDate.withDayOfMonth(1);

        String[] months = {"Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5", "Tháng 6", 
                           "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"};
        cmbMonth = new JComboBox<>(months);

        int currentYear = LocalDate.now().getYear();
        Integer[] years = new Integer[101];
        for (int i = 0; i <= 100; i++) {
            years[i] = currentYear - 50 + i;
        }
        cmbYear = new JComboBox<>(years);

        initComponents();
    }

    private void initComponents() {
        setSize(350, 350);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout(5, 5));
        ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Header: Prev, Month, Year, Next
        JPanel header = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        JButton btnPrev = new JButton("<");
        JButton btnNext = new JButton(">");

        updateComboBoxes();

        btnPrev.addActionListener(e -> {
            displayDate = displayDate.minusMonths(1);
            updateComboBoxes();
            updateCalendar();
        });

        btnNext.addActionListener(e -> {
            displayDate = displayDate.plusMonths(1);
            updateComboBoxes();
            updateCalendar();
        });

        cmbMonth.addActionListener(e -> {
            if (isUpdating) return;
            int m = cmbMonth.getSelectedIndex() + 1;
            displayDate = displayDate.withMonth(m);
            updateCalendar();
        });

        cmbYear.addActionListener(e -> {
            if (isUpdating) return;
            int y = (Integer) cmbYear.getSelectedItem();
            displayDate = displayDate.withYear(y);
            updateCalendar();
        });

        header.add(btnPrev);
        header.add(cmbMonth);
        header.add(cmbYear);
        header.add(btnNext);
        add(header, BorderLayout.NORTH);

        // Weekdays row
        String[] weekDays = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
        JPanel weekPanel = new JPanel(new GridLayout(1, 7));
        for (String day : weekDays) {
            JLabel lbl = new JLabel(day, SwingConstants.CENTER);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lbl.setForeground(new Color(100, 100, 100));
            weekPanel.add(lbl);
        }

        JPanel centerPanel = new JPanel(new BorderLayout(0, 5));
        centerPanel.add(weekPanel, BorderLayout.NORTH);
        centerPanel.add(daysPanel, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // Footer: Today button
        JButton btnToday = new JButton("Hôm nay");
        btnToday.addActionListener(e -> {
            selectedDate = LocalDate.now();
            dispose();
        });
        add(btnToday, BorderLayout.SOUTH);

        updateCalendar();
    }

    private void updateComboBoxes() {
        isUpdating = true;
        cmbMonth.setSelectedIndex(displayDate.getMonthValue() - 1);
        cmbYear.setSelectedItem(displayDate.getYear());
        isUpdating = false;
    }

    private void updateCalendar() {
        daysPanel.removeAll();

        YearMonth ym = YearMonth.from(displayDate);
        int firstDay = displayDate.getDayOfWeek().getValue(); // 1 (Mon) to 7 (Sun)
        int daysInMonth = ym.lengthOfMonth();

        // Fill empty days before first day of month
        for (int i = 1; i < firstDay; i++) {
            daysPanel.add(new JLabel(""));
        }

        // Fill days of month
        LocalDate today = LocalDate.now();
        for (int day = 1; day <= daysInMonth; day++) {
            final int d = day;
            LocalDate date = displayDate.withDayOfMonth(day);
            JButton btn = new JButton(String.valueOf(day));
            btn.setMargin(new Insets(2, 2, 2, 2));
            btn.setFocusPainted(false);
            
            if (date.equals(selectedDate)) {
                btn.setBackground(new Color(64, 133, 240));
                btn.setForeground(Color.WHITE);
            } else if (date.equals(today)) {
                btn.setForeground(new Color(64, 133, 240));
                btn.setFont(btn.getFont().deriveFont(Font.BOLD));
            }
            
            btn.addActionListener(e -> {
                selectedDate = displayDate.withDayOfMonth(d);
                dispose();
            });
            daysPanel.add(btn);
        }

        daysPanel.revalidate();
        daysPanel.repaint();
    }

    public static LocalDate showDialog(Component parent, LocalDate initialDate) {
        Window window = SwingUtilities.getWindowAncestor(parent);
        DateChooser chooser;
        if (window instanceof Frame) chooser = new DateChooser((Frame) window, initialDate);
        else chooser = new DateChooser(null, initialDate);
        chooser.setVisible(true);
        return chooser.selectedDate;
    }
}
