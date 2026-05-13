package com.handbagstore.gui.panels;

import com.handbagstore.bll.*;
import com.handbagstore.dto.*;
import com.handbagstore.gui.components.*;
import com.handbagstore.utils.OrderTimerManager;
import com.handbagstore.utils.PdfExporter;
import com.handbagstore.utils.CurrencyUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel bán hàng chính cho Staff — chọn SP, nhập KH, apply voucher, thanh
 * toán/pending.
 */
public class SalePanel extends JPanel {
    private JTextField txtSearchProduct, txtCustomerPhone, txtPaymentReceived;
    private JComboBox<DiscountComboItem> cmbDiscount;
    private JTable productTable, cartTable;
    private DefaultTableModel productModel, cartModel;
    private JLabel lblSubtotal, lblDiscount, lblTotal, lblChange, lblCustomerInfo;
    private JComboBox<String> cmbPaymentMethod;
    private JSpinner spnQuantity;
    private JButton btnAddToCart, btnRemoveFromCart, btnApplyDiscount;
    private JButton btnPayNow, btnPending;

    private final ProductBLL productBLL = new ProductBLL();
    private final CustomerBLL customerBLL = new CustomerBLL();
    private final DiscountBLL discountBLL = new DiscountBLL();
    private final OrderBLL orderBLL = new OrderBLL();
    private DiscountDTO birthdayDiscount;
    private final InventoryBLL inventoryBLL = new InventoryBLL();

    private CustomerDTO selectedCustomer;
    private DiscountDTO selectedDiscount;
    private final List<InvoiceDetailDTO> cartItems = new ArrayList<>();
    private int lastInvoiceId = 0;

    class DiscountComboItem {
        DiscountDTO discount;
        String label;

        public DiscountComboItem(DiscountDTO discount, String label) {
            this.discount = discount;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    // Panel đơn pending
    private PendingOrdersWidget pendingWidget;

    public SalePanel() {
        initComponents();
        refreshAllData();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // === LEFT: Product list + search ===
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setPreferredSize(new Dimension(450, 0));

        JLabel lblProducts = new JLabel("📦 Danh sách sản phẩm");
        lblProducts.setFont(new Font("Segoe UI", Font.BOLD, 14));
        leftPanel.add(lblProducts, BorderLayout.NORTH);

        // Search
        JPanel searchPanel = new JPanel(new BorderLayout(5, 0));
        txtSearchProduct = new JTextField();
        txtSearchProduct.putClientProperty("JTextField.placeholderText", "Tìm theo mã, tên, thương hiệu...");
        txtSearchProduct.addActionListener(e -> searchProducts());
        txtSearchProduct.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                searchProducts();
            }

            public void removeUpdate(DocumentEvent e) {
                searchProducts();
            }

            public void changedUpdate(DocumentEvent e) {
                searchProducts();
            }
        });
        searchPanel.add(txtSearchProduct, BorderLayout.CENTER);
        JButton btnSearch = new JButton("🔍 Tìm");
        btnSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSearch.addActionListener(e -> searchProducts());
        searchPanel.add(btnSearch, BorderLayout.EAST);

        JPanel topLeft = new JPanel(new BorderLayout(0, 5));
        topLeft.add(lblProducts, BorderLayout.NORTH);
        topLeft.add(searchPanel, BorderLayout.SOUTH);
        leftPanel.add(topLeft, BorderLayout.NORTH);

