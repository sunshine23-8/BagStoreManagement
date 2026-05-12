package com.handbagstore.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO cho bảng products.
 * Chứa thông tin cơ bản của sản phẩm túi xách.
 */
public class ProductDTO {
    private int productId;
    private String productCode;
    private String name;
    private String brand;
    private BigDecimal price;
    private String style;       // Tote, Crossbody, Backpack, Clutch
    private String material;    // Da thật, PU, Canvas
    private String color;
    private String imagePath;
    private String status;      // ACTIVE, INACTIVE (soft delete)
    private LocalDateTime createdAt;

    public ProductDTO() {}

    public ProductDTO(int productId, String productCode, String name, String brand,
                      BigDecimal price, String style, String material, String color,
                      String imagePath, String status) {
        this.productId = productId;
        this.productCode = productCode;
        this.name = name;
        this.brand = brand;
        this.price = price;
        this.style = style;
        this.material = material;
        this.color = color;
        this.imagePath = imagePath;
        this.status = status;
    }

    // --- Getters & Setters ---

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }

    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return productCode + " - " + name + " (" + brand + ")";
    }
}
