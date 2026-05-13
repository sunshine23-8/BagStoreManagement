package com.handbagstore.gui.panels;

import com.handbagstore.bll.AccountBLL;
import com.handbagstore.bll.InventoryBLL;
import com.handbagstore.bll.ProductBLL;
import com.handbagstore.dto.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class InventoryPanel extends JPanel {
    private JTable stockTable, importTable;
    private DefaultTableModel stockModel, importModel;
    private JComboBox<ProductDTO> cmbProduct;
    private JTextField txtQty, txtCostPrice, txtNote;
    private JLabel lblLowStockWarning;
    private final InventoryBLL inventoryBLL = new InventoryBLL();
    private final ProductBLL productBLL = new ProductBLL();

    public InventoryPanel() {
        initComponents();
        refreshData();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel lblTitle = new JLabel("🏭 Quản lý Kho hàng");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        add(lblTitle, BorderLayout.NORTH);

        // Tabs: Tồn kho + Nhập kho
        JTabbedPane tabs = new JTabbedPane();

        // Tab 1: Tồn kho
        JPanel stockPanel = new JPanel(new BorderLayout(5, 5));
        lblLowStockWarning = new JLabel(" ");
        lblLowStockWarning.setForeground(new Color(220, 53, 69));
        lblLowStockWarning.setFont(new Font("Segoe UI", Font.BOLD, 13));
        stockPanel.add(lblLowStockWarning, BorderLayout.NORTH);

        String[] stockCols = {"Mã SP", "Tên SP", "Thương hiệu", "Tổng kho", "Đang giữ", "Khả dụng", "Giá vốn", "Giá bán"};
        stockModel = new DefaultTableModel(stockCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        stockTable = new JTable(stockModel);
        stockTable.setRowHeight(28);
        stockPanel.add(new JScrollPane(stockTable), BorderLayout.CENTER);
        tabs.addTab("📊 Tồn kho", stockPanel);

        // Tab 2: Nhập kho
        JPanel importPanel = new JPanel(new BorderLayout(5, 5));
        JPanel importForm = new JPanel(new GridLayout(2, 4, 10, 5));
        importForm.setBorder(BorderFactory.createTitledBorder("Nhập kho mới"));

        cmbProduct = new JComboBox<>();
        txtQty = new JTextField(); txtCostPrice = new JTextField(); txtNote = new JTextField();

        importForm.add(new JLabel("Sản phẩm:")); importForm.add(cmbProduct);
        importForm.add(new JLabel("Số lượng:")); importForm.add(txtQty);
        importForm.add(new JLabel("Giá vốn:")); importForm.add(txtCostPrice);
        importForm.add(new JLabel("Ghi chú:")); importForm.add(txtNote);

        JButton btnImport = new JButton("📥 Nhập kho");
        btnImport.setBackground(new Color(40, 167, 69)); btnImport.setForeground(Color.WHITE);
        btnImport.addActionListener(e -> importStock());

        JPanel importTop = new JPanel(new BorderLayout());
        importTop.add(importForm, BorderLayout.CENTER);
        importTop.add(btnImport, BorderLayout.EAST);
        importPanel.add(importTop, BorderLayout.NORTH);

        String[] importCols = {"Sản phẩm", "Số lượng", "Giá vốn", "Ngày nhập", "Người nhập", "Ghi chú"};
        importModel = new DefaultTableModel(importCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        importTable = new JTable(importModel);
        importTable.setRowHeight(28);
        importPanel.add(new JScrollPane(importTable), BorderLayout.CENTER);
        tabs.addTab("📥 Nhập kho", importPanel);

        add(tabs, BorderLayout.CENTER);
    }

    public void refreshData() {
        try {
            // Tồn kho
            stockModel.setRowCount(0);
            List<InventoryLogDTO> stocks = inventoryBLL.getAll();
            for (InventoryLogDTO s : stocks) {
                stockModel.addRow(new Object[]{
                    s.getProductCode(), s.getProductName(), s.getBrand(),
                    s.getQuantity(), s.getReservedQty(), s.getAvailableQty(),
                    s.getCostPrice(), s.getSellPrice()
                });
            }

            // Low stock warning
            List<InventoryLogDTO> lowStock = inventoryBLL.getLowStockItems();
            if (!lowStock.isEmpty()) {
                StringBuilder warn = new StringBuilder("⚠️ Sắp hết hàng: ");
                for (InventoryLogDTO ls : lowStock) {
                    warn.append(ls.getProductName()).append(" (").append(ls.getAvailableQty()).append("), ");
                }
                lblLowStockWarning.setText(warn.toString());
            } else {
                lblLowStockWarning.setText("✅ Tất cả sản phẩm đều đủ hàng");
                lblLowStockWarning.setForeground(new Color(40, 167, 69));
            }

            // Load product combo
            cmbProduct.removeAllItems();
            List<ProductDTO> products = productBLL.getAll(false);
            for (ProductDTO p : products) cmbProduct.addItem(p);

            // Import history
            importModel.setRowCount(0);
            List<ImportBatchDTO> imports = inventoryBLL.getImportHistory();
            for (ImportBatchDTO b : imports) {
                importModel.addRow(new Object[]{
                    b.getProductName(), b.getQuantity(), b.getCostPrice(),
                    b.getImportDate(), b.getCreatedByName(), b.getNote()
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
        }
    }

    private void importStock() {
        if (cmbProduct.getSelectedItem() == null || txtQty.getText().trim().isEmpty() || txtCostPrice.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng điền đầy đủ thông tin nhập kho.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            ProductDTO selected = (ProductDTO) cmbProduct.getSelectedItem();

            ImportBatchDTO batch = new ImportBatchDTO();
            batch.setProductId(selected.getProductId());
            batch.setQuantity(Integer.parseInt(txtQty.getText().trim()));
            batch.setCostPrice(new BigDecimal(txtCostPrice.getText().trim()));
            batch.setImportDate(LocalDate.now());
            batch.setNote(txtNote.getText().trim());
            batch.setCreatedBy(AccountBLL.getCurrentUser().getAccountId());

            inventoryBLL.importStock(batch);
            JOptionPane.showMessageDialog(this, "Nhập kho thành công!");
            txtQty.setText(""); txtCostPrice.setText(""); txtNote.setText("");
            refreshData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}
