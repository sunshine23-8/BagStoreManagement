package com.handbagstore.dto;

import java.time.LocalDateTime;

/**
 * DTO cho bảng system_logs.
 * Ghi nhận mọi hành động quan trọng trong hệ thống.
 */
public class SystemLogDTO {
    private int logId;
    private int accountId;
    private String action;
    private String detail;
    private LocalDateTime createdAt;

    // Transient field - join data
    private String accountName;

    public SystemLogDTO() {}

    public SystemLogDTO(int accountId, String action, String detail) {
        this.accountId = accountId;
        this.action = action;
        this.detail = detail;
    }

    // --- Getters & Setters ---

    public int getLogId() { return logId; }
    public void setLogId(int logId) { this.logId = logId; }

    public int getAccountId() { return accountId; }
    public void setAccountId(int accountId) { this.accountId = accountId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    @Override
    public String toString() {
        return "[" + createdAt + "] " + action + " - " + detail;
    }
}
