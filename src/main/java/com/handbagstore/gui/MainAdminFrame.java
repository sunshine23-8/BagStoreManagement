package com.handbagstore.gui;

import com.handbagstore.bll.AccountBLL;
import com.handbagstore.gui.panels.*;
import com.handbagstore.utils.OrderTimerManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatDarkLaf;

/**
 * Màn hình chính cho Admin — sidebar navigation với tất cả các chức năng.
 */
public class MainAdminFrame extends JFrame {
    private JPanel contentPanel;
    private CardLayout cardLayout;

    // Panels
    private ProductManagerPanel productPanel;
    private InventoryPanel inventoryPanel;
    private StaffManagerPanel staffPanel;
    private InvoiceHistoryPanel invoicePanel;
    private DiscountManagerPanel discountPanel;
    private StatisticPanel statisticPanel;
    private SystemLogPanel systemLogPanel;
    private ProfilePanel profilePanel;
    
    private java.util.List<JButton> menuButtons = new java.util.ArrayList<>();
    private JButton currentActiveButton;
    private Color normalColor = new Color(45, 45, 65);
    private Color activeColor = new Color(64, 133, 240);
    private JPanel sidebar;
    private JLabel lblLogo;
    private JLabel lblRole;
    private JButton btnToggleTheme;

    public MainAdminFrame() {
        initComponents();
    }

