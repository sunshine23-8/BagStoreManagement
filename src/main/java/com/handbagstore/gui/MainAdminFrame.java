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
    private JLabel lblLogo, lblLogoIcon;
    private JLabel lblRole, lblRoleIcon;
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

        // Apply theme colors to sidebar on startup
        updateSidebarColors();
    }

    private JPanel createSidebar() {
        sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(250, 0));
        sidebar.setBackground(new Color(30, 30, 46));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));

        // Logo / Title
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        logoPanel.setOpaque(false);
        lblLogoIcon = new JLabel("🛍");
        lblLogoIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        lblLogo = new JLabel("BAG STORE");
        lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblLogo.setForeground(Color.WHITE);
        logoPanel.add(lblLogoIcon);
        logoPanel.add(lblLogo);
        logoPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(logoPanel);
        sidebar.add(Box.createVerticalStrut(5)); // Minimal space after logo

        JPanel rolePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        rolePanel.setOpaque(false);
        lblRoleIcon = new JLabel("👤");
        lblRoleIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        lblRole = new JLabel("Admin");
        lblRole.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblRole.setForeground(new Color(166, 173, 186));
        rolePanel.add(lblRoleIcon);
        rolePanel.add(lblRole);
        rolePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(rolePanel);
        sidebar.add(Box.createVerticalStrut(5)); // Minimal space before menu

        // Menu items container (Top)
        JPanel menuContainer = new JPanel();
        menuContainer.setLayout(new BoxLayout(menuContainer, BoxLayout.Y_AXIS));
        menuContainer.setOpaque(false);

        // Menu items
        String[][] menuItems = {
                { "📦", "Sản phẩm", "PRODUCT" },
                { "🏭", "Kho hàng", "INVENTORY" },
                { "👥", "Nhân viên", "STAFF" },
                { "🧾", "Hóa đơn", "INVOICE" },
                { "🎫", "Giảm giá", "DISCOUNT" },
                { "📊", "Thống kê", "STATISTIC" },
                { "📋", "Nhật ký", "LOG" },
                { "👤", "Cá nhân", "PROFILE" }
        };

        for (String[] item : menuItems) {
            JButton btn = createMenuButton(item[0], item[1], item[2]);
            menuContainer.add(btn);
            menuContainer.add(Box.createVerticalStrut(5));
            menuButtons.add(btn);
        }
        sidebar.add(menuContainer);

        sidebar.add(Box.createVerticalGlue()); // Pushes following items to bottom

        // System items container (Bottom)
        JPanel systemContainer = new JPanel();
        systemContainer.setLayout(new BoxLayout(systemContainer, BoxLayout.Y_AXIS));
        systemContainer.setOpaque(false);

        String themeText = FlatLaf.isLafDark() ? "Giao diện sáng" : "Giao diện tối";
        String themeIcon = FlatLaf.isLafDark() ? "☀️" : "🌙";
        btnToggleTheme = createMenuButton(themeIcon, themeText, "TOGGLE_THEME");
        systemContainer.add(btnToggleTheme);
        systemContainer.add(Box.createVerticalStrut(5));
        menuButtons.add(btnToggleTheme);

        // Logout button
        JButton btnLogout = createMenuButton("🚪", "Đăng xuất", "LOGOUT");
        btnLogout.setBackground(new Color(220, 53, 69));
        systemContainer.add(btnLogout);

        sidebar.add(systemContainer);

        // Highlight first button by default
        if (!menuButtons.isEmpty()) {
            currentActiveButton = menuButtons.get(0);
            currentActiveButton.setBackground(activeColor);
        }

        return sidebar;
    }

    private JButton createMenuButton(String icon, String text, String command) {
        JButton btn = new JButton();
        btn.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        JLabel lblIcon = new JLabel(icon);
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        lblIcon.setForeground(Color.WHITE);
        lblIcon.setHorizontalAlignment(SwingConstants.CENTER);
        
        JLabel lblText = new JLabel(text);
        lblText.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblText.setForeground(Color.WHITE);
        
        // Use a wrapper panel to align to the left with padding
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(0, 40, 0, 0); // 40px padding from left
        
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints cbc = new GridBagConstraints();
        cbc.gridx = 0;
        cbc.gridy = 0;
        cbc.insets = new java.awt.Insets(0, 0, 0, 10);
        content.add(lblIcon, cbc);
        cbc.gridx = 1;
        cbc.insets = new java.awt.Insets(0, 0, 0, 0);
        content.add(lblText, cbc);
        
        btn.add(content, gbc);

        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(250, 40)); // Full width
        btn.setPreferredSize(new Dimension(250, 40));
        btn.setBackground(normalColor);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(null);

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
        lblLogoIcon.setForeground(isDark ? Color.WHITE : Color.BLACK);
        lblRole.setForeground(isDark ? new Color(166, 173, 186) : new Color(100, 100, 100));
        lblRoleIcon.setForeground(isDark ? new Color(166, 173, 186) : new Color(100, 100, 100));

        normalColor = isDark ? new Color(45, 45, 65) : new Color(230, 230, 230);
        activeColor = new Color(64, 133, 240); // Keep blue

        for (JButton btn : menuButtons) {
            // Search inside nested content panel
            for (Component c : btn.getComponents()) {
                if (c instanceof JPanel) {
                    for (Component inner : ((JPanel) c).getComponents()) {
                        if (inner instanceof JLabel) {
                            inner.setForeground(isDark ? Color.WHITE : Color.BLACK);
                        }
                    }
                }
            }
            if (btn == currentActiveButton) {
                btn.setBackground(activeColor);
            } else {
                btn.setBackground(normalColor);
            }
        }

        // Update toggle button text/icon
        if (btnToggleTheme != null) {
            JLabel lblIcon = null;
            JLabel lblText = null;
            for (Component c : btnToggleTheme.getComponents()) {
                if (c instanceof JPanel) {
                    for (Component inner : ((JPanel) c).getComponents()) {
                        if (inner instanceof JLabel) {
                            JLabel lbl = (JLabel) inner;
                            if (lbl.getText().length() <= 2)
                                lblIcon = lbl; // Likely emoji
                            else
                                lblText = lbl;
                        }
                    }
                }
            }
            if (lblIcon != null)
                lblIcon.setText(isDark ? "☀️" : "🌙");
            if (lblText != null)
                lblText.setText(isDark ? "Giao diện sáng" : "Giao diện tối");
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
            } catch (Exception ignored) {
            }
            OrderTimerManager.getInstance().shutdown();
            dispose();
            new LoginFrame().setVisible(true);
        }
    }
}
