package com.handbagstore.dto;

import java.time.LocalDateTime;

/**
 * DTO cho bảng accounts.
 * Chứa thông tin tài khoản đăng nhập và phân quyền.
 */
public class AccountDTO {
    private int accountId;
    private String username;
    private String passwordHash;
    private String fullName;
    private String role; // ADMIN, STAFF
    private boolean isActive;
    private boolean mustChangePassword;
    private LocalDateTime createdAt;

    public AccountDTO() {
    }

    public AccountDTO(int accountId, String username, String passwordHash,
            String fullName, String role, boolean isActive) {
        this.accountId = accountId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.isActive = isActive;
    }

    // --- Getters & Setters ---

    public int getAccountId() {
        return accountId;
    }

    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public boolean isStaff() {
        return "STAFF".equals(role);
    }

    @Override
    public String toString() {
        return fullName + " (" + role + ")";
    }
}
