package com.handbagstore.dal;

import com.handbagstore.dto.InvoiceDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InvoiceDAL {
    private Connection getConnection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public int insert(InvoiceDTO inv) throws SQLException {
        String sql = "INSERT INTO invoices (invoice_code, customer_id, staff_id, discount_id, subtotal, " +
                     "discount_amount, total, payment_method, payment_received, change_amount, status, expires_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, inv.getInvoiceCode());
            if (inv.getCustomerId() != null) ps.setInt(2, inv.getCustomerId());
            else ps.setNull(2, Types.INTEGER);
            ps.setInt(3, inv.getStaffId());
            if (inv.getDiscountId() != null) ps.setInt(4, inv.getDiscountId());
            else ps.setNull(4, Types.INTEGER);
            ps.setBigDecimal(5, inv.getSubtotal());
            ps.setBigDecimal(6, inv.getDiscountAmount());
            ps.setBigDecimal(7, inv.getTotal());
            ps.setString(8, inv.getPaymentMethod());
            ps.setBigDecimal(9, inv.getPaymentReceived());
            ps.setBigDecimal(10, inv.getChangeAmount());
            ps.setString(11, inv.getStatus());
            if (inv.getExpiresAt() != null) ps.setTimestamp(12, Timestamp.valueOf(inv.getExpiresAt()));
            else ps.setNull(12, Types.TIMESTAMP);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    public void updateStatus(int invoiceId, String status) throws SQLException {
        String sql = "UPDATE invoices SET status = ?" +
                     (status.equals("PAID") ? ", paid_at = GETDATE()" : "") +
                     " WHERE invoice_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, invoiceId);
            ps.executeUpdate();
        }
    }

    public void updatePaymentInfo(int invoiceId, String paymentMethod, java.math.BigDecimal received, java.math.BigDecimal change) throws SQLException {
        String sql = "UPDATE invoices SET payment_method = ?, payment_received = ?, change_amount = ?, " +
                     "status = 'PAID', paid_at = GETDATE() WHERE invoice_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, paymentMethod);
            ps.setBigDecimal(2, received);
            ps.setBigDecimal(3, change);
            ps.setInt(4, invoiceId);
            ps.executeUpdate();
        }
    }

    public InvoiceDTO getById(int invoiceId) throws SQLException {
        String sql = "SELECT i.*, c.full_name AS customer_name, c.phone AS customer_phone, " +
                     "a.full_name AS staff_name, d.code AS discount_code " +
                     "FROM invoices i LEFT JOIN customers c ON i.customer_id = c.customer_id " +
                     "LEFT JOIN accounts a ON i.staff_id = a.account_id " +
                     "LEFT JOIN discounts d ON i.discount_id = d.discount_id " +
                     "WHERE i.invoice_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSet(rs);
            }
        }
        return null;
    }

    public List<InvoiceDTO> getAll() throws SQLException {
        String sql = "SELECT i.*, c.full_name AS customer_name, c.phone AS customer_phone, " +
                     "a.full_name AS staff_name, d.code AS discount_code " +
                     "FROM invoices i LEFT JOIN customers c ON i.customer_id = c.customer_id " +
                     "LEFT JOIN accounts a ON i.staff_id = a.account_id " +
                     "LEFT JOIN discounts d ON i.discount_id = d.discount_id " +
                     "ORDER BY i.created_at DESC";
        List<InvoiceDTO> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapResultSet(rs));
        }
        return list;
    }

    public List<InvoiceDTO> search(String invoiceCode, Integer customerId, java.sql.Date from, java.sql.Date to) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT i.*, c.full_name AS customer_name, c.phone AS customer_phone, " +
            "a.full_name AS staff_name, d.code AS discount_code " +
            "FROM invoices i LEFT JOIN customers c ON i.customer_id = c.customer_id " +
            "LEFT JOIN accounts a ON i.staff_id = a.account_id " +
            "LEFT JOIN discounts d ON i.discount_id = d.discount_id WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (invoiceCode != null && !invoiceCode.isEmpty()) {
            sql.append("AND i.invoice_code LIKE ? ");
            params.add("%" + invoiceCode + "%");
        }
        if (customerId != null) {
            sql.append("AND i.customer_id = ? ");
            params.add(customerId);
        }
        if (from != null) {
            sql.append("AND CAST(i.created_at AS DATE) >= ? ");
            params.add(from);
        }
        if (to != null) {
            sql.append("AND CAST(i.created_at AS DATE) <= ? ");
            params.add(to);
        }
        sql.append("ORDER BY i.created_at DESC");

        List<InvoiceDTO> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    /** Lấy các đơn PENDING đã hết hạn */
    public List<InvoiceDTO> getPendingExpired() throws SQLException {
        String sql = "SELECT i.*, c.full_name AS customer_name, c.phone AS customer_phone, " +
                     "a.full_name AS staff_name, d.code AS discount_code " +
                     "FROM invoices i LEFT JOIN customers c ON i.customer_id = c.customer_id " +
                     "LEFT JOIN accounts a ON i.staff_id = a.account_id " +
                     "LEFT JOIN discounts d ON i.discount_id = d.discount_id " +
                     "WHERE i.status = 'PENDING' AND i.expires_at <= GETDATE()";
        List<InvoiceDTO> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapResultSet(rs));
        }
        return list;
    }

    /** Lấy các đơn PENDING đang chờ */
    public List<InvoiceDTO> getPendingOrders() throws SQLException {
        String sql = "SELECT i.*, c.full_name AS customer_name, c.phone AS customer_phone, " +
                     "a.full_name AS staff_name, d.code AS discount_code " +
                     "FROM invoices i LEFT JOIN customers c ON i.customer_id = c.customer_id " +
                     "LEFT JOIN accounts a ON i.staff_id = a.account_id " +
                     "LEFT JOIN discounts d ON i.discount_id = d.discount_id " +
                     "WHERE i.status = 'PENDING' ORDER BY i.expires_at ASC";
        List<InvoiceDTO> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapResultSet(rs));
        }
        return list;
    }

    /** Lấy hóa đơn theo khách hàng (lịch sử mua) */
    public List<InvoiceDTO> getByCustomerId(int customerId) throws SQLException {
        String sql = "SELECT i.*, c.full_name AS customer_name, c.phone AS customer_phone, " +
                     "a.full_name AS staff_name, d.code AS discount_code " +
                     "FROM invoices i LEFT JOIN customers c ON i.customer_id = c.customer_id " +
                     "LEFT JOIN accounts a ON i.staff_id = a.account_id " +
                     "LEFT JOIN discounts d ON i.discount_id = d.discount_id " +
                     "WHERE i.customer_id = ? AND i.status = 'PAID' ORDER BY i.created_at DESC";
        List<InvoiceDTO> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    /** Sinh mã hóa đơn tự động: HD + yyyyMMdd + sequence */
    public String generateInvoiceCode() throws SQLException {
        String sql = "SELECT COUNT(*) + 1 AS seq FROM invoices WHERE CAST(created_at AS DATE) = CAST(GETDATE() AS DATE)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                int seq = rs.getInt("seq");
                java.time.LocalDate today = java.time.LocalDate.now();
                return String.format("HD%s%03d", today.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")), seq);
            }
        }
        return "HD" + System.currentTimeMillis();
    }

    private InvoiceDTO mapResultSet(ResultSet rs) throws SQLException {
        InvoiceDTO inv = new InvoiceDTO();
        inv.setInvoiceId(rs.getInt("invoice_id"));
        inv.setInvoiceCode(rs.getString("invoice_code"));
        inv.setCustomerId(rs.getObject("customer_id") != null ? rs.getInt("customer_id") : null);
        inv.setStaffId(rs.getInt("staff_id"));
        inv.setDiscountId(rs.getObject("discount_id") != null ? rs.getInt("discount_id") : null);
        inv.setSubtotal(rs.getBigDecimal("subtotal"));
        inv.setDiscountAmount(rs.getBigDecimal("discount_amount"));
        inv.setTotal(rs.getBigDecimal("total"));
        inv.setPaymentMethod(rs.getString("payment_method"));
        inv.setPaymentReceived(rs.getBigDecimal("payment_received"));
        inv.setChangeAmount(rs.getBigDecimal("change_amount"));
        inv.setStatus(rs.getString("status"));
        Timestamp ct = rs.getTimestamp("created_at");
        if (ct != null) inv.setCreatedAt(ct.toLocalDateTime());
        Timestamp pt = rs.getTimestamp("paid_at");
        if (pt != null) inv.setPaidAt(pt.toLocalDateTime());
        Timestamp et = rs.getTimestamp("expires_at");
        if (et != null) inv.setExpiresAt(et.toLocalDateTime());
        // Transient fields
        try { inv.setCustomerName(rs.getString("customer_name")); } catch (SQLException ignored) {}
        try { inv.setCustomerPhone(rs.getString("customer_phone")); } catch (SQLException ignored) {}
        try { inv.setStaffName(rs.getString("staff_name")); } catch (SQLException ignored) {}
        try { inv.setDiscountCode(rs.getString("discount_code")); } catch (SQLException ignored) {}
        return inv;
    }
}
