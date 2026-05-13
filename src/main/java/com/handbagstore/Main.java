package com.handbagstore;

import com.formdev.flatlaf.FlatDarkLaf;
import com.handbagstore.gui.LoginFrame;
import com.handbagstore.gui.MainAdminFrame;

import javax.swing.*;

/**
 * Entry point chính của ứng dụng.
 * Khởi tạo FlatLaf theme và hiển thị LoginFrame.
 */
public class Main {
    public static void main(String[] args) {
        // Cài đặt FlatLaf Look and Feel
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (UnsupportedLookAndFeelException e) {
            System.err.println("Không thể cài đặt FlatLaf, dùng theme mặc định.");
        }

        // Khởi chạy GUI trên Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });

        // SwingUtilities.invokeLater(() -> {
        // MainAdminFrame mainAdminFrame = new MainAdminFrame();
        // mainAdminFrame.setVisible(true);
        // });
    }
}
