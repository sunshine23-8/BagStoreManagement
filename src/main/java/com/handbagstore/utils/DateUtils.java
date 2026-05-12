package com.handbagstore.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Tiện ích xử lý ngày tháng.
 */
public class DateUtils {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /** Kiểm tra 2 ngày có cùng ngày-tháng không (dùng check sinh nhật) */
    public static boolean isSameMonthDay(LocalDate a, LocalDate b) {
        if (a == null || b == null) return false;
        return a.getMonthValue() == b.getMonthValue() && a.getDayOfMonth() == b.getDayOfMonth();
    }

    /** Kiểm tra ngày hôm nay có phải sinh nhật không */
    public static boolean isBirthdayToday(LocalDate birthday) {
        return isSameMonthDay(birthday, LocalDate.now());
    }

    public static String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FMT) : "";
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FMT) : "";
    }

    public static LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, DATE_FMT);
        } catch (Exception e) {
            return null;
        }
    }
}
