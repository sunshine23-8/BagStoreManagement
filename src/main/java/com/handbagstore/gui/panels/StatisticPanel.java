package com.handbagstore.gui.panels;

import com.handbagstore.bll.StatisticBLL;
import com.handbagstore.utils.CurrencyUtils;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.axis.NumberTickUnit;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

public class StatisticPanel extends JPanel {
    private JLabel lblDailyRevenue, lblMonthlyRevenue;
    private JTable topProductsTable, stockTable;
    private DefaultTableModel topModel, stockModel;
    private JSpinner spnYear, spnMonth;
    private JComboBox<String> cbChartType, cbDayRange;
    private JSpinner spnChartYear, spnFromDate, spnToDate;
    private JPanel chartContainer, dateRangePanel;
    private final StatisticBLL statisticBLL = new StatisticBLL();

    public StatisticPanel() { initComponents(); refreshData(); updateChart(); }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel lblTitle = new JLabel("📊 Thống kê Doanh thu");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        add(lblTitle, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));

        // Tab 1: Doanh thu
        JPanel revenuePanel = new JPanel(new BorderLayout(10, 10));
        
        JPanel topHeader = new JPanel(new BorderLayout(5, 5));
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

        topHeader.add(revenueCards, BorderLayout.NORTH);

        // Filter cards
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Năm:"));
        spnYear = new JSpinner(new SpinnerNumberModel(LocalDate.now().getYear(), 2020, 2030, 1));
        spnYear.setEditor(new JSpinner.NumberEditor(spnYear, "#"));
        filterPanel.add(spnYear);
        filterPanel.add(new JLabel("Tháng:"));
        spnMonth = new JSpinner(new SpinnerNumberModel(LocalDate.now().getMonthValue(), 1, 12, 1));
        filterPanel.add(spnMonth);
        JButton btnRefresh = new JButton("🔄 Cập nhật");
        btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRefresh.addActionListener(e -> refreshData());
        filterPanel.add(btnRefresh);
        topHeader.add(filterPanel, BorderLayout.SOUTH);
        
        revenuePanel.add(topHeader, BorderLayout.NORTH);

        // Biểu đồ
        JPanel chartSection = new JPanel(new BorderLayout(5, 5));
        chartSection.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)), "📊 Biểu đồ doanh thu"));

        JPanel chartControls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        chartControls.add(new JLabel("Xem theo:"));
        cbChartType = new JComboBox<>(new String[]{"Ngày", "Tháng", "Năm"});
        cbChartType.addActionListener(e -> toggleChartFilters());
        chartControls.add(cbChartType);

        cbDayRange = new JComboBox<>(new String[]{"7 ngày qua", "30 ngày qua", "Tùy chọn"});
        cbDayRange.addActionListener(e -> toggleChartFilters());
        chartControls.add(cbDayRange);

        dateRangePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        dateRangePanel.add(new JLabel(" Từ:"));
        spnFromDate = new JSpinner(new SpinnerDateModel());
        spnFromDate.setEditor(new JSpinner.DateEditor(spnFromDate, "dd/MM/yyyy"));
        dateRangePanel.add(spnFromDate);
        dateRangePanel.add(new JLabel(" Đến:"));
        spnToDate = new JSpinner(new SpinnerDateModel());
        spnToDate.setEditor(new JSpinner.DateEditor(spnToDate, "dd/MM/yyyy"));
        dateRangePanel.add(spnToDate);
        chartControls.add(dateRangePanel);

        spnChartYear = new JSpinner(new SpinnerNumberModel(LocalDate.now().getYear(), 2020, 2030, 1));
        spnChartYear.setEditor(new JSpinner.NumberEditor(spnChartYear, "#"));
        chartControls.add(spnChartYear);

        JButton btnUpdateChart = new JButton("⚡ Xem");
        btnUpdateChart.addActionListener(e -> updateChart());
        chartControls.add(btnUpdateChart);

        chartSection.add(chartControls, BorderLayout.NORTH);
        chartContainer = new JPanel(new BorderLayout());
        chartSection.add(chartContainer, BorderLayout.CENTER);

        revenuePanel.add(chartSection, BorderLayout.CENTER);

        tabs.addTab("💰 Doanh thu", revenuePanel);

        toggleChartFilters();

        // Tab 2: Top sản phẩm
        JPanel topPanel = new JPanel(new BorderLayout());
        String[] topCols = {"Mã SP", "Tên SP", "Số lượng bán", "Doanh thu"};
        topModel = new DefaultTableModel(topCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        topProductsTable = new JTable(topModel);
        topProductsTable.setRowHeight(28);
        topProductsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
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
        stockTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
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

    private void toggleChartFilters() {
        String type = (String) cbChartType.getSelectedItem();
        cbDayRange.setVisible("Ngày".equals(type));
        dateRangePanel.setVisible("Ngày".equals(type) && "Tùy chọn".equals(cbDayRange.getSelectedItem()));
        spnChartYear.setVisible("Tháng".equals(type));
        revalidate(); repaint();
    }

    private void updateChart() {
        String type = (String) cbChartType.getSelectedItem();
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        String title = "";
        String xLabel = "";
        boolean isLine = false;

        try {
            if ("Ngày".equals(type)) {
                isLine = true;
                LocalDate to = LocalDate.now();
                LocalDate from;
                String range = (String) cbDayRange.getSelectedItem();
                if ("7 ngày qua".equals(range)) {
                    from = to.minusDays(6);
                    title = "Doanh thu 7 ngày qua";
                } else if ("30 ngày qua".equals(range)) {
                    from = to.minusDays(29);
                    title = "Doanh thu 30 ngày qua";
                } else {
                    from = new java.sql.Date(((java.util.Date) spnFromDate.getValue()).getTime()).toLocalDate();
                    to = new java.sql.Date(((java.util.Date) spnToDate.getValue()).getTime()).toLocalDate();
                    title = "Doanh thu từ " + from + " đến " + to;
                }
                xLabel = "Ngày";
                Map<String, BigDecimal> data = statisticBLL.getRevenueByDateRange(Date.valueOf(from), Date.valueOf(to));
                for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                    dataset.addValue(data.getOrDefault(d.toString(), BigDecimal.ZERO), "Doanh thu", d.format(DateTimeFormatter.ofPattern("dd/MM")));
                }
            } else if ("Tháng".equals(type)) {
                int year = (int) spnChartYear.getValue();
                title = "Doanh thu các tháng năm " + year;
                xLabel = "Tháng";
                Map<Integer, BigDecimal> data = statisticBLL.getRevenueByMonthsInYear(year);
                for (int m = 1; m <= 12; m++) {
                    dataset.addValue(data.getOrDefault(m, BigDecimal.ZERO), "Doanh thu", "Tháng " + m);
                }
            } else if ("Năm".equals(type)) {
                title = "Doanh thu các năm gần nhất";
                xLabel = "Năm";
                Map<Integer, BigDecimal> data = statisticBLL.getRevenueByRecentYears(5);
                for (Map.Entry<Integer, BigDecimal> entry : data.entrySet()) {
                    dataset.addValue(entry.getValue(), "Doanh thu", entry.getKey().toString());
                }
            }

            JFreeChart chart;
            if (isLine) {
                chart = ChartFactory.createLineChart(title, xLabel, "Doanh thu (VNĐ)", dataset, PlotOrientation.VERTICAL, false, true, false);
            } else {
                chart = ChartFactory.createBarChart(title, xLabel, "Doanh thu (VNĐ)", dataset, PlotOrientation.VERTICAL, false, true, false);
            }

            // --- Tùy chỉnh giao diện (UI/UX) ---
            chart.setBackgroundPaint(Color.WHITE);
            chart.getTitle().setFont(new Font("Segoe UI", Font.BOLD, 16));

            CategoryPlot plot = chart.getCategoryPlot();
            plot.setBackgroundPaint(Color.WHITE);
            plot.setRangeGridlinePaint(new Color(230, 230, 230));
            plot.setOutlineVisible(false);

            // Font cho các trục
            Font axisTitleFont = new Font("Segoe UI", Font.BOLD, 12);
            Font tickLabelFont = new Font("Segoe UI", Font.PLAIN, 11);
            
            CategoryAxis domainAxis = plot.getDomainAxis();
            domainAxis.setLabelFont(axisTitleFont);
            domainAxis.setTickLabelFont(tickLabelFont);

            NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
            rangeAxis.setLabelFont(axisTitleFont);
            rangeAxis.setTickLabelFont(tickLabelFont);
            
            // Format trục Y (Doanh thu)
            rangeAxis.setAutoTickUnitSelection(true); // Tự động chọn khoảng chia để không bị chồng chéo
            rangeAxis.setNumberFormatOverride(new DecimalFormat("#,###")); // Định dạng số có dấu phẩy

            if (isLine) {
                LineAndShapeRenderer renderer = new LineAndShapeRenderer();
                renderer.setSeriesPaint(0, new Color(13, 110, 253)); // Màu xanh dương phẳng
                renderer.setSeriesStroke(0, new BasicStroke(2.5f));
                plot.setRenderer(renderer);
            } else {
                BarRenderer renderer = (BarRenderer) plot.getRenderer();
                renderer.setSeriesPaint(0, new Color(13, 110, 253)); // Màu xanh dương #0d6efd
                renderer.setBarPainter(new StandardBarPainter()); // Flat Design
                renderer.setShadowVisible(false); // Bỏ bóng
                renderer.setMaximumBarWidth(0.1);
            }

            chartContainer.removeAll();
            ChartPanel chartPanel = new ChartPanel(chart);
            chartPanel.setBackground(Color.WHITE);
            chartContainer.add(chartPanel, BorderLayout.CENTER);
            chartContainer.revalidate();
            chartContainer.repaint();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi biểu đồ: " + ex.getMessage());
        }
    }

    public void refreshData() {
        try {
            LocalDate today = LocalDate.now();
            int year = (int) spnYear.getValue();
            int month = (int) spnMonth.getValue();

            // Doanh thu ngày
            BigDecimal daily = statisticBLL.getDailyRevenue(Date.valueOf(today));
            lblDailyRevenue.setText(CurrencyUtils.format(daily));

            // Doanh thu tháng
            BigDecimal monthly = statisticBLL.getMonthlyRevenue(year, month);
            lblMonthlyRevenue.setText(CurrencyUtils.format(monthly));

            // Top sản phẩm tháng
            topModel.setRowCount(0);
            LocalDate from = LocalDate.of(year, month, 1);
            LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
            List<Object[]> topProducts = statisticBLL.getTopProducts(Date.valueOf(from), Date.valueOf(to), 10);
            for (Object[] row : topProducts) {
                // row[3] is revenue
                row[3] = CurrencyUtils.format(row[3]);
                topModel.addRow(row);
            }

            // Tồn kho
            stockModel.setRowCount(0);
            List<Object[]> stocks = statisticBLL.getInventoryStock();
            for (Object[] row : stocks) {
                // row[5] is cost price, row[6] is sell price
                row[5] = CurrencyUtils.format(row[5]);
                row[6] = CurrencyUtils.format(row[6]);
                stockModel.addRow(row);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
        }
    }
}
