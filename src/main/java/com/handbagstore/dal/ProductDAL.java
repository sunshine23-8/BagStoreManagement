package com.handbagstore.dal;

import com.handbagstore.dto.ProductDTO;
import com.handbagstore.utils.StringUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Data Access Layer cho bảng products.
 * CRUD sản phẩm, tìm kiếm, soft delete.
 */
public class ProductDAL {

    private Connection getConnection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    /**
     * Lấy tất cả sản phẩm. Nếu includeInactive = false, chỉ lấy ACTIVE.
     */
    public List<ProductDTO> getAll(boolean includeInactive) throws SQLException {
        String sql = includeInactive
                ? "SELECT * FROM products ORDER BY product_code ASC"
                : "SELECT * FROM products WHERE status = 'ACTIVE' ORDER BY product_code ASC";
        List<ProductDTO> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    /**
     * Tìm sản phẩm theo mã sản phẩm (product_code).
     */
    public ProductDTO getByCode(String productCode) throws SQLException {
        String sql = "SELECT * FROM products WHERE product_code = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, productCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }
        return null;
    }

    /**
     * Tìm sản phẩm theo ID.
     */
    public ProductDTO getById(int productId) throws SQLException {
        String sql = "SELECT * FROM products WHERE product_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }
        return null;
    }

    /**
     * Tìm kiếm sản phẩm theo từ khóa (tên, mã, thương hiệu...).
     * Nếu includeInactive = false, chỉ tìm trong các sản phẩm ACTIVE.
     */
    public List<ProductDTO> search(String keyword, boolean includeInactive) throws SQLException {
        List<ProductDTO> list = getAll(includeInactive);
        if (keyword == null || keyword.trim().isEmpty()) return list;

        return list.stream()
                .filter(p -> StringUtils.containsIgnoreCase(p.getProductCode(), keyword) ||
                        StringUtils.containsIgnoreCase(p.getName(), keyword) ||
                        StringUtils.containsIgnoreCase(p.getBrand(), keyword) ||
                        StringUtils.containsIgnoreCase(p.getStyle(), keyword) ||
                        StringUtils.containsIgnoreCase(p.getMaterial(), keyword) ||
                        StringUtils.containsIgnoreCase(p.getColor(), keyword))
                .collect(Collectors.toList());
    }

    /**
     * Thêm sản phẩm mới.
     */
    public int insert(ProductDTO product) throws SQLException {
        String sql = "INSERT INTO products (product_code, name, brand, price, style, material, color, image_path, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, product.getProductCode());
            ps.setString(2, product.getName());
            ps.setString(3, product.getBrand());
            ps.setBigDecimal(4, product.getPrice());
            ps.setString(5, product.getStyle());
            ps.setString(6, product.getMaterial());
            ps.setString(7, product.getColor());
            ps.setString(8, product.getImagePath());
            ps.setString(9, product.getStatus() != null ? product.getStatus() : "ACTIVE");
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    /**
     * Cập nhật thông tin sản phẩm.
     */
    public void update(ProductDTO product) throws SQLException {
        String sql = "UPDATE products SET name = ?, brand = ?, price = ?, style = ?, " +
                     "material = ?, color = ?, image_path = ?, status = ? WHERE product_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, product.getName());
            ps.setString(2, product.getBrand());
            ps.setBigDecimal(3, product.getPrice());
            ps.setString(4, product.getStyle());
            ps.setString(5, product.getMaterial());
            ps.setString(6, product.getColor());
            ps.setString(7, product.getImagePath());
            ps.setString(8, product.getStatus());
            ps.setInt(9, product.getProductId());
            ps.executeUpdate();
        }
    }

    /**
     * Soft delete — chuyển status sang INACTIVE.
     */
    public void softDelete(int productId) throws SQLException {
        String sql = "UPDATE products SET status = 'INACTIVE' WHERE product_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.executeUpdate();
        }
    }

    /**
     * Khôi phục sản phẩm — chuyển status sang ACTIVE.
     */
    public void restore(int productId) throws SQLException {
        String sql = "UPDATE products SET status = 'ACTIVE' WHERE product_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.executeUpdate();
        }
    }

    /**
     * Lấy sản phẩm gợi ý upsell: giá >= delta, còn hàng, đang kinh doanh.
     * Sắp xếp giá tăng dần.
     */
    public List<ProductDTO> getUpsellSuggestions(long deltaAmount) throws SQLException {
        String sql = "SELECT p.* FROM products p " +
                     "WHERE p.price >= ? AND p.status = 'ACTIVE' " +
                     "AND EXISTS (SELECT 1 FROM inventory i WHERE i.product_id = p.product_id AND (i.quantity - i.reserved_qty) > 0) " +
                     "ORDER BY p.price ASC";
        List<ProductDTO> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, deltaAmount);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    public String getNextProductCode() throws SQLException {
        String sql = "SELECT MAX(product_code) FROM products WHERE product_code LIKE 'TX%'";
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                String maxCode = rs.getString(1);
                if (maxCode != null && maxCode.length() > 2) {
                    try {
                        int num = Integer.parseInt(maxCode.substring(2)) + 1;
                        return String.format("TX%03d", num);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return "TX001";
    }

    /**
     * Map ResultSet → ProductDTO.
     */
    private ProductDTO mapResultSet(ResultSet rs) throws SQLException {
        ProductDTO product = new ProductDTO();
        product.setProductId(rs.getInt("product_id"));
        product.setProductCode(rs.getString("product_code"));
        product.setName(rs.getString("name"));
        product.setBrand(rs.getString("brand"));
        product.setPrice(rs.getBigDecimal("price"));
        product.setStyle(rs.getString("style"));
        product.setMaterial(rs.getString("material"));
        product.setColor(rs.getString("color"));
        product.setImagePath(rs.getString("image_path"));
        product.setStatus(rs.getString("status"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) product.setCreatedAt(ts.toLocalDateTime());
        return product;
    }
}
