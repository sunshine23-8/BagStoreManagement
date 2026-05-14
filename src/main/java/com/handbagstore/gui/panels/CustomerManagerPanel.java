package com.handbagstore.gui.panels;

import com.handbagstore.bll.CustomerBLL;
import com.handbagstore.bll.OrderBLL;
import com.handbagstore.dto.CustomerDTO;
import com.handbagstore.dto.InvoiceDTO;
import com.handbagstore.dto.InvoiceDetailDTO;
import com.handbagstore.gui.components.DateChooser;
import com.handbagstore.utils.ButtonUtils;
import com.handbagstore.utils.DateUtils;
import com.handbagstore.utils.CurrencyUtils;
import com.handbagstore.utils.TableUtils;

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
    private final OrderBLL orderBLL = new OrderBLL();
    private java.util.List<InvoiceDTO> currentHistory = new java.util.ArrayList<>();

    public CustomerManagerPanel() {
        initComponents();
        refreshData();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JLabel lblIcon = new JLabel("👤");
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        JLabel lblText = new JLabel("Quản lý Khách hàng");
        lblText.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titlePanel.add(lblIcon);
        titlePanel.add(lblText);
        // Removed redundant add(titlePanel, BorderLayout.NORTH);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        txtSearch = new JTextField(20);
        txtSearch.putClientProperty("JTextField.placeholderText", "Tìm tên hoặc SĐT...");
        JButton btnSearch = new JButton();
        ButtonUtils.setupButton(btnSearch, "🔍", "Tìm", null, null);
        btnSearch.addActionListener(e -> searchCustomers());
        txtSearch.addActionListener(e -> searchCustomers());
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                searchCustomers();
            }

            public void removeUpdate(DocumentEvent e) {
                searchCustomers();
            }

            public void changedUpdate(DocumentEvent e) {
                searchCustomers();
            }
        });
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(titlePanel, BorderLayout.WEST);
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
        customerTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Align columns
        TableUtils.alignCenter(customerTable, 0, 2, 3);
        TableUtils.alignLeft(customerTable, 1);

        customerTable.setFillsViewportHeight(true);
        customerTable.getSelectionModel().addListSelectionListener(e -> loadHistory());
        customerTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (customerTable.rowAtPoint(e.getPoint()) == -1) {
                    customerTable.clearSelection();
                }
            }
        });
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
        historyTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Align columns
        TableUtils.alignCenter(historyTable, 0, 1, 3);
        TableUtils.alignRight(historyTable, 2);
        JPanel historyPanel = new JPanel(new BorderLayout());

        JPanel historyHeader = new JPanel(new BorderLayout());
        JPanel historyTitlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JLabel histIcon = new JLabel("📜");
        histIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        JLabel histText = new JLabel("Lịch sử mua hàng:");
        histText.setFont(new Font("Segoe UI", Font.BOLD, 12));
        historyTitlePanel.add(histIcon);
        historyTitlePanel.add(histText);
        historyHeader.add(historyTitlePanel, BorderLayout.WEST);

        JButton btnExportPdf = new JButton();
        ButtonUtils.setupButton(btnExportPdf, "📄", "In PDF", null, null);
        btnExportPdf.addActionListener(e -> exportPdf());
        historyHeader.add(btnExportPdf, BorderLayout.EAST);

        historyPanel.add(historyHeader, BorderLayout.NORTH);
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

        JButton btnCal = new JButton();
        ButtonUtils.setupButton(btnCal, "📅", "", null, null);
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
        JButton btnAdd = new JButton();
        ButtonUtils.setupButton(btnAdd, "➕", "Thêm", new Color(40, 167, 69), Color.WHITE);
        btnAdd.addActionListener(e -> addCustomer());
        
        JButton btnUpdate = new JButton();
        ButtonUtils.setupButton(btnUpdate, "✏️", "Cập nhật", new Color(13, 110, 253), Color.WHITE);
        btnUpdate.addActionListener(e -> updateCustomer());
        btnPanel.add(btnAdd);
        btnPanel.add(btnUpdate);
        bottom.add(btnPanel, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);
    }

    public void refreshData() {
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
        if (row < 0) {
            clearForm();
            historyModel.setRowCount(0);
            return;
        }
        try {
            int custId = (int) customerModel.getValueAt(row, 0);
            txtName.setText((String) customerModel.getValueAt(row, 1));
            txtPhone.setText((String) customerModel.getValueAt(row, 2));
            txtBirthday.setText((String) customerModel.getValueAt(row, 3));

            historyModel.setRowCount(0);
            currentHistory = customerBLL.getPurchaseHistory(custId);
            for (InvoiceDTO inv : currentHistory) {
                historyModel.addRow(new Object[] {
                        inv.getInvoiceCode(), DateUtils.formatDateTime(inv.getCreatedAt()),
                        CurrencyUtils.format(inv.getTotal()), inv.getStatus()
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

    public void prefillForNewCustomer(String phone) {
        txtPhone.setText(phone);
        txtName.setText("");
        txtBirthday.setText("");
        customerTable.clearSelection();
        txtName.requestFocus();
    }

    private void exportPdf() {
        int row = historyTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một hóa đơn từ lịch sử mua hàng!");
            return;
        }
        try {
            InvoiceDTO inv = currentHistory.get(row);
            java.util.List<InvoiceDetailDTO> details = orderBLL.getInvoiceDetails(inv.getInvoiceId());

            CustomerDTO customer = null;
            if (inv.getCustomerId() != null && inv.getCustomerId() > 0) {
                customer = customerBLL.getById(inv.getCustomerId());
            }

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Lưu Hóa Đơn PDF");
            fileChooser.setSelectedFile(new java.io.File(inv.getInvoiceCode() + ".pdf"));

            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                java.io.File file = fileChooser.getSelectedFile();
                String path = file.getAbsolutePath();
                if (!path.toLowerCase().endsWith(".pdf")) {
                    path += ".pdf";
                }

                com.handbagstore.utils.PdfExporter.exportInvoice(path, inv, details, customer);
                JOptionPane.showMessageDialog(this, "Xuất PDF thành công!\n" + path);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi xuất PDF: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}
