package com.handbagstore.gui;

import com.handbagstore.bll.AccountBLL;
import com.handbagstore.gui.panels.*;
import com.handbagstore.utils.OrderTimerManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Màn hình chính cho Staff — chỉ có quyền bán hàng, xem SP, quản lý KH.
 */
public class MainStaffFrame extends JFrame {
    private JPanel contentPanel;
    private CardLayout cardLayout;
    private SalePanel salePanel;
    private CustomerManagerPanel customerPanel;
    private InvoiceHistoryPanel invoicePanel;

    public MainStaffFrame() {
        initComponents();
    }

    private void initComponents() {
        setTitle("Staff - Hệ thống Quản lý Túi Xách | " + AccountBLL.getCurrentUser().getFullName());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1280, 800);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(1024, 700));

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleLogout();
            }
        });

        setLayout(new BorderLayout());

        // === SIDEBAR ===
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBackground(new Color(30, 30, 46));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));

        JLabel lblLogo = new JLabel("🛍 BAG STORE", SwingConstants.CENTER);
        lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblLogo.setForeground(Color.WHITE);
        lblLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(lblLogo);
        sidebar.add(Box.createVerticalStrut(10));

        JLabel lblRole = new JLabel("👤 Nhân viên", SwingConstants.CENTER);
        lblRole.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblRole.setForeground(new Color(166, 173, 186));
        lblRole.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(lblRole);
        sidebar.add(Box.createVerticalStrut(30));

        // Staff menu items
        String[][] menuItems = {
            {"🛒 Bán hàng", "SALE"},
            {"👤 Khách hàng", "CUSTOMER"},
            {"🧾 Hóa đơn", "INVOICE"}
        };

        for (String[] item : menuItems) {
            JButton btn = createMenuButton(item[0], item[1]);
            sidebar.add(btn);
            sidebar.add(Box.createVerticalStrut(5));
        }

        sidebar.add(Box.createVerticalGlue());

        JButton btnLogout = createMenuButton("🚪 Đăng xuất", "LOGOUT");
        btnLogout.setBackground(new Color(220, 53, 69));
        sidebar.add(btnLogout);

        add(sidebar, BorderLayout.WEST);

        // === CONTENT ===
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);

        salePanel = new SalePanel();
        customerPanel = new CustomerManagerPanel();
        invoicePanel = new InvoiceHistoryPanel();

        contentPanel.add(salePanel, "SALE");
        contentPanel.add(customerPanel, "CUSTOMER");
        contentPanel.add(invoicePanel, "INVOICE");

        add(contentPanel, BorderLayout.CENTER);
        cardLayout.show(contentPanel, "SALE");
    }

    private JButton createMenuButton(String text, String command) {
        JButton btn = new JButton(text);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(200, 40));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(45, 45, 65));
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(new Color(64, 133, 240));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (!"LOGOUT".equals(command))
                    btn.setBackground(new Color(45, 45, 65));
                else
                    btn.setBackground(new Color(220, 53, 69));
            }
        });

        btn.addActionListener(e -> {
            if ("LOGOUT".equals(command)) {
                handleLogout();
            } else {
                cardLayout.show(contentPanel, command);
            }
        });
        return btn;
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn đăng xuất?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try { new AccountBLL().logout(); } catch (Exception ignored) {}
            OrderTimerManager.getInstance().shutdown();
            dispose();
            new LoginFrame().setVisible(true);
        }
    }

    public void switchToCustomerAndRegister(String phone) {
        cardLayout.show(contentPanel, "CUSTOMER");
        customerPanel.prefillForNewCustomer(phone);
    }
}
