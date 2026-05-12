package com.handbagstore.dal;

import com.handbagstore.dto.InvoiceDetailDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InvoiceDetailDAL {
    private Connection getConnection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public void insertBatch(List<InvoiceDetailDTO> details) throws SQLException {
        String sql = "INSERT INTO invoice_details (invoice_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            for (InvoiceDetailDTO d : details) {
                ps.setInt(1, d.getInvoiceId());
                ps.setInt(2, d.getProductId());
                ps.setInt(3, d.getQuantity());
                ps.setBigDecimal(4, d.getUnitPrice());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public List<InvoiceDetailDTO> getByInvoiceId(int invoiceId) throws SQLException {
        String sql = "SELECT id.*, p.product_code, p.name AS product_name " +
                     "FROM invoice_details id JOIN products p ON id.product_id = p.product_id " +
                     "WHERE id.invoice_id = ?";
        List<InvoiceDetailDTO> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    InvoiceDetailDTO d = new InvoiceDetailDTO();
                    d.setDetailId(rs.getInt("detail_id"));
                    d.setInvoiceId(rs.getInt("invoice_id"));
                    d.setProductId(rs.getInt("product_id"));
                    d.setQuantity(rs.getInt("quantity"));
                    d.setUnitPrice(rs.getBigDecimal("unit_price"));
                    d.setProductCode(rs.getString("product_code"));
                    d.setProductName(rs.getString("product_name"));
                    list.add(d);
                }
            }
        }
        return list;
    }
}
