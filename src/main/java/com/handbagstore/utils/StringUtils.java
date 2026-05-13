package com.handbagstore.utils;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Tiện ích xử lý chuỗi: xóa dấu Tiếng Việt, chuyển chữ thường.
 */
public class StringUtils {
    public static String removeAccents(String s) {
        if (s == null) return "";
        String normalized = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String temp = pattern.matcher(normalized).replaceAll("");
        return temp.replaceAll("đ", "d").replaceAll("Đ", "D").toLowerCase();
    }

    public static boolean containsIgnoreCase(String source, String keyword) {
        if (source == null || keyword == null) return false;
        return removeAccents(source).contains(removeAccents(keyword));
    }
}