    private void initComponents() {
        setTitle("Admin - Hệ thống Quản lý Túi Xách | " + AccountBLL.getCurrentUser().getFullName());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1280, 800);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(1024, 700));

        // Xử lý đóng cửa sổ
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleLogout();
            }
        });

        // Layout chính: Sidebar (trái) + Content (phải)
        setLayout(new BorderLayout());

        // === SIDEBAR ===
        JPanel sidebar = createSidebar();
        add(sidebar, BorderLayout.WEST);

        // === CONTENT AREA ===
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);

        productPanel = new ProductManagerPanel();
        inventoryPanel = new InventoryPanel();
        staffPanel = new StaffManagerPanel();
        invoicePanel = new InvoiceHistoryPanel();
        discountPanel = new DiscountManagerPanel();
        statisticPanel = new StatisticPanel();
        systemLogPanel = new SystemLogPanel();
        profilePanel = new ProfilePanel();

        contentPanel.add(productPanel, "PRODUCT");
        contentPanel.add(inventoryPanel, "INVENTORY");
        contentPanel.add(staffPanel, "STAFF");
        contentPanel.add(invoicePanel, "INVOICE");
        contentPanel.add(discountPanel, "DISCOUNT");
        contentPanel.add(statisticPanel, "STATISTIC");
        contentPanel.add(systemLogPanel, "LOG");
        contentPanel.add(profilePanel, "PROFILE");

        add(contentPanel, BorderLayout.CENTER);

        // Hiển thị panel đầu tiên
        cardLayout.show(contentPanel, "PRODUCT");
    }

    private JPanel createSidebar() {
        sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBackground(new Color(30, 30, 46));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));

        // Logo / Title
        lblLogo = new JLabel("🛍 BAG STORE", SwingConstants.CENTER);
        lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblLogo.setForeground(Color.WHITE);
        lblLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(lblLogo);
        sidebar.add(Box.createVerticalStrut(10));

        lblRole = new JLabel("👤 Admin", SwingConstants.CENTER);
        lblRole.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblRole.setForeground(new Color(166, 173, 186));
        lblRole.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(lblRole);
        sidebar.add(Box.createVerticalStrut(30));

        // Menu buttons
        String[][] menuItems = {
            {"📦 Sản phẩm", "PRODUCT"},
            {"🏭 Kho hàng", "INVENTORY"},
            {"👥 Nhân viên", "STAFF"},
            {"🧾 Hóa đơn", "INVOICE"},
            {"🎫 Giảm giá", "DISCOUNT"},
            {"📊 Thống kê", "STATISTIC"},
            {"📋 Nhật ký", "LOG"},
            {"👤 Cá nhân", "PROFILE"}
        };

        for (String[] item : menuItems) {
            JButton btn = createMenuButton(item[0], item[1]);
            sidebar.add(btn);
            sidebar.add(Box.createVerticalStrut(5));
            menuButtons.add(btn);
        }

        // Highlight first button by default
        if (!menuButtons.isEmpty()) {
            currentActiveButton = menuButtons.get(0);
            currentActiveButton.setBackground(activeColor);
        }

        sidebar.add(Box.createVerticalGlue());

        btnToggleTheme = createMenuButton(FlatLaf.isLafDark() ? "☀️ Giao diện sáng" : "🌙 Giao diện tối", "TOGGLE_THEME");
        sidebar.add(btnToggleTheme);
        sidebar.add(Box.createVerticalStrut(5));
        menuButtons.add(btnToggleTheme);

        // Logout button
        JButton btnLogout = createMenuButton("🚪 Đăng xuất", "LOGOUT");
        btnLogout.setBackground(new Color(220, 53, 69));
        sidebar.add(btnLogout);

        return sidebar;
    }

    private JButton createMenuButton(String text, String command) {
        JButton btn = new JButton(text);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(200, 40));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(normalColor);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(activeColor);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (btn != currentActiveButton) {
                    if ("LOGOUT".equals(command))
                        btn.setBackground(new Color(220, 53, 69));
                    else
                        btn.setBackground(normalColor);
                }
            }
        });

        btn.addActionListener(e -> {
            if ("LOGOUT".equals(command)) {
                handleLogout();
            } else if ("TOGGLE_THEME".equals(command)) {
                toggleTheme();
            } else {
                cardLayout.show(contentPanel, command);
                updateButtonColors(btn);
                refreshPanel(command);
            }
        });
        return btn;
    }

    private void updateButtonColors(JButton activeBtn) {
        if (currentActiveButton != null) {
            currentActiveButton.setBackground(normalColor);
        }
        currentActiveButton = activeBtn;
        currentActiveButton.setBackground(activeColor);
    }

    private void toggleTheme() {
        try {
            boolean wasDark = FlatLaf.isLafDark();
            if (wasDark) {
                UIManager.setLookAndFeel(new FlatIntelliJLaf());
            } else {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            }
            FlatLaf.updateUI();
            
            // Update colors
            updateSidebarColors();
            
            // Save preference
            saveThemePreference(!wasDark);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void updateSidebarColors() {
        boolean isDark = FlatLaf.isLafDark();
        sidebar.setBackground(isDark ? new Color(30, 30, 46) : new Color(210, 225, 240));
        lblLogo.setForeground(isDark ? Color.WHITE : Color.BLACK);
        lblRole.setForeground(isDark ? new Color(166, 173, 186) : new Color(100, 100, 100));
        
        normalColor = isDark ? new Color(45, 45, 65) : new Color(230, 230, 230);
        activeColor = new Color(64, 133, 240); // Keep blue
        
        for (JButton btn : menuButtons) {
            btn.setForeground(isDark ? Color.WHITE : Color.BLACK);
            if (btn == currentActiveButton) {
                btn.setBackground(activeColor);
            } else {
                btn.setBackground(normalColor);
            }
        }
        
        // Update toggle button text
        if (btnToggleTheme != null) {
            btnToggleTheme.setText(isDark ? "☀️ Giao diện sáng" : "🌙 Giao diện tối");
        }
    }

    private void saveThemePreference(boolean isDark) {
        java.util.Properties props = new java.util.Properties();
        props.setProperty("theme", isDark ? "dark" : "light");
        try (java.io.FileOutputStream out = new java.io.FileOutputStream("app.properties")) {
            props.store(out, "Application Properties");
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
        }
    }

    private void refreshPanel(String panelName) {
        try {
            switch (panelName) {
                case "PRODUCT" -> productPanel.refreshData();
                case "INVENTORY" -> inventoryPanel.refreshData();
                case "STAFF" -> staffPanel.refreshData();
                case "INVOICE" -> invoicePanel.refreshData();
                case "DISCOUNT" -> discountPanel.refreshData();
                case "STATISTIC" -> statisticPanel.refreshData();
                case "LOG" -> systemLogPanel.refreshData();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi tải dữ liệu: " + ex.getMessage());
        }
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn đăng xuất?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                new AccountBLL().logout();
            } catch (Exception ignored) {}
            OrderTimerManager.getInstance().shutdown();
            dispose();
            new LoginFrame().setVisible(true);
        }
    }
}
