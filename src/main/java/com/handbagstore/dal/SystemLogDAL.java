package com.handbagstore.dal;

import com.handbagstore.dto.SystemLogDTO;
import com.handbagstore.utils.StringUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SystemLogDAL {
    private Connection getConnection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public void insert(SystemLogDTO log) throws SQLException {
        String sql = "INSERT INTO system_logs (account_id, action, detail) VALUES (?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, log.getAccountId());
            ps.setString(2, log.getAction());
            ps.setString(3, log.getDetail());
            ps.executeUpdate();
        }
    }

    public List<SystemLogDTO> getAll() throws SQLException {
        String sql = "SELECT sl.*, a.full_name AS account_name FROM system_logs sl " +
                     "LEFT JOIN accounts a ON sl.account_id = a.account_id ORDER BY sl.created_at DESC";
        List<SystemLogDTO> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapResultSet(rs));
        }
        return list;
    }

    public List<SystemLogDTO> getByAccount(int accountId) throws SQLException {
        String sql = "SELECT sl.*, a.full_name AS account_name FROM system_logs sl " +
                     "LEFT JOIN accounts a ON sl.account_id = a.account_id " +
                     "WHERE sl.account_id = ? ORDER BY sl.created_at DESC";
        List<SystemLogDTO> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    public List<SystemLogDTO> search(String keyword, java.sql.Date from, java.sql.Date to) throws SQLException {
        List<SystemLogDTO> list = getAll(); // Basic fetch
        // Filter by date first if needed (to be efficient, but let's just do it in Java for simplicity if list is small)
        // Or keep the SQL for dates and filter keyword in Java.
        
        StringBuilder sql = new StringBuilder(
            "SELECT sl.*, a.full_name AS account_name FROM system_logs sl " +
            "LEFT JOIN accounts a ON sl.account_id = a.account_id WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (from != null) { sql.append("AND CAST(sl.created_at AS DATE) >= ? "); params.add(from); }
        if (to != null) { sql.append("AND CAST(sl.created_at AS DATE) <= ? "); params.add(to); }
        sql.append("ORDER BY sl.created_at DESC");

        List<SystemLogDTO> results = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(mapResultSet(rs));
            }
        }

        if (keyword == null || keyword.trim().isEmpty()) return results;

        return results.stream()
                .filter(log -> StringUtils.containsIgnoreCase(log.getAction(), keyword) ||
                               StringUtils.containsIgnoreCase(log.getDetail(), keyword) ||
                               StringUtils.containsIgnoreCase(log.getAccountName(), keyword))
                .collect(Collectors.toList());
    }

    private SystemLogDTO mapResultSet(ResultSet rs) throws SQLException {
        SystemLogDTO log = new SystemLogDTO();
        log.setLogId(rs.getInt("log_id"));
        log.setAccountId(rs.getInt("account_id"));
        log.setAction(rs.getString("action"));
        log.setDetail(rs.getString("detail"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) log.setCreatedAt(ts.toLocalDateTime());
        try { log.setAccountName(rs.getString("account_name")); } catch (SQLException ignored) {}
        return log;
    }
}
