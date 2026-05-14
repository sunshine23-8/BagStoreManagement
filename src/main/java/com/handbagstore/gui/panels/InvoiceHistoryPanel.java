package com.handbagstore.gui.panels;

import com.handbagstore.dal.InvoiceDAL;
import com.handbagstore.bll.OrderBLL;
import com.handbagstore.dto.InvoiceDTO;
import com.handbagstore.dto.InvoiceDetailDTO;
import com.handbagstore.utils.DateUtils;
import com.handbagstore.utils.PdfExporter;
import com.handbagstore.bll.CustomerBLL;
import com.handbagstore.utils.CurrencyUtils;
import com.handbagstore.utils.TableUtils;
import com.handbagstore.gui.components.DateChooser;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

public class InvoiceHistoryPanel extends JPanel {
    private JTable invoiceTable, detailTable;
    private DefaultTableModel invoiceModel, detailModel;
    private JTextField txtSearch;
    private final InvoiceDAL invoiceDAL = new InvoiceDAL();
    private final OrderBLL orderBLL = new OrderBLL();

    private String filterStatus = "ALL";
    private java.sql.Date filterFrom = null;
    private java.sql.Date filterTo = null;

    public InvoiceHistoryPanel() {
        initComponents();
        refreshData();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Top
        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JLabel lblIcon = new JLabel("🧾");
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        JLabel lblText = new JLabel("Quản lý Hóa đơn");
        lblText.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titlePanel.add(lblIcon);
        titlePanel.add(lblText);
        topPanel.add(titlePanel, BorderLayout.WEST);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        txtSearch = new JTextField(20);
        txtSearch.putClientProperty("JTextField.placeholderText", "Tìm mã hóa đơn...");
        JButton btnSearch = new JButton(
                "<html><table><tr><td nowrap><font face='Segoe UI Emoji'>🔍</font>&nbsp;Tìm</td></tr></table></html>");
        btnSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSearch.addActionListener(e -> searchInvoices());

        JButton btnFilter = new JButton(
                "<html><table><tr><td nowrap><font face='Segoe UI Emoji'>⏳</font>&nbsp;Bộ&nbsp;lọc</td></tr></table></html>");
        btnFilter.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnFilter.addActionListener(e -> showFilterDialog());

        txtSearch.addActionListener(e -> searchInvoices());
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                searchInvoices();
            }

            public void removeUpdate(DocumentEvent e) {
                searchInvoices();
            }