        // Product table
        String[] productCols = { "Mã SP", "Tên", "Giá", "Tồn kho" };
        productModel = new DefaultTableModel(productCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        productTable = new JTable(productModel);
        productTable.setRowHeight(28);
        productTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        productTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    showProductDetails();
                }
            }
        });
        JScrollPane productScrollPane = new JScrollPane(productTable);
        productScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        leftPanel.add(productScrollPane, BorderLayout.CENTER);

        // Add to cart
        JPanel addPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addPanel.add(new JLabel("SL:"));
        spnQuantity = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
        addPanel.add(spnQuantity);

        productTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                spnQuantity.setValue(1);
            }
        });

        btnAddToCart = new JButton("➕ Thêm vào đơn");
        btnAddToCart.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAddToCart.setBackground(new Color(40, 167, 69));
        btnAddToCart.setForeground(Color.WHITE);
        btnAddToCart.addActionListener(e -> addToCart());
        addPanel.add(btnAddToCart);
        leftPanel.add(addPanel, BorderLayout.SOUTH);

        add(leftPanel, BorderLayout.WEST);

        // === CENTER: Cart + Payment ===
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));

        JPanel cartHeaderPanel = new JPanel(new BorderLayout());
        JLabel lblCart = new JLabel("🛒 Đơn hàng hiện tại");
        lblCart.setFont(new Font("Segoe UI", Font.BOLD, 14));
        cartHeaderPanel.add(lblCart, BorderLayout.WEST);

        btnRemoveFromCart = new JButton("Xóa sản phẩm");
        btnRemoveFromCart.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRemoveFromCart.setBackground(new Color(220, 53, 69));
        btnRemoveFromCart.setForeground(Color.WHITE);
        btnRemoveFromCart.addActionListener(e -> removeFromCart());
        cartHeaderPanel.add(btnRemoveFromCart, BorderLayout.EAST);

        centerPanel.add(cartHeaderPanel, BorderLayout.NORTH);

        // Cart table
        String[] cartCols = { "Mã SP", "Tên SP", "Đơn giá", "SL", "Thành tiền" };
        cartModel = new DefaultTableModel(cartCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        cartTable = new JTable(cartModel);
        cartTable.setRowHeight(28);
        cartTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        centerPanel.add(new JScrollPane(cartTable), BorderLayout.CENTER);

        // Payment section
        JPanel paymentPanel = new JPanel(new GridBagLayout());
        paymentPanel.setBorder(BorderFactory.createTitledBorder("Thanh toán"));
        GridBagConstraints gbcP = new GridBagConstraints();
        gbcP.fill = GridBagConstraints.HORIZONTAL;
        gbcP.insets = new Insets(4, 8, 4, 8);
        gbcP.anchor = GridBagConstraints.WEST;

        // Row 1: Customer Phone
        gbcP.gridx = 0;
        gbcP.gridy = 0;
        gbcP.weightx = 0;
        paymentPanel.add(new JLabel("SĐT Khách:"), gbcP);

        JPanel customerInputPanel = new JPanel(new BorderLayout(0, 2));
        txtCustomerPhone = new JTextField(12);
        txtCustomerPhone.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                lookupCustomer();
            }

            public void removeUpdate(DocumentEvent e) {
                lookupCustomer();
            }

            public void changedUpdate(DocumentEvent e) {
                lookupCustomer();
            }
        });
        customerInputPanel.add(txtCustomerPhone, BorderLayout.NORTH);
        lblCustomerInfo = new JLabel(" ");
        lblCustomerInfo.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        customerInputPanel.add(lblCustomerInfo, BorderLayout.SOUTH);

        gbcP.gridx = 1;
        gbcP.weightx = 1.0;
        paymentPanel.add(customerInputPanel, gbcP);

        // Row 2: Discount
        gbcP.gridx = 0;
        gbcP.gridy = 1;
        gbcP.weightx = 0;
        paymentPanel.add(new JLabel("Mã giảm giá:"), gbcP);

        JPanel discountInputPanel = new JPanel(new BorderLayout(5, 0));
        cmbDiscount = new JComboBox<>();
        cmbDiscount.addItem(new DiscountComboItem(null, "--- Chọn mã giảm giá ---"));
        // ... (populate discounts - same logic)
        try {
            List<DiscountDTO> activeDiscounts = discountBLL.getActive();
            for (DiscountDTO d : activeDiscounts) {
                if ("BIRTHDAY".equals(d.getOccasion())) continue; // Bỏ qua mã sinh nhật trong dropdown
                String label = d.getCode() + " ("
                        + (d.isPercentType() ? d.getValue() + "%" : CurrencyUtils.format(d.getValue())) + ")";
                cmbDiscount.addItem(new DiscountComboItem(d, label));
            }
        } catch (Exception ignored) {
        }

        cmbDiscount.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof DiscountComboItem) {
                    DiscountDTO d = ((DiscountComboItem) value).discount;
                    if (d != null) {
                        String tooltip = "<html>Mã: <b>" + d.getCode() + "</b><br>" +
                                "Giảm: " + (d.isPercentType() ? d.getValue() + "%" : CurrencyUtils.format(d.getValue()))
                                + "<br>" +
                                "Đơn tối thiểu: " + CurrencyUtils.format(d.getMinOrderAmt()) + "<br>" +
                                "Từ: " + com.handbagstore.utils.DateUtils.formatDateTime(d.getStartTime()) + "<br>" +
                                "Đến: " + com.handbagstore.utils.DateUtils.formatDateTime(d.getEndTime()) + "</html>";
                        ((JComponent) c).setToolTipText(tooltip);
                    }
                }
                return c;
            }
        });
        cmbDiscount.addActionListener(e -> applyDiscount());
        discountInputPanel.add(cmbDiscount, BorderLayout.CENTER);

        gbcP.gridx = 1;
        gbcP.weightx = 1.0;
        paymentPanel.add(discountInputPanel, gbcP);

        // Row 3: Subtotal
        gbcP.gridx = 0;
        gbcP.gridy = 2;
        gbcP.weightx = 0;
        paymentPanel.add(new JLabel("Tạm tính:"), gbcP);
        lblSubtotal = new JLabel("0đ");
        gbcP.gridx = 1;
        gbcP.weightx = 1.0;
        paymentPanel.add(lblSubtotal, gbcP);

        // Row 4: Discount Amount
        gbcP.gridx = 0;
        gbcP.gridy = 3;
        gbcP.weightx = 0;
        paymentPanel.add(new JLabel("Giảm giá:"), gbcP);
        lblDiscount = new JLabel("0đ");
        gbcP.gridx = 1;
        gbcP.weightx = 1.0;
        paymentPanel.add(lblDiscount, gbcP);

        // Row 5: Total
        gbcP.gridx = 0;
        gbcP.gridy = 4;
        gbcP.weightx = 0;
        paymentPanel.add(new JLabel("TỔNG CỘNG:"), gbcP);
        lblTotal = new JLabel("0đ");
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 16));
        gbcP.gridx = 1;
        gbcP.weightx = 1.0;
        paymentPanel.add(lblTotal, gbcP);

        // Row 6: Payment Method
        gbcP.gridx = 0;
        gbcP.gridy = 5;
        gbcP.weightx = 0;
        paymentPanel.add(new JLabel("Thanh toán:"), gbcP);
        cmbPaymentMethod = new JComboBox<>(new String[] { "Tiền mặt", "Chuyển khoản" });
        gbcP.gridx = 1;
        gbcP.weightx = 1.0;
        paymentPanel.add(cmbPaymentMethod, gbcP);

        // Row 7: Received
        gbcP.gridx = 0;
        gbcP.gridy = 6;
        gbcP.weightx = 0;
        paymentPanel.add(new JLabel("Tiền nhận:"), gbcP);

        txtPaymentReceived = new JTextField();
        txtPaymentReceived.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { calculateChange(); }
            public void removeUpdate(DocumentEvent e) { calculateChange(); }
            public void changedUpdate(DocumentEvent e) { calculateChange(); }
        });
        gbcP.gridx = 1;
        gbcP.weightx = 1.0;
        paymentPanel.add(txtPaymentReceived, gbcP);

        // Row 8: Change
        gbcP.gridx = 0;
        gbcP.gridy = 7;
        gbcP.weightx = 0;
        paymentPanel.add(new JLabel("Tiền thối:"), gbcP);

        lblChange = new JLabel("0đ");
        lblChange.setFont(new Font("Segoe UI", Font.BOLD, 14));
        gbcP.gridx = 1;
        gbcP.weightx = 1.0;
        paymentPanel.add(lblChange, gbcP);

        cmbPaymentMethod.addActionListener(e -> {
            boolean isCash = cmbPaymentMethod.getSelectedIndex() == 0;
            txtPaymentReceived.setEnabled(isCash);
            if (!isCash) {
                txtPaymentReceived.setText("");
                lblChange.setText("0đ");
            }
            calculateChange();
        });

        // Action buttons
        JPanel actionRow = new JPanel(new GridLayout(1, 2, 5, 5));
        actionRow.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        btnPayNow = new JButton("💰 Thanh toán ngay");
        btnPayNow.setBackground(new Color(40, 167, 69));
        btnPayNow.setForeground(Color.WHITE);
        btnPayNow.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnPayNow.addActionListener(e -> payNow());

        btnPending = new JButton("⏳ Giữ đơn (Pending)");
        btnPending.setBackground(new Color(13, 110, 253));
        btnPending.setForeground(Color.WHITE);
        btnPending.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnPending.addActionListener(e -> createPending());

        actionRow.add(btnPayNow);
        actionRow.add(btnPending);

        gbcP.gridx = 0;
        gbcP.gridy = 8;
        gbcP.gridwidth = 2;
        paymentPanel.add(actionRow, gbcP);

        centerPanel.add(paymentPanel, BorderLayout.SOUTH);
        add(centerPanel, BorderLayout.CENTER);

        // === RIGHT: Pending Orders ===
        pendingWidget = new PendingOrdersWidget(this::refreshAllData, this::loadPendingOrder);
        pendingWidget.setPreferredSize(new Dimension(280, 0));
        add(pendingWidget, BorderLayout.EAST);
    }

    // ==================== Logic Methods ====================

    private String formatCurrency(BigDecimal amount) {
        return CurrencyUtils.format(amount);
    }

    private void showProductDetails() {
        int row = productTable.getSelectedRow();
        if (row < 0)
            return;
        try {
            String code = (String) productModel.getValueAt(row, 0);
            ProductDTO product = productBLL.getByCode(code);
            if (product != null) {
                showProductDetails(product);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi khi lấy chi tiết sản phẩm: " + ex.getMessage(), "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showProductDetails(ProductDTO product) {
        try {
            int stock = inventoryBLL.getAvailableQuantity(product.getProductId());

            // Create custom dialog
            JDialog dialog = new JDialog((Window) SwingUtilities.getWindowAncestor(this),
                    "Chi tiết sản phẩm - " + product.getProductCode());
            dialog.setModal(true);
            dialog.setLayout(new BorderLayout(10, 10));
            dialog.setSize(450, 450);
            dialog.setLocationRelativeTo(this);

            // Title panel with accent color
            JPanel titlePanel = new JPanel();
            titlePanel.setBackground(new Color(52, 152, 219));
            titlePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            JLabel lblTitle = new JLabel("THÔNG TIN CHI TIẾT SẢN PHẨM");
            lblTitle.setForeground(Color.WHITE);
            lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
            titlePanel.add(lblTitle);
            dialog.add(titlePanel, BorderLayout.NORTH);

            // Content panel using GridBagLayout for alignment
            JPanel contentPanel = new JPanel(new GridBagLayout());
            contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(8, 5, 8, 5);

            String[] labels = {
                    "Mã sản phẩm:", "Tên sản phẩm:", "Thương hiệu:",
                    "Mức giá:", "Kiểu dáng:", "Chất liệu:",
                    "Màu sắc:", "Số lượng tồn:", "Trạng thái:"
            };
            String[] values = {
                    product.getProductCode(),
                    product.getName(),
                    product.getBrand(),
                    formatCurrency(product.getPrice()),
                    product.getStyle(),
                    product.getMaterial(),
                    product.getColor(),
                    String.valueOf(stock),
                    product.getStatus()
            };

            for (int i = 0; i < labels.length; i++) {
                gbc.gridy = i;

                // Label
                gbc.gridx = 0;
                gbc.weightx = 0.3;
                JLabel lbl = new JLabel(labels[i]);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
                contentPanel.add(lbl, gbc);

                // Value
                gbc.gridx = 1;
                gbc.weightx = 0.7;
                JLabel val = new JLabel(values[i]);
                val.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                contentPanel.add(val, gbc);
            }

            dialog.add(new JScrollPane(contentPanel), BorderLayout.CENTER);

            // Button panel
            JPanel btnPanel = new JPanel();
            btnPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
            JButton btnClose = new JButton("Đóng");
            btnClose.setFont(new Font("Segoe UI", Font.BOLD, 12));
            btnClose.addActionListener(e -> dialog.dispose());
            btnPanel.add(btnClose);
            dialog.add(btnPanel, BorderLayout.SOUTH);

            dialog.setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi khi lấy chi tiết sản phẩm: " + ex.getMessage(), "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshProductList() {
        try {
            productModel.setRowCount(0);
            List<ProductDTO> products = productBLL.getAll(false);
            for (ProductDTO p : products) {
                int stock = inventoryBLL.getAvailableQuantity(p.getProductId());
                productModel
                        .addRow(new Object[] { p.getProductCode(), p.getName(), formatCurrency(p.getPrice()), stock });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
        }
    }

    private void refreshAllData() {
        refreshProductList();
        Window win = SwingUtilities.getWindowAncestor(this);
        if (win instanceof com.handbagstore.gui.MainStaffFrame) {
            ((com.handbagstore.gui.MainStaffFrame) win).refreshInvoicePanel();
        }
    }

    public void loadPendingOrder(InvoiceDTO inv) {
        try {
            if (!cartItems.isEmpty()) {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Giỏ hàng hiện tại đang có sản phẩm. Bạn có muốn xóa giỏ hàng để tải đơn hàng chờ này không?",
                        "Xác nhận", JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION)
                    return;
            }

            clearCart(true);

            // Load customer
            if (inv.getCustomerId() != null) {
                CustomerDTO customer = customerBLL.getById(inv.getCustomerId());
                if (customer != null) {
                    txtCustomerPhone.setText(customer.getPhone());
                    lookupCustomer();
                }
            }

            // Load items
            List<InvoiceDetailDTO> details = orderBLL.getInvoiceDetails(inv.getInvoiceId());
            cartItems.addAll(details);

            // Load discount
            if (inv.getDiscountId() != null) {
                for (int i = 1; i < cmbDiscount.getItemCount(); i++) {
                    DiscountComboItem item = cmbDiscount.getItemAt(i);
                    if (item != null && item.discount != null
                            && item.discount.getDiscountId() == inv.getDiscountId()) {
                        cmbDiscount.setSelectedIndex(i);
                        break;
                    }
                }
            }

            // Delete the pending order (REMOVE from DB, but KEEP stock deducted because it's now in active cart)
            orderBLL.deletePendingOrder(inv.getInvoiceId(), false);

            updateCartDisplay();
            refreshAllData();
            pendingWidget.refreshData();

            JOptionPane.showMessageDialog(this, "Đã tải đơn hàng " + inv.getInvoiceCode() + " vào giỏ hàng.");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi khi tải đơn hàng: " + ex.getMessage(), "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void searchProducts() {
        String keyword = txtSearchProduct.getText().trim();
        if (keyword.isEmpty()) {
            refreshProductList();
            return;
        }
        try {
            productModel.setRowCount(0);
            for (ProductDTO p : productBLL.search(keyword, false)) {
                int stock = inventoryBLL.getAvailableQuantity(p.getProductId());
                productModel
                        .addRow(new Object[] { p.getProductCode(), p.getName(), formatCurrency(p.getPrice()), stock });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
        }
    }

    private void doAddToCart(ProductDTO product, int qty) {
        try {
            if (!inventoryBLL.checkAvailability(product.getProductId(), qty)) {
                JOptionPane.showMessageDialog(this, "Không đủ hàng trong kho cho sản phẩm: " + product.getName() + "!");
                return;
            }

            // Check nếu đã có trong cart → cộng dồn
            for (InvoiceDetailDTO d : cartItems) {
                if (d.getProductId() == product.getProductId()) {
                    inventoryBLL.updateQuantity(product.getProductId(), -qty);
                    d.setQuantity(d.getQuantity() + qty);
                    updateCartDisplay();
                    refreshProductList();
                    return;
                }
            }

            inventoryBLL.updateQuantity(product.getProductId(), -qty);
            InvoiceDetailDTO detail = new InvoiceDetailDTO();
            detail.setProductId(product.getProductId());
            detail.setProductCode(product.getProductCode());
            detail.setProductName(product.getName());
            detail.setUnitPrice(product.getPrice());
            detail.setQuantity(qty);
            cartItems.add(detail);
            updateCartDisplay();
            refreshProductList();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
        }
    }

    private void addToCart() {
        int row = productTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Chọn sản phẩm!");
            return;
        }
        try {
            String code = (String) productModel.getValueAt(row, 0);
            int qty = (int) spnQuantity.getValue();
            ProductDTO product = productBLL.getByCode(code);
            if (product != null) {
                doAddToCart(product, qty);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
        }
    }

    private void removeFromCart() {
        int row = cartTable.getSelectedRow();
        if (row < 0)
            return;
        InvoiceDetailDTO item = cartItems.get(row);
        try {
            inventoryBLL.updateQuantity(item.getProductId(), item.getQuantity());
        } catch (Exception ex) {
            System.err.println("Lỗi trả hàng về kho: " + ex.getMessage());
        }
        cartItems.remove(row);
        updateCartDisplay();
        refreshProductList();
    }

    private void updateCartDisplay() {
        cartModel.setRowCount(0);
        BigDecimal subtotal = BigDecimal.ZERO;
        for (InvoiceDetailDTO d : cartItems) {
            cartModel.addRow(new Object[] {
                    d.getProductCode(), d.getProductName(),
                    formatCurrency(d.getUnitPrice()), d.getQuantity(), formatCurrency(d.getLineTotal())
            });
            subtotal = subtotal.add(d.getLineTotal());
        }
        lblSubtotal.setText(formatCurrency(subtotal));

        BigDecimal discountAmt = getDiscountAmount(subtotal);
        lblDiscount.setText("-" + formatCurrency(discountAmt));

        BigDecimal total = subtotal.subtract(discountAmt);
        if (total.compareTo(BigDecimal.ZERO) < 0)
            total = BigDecimal.ZERO;
        lblTotal.setText(formatCurrency(total));
    }

    private void lookupCustomer() {
        String rawPhone = txtCustomerPhone.getText();
        String phone = rawPhone.trim().replaceAll("[^0-9]", "");
        if (phone.isEmpty()) {
            selectedCustomer = null;
            birthdayDiscount = null;
            lblCustomerInfo.setText(" ");
            Container parent = lblCustomerInfo.getParent();
            if (parent != null) {
                parent.revalidate();
                parent.repaint();
            }
            return;
        }
        try {
            selectedCustomer = customerBLL.getByPhone(phone);
            if (selectedCustomer == null) {
                lblCustomerInfo.setText("Không tìm thấy — khách vãng lai");
                lblCustomerInfo.setForeground(new Color(166, 173, 186));
                if (phone.length() == 10) {
                    SwingUtilities.invokeLater(() -> {
                        int choice = JOptionPane.showConfirmDialog(this,
                                "Khách hàng này chưa có trong hệ thống.\nBạn có muốn đăng ký khách hàng mới không?",
                                "Đăng ký thành viên", JOptionPane.YES_NO_OPTION);
                        if (choice == JOptionPane.YES_OPTION) {
                            showRegisterCustomerDialog(phone);
                        }
                    });
                }
            } else {
                String bday = selectedCustomer.getBirthday() != null
                        ? com.handbagstore.utils.DateUtils.formatDate(selectedCustomer.getBirthday())
                        : "N/A";
                String info = selectedCustomer.getFullName() + " (" + bday + ")";
                if (com.handbagstore.utils.DateUtils.isBirthdayToday(selectedCustomer.getBirthday())) {
                    info += " 🎂 Sinh nhật hôm nay!";
                    lblCustomerInfo.setForeground(new Color(255, 193, 7));
                    applyAutomaticBirthdayDiscount();
                } else {
                    lblCustomerInfo.setForeground(new Color(40, 167, 69)); // Green for found
                }
                lblCustomerInfo.setText(info);
            }
        } catch (Exception ex) {
            lblCustomerInfo.setText("Lỗi: " + ex.getMessage());
            lblCustomerInfo.setForeground(new Color(220, 53, 69)); // Red for error
        }

        Container parent = lblCustomerInfo.getParent();
        if (parent != null) {
            parent.revalidate();
            parent.repaint();
        }
    }

    private void applyAutomaticBirthdayDiscount() {
        try {
            List<DiscountDTO> birthdayDiscounts = discountBLL.getActiveBirthdayDiscounts();
            if (!birthdayDiscounts.isEmpty()) {
                birthdayDiscount = birthdayDiscounts.get(0);
                JOptionPane.showMessageDialog(this,
                        "🎂 Hôm nay là sinh nhật khách hàng!\nMã giảm giá sinh nhật '" + birthdayDiscount.getCode() + "' đã được tự động áp dụng.",
                        "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                updateCartDisplay();
            }
        } catch (Exception ignored) {
        }
    }

    private void applyDiscount() {
        if (cmbDiscount == null || cmbDiscount.getSelectedIndex() <= 0) {
            selectedDiscount = null;
            updateCartDisplay();
            return;
        }
        Object selectedObj = cmbDiscount.getSelectedItem();
        if (!(selectedObj instanceof DiscountComboItem))
            return;
        DiscountComboItem item = (DiscountComboItem) selectedObj;
        String code = item.discount.getCode();
        try {
            selectedDiscount = discountBLL.validateCode(code);
            // Check min order amount
            BigDecimal subtotal = getSubtotal();
            BigDecimal needed = discountBLL.getAmountNeeded(selectedDiscount, subtotal);
            if (needed.compareTo(BigDecimal.ZERO) > 0) {
                JOptionPane.showMessageDialog(this,
                        "Cần mua thêm " + formatCurrency(needed) + " để áp dụng mã này.\nGợi ý sản phẩm mua thêm:");
                // Show upsell suggestions
                List<ProductDTO> suggestions = productBLL.getUpsellSuggestions(subtotal,
                        selectedDiscount.getMinOrderAmt());
                showUpsellDialog(suggestions, needed);
            }
            updateCartDisplay();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi mã giảm giá", JOptionPane.ERROR_MESSAGE);
            selectedDiscount = null;
            cmbDiscount.setSelectedIndex(0);
        }
    }

    private void showUpsellDialog(List<ProductDTO> suggestions, BigDecimal needed) {
        if (suggestions.isEmpty())
            return;

        JDialog dialog = new JDialog((Window) SwingUtilities.getWindowAncestor(this), "Gợi ý sản phẩm mua thêm");
        dialog.setModal(true);
        dialog.setSize(600, 450);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(52, 152, 219));
        JLabel lblHeader = new JLabel("Để được giảm giá, cần mua thêm ≥ " + formatCurrency(needed));
        lblHeader.setForeground(Color.WHITE);
        lblHeader.setFont(new Font("Segoe UI", Font.BOLD, 14));
        headerPanel.add(lblHeader);
        dialog.add(headerPanel, BorderLayout.NORTH);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        int count = 0;
        for (ProductDTO p : suggestions) {
            if (count++ >= 10)
                break;

            JPanel itemPanel = new JPanel(new BorderLayout(5, 5));
            itemPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)));

            int stock = 0;
            try {
                stock = inventoryBLL.getAvailableQuantity(p.getProductId());
            } catch (Exception ignored) {
            }

            itemPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            itemPanel.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    showProductDetails(p);
                }

                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    itemPanel.setBackground(new Color(240, 240, 240));
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    itemPanel.setBackground(null);
                }
            });

            String info = String.format(
                    "<html><b>%s</b> - %s<br><font color='#e74c3c'>%s</font> | Tồn kho: %d | %s</html>",
                    p.getProductCode(), p.getName(), formatCurrency(p.getPrice()), stock, p.getBrand());
            JLabel lblInfo = new JLabel(info);
            lblInfo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            itemPanel.add(lblInfo, BorderLayout.CENTER);

            JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            actionPanel.setOpaque(false);
            JSpinner spnQty = new JSpinner(new SpinnerNumberModel(1, 1, stock > 0 ? stock : 1, 1));
            actionPanel.add(spnQty);

            JButton btnAdd = new JButton("Thêm vào đơn");
            btnAdd.setBackground(new Color(40, 167, 69));
            btnAdd.setForeground(Color.WHITE);
            btnAdd.setFont(new Font("Segoe UI", Font.BOLD, 12));
            btnAdd.addActionListener(e -> {
                doAddToCart(p, (int) spnQty.getValue());
                JOptionPane.showMessageDialog(dialog, "Đã thêm " + p.getName() + " vào đơn hàng!");
            });
            actionPanel.add(btnAdd);
            itemPanel.add(actionPanel, BorderLayout.EAST);

            listPanel.add(itemPanel);
        }

        JScrollPane scrollPane = new JScrollPane(listPanel);
        dialog.add(scrollPane, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        JButton btnClose = new JButton("Đóng");
        btnClose.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnClose.addActionListener(e -> dialog.dispose());
        btnPanel.add(btnClose);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void calculateChange() {
        try {
            BigDecimal total = getTotal();
            String input = txtPaymentReceived.getText().trim().replaceAll("[^0-9]", "");
            if (input.isEmpty()) {
                lblChange.setText("0đ");
                return;
            }
            BigDecimal received = new BigDecimal(input);
            BigDecimal change = received.subtract(total);
            lblChange.setText(change.compareTo(BigDecimal.ZERO) >= 0 ? formatCurrency(change)
                    : "Thiếu " + formatCurrency(change.abs()));
            lblChange.setForeground(change.compareTo(BigDecimal.ZERO) >= 0
                    ? new Color(40, 167, 69)
                    : new Color(220, 53, 69));
        } catch (Exception ex) {
            lblChange.setText("—");
        }
    }

    private void payNow() {
        if (cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Giỏ hàng trống!");
            return;
        }
        try {
            boolean isCash = cmbPaymentMethod.getSelectedIndex() == 0;
            BigDecimal received = BigDecimal.ZERO;
            BigDecimal change = BigDecimal.ZERO;

            if (isCash) {
                String input = txtPaymentReceived.getText().trim().replaceAll("[^0-9]", "");
                if (input.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Nhập số tiền khách đưa!");
                    return;
                }
                received = new BigDecimal(input);
                BigDecimal total = getTotal();
                if (received.compareTo(total) < 0) {
                    JOptionPane.showMessageDialog(this, "Tiền khách đưa chưa đủ!");
                    return;
                }
                change = received.subtract(total);
            }

            InvoiceDTO invoice = buildInvoice(isCash ? "CASH" : "TRANSFER");
            invoice.setPaymentMethod(isCash ? "CASH" : "TRANSFER");
            invoice.setPaymentReceived(received);
            invoice.setChangeAmount(change);

            int invoiceId = orderBLL.createAndPayOrder(invoice, new ArrayList<>(cartItems));
            lastInvoiceId = invoiceId;

            String changeMsg = isCash ? "\nTiền thối: " + formatCurrency(change) : "";

            Object[] options = { "Đóng", "📄 Xuất PDF" };
            int choice = JOptionPane.showOptionDialog(this,
                    "✅ Thanh toán thành công!\nMã HĐ: " + invoice.getInvoiceCode() + changeMsg,
                    "Thanh toán",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null,
                    options,
                    options[0]);

            clearCart(false);
            refreshAllData();

            if (choice == 1) {
                exportPdf();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createPending() {
        if (cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Giỏ hàng trống!");
            return;
        }
        try {
            InvoiceDTO invoice = buildInvoice("PENDING");
            int invoiceId = orderBLL.createPendingOrder(invoice, new ArrayList<>(cartItems), () -> {
                SwingUtilities.invokeLater(() -> {
                    pendingWidget.refreshData();
                    refreshAllData();
                });
            });

            JOptionPane.showMessageDialog(this,
                    "⏳ Đơn hàng đang chờ thanh toán!\nMã HĐ: " + invoice.getInvoiceCode());

            clearCart(false);
            refreshAllData();
            pendingWidget.refreshData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private InvoiceDTO buildInvoice(String status) {
        InvoiceDTO inv = new InvoiceDTO();
        inv.setStaffId(AccountBLL.getCurrentUser().getAccountId());
        inv.setCustomerId(selectedCustomer != null ? selectedCustomer.getCustomerId() : null);
        inv.setSubtotal(getSubtotal());

        BigDecimal discountAmt = getDiscountAmount(getSubtotal());
        
        inv.setDiscountAmount(discountAmt);
        inv.setTotal(getSubtotal().subtract(discountAmt).max(BigDecimal.ZERO));
        
        if (selectedDiscount != null) {
            inv.setDiscountId(selectedDiscount.getDiscountId());
        } else if (birthdayDiscount != null) {
            inv.setDiscountId(birthdayDiscount.getDiscountId());
        }
        
        StringBuilder codes = new StringBuilder();
        if (birthdayDiscount != null) codes.append("Sinh nhật");
        if (selectedDiscount != null) {
            if (codes.length() > 0) codes.append(" + ");
            codes.append(selectedDiscount.getCode());
        }
        if (codes.length() > 0) {
            inv.setDiscountCode(codes.toString());
        }
        inv.setStatus(status);
        return inv;
    }

    private BigDecimal getSubtotal() {
        BigDecimal sum = BigDecimal.ZERO;
        for (InvoiceDetailDTO d : cartItems)
            sum = sum.add(d.getLineTotal());
        return sum;
    }

    private BigDecimal getTotal() {
        BigDecimal subtotal = getSubtotal();
        BigDecimal disc = getDiscountAmount(subtotal);
        return subtotal.subtract(disc).max(BigDecimal.ZERO);
    }

    private BigDecimal getDiscountAmount(BigDecimal subtotal) {
        BigDecimal discountAmt = BigDecimal.ZERO;
        if (selectedDiscount != null && birthdayDiscount != null) {
            if (selectedDiscount.isStackable() || birthdayDiscount.isStackable()) {
                discountAmt = discountBLL.calculateDiscount(selectedDiscount, subtotal)
                        .add(discountBLL.calculateDiscount(birthdayDiscount, subtotal));
            } else {
                discountAmt = discountBLL.calculateDiscount(selectedDiscount, subtotal);
            }
        } else if (selectedDiscount != null) {
            discountAmt = discountBLL.calculateDiscount(selectedDiscount, subtotal);
        } else if (birthdayDiscount != null) {
            discountAmt = discountBLL.calculateDiscount(birthdayDiscount, subtotal);
        }
        return discountAmt;
    }

    private void exportPdf() {
        if (lastInvoiceId == 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng thanh toán hóa đơn trước khi xuất PDF.");
            return;
        }
        try {
            InvoiceDTO inv = orderBLL.getInvoiceById(lastInvoiceId);
            if (inv == null)
                return;
            List<InvoiceDetailDTO> details = orderBLL.getInvoiceDetails(lastInvoiceId);
            CustomerDTO customer = null;
            if (inv.getCustomerId() != null && inv.getCustomerId() > 0) {
                customer = customerBLL.getById(inv.getCustomerId());
            }

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Lưu PDF Hóa Đơn");
            fileChooser.setSelectedFile(new java.io.File("HoaDon_" + inv.getInvoiceCode() + ".pdf"));

            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                String path = fileChooser.getSelectedFile().getAbsolutePath();
                if (!path.toLowerCase().endsWith(".pdf"))
                    path += ".pdf";

                PdfExporter.exportInvoice(path, inv, details, customer);
                JOptionPane.showMessageDialog(this, "Đã xuất PDF thành công: " + path);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi xuất PDF: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearCart(boolean returnStock) {
        if (returnStock) {
            for (InvoiceDetailDTO d : cartItems) {
                try {
                    inventoryBLL.updateQuantity(d.getProductId(), d.getQuantity());
                } catch (Exception ignored) {
                }
            }
        }
        cartItems.clear();
        selectedCustomer = null;
        selectedDiscount = null;
        txtCustomerPhone.setText("");
        if (cmbDiscount != null && cmbDiscount.getItemCount() > 0) {
            cmbDiscount.setSelectedIndex(0);
        }
        txtPaymentReceived.setText("");
        lblCustomerInfo.setText(" ");
        lblChange.setText("0đ");
        spnQuantity.setValue(1);
        updateCartDisplay();
        refreshProductList();
    }

    private void showRegisterCustomerDialog(String phone) {
        JDialog dialog = new JDialog((Window) SwingUtilities.getWindowAncestor(this), "Đăng ký khách hàng mới");
        dialog.setModal(true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(400, 280);
        dialog.setLocationRelativeTo(this);

        JPanel main = new JPanel(new GridBagLayout());
        main.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        JTextField txtName = new JTextField(15);
        JTextField txtRegPhone = new JTextField(phone, 15);
        JTextField txtBday = new JTextField(15);
        txtBday.putClientProperty("JTextField.placeholderText", "dd/MM/yyyy");

        JPanel birthdayPanel = new JPanel(new BorderLayout(2, 0));
        birthdayPanel.add(txtBday, BorderLayout.CENTER);

        JButton btnCal = new JButton("📅");
        btnCal.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCal.setToolTipText("Chọn ngày từ lịch");
        btnCal.addActionListener(e -> {
            LocalDate current = com.handbagstore.utils.DateUtils.parseDate(txtBday.getText().trim());
            LocalDate picked = DateChooser.showDialog(dialog, current);
            if (picked != null) {
                txtBday.setText(com.handbagstore.utils.DateUtils.formatDate(picked));
            }
        });
        birthdayPanel.add(btnCal, BorderLayout.EAST);

        gbc.gridy = 0;
        gbc.gridx = 0;
        main.add(new JLabel("Họ tên:"), gbc);
        gbc.gridx = 1;
        main.add(txtName, gbc);
        gbc.gridy = 1;
        gbc.gridx = 0;
        main.add(new JLabel("Số điện thoại:"), gbc);
        gbc.gridx = 1;
        main.add(txtRegPhone, gbc);
        gbc.gridy = 2;
        gbc.gridx = 0;
        main.add(new JLabel("Ngày sinh:"), gbc);
        gbc.gridx = 1;
        main.add(birthdayPanel, gbc);

        dialog.add(main, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnReg = new JButton("✅ Đăng ký");
        btnReg.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnReg.setBackground(new Color(40, 167, 69));
        btnReg.setForeground(Color.WHITE);
        btnReg.addActionListener(e -> {
            try {
                CustomerDTO c = new CustomerDTO();
                c.setFullName(txtName.getText().trim());
                c.setPhone(txtRegPhone.getText().trim());
                String bdayStr = txtBday.getText().trim();
                if (!bdayStr.isEmpty()) {
                    c.setBirthday(com.handbagstore.utils.DateUtils.parseDate(bdayStr));
                }

                customerBLL.addCustomer(c);
                JOptionPane.showMessageDialog(dialog, "Đăng ký khách hàng thành công!");

                // Refresh customer panel in main frame
                Window win = SwingUtilities.getWindowAncestor(SalePanel.this);
                if (win instanceof com.handbagstore.gui.MainStaffFrame) {
                    ((com.handbagstore.gui.MainStaffFrame) win).refreshCustomerPanel();
                }

                dialog.dispose();
                txtCustomerPhone.setText(c.getPhone());
                lookupCustomer();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton btnCancel = new JButton("Hủy");
        btnCancel.addActionListener(e -> dialog.dispose());

        btnPanel.add(btnReg);
        btnPanel.add(btnCancel);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }
}
