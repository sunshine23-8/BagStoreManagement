package com.handbagstore.gui.panels;

import com.handbagstore.dal.InvoiceDAL;
import com.handbagstore.bll.OrderBLL;
import com.handbagstore.dto.InvoiceDTO;
import com.handbagstore.dto.InvoiceDetailDTO;
import com.handbagstore.utils.DateUtils;
import com.handbagstore.utils.PdfExporter;
import com.handbagstore.bll.CustomerBLL;
import com.handbagstore.utils.CurrencyUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class InvoiceHistoryPanel extends JPanel {
    private JTable invoiceTable, detailTable;
    private DefaultTableModel invoiceModel, detailModel;
    private JTextField txtSearch;
    private final InvoiceDAL invoiceDAL = new InvoiceDAL();
    private final OrderBLL orderBLL = new OrderBLL();

    public InvoiceHistoryPanel() { initComponents(); refreshData(); }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Top
        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel lblTitle = new JLabel("🧾 Quản lý Hóa đơn");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        topPanel.add(lblTitle, BorderLayout.WEST);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        txtSearch = new JTextField(20);
        txtSearch.putClientProperty("JTextField.placeholderText", "Tìm mã hóa đơn...");
        JButton btnSearch = new JButton("🔍 Tìm");
        btnSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSearch.addActionListener(e -> searchInvoices());
        txtSearch.addActionListener(e -> searchInvoices());
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { searchInvoices(); }
            public void removeUpdate(DocumentEvent e) { searchInvoices(); }
            public void changedUpdate(DocumentEvent e) { searchInvoices(); }
        });
        searchPanel.add(txtSearch); searchPanel.add(btnSearch);
        topPanel.add(searchPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // Split: invoices (top) + details (bottom)
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.6);

        String[] invCols = {"Mã HĐ", "Ngày", "Khách hàng", "Nhân viên", "Tạm tính", "Giảm giá", "Tổng", "Thanh toán", "Trạng thái"};
        invoiceModel = new DefaultTableModel(invCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        invoiceTable = new JTable(invoiceModel);
        invoiceTable.setRowHeight(28);
        invoiceTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        invoiceTable.getSelectionModel().addListSelectionListener(e -> loadInvoiceDetails());
        splitPane.setTopComponent(new JScrollPane(invoiceTable));

        String[] detCols = {"Mã SP", "Tên SP", "Đơn giá", "SL", "Thành tiền"};
        detailModel = new DefaultTableModel(detCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        detailTable = new JTable(detailModel);
        detailTable.setRowHeight(28);
        detailTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        JPanel detailPanel = new JPanel(new BorderLayout());
        JPanel detailHeaderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        detailHeaderPanel.add(new JLabel("  Chi tiết hóa đơn:"));
        JButton btnExportPdf = new JButton("📄 Xuất PDF");
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
                invoiceModel.addRow(new Object[]{
                    inv.getInvoiceCode(),
                    DateUtils.formatDateTime(inv.getCreatedAt()),
                    inv.getCustomerName() != null ? inv.getCustomerName() : "Khách vãng lai",
                    inv.getStaffName(),
                    CurrencyUtils.format(inv.getSubtotal()), CurrencyUtils.format(inv.getDiscountAmount()), CurrencyUtils.format(inv.getTotal()),
                    inv.getPaymentMethod() == null ? "—" : ("CASH".equals(inv.getPaymentMethod()) ? "Tiền mặt" : "Chuyển khoản"),
                    inv.getStatus()
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
        }
    }

    private void searchInvoices() {
        String keyword = txtSearch.getText().trim();
        if (keyword.isEmpty()) { refreshData(); return; }
        try {
            invoiceModel.setRowCount(0);
            List<InvoiceDTO> invoices = invoiceDAL.search(keyword, null, null, null);
            for (InvoiceDTO inv : invoices) {
                invoiceModel.addRow(new Object[]{
                    inv.getInvoiceCode(), DateUtils.formatDateTime(inv.getCreatedAt()),
                    inv.getCustomerName() != null ? inv.getCustomerName() : "Khách vãng lai",
                    inv.getStaffName(), CurrencyUtils.format(inv.getSubtotal()), CurrencyUtils.format(inv.getDiscountAmount()), CurrencyUtils.format(inv.getTotal()),
                    inv.getPaymentMethod() == null ? "—" : ("CASH".equals(inv.getPaymentMethod()) ? "Tiền mặt" : "Chuyển khoản"),
                    inv.getStatus()
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
        }
    }

    private void loadInvoiceDetails() {
        int row = invoiceTable.getSelectedRow();
        if (row < 0) return;
        try {
            String code = (String) invoiceModel.getValueAt(row, 0);
            // Find invoice by code to get ID — for simplicity, search all
            List<InvoiceDTO> all = invoiceDAL.search(code, null, null, null);
            if (all.isEmpty()) return;
            InvoiceDTO inv = all.get(0);

            detailModel.setRowCount(0);
            List<InvoiceDetailDTO> details = orderBLL.getInvoiceDetails(inv.getInvoiceId());
            for (InvoiceDetailDTO d : details) {
                detailModel.addRow(new Object[]{
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
            List<InvoiceDTO> all = invoiceDAL.search(code, null, null, null);
            if (all.isEmpty()) return;
            InvoiceDTO inv = all.get(0);

            if ("CANCELLED".equals(inv.getStatus())) {
                JOptionPane.showMessageDialog(this, "Không thể xuất PDF cho hóa đơn đã bị hủy.", "Thông báo", JOptionPane.WARNING_MESSAGE);
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
                if (!path.toLowerCase().endsWith(".pdf")) path += ".pdf";
                
                PdfExporter.exportInvoice(path, inv, details, customer);
                JOptionPane.showMessageDialog(this, "Đã xuất PDF thành công: " + path);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi xuất PDF: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}
