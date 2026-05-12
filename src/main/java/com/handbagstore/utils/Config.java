package com.handbagstore.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Đọc cấu hình từ application.properties.
 * Singleton pattern — chỉ load 1 lần.
 */
public class Config {
    private static Config instance;
    private final Properties properties;

    private Config() {
        properties = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) {
                properties.load(is);
            } else {
                System.err.println("Không tìm thấy file application.properties!");
            }
        } catch (IOException e) {
            System.err.println("Lỗi đọc application.properties: " + e.getMessage());
        }
    }

    public static synchronized Config getInstance() {
        if (instance == null) {
            instance = new Config();
        }
        return instance;
    }

    public String get(String key) {
        return properties.getProperty(key);
    }

    public String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(properties.getProperty(key));
        } catch (NumberFormatException | NullPointerException e) {
            return defaultValue;
        }
    }

    // --- Convenience methods ---

    public String getDbUrl() {
        return get("db.url");
    }

    public String getDbUsername() {
        return get("db.username");
    }

    public String getDbPassword() {
        return get("db.password");
    }

    public int getPendingTimeoutMinutes() {
        return getInt("app.pending.timeout.minutes", 5);
    }

    public int getLowStockThreshold() {
        return getInt("app.inventory.low.stock.threshold", 3);
    }
}
