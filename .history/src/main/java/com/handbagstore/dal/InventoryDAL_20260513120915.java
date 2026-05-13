package com.handbagstore.dal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.handbagstore.dto.ImportBatchDTO;
import com.handbagstore.dto.InventoryLogDTO;

public class InventoryDAL {
    private Connection getConnection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public InventoryLogDTO getByProductId(int productId) throws SQLException {
        String sql = "SELECT i.*, p.product_code, p.name AS product_name, p.brand, p.price AS sell_price " +
                     "FROM inventory i JOIN products p ON i.product_id = p.product_id WHERE i.product_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSet(rs);
            }
        }
        return null;
    }

    public int getAvailableQuantity(int productId) throws SQLException {
        String sql = "SELECT (quantity - reserved_qty) AS available FROM inventory WHERE product_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("available");
            }
        }
        return 0;
    }

    public List<InventoryLogDTO> getAll() throws SQLException {
        String sql = "SELECT i.*, p.product_code, p.name AS product_name, p.brand, p.price AS sell_price " +
                     "FROM inventory i JOIN products p ON i.product_id = p.product_id " +
                     "WHERE p.status = 'ACTIVE' ORDER BY p.product_code ASC";
        List<InventoryLogDTO> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapResultSet(rs));
        }
        return list;
    }

    public List<InventoryLogDTO> getLowStockItems(int threshold) throws SQLException {
        String sql = "SELECT i.*, p.product_code, p.name AS product_name, p.brand, p.price AS sell_price " +
                     "FROM inventory i JOIN products p ON i.product_id = p.product_id " +
                     "WHERE (i.quantity - i.reserved_qty) <= ? AND p.status = 'ACTIVE' " +
                     "ORDER BY (i.quantity - i.reserved_qty) ASC";
        List<InventoryLogDTO> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, threshold);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    public void updateQuantity(int productId, int delta) throws SQLException {
        String sql = "UPDATE inventory SET quantity = quantity + ?, updated_at = GETDATE() WHERE product_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, delta); ps.setInt(2, productId); ps.executeUpdate();
        }
    }

    public void reserveQuantity(int productId, int qty) throws SQLException {
        String sql = "UPDATE inventory SET reserved_qty = reserved_qty + ?, updated_at = GETDATE() WHERE product_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, qty); ps.setInt(2, productId); ps.executeUpdate();
        }
    }

    public void releaseReserve(int productId, int qty) throws SQLException {
        String sql = "UPDATE inventory SET reserved_qty = CASE WHEN reserved_qty >= ? THEN reserved_qty - ? ELSE 0 END, " +
                     "updated_at = GETDATE() WHERE product_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, qty); ps.setInt(2, qty); ps.setInt(3, productId); ps.executeUpdate();
        }
    }

    public void confirmSold(int productId, int qty) throws SQLException {
        String sql = "UPDATE inventory SET quantity = quantity - ?, " +
                     "reserved_qty = CASE WHEN reserved_qty >= ? THEN reserved_qty - ? ELSE 0 END, " +
                     "updated_at = GETDATE() WHERE product_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, qty); ps.setInt(2, qty); ps.setInt(3, qty); ps.setInt(4, productId); ps.executeUpdate();
        }
    }

    public void createInventory(int productId) throws SQLException {
        String sql = "INSERT INTO inventory (product_id, quantity, reserved_qty, cost_price) VALUES (?, 0, 0, 0)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, productId); ps.executeUpdate();
        }
    }

    public void insertImportBatch(ImportBatchDTO batch) throws SQLException {
        Connection conn = getConnection();
        conn.setAutoCommit(false);
        try {
            String sqlBatch = "INSERT INTO import_batches (product_id, supplier_id, quantity, cost_price, import_date, note, created_by) " +
                              "VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sqlBatch)) {
                ps.setInt(1, batch.getProductId());
                if (batch.getSupplierId() != null) ps.setInt(2, batch.getSupplierId());
                else ps.setNull(2, Types.INTEGER);
                ps.setInt(3, batch.getQuantity());
                ps.setBigDecimal(4, batch.getCostPrice());
                ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
                ps.setString(6, batch.getNote());
                ps.setInt(7, batch.getCreatedBy());
                ps.executeUpdate();
            }
            String sqlInv = "UPDATE inventory SET quantity = quantity + ?, cost_price = ?, updated_at = GETDATE() WHERE product_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlInv)) {
                ps.setInt(1, batch.getQuantity());
                ps.setBigDecimal(2, batch.getCostPrice());
                ps.setInt(3, batch.getProductId());
                int affected = ps.executeUpdate();
                if (affected == 0) { createInventory(batch.getProductId()); ps.executeUpdate(); }
            }
            conn.commit();
        } catch (SQLException e) { conn.rollback(); throw e;
        } finally { conn.setAutoCommit(true); }
    }

    public List<ImportBatchDTO> getImportHistory() throws SQLException {
        String sql = "SELECT ib.*, p.name AS product_name, s.name AS supplier_name, a.full_name AS created_by_name " +
                     "FROM import_batches ib LEFT JOIN products p ON ib.product_id = p.product_id " +
                     "LEFT JOIN suppliers s ON ib.supplier_id = s.supplier_id " +
                     "LEFT JOIN accounts a ON ib.created_by = a.account_id ORDER BY ib.import_date DESC";
        List<ImportBatchDTO> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ImportBatchDTO b = new ImportBatchDTO();
                b.setBatchId(rs.getInt("batch_id")); b.setProductId(rs.getInt("product_id"));
                b.setSupplierId(rs.getObject("supplier_id") != null ? rs.getInt("supplier_id") : null);
                b.setQuantity(rs.getInt("quantity")); b.setCostPrice(rs.getBigDecimal("cost_price"));
                Timestamp ts = rs.getTimestamp("import_date");
                if (ts != null) b.setImportDate(ts.toLocalDateTime());
                b.setNote(rs.getString("note")); b.setCreatedBy(rs.getInt("created_by"));
                b.setProductName(rs.getString("product_name"));
                b.setSupplierName(rs.getString("supplier_name"));
                b.setCreatedByName(rs.getString("created_by_name"));
                list.add(b);
            }
        }
        return list;
    }

    private InventoryLogDTO mapResultSet(ResultSet rs) throws SQLException {
        InventoryLogDTO inv = new InventoryLogDTO();
        inv.setInventoryId(rs.getInt("inventory_id")); inv.setProductId(rs.getInt("product_id"));
        inv.setQuantity(rs.getInt("quantity")); inv.setReservedQty(rs.getInt("reserved_qty"));
        inv.setCostPrice(rs.getBigDecimal("cost_price"));
        Timestamp ts = rs.getTimestamp("updated_at");
        if (ts != null) inv.setUpdatedAt(ts.toLocalDateTime());
        inv.setProductCode(rs.getString("product_code")); inv.setProductName(rs.getString("product_name"));
        inv.setBrand(rs.getString("brand")); inv.setSellPrice(rs.getBigDecimal("sell_price"));
        return inv;
    }
}
