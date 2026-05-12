package com.handbagstore.dto;

import java.math.BigDecimal;

/**
 * DTO cho bảng invoice_details.
 * Chứa chi tiết từng mặt hàng trong hóa đơn.
 */
public class InvoiceDetailDTO {
    private int detailId;
    private int invoiceId;
    private int productId;
    private int quantity;
    private BigDecimal unitPrice;

    // Transient fields - join data hiển thị UI
    private String productCode;
    private String productName;

    public InvoiceDetailDTO() {}

    public InvoiceDetailDTO(int invoiceId, int productId, int quantity, BigDecimal unitPrice) {
        this.invoiceId = invoiceId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    // --- Getters & Setters ---

    public int getDetailId() { return detailId; }
    public void setDetailId(int detailId) { this.detailId = detailId; }

    public int getInvoiceId() { return invoiceId; }
    public void setInvoiceId(int invoiceId) { this.invoiceId = invoiceId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    /**
     * Tính thành tiền = đơn giá × số lượng
     */
    public BigDecimal getLineTotal() {
        if (unitPrice == null) return BigDecimal.ZERO;
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    @Override
    public String toString() {
        return productName + " x" + quantity + " = " + getLineTotal() + "đ";
    }
}
