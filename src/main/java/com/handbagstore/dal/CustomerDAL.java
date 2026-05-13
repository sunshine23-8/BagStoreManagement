package com.handbagstore.dal;

import com.handbagstore.dto.CustomerDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAL {
    private Connection getConnection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public CustomerDTO getByPhone(String phone) throws SQLException {
        String sql = "SELECT * FROM customers WHERE phone = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSet(rs);
            }
        }
        return null;
    }

    public CustomerDTO getById(int customerId) throws SQLException {
        String sql = "SELECT * FROM customers WHERE customer_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSet(rs);
            }
        }
        return null;
    }

    public List<CustomerDTO> getAll() throws SQLException {
        String sql = "SELECT * FROM customers ORDER BY customer_id ASC";
        List<CustomerDTO> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapResultSet(rs));
        }
        return list;
    }

    public List<CustomerDTO> search(String keyword) throws SQLException {
        String sql = "SELECT * FROM customers WHERE full_name LIKE ? OR phone LIKE ? ORDER BY customer_id ASC";
        String term = "%" + keyword + "%";
        List<CustomerDTO> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, term);
            ps.setString(2, term);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    public int insert(CustomerDTO c) throws SQLException {
        String sql = "INSERT INTO customers (full_name, phone, birthday) VALUES (?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getFullName());
            ps.setString(2, c.getPhone());
            if (c.getBirthday() != null) ps.setDate(3, Date.valueOf(c.getBirthday()));
            else ps.setNull(3, Types.DATE);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    public void update(CustomerDTO c) throws SQLException {
        String sql = "UPDATE customers SET full_name = ?, phone = ?, birthday = ? WHERE customer_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, c.getFullName());
            ps.setString(2, c.getPhone());
            if (c.getBirthday() != null) ps.setDate(3, Date.valueOf(c.getBirthday()));
            else ps.setNull(3, Types.DATE);
            ps.setInt(4, c.getCustomerId());
            ps.executeUpdate();
        }
    }

    private CustomerDTO mapResultSet(ResultSet rs) throws SQLException {
        CustomerDTO c = new CustomerDTO();
        c.setCustomerId(rs.getInt("customer_id"));
        c.setFullName(rs.getString("full_name"));
        c.setPhone(rs.getString("phone"));
        Date d = rs.getDate("birthday");
        if (d != null) c.setBirthday(d.toLocalDate());
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) c.setCreatedAt(ts.toLocalDateTime());
        return c;
    }
}
