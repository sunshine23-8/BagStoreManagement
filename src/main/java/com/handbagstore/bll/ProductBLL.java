package com.handbagstore.bll;

import com.handbagstore.dal.InventoryDAL;
import com.handbagstore.dal.ProductDAL;
import com.handbagstore.dal.SystemLogDAL;
import com.handbagstore.dto.ProductDTO;
import com.handbagstore.dto.SystemLogDTO;
import com.handbagstore.utils.Validation;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class ProductBLL {
    private final ProductDAL productDAL = new ProductDAL();
    private final InventoryDAL inventoryDAL = new InventoryDAL();
    private final SystemLogDAL logDAL = new SystemLogDAL();

    public List<ProductDTO> getAll(boolean includeInactive) throws SQLException {
        return productDAL.getAll(includeInactive);
    }

    public String getNextProductCode() throws SQLException {
        return productDAL.getNextProductCode();
    }

    public ProductDTO getByCode(String code) throws SQLException {
        return productDAL.getByCode(code);
    }

    public List<ProductDTO> search(String keyword) throws SQLException {
        return productDAL.search(keyword);
    }

    public int addProduct(ProductDTO product) throws SQLException {
        validate(product);
        if (productDAL.getByCode(product.getProductCode()) != null)
            throw new RuntimeException("Mã sản phẩm đã tồn tại!");

        int productId = productDAL.insert(product);
        // Tạo record inventory tương ứng
        inventoryDAL.createInventory(productId);

        if (AccountBLL.getCurrentUser() != null) {
            logDAL.insert(new SystemLogDTO(AccountBLL.getCurrentUser().getAccountId(),
                    "THÊM SẢN PHẨM", "Thêm: " + product.getProductCode() + " - " + product.getName()));
        }
        return productId;
    }

    public void updateProduct(ProductDTO product) throws SQLException {
        validate(product);
        productDAL.update(product);
        if (AccountBLL.getCurrentUser() != null) {
            logDAL.insert(new SystemLogDTO(AccountBLL.getCurrentUser().getAccountId(),
                    "CẬP NHẬT SẢN PHẨM", "Cập nhật: " + product.getProductCode()));
        }
    }

    public void softDelete(int productId) throws SQLException {
        productDAL.softDelete(productId);
        if (AccountBLL.getCurrentUser() != null) {
            logDAL.insert(new SystemLogDTO(AccountBLL.getCurrentUser().getAccountId(),
                    "NGỪNG KINH DOANH", "Product ID: " + productId));
        }
    }

    public void restore(int productId) throws SQLException {
        productDAL.restore(productId);
    }

    /** Lấy sản phẩm gợi ý upsell để đạt ngưỡng min_order_amt */
    public List<ProductDTO> getUpsellSuggestions(BigDecimal currentSubtotal, BigDecimal minOrderAmt) throws SQLException {
        long delta = minOrderAmt.subtract(currentSubtotal).longValue();
        if (delta <= 0) return List.of();
        return productDAL.getUpsellSuggestions(delta);
    }

    private void validate(ProductDTO p) {
        if (!Validation.isNotEmpty(p.getProductCode()))
            throw new RuntimeException("Mã sản phẩm không được để trống!");
        if (!Validation.isNotEmpty(p.getName()))
            throw new RuntimeException("Tên sản phẩm không được để trống!");
        if (p.getPrice() == null || p.getPrice().compareTo(BigDecimal.ZERO) <= 0)
            throw new RuntimeException("Đơn giá phải lớn hơn 0!");
    }
}
