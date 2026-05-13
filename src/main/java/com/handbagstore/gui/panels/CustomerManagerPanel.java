package com.handbagstore.gui.panels;

import com.handbagstore.bll.CustomerBLL;
import com.handbagstore.dto.CustomerDTO;
import com.handbagstore.dto.InvoiceDTO;
import com.handbagstore.gui.components.DateChooser;
import com.handbagstore.utils.DateUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

public class CustomerManagerPanel extends JPanel {
    private JTable customerTable, historyTable;
    private DefaultTableModel customerModel, historyModel;
    private JTextField txtSearch, txtName, txtPhone, txtBirthday;
    private final CustomerBLL customerBLL = new CustomerBLL();

    public CustomerManagerPanel() {
        initComponents();
        refreshData();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel lblTitle = new JLabel("👤 Quản lý Khách hàng");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        txtSearch = new JTextField(20);
        txtSearch.putClientProperty("JTextField.placeholderText", "Tìm tên hoặc SĐT...");
        JButton btnSearch = new JButton("🔍");
        btnSearch.addActionListener(e -> searchCustomers());
        txtSearch.addActionListener(e -> searchCustomers());
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { searchCustomers(); }
            public void removeUpdate(DocumentEvent e) { searchCustomers(); }
            public void changedUpdate(DocumentEvent e) { searchCustomers(); }
        });
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(lblTitle, BorderLayout.WEST);
        topPanel.add(searchPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setResizeWeight(0.5);

        // Customer table
        String[] custCols = { "ID", "Họ tên", "SĐT", "Sinh nhật" };
        customerModel = new DefaultTableModel(custCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        customerTable = new JTable(customerModel);
        customerTable.setRowHeight(28);
        customerTable.getSelectionModel().addListSelectionListener(e -> loadHistory());
        JScrollPane customerScrollPane = new JScrollPane(customerTable);
        customerScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        split.setTopComponent(customerScrollPane);

        // History table
        String[] hisCols = { "Mã HĐ", "Ngày", "Tổng tiền", "Trạng thái" };
        historyModel = new DefaultTableModel(hisCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        historyTable = new JTable(historyModel);
        historyTable.setRowHeight(28);
        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.add(new JLabel("  📜 Lịch sử mua hàng:"), BorderLayout.NORTH);
        historyPanel.add(new JScrollPane(historyTable), BorderLayout.CENTER);
        split.setBottomComponent(historyPanel);

        add(split, BorderLayout.CENTER);

        // Form
        JPanel bottom = new JPanel(new BorderLayout(10, 5));
        JPanel form = new JPanel(new GridLayout(1, 6, 10, 5));
        form.setBorder(BorderFactory.createTitledBorder("Thông tin khách hàng"));
        txtName = new JTextField();
        txtPhone = new JTextField();
        txtBirthday = new JTextField();
        txtBirthday.putClientProperty("JTextField.placeholderText", "dd/MM/yyyy");

        JPanel birthdayPanel = new JPanel(new BorderLayout(2, 0));
        birthdayPanel.add(txtBirthday, BorderLayout.CENTER);

        JButton btnCal = new JButton("📅");
        btnCal.setToolTipText("Chọn ngày từ lịch");
        btnCal.addActionListener(e -> {
            LocalDate current = DateUtils.parseDate(txtBirthday.getText());
            LocalDate picked = DateChooser.showDialog(CustomerManagerPanel.this, current);
            if (picked != null)
                txtBirthday.setText(DateUtils.formatDate(picked));
        });
        birthdayPanel.add(btnCal, BorderLayout.EAST);

        form.add(new JLabel("Họ tên:"));
        form.add(txtName);
        form.add(new JLabel("SĐT:"));
        form.add(txtPhone);
        form.add(new JLabel("Sinh nhật:"));
        form.add(birthdayPanel);
        bottom.add(form, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton btnAdd = new JButton("➕ Thêm");
        btnAdd.setBackground(new Color(40, 167, 69));
        btnAdd.setForeground(Color.WHITE);
        btnAdd.addActionListener(e -> addCustomer());
        JButton btnUpdate = new JButton("✏️ Cập nhật");
        btnUpdate.setBackground(new Color(255, 193, 7));
        btnUpdate.setForeground(Color.WHITE);
        btnUpdate.addActionListener(e -> updateCustomer());
        btnPanel.add(btnAdd);
        btnPanel.add(btnUpdate);
        bottom.add(btnPanel, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);
    }

    private void refreshData() {
        try {
            customerModel.setRowCount(0);
            List<CustomerDTO> list = customerBLL.getAll();
            for (CustomerDTO c : list) {
                customerModel.addRow(new Object[] {
                        c.getCustomerId(), c.getFullName(), c.getPhone(),
                        c.getBirthday() != null ? DateUtils.formatDate(c.getBirthday()) : ""
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
        }
    }

    private void searchCustomers() {
        String kw = txtSearch.getText().trim();
        if (kw.isEmpty()) {
            refreshData();
            return;
        }
        try {
            customerModel.setRowCount(0);
            for (CustomerDTO c : customerBLL.search(kw)) {
                customerModel.addRow(new Object[] {
                        c.getCustomerId(), c.getFullName(), c.getPhone(),
                        c.getBirthday() != null ? DateUtils.formatDate(c.getBirthday()) : ""
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
        }
    }

    private void loadHistory() {
        int row = customerTable.getSelectedRow();
        if (row < 0)
            return;
        try {
            int custId = (int) customerModel.getValueAt(row, 0);
            txtName.setText((String) customerModel.getValueAt(row, 1));
            txtPhone.setText((String) customerModel.getValueAt(row, 2));
            txtBirthday.setText((String) customerModel.getValueAt(row, 3));

            historyModel.setRowCount(0);
            List<InvoiceDTO> history = customerBLL.getPurchaseHistory(custId);
            for (InvoiceDTO inv : history) {
                historyModel.addRow(new Object[] {
                        inv.getInvoiceCode(), DateUtils.formatDateTime(inv.getCreatedAt()),
                        inv.getTotal(), inv.getStatus()
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
        }
    }

    private void addCustomer() {
        try {
            CustomerDTO c = new CustomerDTO();
            c.setFullName(txtName.getText().trim());
            c.setPhone(txtPhone.getText().trim());
            LocalDate bd = DateUtils.parseDate(txtBirthday.getText().trim());
            c.setBirthday(bd);
            customerBLL.addCustomer(c);
            JOptionPane.showMessageDialog(this, "Thêm khách hàng thành công!");
            clearForm();
            refreshData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateCustomer() {
        int row = customerTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Chọn khách hàng!");
            return;
        }
        try {
            CustomerDTO c = new CustomerDTO();
            c.setCustomerId((int) customerModel.getValueAt(row, 0));
            c.setFullName(txtName.getText().trim());
            c.setPhone(txtPhone.getText().trim());
            LocalDate bd = DateUtils.parseDate(txtBirthday.getText().trim());
            c.setBirthday(bd);
            customerBLL.updateCustomer(c);
            JOptionPane.showMessageDialog(this, "Cập nhật thành công!");
            refreshData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearForm() {
        txtName.setText("");
        txtPhone.setText("");
        txtBirthday.setText("");
    }
}
