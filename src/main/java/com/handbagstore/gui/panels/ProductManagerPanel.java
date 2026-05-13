package com.handbagstore.gui.panels;

import com.handbagstore.bll.InventoryBLL;
import com.handbagstore.bll.ProductBLL;
import com.handbagstore.dto.InventoryLogDTO;
import com.handbagstore.dto.ProductDTO;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

public class ProductManagerPanel extends JPanel {
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField txtSearch;
    private JTextField txtCode, txtName, txtBrand, txtPrice, txtColor;
    private JComboBox<String> cmbStyle, cmbMaterial, cmbStatus;
    private JButton btnAdd, btnUpdate, btnDelete, btnClear;
    private final ProductBLL productBLL = new ProductBLL();
    private final InventoryBLL inventoryBLL = new InventoryBLL();

    public ProductManagerPanel() {
        initComponents();
        refreshData();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // === TOP: Title + Search ===
        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        JLabel lblTitle = new JLabel("📦 Quản lý Sản phẩm");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        topPanel.add(lblTitle, BorderLayout.WEST);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        txtSearch = new JTextField(20);
        txtSearch.putClientProperty("JTextField.placeholderText", "Tìm theo mã, tên, thương hiệu...");
        JButton btnSearch = new JButton("🔍 Tìm");
        btnSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSearch.addActionListener(e -> searchProducts());
        txtSearch.addActionListener(e -> searchProducts());
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { searchProducts(); }
            public void removeUpdate(DocumentEvent e) { searchProducts(); }
            public void changedUpdate(DocumentEvent e) { searchProducts(); }
        });
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);
        topPanel.add(searchPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // === CENTER: Table ===
        String[] columns = { "Mã SP", "Tên sản phẩm", "Thương hiệu", "Đơn giá", "Kiểu dáng",
                "Chất liệu", "Màu sắc", "Tồn kho", "Trạng thái" };
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(30);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> loadSelectedRow());

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // === BOTTOM: Form + Buttons ===
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));

        // Form panel
        JPanel formPanel = new JPanel(new GridLayout(4, 4, 10, 8));
        formPanel.setBorder(BorderFactory.createTitledBorder("Thông tin sản phẩm"));

        txtCode = new JTextField();
        txtName = new JTextField();
        txtBrand = new JTextField();
        txtPrice = new JTextField();
        txtPrice.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                if (!Character.isDigit(evt.getKeyChar())) evt.consume();
            }
        });
        txtColor = new JTextField();
        cmbStyle = new JComboBox<>(new String[] { "Tote", "Crossbody", "Backpack", "Clutch" });
        cmbMaterial = new JComboBox<>(new String[] { "Da thật", "PU", "Canvas" });
        cmbStatus = new JComboBox<>(new String[] { "ACTIVE", "INACTIVE" });
        cmbStatus.setEnabled(false);

        formPanel.add(new JLabel("Mã SP:"));
        formPanel.add(txtCode);
        formPanel.add(new JLabel("Tên:"));
        formPanel.add(txtName);
        formPanel.add(new JLabel("Thương hiệu:"));
        formPanel.add(txtBrand);
        formPanel.add(new JLabel("Đơn giá:"));
        formPanel.add(txtPrice);
        formPanel.add(new JLabel("Kiểu dáng:"));
        formPanel.add(cmbStyle);
        formPanel.add(new JLabel("Chất liệu:"));
        formPanel.add(cmbMaterial);
        formPanel.add(new JLabel("Màu sắc:"));
        formPanel.add(txtColor);
        formPanel.add(new JLabel("Trạng thái:"));
        formPanel.add(cmbStatus);

        bottomPanel.add(formPanel, BorderLayout.CENTER);

        // Button panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        btnAdd = new JButton("➕ Thêm");
        btnAdd.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnUpdate = new JButton("✏️ Sửa");
        btnUpdate.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnDelete = new JButton("🗑 Ngừng kinh doanh");
        btnDelete.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnClear = new JButton("🔄 Làm mới");
        btnClear.setFont(new Font("Segoe UI", Font.BOLD, 12));

        btnAdd.setBackground(new Color(40, 167, 69));
        btnAdd.setForeground(Color.WHITE);
        btnUpdate.setBackground(new Color(13, 110, 253));
        btnUpdate.setForeground(Color.WHITE);
        btnDelete.setBackground(new Color(220, 53, 69));
        btnDelete.setForeground(Color.WHITE);

        btnAdd.addActionListener(e -> addProduct());
        btnUpdate.addActionListener(e -> updateProduct());
        btnDelete.addActionListener(e -> deleteProduct());
        btnClear.addActionListener(e -> clearForm());

        btnPanel.add(btnAdd);
        btnPanel.add(btnUpdate);
        btnPanel.add(btnDelete);
        btnPanel.add(btnClear);
        bottomPanel.add(btnPanel, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void refreshData() {
        try {
            tableModel.setRowCount(0);
            List<ProductDTO> products = productBLL.getAll(true);
            for (ProductDTO p : products) {
                int stock = 0;
                try {
                    stock = inventoryBLL.getAvailableQuantity(p.getProductId());
                } catch (Exception ignored) {
                }

                tableModel.addRow(new Object[] {
                        p.getProductCode(), p.getName(), p.getBrand(),
                        p.getPrice(), p.getStyle(), p.getMaterial(), p.getColor(),
                        stock, p.getStatus()
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi tải dữ liệu: " + ex.getMessage());
        }
    }

    private void searchProducts() {
        String keyword = txtSearch.getText().trim();
        if (keyword.isEmpty()) {
            refreshData();
            return;
        }
        try {
            tableModel.setRowCount(0);
            List<ProductDTO> products = productBLL.search(keyword);
            for (ProductDTO p : products) {
                int stock = inventoryBLL.getAvailableQuantity(p.getProductId());
                tableModel.addRow(new Object[] {
                        p.getProductCode(), p.getName(), p.getBrand(),
                        p.getPrice(), p.getStyle(), p.getMaterial(), p.getColor(),
                        stock, p.getStatus()
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi tìm kiếm: " + ex.getMessage());
        }
    }

    private void loadSelectedRow() {
        int row = table.getSelectedRow();
        if (row < 0)
            return;
        txtCode.setText((String) tableModel.getValueAt(row, 0));
        txtName.setText((String) tableModel.getValueAt(row, 1));
        txtBrand.setText((String) tableModel.getValueAt(row, 2));
        txtPrice.setText(tableModel.getValueAt(row, 3).toString());
        cmbStyle.setSelectedItem(tableModel.getValueAt(row, 4));
        cmbMaterial.setSelectedItem(tableModel.getValueAt(row, 5));
        txtColor.setText((String) tableModel.getValueAt(row, 6));
        String status = (String) tableModel.getValueAt(row, 8);
        cmbStatus.setSelectedItem(status);

        if ("INACTIVE".equals(status)) {
            btnDelete.setText("🔄 Kinh doanh tiếp");
            btnDelete.setBackground(new Color(40, 167, 69));
        } else {
            btnDelete.setText("🗑 Ngừng kinh doanh");
            btnDelete.setBackground(new Color(220, 53, 69));
        }
    }

    private void addProduct() {
        if (txtName.getText().trim().isEmpty() ||
                txtBrand.getText().trim().isEmpty() || txtPrice.getText().trim().isEmpty() ||
                txtColor.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng điền đầy đủ thông tin sản phẩm.", "Thông báo",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            if (txtCode.getText().trim().isEmpty()) {
                txtCode.setText(productBLL.getNextProductCode());
            }
            ProductDTO p = getFormData();
            productBLL.addProduct(p);
            JOptionPane.showMessageDialog(this, "Thêm sản phẩm thành công!");
            clearForm();
            refreshData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateProduct() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn sản phẩm!");
            return;
        }
        try {
            String code = (String) tableModel.getValueAt(row, 0);
            ProductDTO existing = productBLL.getByCode(code);
            if (existing == null)
                return;

            ProductDTO p = getFormData();
            p.setProductId(existing.getProductId());
            productBLL.updateProduct(p);
            JOptionPane.showMessageDialog(this, "Cập nhật thành công!");
            refreshData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteProduct() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn sản phẩm!");
            return;
        }

        String status = (String) tableModel.getValueAt(row, 8);
        String msg = "ACTIVE".equals(status) ? "Ngừng kinh doanh sản phẩm này?" : "Tiếp tục kinh doanh sản phẩm này?";

        int confirm = JOptionPane.showConfirmDialog(this, msg, "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION)
            return;

        try {
            String code = (String) tableModel.getValueAt(row, 0);
            ProductDTO existing = productBLL.getByCode(code);
            if (existing != null) {
                if ("ACTIVE".equals(status)) {
                    productBLL.softDelete(existing.getProductId());
                } else {
                    productBLL.restore(existing.getProductId());
                }
            }
            refreshData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private ProductDTO getFormData() {
        ProductDTO p = new ProductDTO();
        p.setProductCode(txtCode.getText().trim());
        p.setName(txtName.getText().trim());
        p.setBrand(txtBrand.getText().trim());
        p.setPrice(new BigDecimal(txtPrice.getText().trim()));
        p.setStyle((String) cmbStyle.getSelectedItem());
        p.setMaterial((String) cmbMaterial.getSelectedItem());
        p.setColor(txtColor.getText().trim());
        p.setStatus((String) cmbStatus.getSelectedItem());
        return p;
    }

    private void clearForm() {
        txtCode.setText("");
        txtName.setText("");
        txtBrand.setText("");
        txtPrice.setText("");
        txtColor.setText("");
        cmbStyle.setSelectedIndex(0);
        cmbMaterial.setSelectedIndex(0);
        cmbStatus.setSelectedIndex(0);
        btnDelete.setText("🗑 Ngừng kinh doanh");
        btnDelete.setBackground(new Color(220, 53, 69));
        table.clearSelection();
    }
}
