package com.handbagstore.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO cho bảng discounts.
 * Chứa thông tin mã giảm giá: loại giảm, giá trị, điều kiện áp dụng.
 */
public class DiscountDTO {
    private int discountId;
    private String code;
    private String type;            // PERCENT, AMOUNT
    private BigDecimal value;       // phần trăm (VD: 10) hoặc số tiền (VD: 50000)
    private BigDecimal minOrderAmt; // số tiền tối thiểu để áp dụng
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String occasion;        // BIRTHDAY, SPECIAL, MANUAL
    private boolean isActive;
    private int createdBy;          // account_id của admin tạo
    private boolean isStackable;

    public DiscountDTO() {}

    public DiscountDTO(int discountId, String code, String type, BigDecimal value,
                       BigDecimal minOrderAmt, LocalDateTime startTime, LocalDateTime endTime,
                       String occasion, boolean isActive) {
        this.discountId = discountId;
        this.code = code;
        this.type = type;
        this.value = value;
        this.minOrderAmt = minOrderAmt;
        this.startTime = startTime;
        this.endTime = endTime;
        this.occasion = occasion;
        this.isActive = isActive;
    }

    // --- Getters & Setters ---

    public int getDiscountId() { return discountId; }
    public void setDiscountId(int discountId) { this.discountId = discountId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }

    public BigDecimal getMinOrderAmt() { return minOrderAmt; }
    public void setMinOrderAmt(BigDecimal minOrderAmt) { this.minOrderAmt = minOrderAmt; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getOccasion() { return occasion; }
    public void setOccasion(String occasion) { this.occasion = occasion; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public int getCreatedBy() { return createdBy; }
    public void setCreatedBy(int createdBy) { this.createdBy = createdBy; }

    public boolean isStackable() { return isStackable; }
    public void setStackable(boolean stackable) { isStackable = stackable; }

    public boolean isPercentType() { return "PERCENT".equals(type); }
    public boolean isAmountType() { return "AMOUNT".equals(type); }

    @Override
    public String toString() {
        return code + " (" + (isPercentType() ? value + "%" : value + "đ") + ")";
    }
}
