package com.handbagstore.dal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.handbagstore.dto.AccountDTO;

/**
 * Data Access Layer cho bảng accounts.
 * CRUD tài khoản, xác thực, quản lý staff.
 */
public class AccountDAL {

    private Connection getConnection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    /**
     * Tìm tài khoản theo username (dùng cho đăng nhập).
     */
    public AccountDTO getByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM accounts WHERE username = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }
        return null;
    }

    /**
     * Lấy tài khoản theo ID.
     */
    public AccountDTO getById(int accountId) throws SQLException {
        String sql = "SELECT * FROM accounts WHERE account_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }
        return null;
    }

    /**
     * Lấy danh sách tất cả tài khoản Staff.
     */
    public List<AccountDTO> getAllStaff() throws SQLException {
        String sql = "SELECT * FROM accounts WHERE role = 'STAFF' ORDER BY created_at DESC";
        List<AccountDTO> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    /**
     * Tạo tài khoản mới.
     */
    public int insert(AccountDTO account) throws SQLException {
        String sql = "INSERT INTO accounts (username, password_hash, full_name, role, is_active) " +
                     "VALUES (?, ?, ?, ?, ?); SELECT SCOPE_IDENTITY();";
        try (PreparedStatement ps = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, account.getUsername());
            ps.setString(2, account.getPasswordHash());
            ps.setString(3, account.getFullName());
            ps.setString(4, account.getRole());
            ps.setBoolean(5, account.isActive());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    /**
     * Reset mật khẩu.
     */
    public void resetPassword(int accountId, String newPasswordHash) throws SQLException {
        String sql = "UPDATE accounts SET password_hash = ? WHERE account_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, newPasswordHash);
            ps.setInt(2, accountId);
            ps.executeUpdate();
        }
    }

    /**
     * Khóa / mở khóa tài khoản.
     */
    public void toggleActive(int accountId, boolean active) throws SQLException {
        String sql = "UPDATE accounts SET is_active = ? WHERE account_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setInt(2, accountId);
            ps.executeUpdate();
        }
    }

    /**
     * Map ResultSet → AccountDTO.
     */
    private AccountDTO mapResultSet(ResultSet rs) throws SQLException {
        AccountDTO account = new AccountDTO();
        account.setAccountId(rs.getInt("account_id"));
        account.setUsername(rs.getString("username"));
        account.setPasswordHash(rs.getString("password_hash"));
        account.setFullName(rs.getString("full_name"));
        account.setRole(rs.getString("role"));
        account.setActive(rs.getBoolean("is_active"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) account.setCreatedAt(ts.toLocalDateTime());
        return account;
    }
}
