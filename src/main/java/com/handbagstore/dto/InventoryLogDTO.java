package com.handbagstore.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO cho bảng inventory + thông tin sản phẩm (join).
 * Dùng để hiển thị tồn kho kèm thông tin sản phẩm.
 */
public class InventoryLogDTO {
    private int inventoryId;
    private int productId;
    private int quantity;           // Tổng số lượng trong kho
    private int reservedQty;        // Số lượng đang giữ chân (pending orders)
    private BigDecimal costPrice;   // Giá vốn
    private LocalDateTime updatedAt;

    // Transient fields - join data
    private String productCode;
    private String productName;
    private String brand;
    private BigDecimal sellPrice;   // Giá bán

    public InventoryLogDTO() {}

    // --- Getters & Setters ---

    public int getInventoryId() { return inventoryId; }
    public void setInventoryId(int inventoryId) { this.inventoryId = inventoryId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getReservedQty() { return reservedQty; }
    public void setReservedQty(int reservedQty) { this.reservedQty = reservedQty; }

    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public BigDecimal getSellPrice() { return sellPrice; }
    public void setSellPrice(BigDecimal sellPrice) { this.sellPrice = sellPrice; }

    /**
     * Số lượng khả dụng = tổng - đang giữ chân
     */
    public int getAvailableQty() {
        return quantity - reservedQty;
    }

    /**
     * Kiểm tra sản phẩm sắp hết hàng (≤ threshold)
     */
    public boolean isLowStock(int threshold) {
        return getAvailableQty() <= threshold;
    }

    @Override
    public String toString() {
        return productName + " - Khả dụng: " + getAvailableQty() + "/" + quantity;
    }
}
