package com.handbagstore.gui;

import com.handbagstore.bll.AccountBLL;
import com.handbagstore.dto.AccountDTO;
import com.handbagstore.gui.components.ChangePasswordDialog;

import javax.swing.*;
import java.awt.*;

/**
 * Màn hình đăng nhập.
 */
public class LoginFrame extends JFrame {
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JLabel lblError;
    private final AccountBLL accountBLL = new AccountBLL();

    public LoginFrame() {
        initComponents();
    }

    private void initComponents() {
        setTitle("Đăng nhập - Hệ thống Quản lý Túi Xách");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(450, 350);
        setLocationRelativeTo(null);
        setResizable(false);

        // Main panel
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        JLabel lblTitle = new JLabel("ĐĂNG NHẬP HỆ THỐNG", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        mainPanel.add(lblTitle, gbc);

        // Username
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        mainPanel.add(new JLabel("Tên đăng nhập:"), gbc);
        txtUsername = new JTextField(20);
        txtUsername.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 1;
        mainPanel.add(txtUsername, gbc);

        // Password
        gbc.gridx = 0;
        gbc.gridy = 2;
        mainPanel.add(new JLabel("Mật khẩu:"), gbc);
        txtPassword = new JPasswordField(20);
        txtPassword.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 1;
        mainPanel.add(txtPassword, gbc);

        // Error label
        lblError = new JLabel(" ", SwingConstants.CENTER);
        lblError.setForeground(new Color(255, 80, 80));
        lblError.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        mainPanel.add(lblError, gbc);

        // Login button
        btnLogin = new JButton("Đăng nhập");
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnLogin.setBackground(new Color(64, 133, 240));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogin.setPreferredSize(new Dimension(200, 40));
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add(btnLogin, gbc);

        add(mainPanel);

        // Events
        btnLogin.addActionListener(e -> handleLogin());
        txtPassword.addActionListener(e -> handleLogin()); // Enter key
        txtUsername.addActionListener(e -> txtPassword.requestFocusInWindow());
    }

    private void handleLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            lblError.setText("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        try {
            AccountDTO account = accountBLL.login(username, password);
            if (account == null) {
                lblError.setText("Sai tên đăng nhập hoặc mật khẩu!");
                txtPassword.setText("");
                return;
            }

            // Chuyển sang màn hình chính theo role
            if (account.isMustChangePassword()) {
                ChangePasswordDialog dialog = new ChangePasswordDialog(this);
                dialog.setVisible(true);
                if (!dialog.isSuccess()) {
                    AccountBLL.setCurrentUser(null); // Reset session if cancelled
                    return;
                }
            }

            dispose();
            if (account.isAdmin()) {
                new MainAdminFrame().setVisible(true);
            } else {
                new MainStaffFrame().setVisible(true);
            }
        } catch (RuntimeException ex) {
            lblError.setText(ex.getMessage());
        } catch (Exception ex) {
            lblError.setText("Lỗi kết nối: " + ex.getMessage());
        }
    }
}
