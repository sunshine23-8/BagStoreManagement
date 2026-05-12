package com.handbagstore.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO cho bảng customers.
 * Chứa thông tin khách hàng, bao gồm ngày sinh để kiểm tra sinh nhật.
 */
public class CustomerDTO {
    private int customerId;
    private String fullName;
    private String phone;
    private LocalDate birthday;
    private LocalDateTime createdAt;

    // Transient field - không lưu DB, dùng cho business logic
    private boolean isBirthday;

    public CustomerDTO() {}

    public CustomerDTO(int customerId, String fullName, String phone, LocalDate birthday) {
        this.customerId = customerId;
        this.fullName = fullName;
        this.phone = phone;
        this.birthday = birthday;
    }

    // --- Getters & Setters ---

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public LocalDate getBirthday() { return birthday; }
    public void setBirthday(LocalDate birthday) { this.birthday = birthday; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isBirthday() { return isBirthday; }
    public void setBirthday(boolean birthday) { isBirthday = birthday; }

    @Override
    public String toString() {
        return fullName + " - " + phone;
    }
}
