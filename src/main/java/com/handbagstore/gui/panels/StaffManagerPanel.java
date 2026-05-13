package com.handbagstore.gui.panels;

import com.handbagstore.bll.AccountBLL;
import com.handbagstore.dto.AccountDTO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;
import java.awt.*;
import java.util.List;

public class StaffManagerPanel extends JPanel {
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField txtUsername, txtFullName, txtPassword, txtSearch;
    private JComboBox<String> cmbStatusFilter;
    private TableRowSorter<DefaultTableModel> sorter;
    private JPanel formPanel;
    private final AccountBLL accountBLL = new AccountBLL();

    public StaffManagerPanel() {
        initComponents();
        refreshData();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel lblTitle = new JLabel("👥 Quản lý Nhân viên");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        add(lblTitle, BorderLayout.NORTH);

        String[] cols = { "ID", "Username", "Họ tên", "Trạng thái", "Ngày tạo" };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(30);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        table.getSelectionModel().addListSelectionListener(e -> loadSelectedRow());
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Filter panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        filterPanel.add(new JLabel("🔍 Tìm tên:"));
        txtSearch = new JTextField(15);
        filterPanel.add(txtSearch);

        filterPanel.add(new JLabel("Trạng thái:"));
        cmbStatusFilter = new JComboBox<>(new String[] { "Tất cả", "Hoạt động", "Đã khóa" });
        filterPanel.add(cmbStatusFilter);

        add(filterPanel, BorderLayout.NORTH);

        // Add listeners for filtering
        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) {
                applyFilter();
            }
        });
        cmbStatusFilter.addActionListener(e -> applyFilter());

        // Form
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 5));
        formPanel = new JPanel(new GridLayout(2, 3, 10, 5));
        formPanel.setBorder(BorderFactory.createTitledBorder("Tạo tài khoản Staff mới"));
        
        txtFullName = new JTextField();
        txtUsername = new JTextField();
        txtPassword = new JPasswordField(); // Use password field for security

        formPanel.add(new JLabel("Họ tên nhân viên:"));
        formPanel.add(new JLabel("Tên tài khoản:"));
        formPanel.add(new JLabel("Mật khẩu:"));
        
        formPanel.add(txtFullName);
        formPanel.add(txtUsername);
        formPanel.add(txtPassword);
        
        bottomPanel.add(formPanel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton btnCreate = new JButton("➕ Tạo tài khoản");
        btnCreate.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCreate.setBackground(new Color(40, 167, 69));
        btnCreate.setForeground(Color.WHITE);
        btnCreate.addActionListener(e -> createAccount());

        JButton btnUpdate = new JButton("✏️ Cập nhật");
        btnUpdate.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnUpdate.setBackground(new Color(13, 110, 253)); // Blue
        btnUpdate.setForeground(Color.WHITE);
        btnUpdate.addActionListener(e -> updateAccount());

        JButton btnReset = new JButton("🔑 Reset mật khẩu");
        btnReset.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnReset.addActionListener(e -> resetPassword());

        JButton btnToggle = new JButton("🔒 Khóa/Mở khóa");
        btnToggle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnToggle.setBackground(new Color(220, 53, 69)); // Red
        btnToggle.setForeground(Color.WHITE);
        btnToggle.addActionListener(e -> toggleActive());

        btnPanel.add(btnCreate);
        btnPanel.add(btnUpdate);
        btnPanel.add(btnReset);
        btnPanel.add(btnToggle);
        bottomPanel.add(btnPanel, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void refreshData() {
        try {
            tableModel.setRowCount(0);
            List<AccountDTO> staffList = accountBLL.getAllStaff();
            for (AccountDTO a : staffList) {
                tableModel.addRow(new Object[] {
                        a.getAccountId(), a.getUsername(), a.getFullName(),
                        a.isActive() ? "Hoạt động" : "Đã khóa",
                        a.getCreatedAt() != null ? a.getCreatedAt().toLocalDate() : ""
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
        }
    }

    private void createAccount() {
        String fullName = txtFullName.getText().trim();
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        if (fullName.isEmpty() || username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!", "Thông báo",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            accountBLL.createStaffAccount(username, password, fullName);
            JOptionPane.showMessageDialog(this, "Tạo tài khoản thành công!");
            txtFullName.setText("");
            txtUsername.setText("");
            txtPassword.setText("");
            refreshData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resetPassword() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Chọn nhân viên!");
            return;
        }
        String newPass = JOptionPane.showInputDialog(this, "Nhập mật khẩu mới");
        if (newPass == null || newPass.isEmpty())
            return;
        try {
            int id = (int) tableModel.getValueAt(row, 0);
            accountBLL.resetPassword(id, newPass);
            JOptionPane.showMessageDialog(this, "Reset mật khẩu thành công!");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void toggleActive() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Chọn nhân viên!");
            return;
        }
        try {
            int id = (int) tableModel.getValueAt(row, 0);
            String status = (String) tableModel.getValueAt(row, 3);
            boolean newActive = "Đã khóa".equals(status);
            accountBLL.toggleActive(id, newActive);
            refreshData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadSelectedRow() {
        int row = table.getSelectedRow();
        if (row < 0) {
            clearForm();
            return;
        }
        try {
            txtUsername.setText((String) tableModel.getValueAt(row, 1));
            txtFullName.setText((String) tableModel.getValueAt(row, 2));
            txtPassword.setText("");
            formPanel.setBorder(BorderFactory.createTitledBorder("Cập nhật tài khoản Staff"));
            formPanel.repaint();
        } catch (Exception ignored) {}
    }

    private void clearForm() {
        txtUsername.setText("");
        txtFullName.setText("");
        txtPassword.setText("");
        if (formPanel != null) {
            formPanel.setBorder(BorderFactory.createTitledBorder("Tạo tài khoản Staff mới"));
            formPanel.repaint();
        }
    }

    private void updateAccount() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Chọn nhân viên cần cập nhật!");
            return;
        }

        String fullName = txtFullName.getText().trim();
        String username = txtUsername.getText().trim();

        if (fullName.isEmpty() || username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Họ tên và Tên tài khoản không được để trống!");
            return;
        }

        try {
            int id = (int) tableModel.getValueAt(row, 0);
            accountBLL.updateStaffAccount(id, username, fullName);
            JOptionPane.showMessageDialog(this, "Cập nhật tài khoản thành công!");
            refreshData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void applyFilter() {
        String text = txtSearch.getText().trim();
        String status = (String) cmbStatusFilter.getSelectedItem();
        
        java.util.List<RowFilter<Object, Object>> filters = new java.util.ArrayList<>();
        
        if (!text.isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i)" + text, 2)); // Column 2 is Name
        }
        
        if (!"Tất cả".equals(status)) {
            filters.add(RowFilter.regexFilter("^" + status + "$", 3)); // Column 3 is Status
        }
        
        if (filters.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.andFilter(filters));
        }
    }
}
