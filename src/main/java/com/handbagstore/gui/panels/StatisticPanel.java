package com.handbagstore.gui.panels;

import com.handbagstore.bll.StatisticBLL;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class StatisticPanel extends JPanel {
    private JLabel lblDailyRevenue, lblMonthlyRevenue;
    private JTable topProductsTable, stockTable;
    private DefaultTableModel topModel, stockModel;
    private JSpinner spnYear, spnMonth;
    private final StatisticBLL statisticBLL = new StatisticBLL();

    public StatisticPanel() { initComponents(); refreshData(); }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel lblTitle = new JLabel("📊 Thống kê Doanh thu");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        add(lblTitle, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();

        // Tab 1: Doanh thu
        JPanel revenuePanel = new JPanel(new BorderLayout(10, 10));
        JPanel revenueCards = new JPanel(new GridLayout(1, 2, 20, 0));
        revenueCards.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Card doanh thu ngày
        JPanel dailyCard = createRevenueCard("💰 Doanh thu hôm nay");
        lblDailyRevenue = new JLabel("0đ");
        lblDailyRevenue.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblDailyRevenue.setForeground(new Color(40, 167, 69));
        dailyCard.add(lblDailyRevenue);
        revenueCards.add(dailyCard);

        // Card doanh thu tháng
        JPanel monthlyCard = createRevenueCard("📅 Doanh thu tháng này");
        lblMonthlyRevenue = new JLabel("0đ");
        lblMonthlyRevenue.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblMonthlyRevenue.setForeground(new Color(64, 133, 240));
        monthlyCard.add(lblMonthlyRevenue);
        revenueCards.add(monthlyCard);

        revenuePanel.add(revenueCards, BorderLayout.NORTH);

        // Filter
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Năm:"));
        spnYear = new JSpinner(new SpinnerNumberModel(LocalDate.now().getYear(), 2020, 2030, 1));
        filterPanel.add(spnYear);
        filterPanel.add(new JLabel("Tháng:"));
        spnMonth = new JSpinner(new SpinnerNumberModel(LocalDate.now().getMonthValue(), 1, 12, 1));
        filterPanel.add(spnMonth);
        JButton btnRefresh = new JButton("🔄 Cập nhật");
        btnRefresh.addActionListener(e -> refreshData());
        filterPanel.add(btnRefresh);
        revenuePanel.add(filterPanel, BorderLayout.CENTER);

        tabs.addTab("💰 Doanh thu", revenuePanel);

        // Tab 2: Top sản phẩm
        JPanel topPanel = new JPanel(new BorderLayout());
        String[] topCols = {"Mã SP", "Tên SP", "Số lượng bán", "Doanh thu"};
        topModel = new DefaultTableModel(topCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        topProductsTable = new JTable(topModel);
        topProductsTable.setRowHeight(28);
        topPanel.add(new JScrollPane(topProductsTable), BorderLayout.CENTER);
        tabs.addTab("🏆 Top bán chạy", topPanel);

        // Tab 3: Tồn kho
        JPanel stockPanel = new JPanel(new BorderLayout());
        String[] stockCols = {"Mã SP", "Tên SP", "Tổng kho", "Đang giữ", "Khả dụng", "Giá vốn", "Giá bán"};
        stockModel = new DefaultTableModel(stockCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        stockTable = new JTable(stockModel);
        stockTable.setRowHeight(28);
        stockPanel.add(new JScrollPane(stockTable), BorderLayout.CENTER);
        tabs.addTab("📦 Tồn kho", stockPanel);

        add(tabs, BorderLayout.CENTER);
    }

    private JPanel createRevenueCard(String title) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 80), 1, true),
            BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        card.add(lbl);
        return card;
    }

    public void refreshData() {
        try {
            LocalDate today = LocalDate.now();
            int year = (int) spnYear.getValue();
            int month = (int) spnMonth.getValue();

            // Doanh thu ngày
            BigDecimal daily = statisticBLL.getDailyRevenue(Date.valueOf(today));
            lblDailyRevenue.setText(daily + "đ");

            // Doanh thu tháng
            BigDecimal monthly = statisticBLL.getMonthlyRevenue(year, month);
            lblMonthlyRevenue.setText(monthly + "đ");

            // Top sản phẩm tháng
            topModel.setRowCount(0);
            LocalDate from = LocalDate.of(year, month, 1);
            LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
            List<Object[]> topProducts = statisticBLL.getTopProducts(Date.valueOf(from), Date.valueOf(to), 10);
            for (Object[] row : topProducts) {
                topModel.addRow(row);
            }

            // Tồn kho
            stockModel.setRowCount(0);
            List<Object[]> stocks = statisticBLL.getInventoryStock();
            for (Object[] row : stocks) {
                stockModel.addRow(row);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
        }
    }
}
