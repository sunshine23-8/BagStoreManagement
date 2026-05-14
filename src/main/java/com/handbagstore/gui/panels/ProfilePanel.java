package com.handbagstore.gui.panels;

import com.handbagstore.bll.AccountBLL;
import com.handbagstore.dto.AccountDTO;

import javax.swing.*;
import java.awt.*;

public class ProfilePanel extends JPanel {
    private JTextField txtUsername, txtFullName, txtRole, txtCreatedAt;
    private JPasswordField txtOldPass, txtNewPass, txtConfirmPass;
    private final AccountBLL accountBLL = new AccountBLL();

    public ProfilePanel() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(20, 20));
        setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JLabel lblIcon = new JLabel("👤");
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
        JLabel lblText = new JLabel("Thông tin cá nhân");
        lblText.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titlePanel.add(lblIcon);
        titlePanel.add(lblText);
        add(titlePanel, BorderLayout.NORTH);

        AccountDTO user = AccountBLL.getCurrentUser();
        boolean isStaff = user != null && user.isStaff();

        JPanel mainContent = new JPanel(new GridLayout(1, isStaff ? 1 : 2, 40, 0));

        // Left: User Info
        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder("Thông tin tài khoản"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // AccountDTO user = AccountBLL.getCurrentUser(); // Moved up

        addInfoField(infoPanel, "<html><nobr>Tên&nbsp;đăng&nbsp;nhập:</nobr></html>",
                txtUsername = new JTextField(user.getUsername()), 0, gbc);
        addInfoField(infoPanel, "<html><nobr>Họ&nbsp;và&nbsp;tên:</nobr></html>",
                txtFullName = new JTextField(user.getFullName()), 1, gbc);
        addInfoField(infoPanel, "<html><nobr>Vai&nbsp;trò:</nobr></html>", txtRole = new JTextField(user.getRole()), 2,
                gbc);
        addInfoField(infoPanel, "<html><nobr>Ngày&nbsp;tham&nbsp;gia:</nobr></html>",
                txtCreatedAt = new JTextField(user.getCreatedAt().toString().replace("T", " ")), 3, gbc);

        txtUsername.setEditable(false);
        txtFullName.setEditable(false);
        txtRole.setEditable(false);
        txtCreatedAt.setEditable(false);

        // Ngăn chặn hoàn toàn việc nhấp chuột/focus vào các ô này
        txtUsername.setFocusable(false);
        txtFullName.setFocusable(false);
        txtRole.setFocusable(false);
        txtCreatedAt.setFocusable(false);

        mainContent.add(infoPanel);

        // Right: Change Password
        JPanel passPanel = new JPanel(new GridBagLayout());
        passPanel.setBorder(BorderFactory.createTitledBorder("Đổi mật khẩu"));

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        passPanel.add(new JLabel("Mật khẩu cũ:"), gbc);
        txtOldPass = new JPasswordField(15);
        gbc.gridx = 1;
        passPanel.add(txtOldPass, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        passPanel.add(new JLabel("Mật khẩu mới:"), gbc);
        txtNewPass = new JPasswordField(15);
        gbc.gridx = 1;
        passPanel.add(txtNewPass, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        passPanel.add(new JLabel("Xác nhận mật khẩu:"), gbc);
        txtConfirmPass = new JPasswordField(15);
        gbc.gridx = 1;
        passPanel.add(txtConfirmPass, gbc);

        JButton btnChange = new JButton("Đổi mật khẩu");
        btnChange.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnChange.setBackground(new Color(13, 110, 253));
        btnChange.setForeground(Color.WHITE);
        btnChange.addActionListener(e -> handleChangePassword());
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        passPanel.add(btnChange, gbc);

        if (!isStaff) {
            mainContent.add(passPanel);
        }

        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.add(mainContent, BorderLayout.NORTH);
        add(centerWrapper, BorderLayout.CENTER);
    }

    private void addInfoField(JPanel panel, String label, JTextField field, int y, GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.weightx = 0.3;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        panel.add(field, gbc);
    }

    private void handleChangePassword() {
        String oldP = new String(txtOldPass.getPassword());
        String newP = new String(txtNewPass.getPassword());
        String confP = new String(txtConfirmPass.getPassword());

        if (oldP.isEmpty() || newP.isEmpty() || confP.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        try {
            accountBLL.changePassword(oldP, newP, confP);
            JOptionPane.showMessageDialog(this, "Đổi mật khẩu thành công!");
            txtOldPass.setText("");
            txtNewPass.setText("");
            txtConfirmPass.setText("");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}
