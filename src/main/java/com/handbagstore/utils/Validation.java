package com.handbagstore.utils;

/**
 * Tiện ích validation dữ liệu đầu vào.
 */
public class Validation {

    /** Kiểm tra số điện thoại VN (10 số, bắt đầu 0) */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.isEmpty())
            return false;
        return phone.matches("^0\\d{9}$");
    }

    /** Kiểm tra chuỗi không rỗng */
    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /** Kiểm tra số dương */
    public static boolean isPositiveNumber(String value) {
        try {
            return Double.parseDouble(value) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** Kiểm tra số nguyên dương */
    public static boolean isPositiveInt(String value) {
        try {
            return Integer.parseInt(value) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** Kiểm tra username hợp lệ (chữ + số, 3-50 ký tự) */
    public static boolean isValidUsername(String username) {
        if (username == null)
            return false;
        return username.matches("^[a-zA-Z0-9_]{3,50}$");
    }

    /** Kiểm tra mật khẩu (tối thiểu 6 ký tự) */
    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    /** Kiểm tra mã sản phẩm (TX + số, VD: TX001) */
    public static boolean isValidProductCode(String code) {
        if (code == null)
            return false;
        return code.matches("^[A-Z]{2}\\d{3,}$");
    }
}
