package com.handbagstore.utils;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Tiện ích định dạng tiền tệ (Ví dụ: 1.500.000đ)
 */
public class CurrencyUtils {
    private static final NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));

    public static String format(BigDecimal amount) {
        if (amount == null) return "0đ";
        return formatter.format(amount) + "đ";
    }

    public static String format(double amount) {
        return format(BigDecimal.valueOf(amount));
    }

    public static String format(Object amount) {
        if (amount == null) return "0đ";
        if (amount instanceof BigDecimal) return format((BigDecimal) amount);
        if (amount instanceof Number) return format(((Number) amount).doubleValue());
        return amount.toString() + "đ";
    }

    public static BigDecimal parse(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty()) return BigDecimal.ZERO;
        String clean = amountStr.replace(".", "").replace("đ", "").trim();
        try {
            return new BigDecimal(clean);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
