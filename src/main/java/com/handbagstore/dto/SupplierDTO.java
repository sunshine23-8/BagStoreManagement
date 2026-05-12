package com.handbagstore.dto;

/**
 * DTO cho bảng suppliers.
 * Chứa thông tin nhà cung cấp.
 */
public class SupplierDTO {
    private int supplierId;
    private String name;
    private String phone;
    private String address;

    public SupplierDTO() {}

    public SupplierDTO(int supplierId, String name, String phone, String address) {
        this.supplierId = supplierId;
        this.name = name;
        this.phone = phone;
        this.address = address;
    }

    // --- Getters & Setters ---

    public int getSupplierId() { return supplierId; }
    public void setSupplierId(int supplierId) { this.supplierId = supplierId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    @Override
    public String toString() {
        return name;
    }
}
