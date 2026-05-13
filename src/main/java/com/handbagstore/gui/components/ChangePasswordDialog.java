package com.handbagstore.gui.components;

import com.handbagstore.bll.AccountBLL;
import com.handbagstore.dto.AccountDTO;

import javax.swing.*;
import java.awt.*;

public class ChangePasswordDialog extends JDialog {
    private JPasswordField txtOldPass, txtNewPass, txtConfirmPass;
    private final AccountBLL accountBLL = new AccountBLL();
    private boolean success = false;

    public ChangePasswordDialog(JFrame parent) {
        super(parent, "Đổi mật khẩu bắt buộc", true);
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(15, 15));
        setSize(400, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE); // Bắt buộc đổi

        JPanel content = new JPanel(new GridBagLayout());
        content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblMsg = new JLabel(
                "<html><body style='width: 250px'>Chào mừng bạn! Đây là lần đầu bạn đăng nhập. Vui lòng đổi mật khẩu mới để tiếp tục.</body></html>");
        lblMsg.setFont(new Font("Segoe UI", Font.BOLD, 13));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        content.add(lblMsg, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.gridx = 0;
        content.add(new JLabel("Mật khẩu hiện tại (mặc định):"), gbc);
        txtOldPass = new JPasswordField();
        gbc.gridx = 1;
        content.add(txtOldPass, gbc);

        gbc.gridy = 2;
        gbc.gridx = 0;
        content.add(new JLabel("Mật khẩu mới:"), gbc);
        txtNewPass = new JPasswordField();
        gbc.gridx = 1;
        content.add(txtNewPass, gbc);

        gbc.gridy = 3;
        gbc.gridx = 0;
        content.add(new JLabel("Xác nhận mật khẩu mới:"), gbc);
        txtConfirmPass = new JPasswordField();
        gbc.gridx = 1;
        content.add(txtConfirmPass, gbc);

        JButton btnSubmit = new JButton("Cập nhật mật khẩu");
        btnSubmit.setBackground(new Color(40, 167, 69));
        btnSubmit.setForeground(Color.WHITE);
        btnSubmit.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSubmit.addActionListener(e -> handleSubmit());
        gbc.gridy = 4;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 5, 5, 5);
        content.add(btnSubmit, gbc);

        add(content, BorderLayout.CENTER);
    }

    private void handleSubmit() {
        String oldP = new String(txtOldPass.getPassword());
        String newP = new String(txtNewPass.getPassword());
        String confP = new String(txtConfirmPass.getPassword());

        try {
            accountBLL.changePassword(oldP, newP, confP);
            JOptionPane.showMessageDialog(this, "Đổi mật khẩu thành công! Chào mừng bạn đến với hệ thống.");
            success = true;
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Thông báo", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isSuccess() {
        return success;
    }
}
