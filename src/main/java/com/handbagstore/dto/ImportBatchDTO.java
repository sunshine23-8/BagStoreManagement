package com.handbagstore.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO cho bảng import_batches.
 * Chứa thông tin lô hàng nhập kho.
 */
public class ImportBatchDTO {
    private int batchId;
    private int productId;
    private Integer supplierId;
    private int quantity;
    private BigDecimal costPrice;
    private LocalDate importDate;
    private String note;
    private int createdBy;

    // Transient fields
    private String productName;
    private String supplierName;
    private String createdByName;

    public ImportBatchDTO() {}

    // --- Getters & Setters ---

    public int getBatchId() { return batchId; }
    public void setBatchId(int batchId) { this.batchId = batchId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public Integer getSupplierId() { return supplierId; }
    public void setSupplierId(Integer supplierId) { this.supplierId = supplierId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }

    public LocalDate getImportDate() { return importDate; }
    public void setImportDate(LocalDate importDate) { this.importDate = importDate; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public int getCreatedBy() { return createdBy; }
    public void setCreatedBy(int createdBy) { this.createdBy = createdBy; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }
}
