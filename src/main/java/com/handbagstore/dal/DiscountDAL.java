package com.handbagstore.dal;

import com.handbagstore.dto.DiscountDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DiscountDAL {
    public DiscountDAL() {
        addStackableColumn();
    }

    private Connection getConnection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public DiscountDTO getByCode(String code) throws SQLException {
        String sql = "SELECT * FROM discounts WHERE code = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSet(rs);
            }
        }
        return null;
    }

    public DiscountDTO getById(int discountId) throws SQLException {
        String sql = "SELECT * FROM discounts WHERE discount_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, discountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSet(rs);
            }
        }
        return null;
    }

    public List<DiscountDTO> getAll() throws SQLException {
        String sql = "SELECT * FROM discounts ORDER BY created_by DESC";
        List<DiscountDTO> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapResultSet(rs));
        }
        return list;
    }

    public List<DiscountDTO> getActive() throws SQLException {
        String sql = "SELECT * FROM discounts WHERE is_active = 1 AND " +
                     "(start_time IS NULL OR start_time <= GETDATE()) AND " +
                     "(end_time IS NULL OR end_time >= GETDATE()) ORDER BY code";
        List<DiscountDTO> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapResultSet(rs));
        }
        return list;
    }

    /** Lấy mã giảm giá BIRTHDAY đang active */
    public List<DiscountDTO> getActiveBirthdayDiscounts() throws SQLException {
        String sql = "SELECT * FROM discounts WHERE occasion = 'BIRTHDAY' AND is_active = 1 AND " +
                     "(start_time IS NULL OR start_time <= GETDATE()) AND " +
                     "(end_time IS NULL OR end_time >= GETDATE())";
        List<DiscountDTO> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapResultSet(rs));
        }
        return list;
    }

    public int insert(DiscountDTO d) throws SQLException {
        String sql = "INSERT INTO discounts (code, type, value, min_order_amt, start_time, end_time, occasion, is_active, created_by, is_stackable) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, d.getCode());
            ps.setString(2, d.getType());
            ps.setBigDecimal(3, d.getValue());
            ps.setBigDecimal(4, d.getMinOrderAmt());
            if (d.getStartTime() != null) ps.setTimestamp(5, Timestamp.valueOf(d.getStartTime()));
            else ps.setNull(5, Types.TIMESTAMP);
            if (d.getEndTime() != null) ps.setTimestamp(6, Timestamp.valueOf(d.getEndTime()));
            else ps.setNull(6, Types.TIMESTAMP);
            ps.setString(7, d.getOccasion() != null ? d.getOccasion() : "MANUAL");
            ps.setBoolean(8, d.isActive());
            ps.setInt(9, d.getCreatedBy());
            ps.setBoolean(10, d.isStackable());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    public void update(DiscountDTO d) throws SQLException {
        String sql = "UPDATE discounts SET code=?, type=?, value=?, min_order_amt=?, start_time=?, end_time=?, " +
                     "occasion=?, is_active=?, is_stackable=? WHERE discount_id=?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, d.getCode());
            ps.setString(2, d.getType());
            ps.setBigDecimal(3, d.getValue());
            ps.setBigDecimal(4, d.getMinOrderAmt());
            if (d.getStartTime() != null) ps.setTimestamp(5, Timestamp.valueOf(d.getStartTime()));
            else ps.setNull(5, Types.TIMESTAMP);
            if (d.getEndTime() != null) ps.setTimestamp(6, Timestamp.valueOf(d.getEndTime()));
            else ps.setNull(6, Types.TIMESTAMP);
            ps.setString(7, d.getOccasion());
            ps.setBoolean(8, d.isActive());
            ps.setBoolean(9, d.isStackable());
            ps.setInt(10, d.getDiscountId());
            ps.executeUpdate();
        }
    }

    public void deactivate(int discountId) throws SQLException {
        String sql = "UPDATE discounts SET is_active = 0 WHERE discount_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, discountId);
            ps.executeUpdate();
        }
    }

    private DiscountDTO mapResultSet(ResultSet rs) throws SQLException {
        DiscountDTO d = new DiscountDTO();
        d.setDiscountId(rs.getInt("discount_id"));
        d.setCode(rs.getString("code"));
        d.setType(rs.getString("type"));
        d.setValue(rs.getBigDecimal("value"));
        d.setMinOrderAmt(rs.getBigDecimal("min_order_amt"));
        Timestamp st = rs.getTimestamp("start_time");
        if (st != null) d.setStartTime(st.toLocalDateTime());
        Timestamp et = rs.getTimestamp("end_time");
        if (et != null) d.setEndTime(et.toLocalDateTime());
        d.setOccasion(rs.getString("occasion"));
        d.setActive(rs.getBoolean("is_active"));
        d.setCreatedBy(rs.getInt("created_by"));
        try { d.setStackable(rs.getBoolean("is_stackable")); } catch (SQLException ignored) {}
        return d;
    }

    public void addStackableColumn() {
        String sql = "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('discounts') AND name = 'is_stackable') " +
                     "ALTER TABLE discounts ADD is_stackable BIT DEFAULT 0;";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Lỗi thêm cột is_stackable: " + e.getMessage());
        }
    }
}
