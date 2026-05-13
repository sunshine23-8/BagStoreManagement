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
        if (a == null || b == null)
            return false;
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
        if (dateStr == null || dateStr.trim().isEmpty())
            return null;
        dateStr = dateStr.trim();

        // Try standard format first
        try {
            return LocalDate.parse(dateStr, DATE_FMT);
        } catch (Exception e) {
            // Fall back to smart parsing for digits only
        }

        String digits = dateStr.replaceAll("[^0-9]", "");

        if (digits.length() == 8) { // ddMMyyyy
            int day = Integer.parseInt(digits.substring(0, 2));
            int month = Integer.parseInt(digits.substring(2, 4));
            int year = Integer.parseInt(digits.substring(4, 8));
            return tryCreateDate(year, month, day);
        } else if (digits.length() == 6) { // dMyyyy
            int day = Integer.parseInt(digits.substring(0, 1));
            int month = Integer.parseInt(digits.substring(1, 2));
            int year = Integer.parseInt(digits.substring(2, 6));
            return tryCreateDate(year, month, day);
        } else if (digits.length() == 7) { // dMMyyyy or ddMyyyy
            // Try dMMyyyy first (e.g. 1/12/2006 -> 1122006)
            int day1 = Integer.parseInt(digits.substring(0, 1));
            int month1 = Integer.parseInt(digits.substring(1, 3));
            int year1 = Integer.parseInt(digits.substring(3, 7));
            LocalDate d1 = tryCreateDate(year1, month1, day1);

            // Try ddMyyyy (e.g. 11/2/2006 -> 1122006)
            int day2 = Integer.parseInt(digits.substring(0, 2));
            int month2 = Integer.parseInt(digits.substring(2, 3));
            int year2 = Integer.parseInt(digits.substring(3, 7));
            LocalDate d2 = tryCreateDate(year2, month2, day2);

            if (d1 != null && d2 != null) {
                return d1; // Default to dMMyyyy
            }
            return d1 != null ? d1 : d2;
        }

        return null;
    }

    private static LocalDate tryCreateDate(int year, int month, int day) {
        try {
            return LocalDate.of(year, month, day);
        } catch (Exception e) {
            return null;
        }
    }
}