            public void changedUpdate(DocumentEvent e) {
                searchInvoices();
            }
        });
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);
        searchPanel.add(btnFilter);
        topPanel.add(searchPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // Split: invoices (top) + details (bottom)
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.6);

        String[] invCols = { "Mã HĐ", "Ngày", "Khách hàng", "Nhân viên", "Tạm tính", "Giảm giá", "Tổng", "Thanh toán",
                "Trạng thái" };
        invoiceModel = new DefaultTableModel(invCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        invoiceTable = new JTable(invoiceModel);
        invoiceTable.setRowHeight(28);
        invoiceTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Align columns
        TableUtils.alignCenter(invoiceTable, 0, 1, 7, 8);
        TableUtils.alignLeft(invoiceTable, 2, 3);
        TableUtils.alignRight(invoiceTable, 4, 5, 6);

        invoiceTable.getSelectionModel().addListSelectionListener(e -> loadInvoiceDetails());
        splitPane.setTopComponent(new JScrollPane(invoiceTable));

        String[] detCols = { "Mã SP", "Tên SP", "Đơn giá", "SL", "Thành tiền" };
        detailModel = new DefaultTableModel(detCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        detailTable = new JTable(detailModel);
        detailTable.setRowHeight(28);
        detailTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Align columns
        TableUtils.alignCenter(detailTable, 0);
        TableUtils.alignLeft(detailTable, 1);
        TableUtils.alignRight(detailTable, 2, 3, 4);
        JPanel detailPanel = new JPanel(new BorderLayout());
        JPanel detailHeaderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        detailHeaderPanel.add(new JLabel("  Chi tiết hóa đơn:"));
        JButton btnExportPdf = new JButton("<html><font face='Segoe UI Emoji'>📄</font> Xuất PDF</html>");
        btnExportPdf.addActionListener(e -> exportSelectedInvoicePdf());
        detailHeaderPanel.add(btnExportPdf);
        detailPanel.add(detailHeaderPanel, BorderLayout.NORTH);
        detailPanel.add(new JScrollPane(detailTable), BorderLayout.CENTER);
        splitPane.setBottomComponent(detailPanel);

        add(splitPane, BorderLayout.CENTER);
    }

    public void refreshData() {
        try {
            invoiceModel.setRowCount(0);
            List<InvoiceDTO> invoices = invoiceDAL.getAll();
            for (InvoiceDTO inv : invoices) {
                invoiceModel.addRow(new Object[] {
                        inv.getInvoiceCode(),
                        DateUtils.formatDateTime(inv.getCreatedAt()),
                        inv.getCustomerName() != null ? inv.getCustomerName() : "Khách vãng lai",
                        inv.getStaffName(),
                        CurrencyUtils.format(inv.getSubtotal()), CurrencyUtils.format(inv.getDiscountAmount()),
                        CurrencyUtils.format(inv.getTotal()),
                        inv.getPaymentMethod() == null ? "—"
                                : ("CASH".equals(inv.getPaymentMethod()) ? "Tiền mặt" : "Chuyển khoản"),
                        inv.getStatus()
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
        }
    }

    private void searchInvoices() {
        String keyword = txtSearch.getText().trim();
        try {
            invoiceModel.setRowCount(0);
            List<InvoiceDTO> invoices = invoiceDAL.search(keyword, null, filterFrom, filterTo, filterStatus);
            for (InvoiceDTO inv : invoices) {
                invoiceModel.addRow(new Object[] {
                        inv.getInvoiceCode(), DateUtils.formatDateTime(inv.getCreatedAt()),
                        inv.getCustomerName() != null ? inv.getCustomerName() : "Khách vãng lai",
                        inv.getStaffName(), CurrencyUtils.format(inv.getSubtotal()),
                        CurrencyUtils.format(inv.getDiscountAmount()), CurrencyUtils.format(inv.getTotal()),
                        inv.getPaymentMethod() == null ? "—"
                                : ("CASH".equals(inv.getPaymentMethod()) ? "Tiền mặt" : "Chuyển khoản"),
                        inv.getStatus()
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
        }
    }

    private void loadInvoiceDetails() {
        int row = invoiceTable.getSelectedRow();
        if (row < 0)
            return;
        try {
            String code = (String) invoiceModel.getValueAt(row, 0);
            // Find invoice by code to get ID — for simplicity, search all
            List<InvoiceDTO> all = invoiceDAL.search(code, null, null, null, null);
            if (all.isEmpty())
                return;
            InvoiceDTO inv = all.get(0);

            detailModel.setRowCount(0);
            List<InvoiceDetailDTO> details = orderBLL.getInvoiceDetails(inv.getInvoiceId());
            for (InvoiceDetailDTO d : details) {
                detailModel.addRow(new Object[] {
                        d.getProductCode(), d.getProductName(),
                        CurrencyUtils.format(d.getUnitPrice()), d.getQuantity(), CurrencyUtils.format(d.getLineTotal())
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
        }
    }

    private void exportSelectedInvoicePdf() {
        int row = invoiceTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một hóa đơn để xuất PDF.");
            return;
        }
        try {
            String code = (String) invoiceModel.getValueAt(row, 0);
            List<InvoiceDTO> all = invoiceDAL.search(code, null, null, null, "ALL");
            if (all.isEmpty())
                return;
            InvoiceDTO inv = all.get(0);

            if ("CANCELLED".equals(inv.getStatus())) {
                JOptionPane.showMessageDialog(this, "Không thể xuất PDF cho hóa đơn đã bị hủy.", "Thông báo",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            List<InvoiceDetailDTO> details = orderBLL.getInvoiceDetails(inv.getInvoiceId());
            com.handbagstore.dto.CustomerDTO customer = null;
            if (inv.getCustomerId() != null && inv.getCustomerId() > 0) {
                customer = new CustomerBLL().getById(inv.getCustomerId());
            }

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Lưu PDF Hóa Đơn");
            fileChooser.setSelectedFile(new java.io.File("HoaDon_" + inv.getInvoiceCode() + ".pdf"));

            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                String path = fileChooser.getSelectedFile().getAbsolutePath();
                if (!path.toLowerCase().endsWith(".pdf"))
                    path += ".pdf";

                PdfExporter.exportInvoice(path, inv, details, customer);
                Object[] options = { "OK", "In hóa đơn" };
                int choice = JOptionPane.showOptionDialog(
                        this,
                        "Đã xuất PDF thành công: " + path,
                        "Thông báo",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.INFORMATION_MESSAGE,
                        null,
                        options,
                        options[0]);

                if (choice == 1) {
                    try {
                        if (java.awt.Desktop.isDesktopSupported()) {
                            java.awt.Desktop.getDesktop().open(new java.io.File(path));
                        } else {
                            JOptionPane.showMessageDialog(this, "Hệ thống không hỗ trợ mở tệp tự động.", "Thông báo",
                                    JOptionPane.WARNING_MESSAGE);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, "Không thể mở tệp: " + ex.getMessage(), "Lỗi",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi xuất PDF: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showFilterDialog() {
        JDialog dialog = new JDialog((Window) SwingUtilities.getWindowAncestor(this), "Bộ lọc hóa đơn");
        dialog.setModal(true);
        dialog.setSize(450, 250);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtFrom = new JTextField(filterFrom != null ? DateUtils.formatDate(filterFrom.toLocalDate()) : "", 12);
        JTextField txtTo = new JTextField(filterTo != null ? DateUtils.formatDate(filterTo.toLocalDate()) : "", 12);
        txtFrom.putClientProperty("JTextField.placeholderText", "dd/MM/yyyy");
        txtTo.putClientProperty("JTextField.placeholderText", "dd/MM/yyyy");

        String[] statuses = { "Tất cả", "PAID", "CANCELLED", "PENDING" };
        String[] statusValues = { "ALL", "PAID", "CANCELLED", "PENDING" };
        JComboBox<String> cmbStatus = new JComboBox<>(statuses);
        for (int i = 0; i < statusValues.length; i++) {
            if (statusValues[i].equals(filterStatus)) {
                cmbStatus.setSelectedIndex(i);
                break;
            }
        }

        JPanel fromPanel = new JPanel(new BorderLayout(2, 0));
        fromPanel.add(txtFrom, BorderLayout.CENTER);
        JButton btnFrom = new JButton("<html><font face='Segoe UI Emoji'>📅</font></html>");
        btnFrom.addActionListener(e -> {
            LocalDate current = DateUtils.parseDate(txtFrom.getText());
            LocalDate picked = DateChooser.showDialog(dialog, current);
            if (picked != null)
                txtFrom.setText(DateUtils.formatDate(picked));
        });
        fromPanel.add(btnFrom, BorderLayout.EAST);

        JPanel toPanel = new JPanel(new BorderLayout(2, 0));
        toPanel.add(txtTo, BorderLayout.CENTER);
        JButton btnTo = new JButton("<html><font face='Segoe UI Emoji'>📅</font></html>");
        btnTo.addActionListener(e -> {
            LocalDate current = DateUtils.parseDate(txtTo.getText());
            LocalDate picked = DateChooser.showDialog(dialog, current);
            if (picked != null)
                txtTo.setText(DateUtils.formatDate(picked));
        });
        toPanel.add(btnTo, BorderLayout.EAST);

        gbc.gridx = 0;
        gbc.gridy = 0;
        dialog.add(new JLabel("Từ ngày:"), gbc);
        gbc.gridx = 1;
        dialog.add(fromPanel, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        dialog.add(new JLabel("Đến ngày:"), gbc);
        gbc.gridx = 1;
        dialog.add(toPanel, gbc);
        gbc.gridx = 0;
        gbc.gridy = 2;
        dialog.add(new JLabel("Trạng thái:"), gbc);
        gbc.gridx = 1;
        dialog.add(cmbStatus, gbc);

        JButton btnApply = new JButton("Áp dụng");
        btnApply.addActionListener(e -> {
            try {
                String fromStr = txtFrom.getText().trim();
                String toStr = txtTo.getText().trim();
                filterFrom = fromStr.isEmpty() ? null : java.sql.Date.valueOf(DateUtils.parseDate(fromStr));
                filterTo = toStr.isEmpty() ? null : java.sql.Date.valueOf(DateUtils.parseDate(toStr));
                filterStatus = statusValues[cmbStatus.getSelectedIndex()];
                dialog.dispose();
                searchInvoices();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Lỗi định dạng ngày: " + ex.getMessage());
            }
        });

        JButton btnReset = new JButton("Đặt lại");
        btnReset.addActionListener(e -> {
            filterFrom = null;
            filterTo = null;
            filterStatus = "ALL";
            dialog.dispose();
            searchInvoices();
        });

        JPanel btnPanel = new JPanel();
        btnPanel.add(btnApply);
        btnPanel.add(btnReset);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        dialog.add(btnPanel, gbc);

        dialog.setVisible(true);
    }
}
