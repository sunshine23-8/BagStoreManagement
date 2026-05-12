package com.handbagstore.dal;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class StatisticDAL {
    private Connection getConnection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    /** Doanh thu theo ngày (chỉ tính hóa đơn PAID) */
    public BigDecimal getDailyRevenue(java.sql.Date date) throws SQLException {
        String sql = "SELECT ISNULL(SUM(total), 0) AS revenue FROM invoices " +
                     "WHERE status = 'PAID' AND CAST(paid_at AS DATE) = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setDate(1, date);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal("revenue");
            }
        }
        return BigDecimal.ZERO;
    }

    /** Doanh thu theo tháng */
    public BigDecimal getMonthlyRevenue(int year, int month) throws SQLException {
        String sql = "SELECT ISNULL(SUM(total), 0) AS revenue FROM invoices " +
                     "WHERE status = 'PAID' AND YEAR(paid_at) = ? AND MONTH(paid_at) = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, year);
            ps.setInt(2, month);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal("revenue");
            }
        }
        return BigDecimal.ZERO;
    }

    /** Doanh thu từng ngày trong tháng (cho biểu đồ) */
    public Map<Integer, BigDecimal> getDailyRevenueInMonth(int year, int month) throws SQLException {
        String sql = "SELECT DAY(paid_at) AS day_num, SUM(total) AS revenue FROM invoices " +
                     "WHERE status = 'PAID' AND YEAR(paid_at) = ? AND MONTH(paid_at) = ? " +
                     "GROUP BY DAY(paid_at) ORDER BY day_num";
        Map<Integer, BigDecimal> map = new LinkedHashMap<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, year);
            ps.setInt(2, month);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getInt("day_num"), rs.getBigDecimal("revenue"));
                }
            }
        }
        return map;
    }

    /** Top N sản phẩm bán chạy theo khoảng thời gian */
    public List<Object[]> getTopProducts(java.sql.Date from, java.sql.Date to, int limit) throws SQLException {
        String sql = "SELECT TOP (?) p.product_code, p.name, SUM(id.quantity) AS total_qty, " +
                     "SUM(id.quantity * id.unit_price) AS total_revenue " +
                     "FROM invoice_details id " +
                     "JOIN invoices i ON id.invoice_id = i.invoice_id " +
                     "JOIN products p ON id.product_id = p.product_id " +
                     "WHERE i.status = 'PAID' AND CAST(i.paid_at AS DATE) BETWEEN ? AND ? " +
                     "GROUP BY p.product_code, p.name ORDER BY total_qty DESC";
        List<Object[]> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setDate(2, from);
            ps.setDate(3, to);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Object[]{
                        rs.getString("product_code"),
                        rs.getString("name"),
                        rs.getInt("total_qty"),
                        rs.getBigDecimal("total_revenue")
                    });
                }
            }
        }
        return list;
    }

    /** Thống kê tồn kho theo sản phẩm */
    public List<Object[]> getInventoryStock() throws SQLException {
        String sql = "SELECT p.product_code, p.name, i.quantity, i.reserved_qty, " +
                     "(i.quantity - i.reserved_qty) AS available, i.cost_price, p.price AS sell_price " +
                     "FROM inventory i JOIN products p ON i.product_id = p.product_id " +
                     "WHERE p.status = 'ACTIVE' ORDER BY p.name";
        List<Object[]> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getString("product_code"),
                    rs.getString("name"),
                    rs.getInt("quantity"),
                    rs.getInt("reserved_qty"),
                    rs.getInt("available"),
                    rs.getBigDecimal("cost_price"),
                    rs.getBigDecimal("sell_price")
                });
            }
        }
        return list;
    }
}
