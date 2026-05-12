package com.handbagstore.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO cho bảng invoices.
 * Chứa thông tin hóa đơn bao gồm trạng thái PENDING/PAID/CANCELLED.
 */
public class InvoiceDTO {
    private int invoiceId;
    private String invoiceCode;
    private Integer customerId;     // null = khách vãng lai (guest)
    private int staffId;
    private Integer discountId;     // null = không dùng giảm giá
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal total;
    private String paymentMethod;   // CASH, TRANSFER
    private BigDecimal paymentReceived; // Số tiền khách đưa (tiền mặt)
    private BigDecimal changeAmount;   // Tiền thối lại
    private String status;          // PENDING, PAID, CANCELLED
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime expiresAt;    // Thời điểm pending hết hạn

    // Transient fields - join data, không lưu trực tiếp trong bảng invoices
    private String customerName;
    private String customerPhone;
    private String staffName;
    private String discountCode;

    public InvoiceDTO() {}

    // --- Getters & Setters ---

    public int getInvoiceId() { return invoiceId; }
    public void setInvoiceId(int invoiceId) { this.invoiceId = invoiceId; }

    public String getInvoiceCode() { return invoiceCode; }
    public void setInvoiceCode(String invoiceCode) { this.invoiceCode = invoiceCode; }

    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }

    public int getStaffId() { return staffId; }
    public void setStaffId(int staffId) { this.staffId = staffId; }

    public Integer getDiscountId() { return discountId; }
    public void setDiscountId(Integer discountId) { this.discountId = discountId; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public BigDecimal getPaymentReceived() { return paymentReceived; }
    public void setPaymentReceived(BigDecimal paymentReceived) { this.paymentReceived = paymentReceived; }

    public BigDecimal getChangeAmount() { return changeAmount; }
    public void setChangeAmount(BigDecimal changeAmount) { this.changeAmount = changeAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    // Transient getters/setters

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }

    public String getDiscountCode() { return discountCode; }
    public void setDiscountCode(String discountCode) { this.discountCode = discountCode; }

    public boolean isPending() { return "PENDING".equals(status); }
    public boolean isPaid() { return "PAID".equals(status); }
    public boolean isCancelled() { return "CANCELLED".equals(status); }
    public boolean isGuest() { return customerId == null; }

    @Override
    public String toString() {
        return invoiceCode + " - " + status + " (" + total + "đ)";
    }
}
