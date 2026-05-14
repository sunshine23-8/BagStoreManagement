package com.handbagstore;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.handbagstore.gui.LoginFrame;

/**
 * Entry point chính của ứng dụng.
 * Khởi tạo FlatLaf theme và hiển thị LoginFrame.
 */
public class Main {
    public static void main(String[] args) {
        // Cài đặt FlatLaf Look and Feel
        try {
            java.util.Properties props = new java.util.Properties();
            try (java.io.FileInputStream in = new java.io.FileInputStream("app.properties")) {
                props.load(in);
            } catch (java.io.IOException ignored) {
                // File might not exist yet, default to dark
            }

            String theme = props.getProperty("theme", "dark");
            if ("light".equals(theme)) {
                UIManager.setLookAndFeel(new FlatIntelliJLaf());
            } else {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            }
        } catch (Exception admadade) {
            System.err.println("Không thể cài đặt FlatLaf, dùng theme mặc định.");
        }

        // Khởi chạy GUI trên Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}
