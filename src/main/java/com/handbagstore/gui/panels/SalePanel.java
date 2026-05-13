package com.handbagstore.gui.panels;

import com.handbagstore.bll.*;
import com.handbagstore.dto.*;
import com.handbagstore.gui.components.*;
import com.handbagstore.utils.OrderTimerManager;
import com.handbagstore.utils.PdfExporter;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
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
        refreshProductList();
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
        txtSearchProduct.putClientProperty("JTextField.placeholderText", "Tìm sản phẩm...");
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
        JButton btnSearch = new JButton("🔍");
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

        JLabel lblCart = new JLabel("🛒 Đơn hàng hiện tại");
        lblCart.setFont(new Font("Segoe UI", Font.BOLD, 14));
        centerPanel.add(lblCart, BorderLayout.NORTH);

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
        JPanel paymentPanel = new JPanel();
        paymentPanel.setLayout(new BoxLayout(paymentPanel, BoxLayout.Y_AXIS));
        paymentPanel.setBorder(BorderFactory.createTitledBorder("Thanh toán"));

        // Customer
        JPanel customerRow = new JPanel();
        customerRow.setLayout(new BoxLayout(customerRow, BoxLayout.Y_AXIS));

        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topRow.add(new JLabel("SĐT Khách:"));
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
        topRow.add(txtCustomerPhone);

        JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblCustomerInfo = new JLabel(" ");
        lblCustomerInfo.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        bottomRow.add(lblCustomerInfo);

        customerRow.add(topRow);
        customerRow.add(bottomRow);
        paymentPanel.add(customerRow);

        // Discount
        JPanel discountRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        discountRow.add(new JLabel("Mã giảm giá:"));
        cmbDiscount = new JComboBox<>();
        cmbDiscount.addItem(new DiscountComboItem(null, "--- Chọn mã giảm giá ---"));
        try {
            List<DiscountDTO> activeDiscounts = discountBLL.getActive();
            for (DiscountDTO d : activeDiscounts) {
                String label = d.getCode() + " ("
                        + (d.isPercentType() ? d.getValue() + "%" : formatCurrency(d.getValue())) + ")";
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
                                "Giảm: " + (d.isPercentType() ? d.getValue() + "%" : formatCurrency(d.getValue()))
                                + "<br>" +
                                "Đơn tối thiểu: " + formatCurrency(d.getMinOrderAmt()) + "<br>" +
                                "Từ: " + com.handbagstore.utils.DateUtils.formatDateTime(d.getStartTime()) + "<br>" +
                                "Đến: " + com.handbagstore.utils.DateUtils.formatDateTime(d.getEndTime()) + "</html>";
                        ((JComponent) c).setToolTipText(tooltip);
                    } else {
                        ((JComponent) c).setToolTipText(null);
                    }
                }
                return c;
            }
        });

        cmbDiscount.addActionListener(e -> applyDiscount());
        discountRow.add(cmbDiscount);
        btnRemoveFromCart = new JButton("❌ Xóa SP");
        btnRemoveFromCart.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRemoveFromCart.addActionListener(e -> removeFromCart());
        discountRow.add(btnRemoveFromCart);
        paymentPanel.add(discountRow);

        // Totals
        JPanel totalsPanel = new JPanel(new GridLayout(4, 2, 5, 3));
        lblSubtotal = new JLabel("0đ");
        lblDiscount = new JLabel("0đ");
        lblTotal = new JLabel("0đ");
        lblChange = new JLabel("0đ");
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 16));
        totalsPanel.add(new JLabel("Tạm tính:"));
        totalsPanel.add(lblSubtotal);
        totalsPanel.add(new JLabel("Giảm giá:"));
        totalsPanel.add(lblDiscount);
        totalsPanel.add(new JLabel("TỔNG CỘNG:"));
        totalsPanel.add(lblTotal);

        // Payment method
        JPanel pmRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pmRow.add(new JLabel("Thanh toán:"));
        cmbPaymentMethod = new JComboBox<>(new String[] { "Tiền mặt", "Chuyển khoản" });
        pmRow.add(cmbPaymentMethod);

        // Received & Change
        JPanel receivedRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        receivedRow.add(new JLabel("Tiền nhận:"));
        txtPaymentReceived = new JTextField(10);
        txtPaymentReceived.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                calculateChange();
            }

            public void removeUpdate(DocumentEvent e) {
                calculateChange();
            }

            public void changedUpdate(DocumentEvent e) {
                calculateChange();
            }
        });
        receivedRow.add(txtPaymentReceived);
        receivedRow.add(new JLabel("Thối:"));
        receivedRow.add(lblChange);

        cmbPaymentMethod.addActionListener(e -> {
            boolean isCash = cmbPaymentMethod.getSelectedIndex() == 0;
            txtPaymentReceived.setEnabled(isCash);
            if (!isCash) {
                txtPaymentReceived.setText("");
                lblChange.setText("0đ");
            }
            calculateChange();
        });

        paymentPanel.add(totalsPanel);
        paymentPanel.add(pmRow);
        paymentPanel.add(receivedRow);

        // Action buttons
        JPanel actionRow = new JPanel(new GridLayout(1, 2, 5, 5));
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
        paymentPanel.add(actionRow);

        centerPanel.add(paymentPanel, BorderLayout.SOUTH);
        add(centerPanel, BorderLayout.CENTER);

        // === RIGHT: Pending Orders ===
        pendingWidget = new PendingOrdersWidget();
        pendingWidget.setPreferredSize(new Dimension(280, 0));
        add(pendingWidget, BorderLayout.EAST);
    }

    // ==================== Logic Methods ====================

    private String formatCurrency(BigDecimal amount) {
        if (amount == null)
            return "0đ";
        java.text.NumberFormat formatter = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
        return formatter.format(amount) + "đ";
    }

    private void showProductDetails() {
        int row = productTable.getSelectedRow();
        if (row < 0)
            return;
        try {
            String code = (String) productModel.getValueAt(row, 0);
            ProductDTO product = productBLL.getByCode(code);
            if (product != null) {
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
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi khi lấy chi tiết SP: " + ex.getMessage(), "Lỗi",
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

    private void searchProducts() {
        String keyword = txtSearchProduct.getText().trim();
        if (keyword.isEmpty()) {
            refreshProductList();
            return;
        }
        try {
            productModel.setRowCount(0);
            for (ProductDTO p : productBLL.search(keyword)) {
                int stock = inventoryBLL.getAvailableQuantity(p.getProductId());
                productModel
                        .addRow(new Object[] { p.getProductCode(), p.getName(), formatCurrency(p.getPrice()), stock });
            }
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
            if (product == null)
                return;

            // Check tồn kho
            if (!inventoryBLL.checkAvailability(product.getProductId(), qty)) {
                JOptionPane.showMessageDialog(this, "Không đủ hàng trong kho!");
                return;
            }

            // Check nếu đã có trong cart → cộng dồn
            for (InvoiceDetailDTO d : cartItems) {
                if (d.getProductId() == product.getProductId()) {
                    d.setQuantity(d.getQuantity() + qty);
                    updateCartDisplay();
                    return;
                }
            }

            InvoiceDetailDTO detail = new InvoiceDetailDTO();
            detail.setProductId(product.getProductId());
            detail.setProductCode(product.getProductCode());
            detail.setProductName(product.getName());
            detail.setUnitPrice(product.getPrice());
            detail.setQuantity(qty);
            cartItems.add(detail);
            updateCartDisplay();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
        }
    }

    private void removeFromCart() {
        int row = cartTable.getSelectedRow();
        if (row < 0)
            return;
        cartItems.remove(row);
        updateCartDisplay();
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

        BigDecimal discountAmt = BigDecimal.ZERO;
        if (selectedDiscount != null) {
            discountAmt = discountBLL.calculateDiscount(selectedDiscount, subtotal);
        }
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
                            Window window = SwingUtilities.getWindowAncestor(this);
                            if (window instanceof com.handbagstore.gui.MainStaffFrame) {
                                ((com.handbagstore.gui.MainStaffFrame) window).switchToCustomerAndRegister(phone);
                            }
                        }
                    });
                }
            } else {
                String bday = selectedCustomer.getBirthday() != null
                        ? com.handbagstore.utils.DateUtils.formatDate(selectedCustomer.getBirthday())
                        : "N/A";
                String info = selectedCustomer.getFullName() + " (" + bday + ")";
                if (selectedCustomer.isBirthday()) {
                    info += " 🎂 Sinh nhật hôm nay!";
                    lblCustomerInfo.setForeground(new Color(255, 193, 7));
                    suggestBirthdayDiscount();
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

    private void suggestBirthdayDiscount() {
        try {
            List<DiscountDTO> birthdayDiscounts = discountBLL.getActiveBirthdayDiscounts();
            if (!birthdayDiscounts.isEmpty()) {
                DiscountDTO bd = birthdayDiscounts.get(0);
                int confirm = JOptionPane.showConfirmDialog(this,
                        "🎂 Hôm nay là sinh nhật khách hàng!\nÁp dụng mã giảm giá sinh nhật: " + bd.getCode() + " (" +
                                (bd.isPercentType() ? bd.getValue() + "%" : formatCurrency(bd.getValue())) + ")?",
                        "Sinh nhật khách hàng", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    selectedDiscount = bd;
                    for (int i = 1; i < cmbDiscount.getItemCount(); i++) {
                        DiscountComboItem item = cmbDiscount.getItemAt(i);
                        if (item != null && item.discount != null && bd.getCode().equals(item.discount.getCode())) {
                            cmbDiscount.setSelectedIndex(i);
                            break;
                        }
                    }
                    updateCartDisplay();
                }
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
        StringBuilder sb = new StringBuilder(
                "Sản phẩm gợi ý mua thêm (cần thêm ≥ " + formatCurrency(needed) + "):\n\n");
        int count = 0;
        for (ProductDTO p : suggestions) {
            if (count++ >= 10)
                break;
            sb.append("• ").append(p.getProductCode()).append(" - ").append(p.getName())
                    .append(" — ").append(formatCurrency(p.getPrice())).append("\n");
        }
        JOptionPane.showMessageDialog(this, sb.toString(), "Gợi ý sản phẩm", JOptionPane.INFORMATION_MESSAGE);
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

            clearCart();
            refreshProductList();

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
                    refreshProductList();
                });
            });

            JOptionPane.showMessageDialog(this,
                    "⏳ Đơn hàng đang chờ thanh toán!\nMã HĐ: " + invoice.getInvoiceCode());

            clearCart();
            refreshProductList();
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

        BigDecimal discountAmt = BigDecimal.ZERO;
        if (selectedDiscount != null) {
            discountAmt = discountBLL.calculateDiscount(selectedDiscount, getSubtotal());
            inv.setDiscountId(selectedDiscount.getDiscountId());
        }
        inv.setDiscountAmount(discountAmt);
        inv.setTotal(getSubtotal().subtract(discountAmt).max(BigDecimal.ZERO));
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
        BigDecimal disc = selectedDiscount != null ? discountBLL.calculateDiscount(selectedDiscount, subtotal)
                : BigDecimal.ZERO;
        return subtotal.subtract(disc).max(BigDecimal.ZERO);
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

    private void clearCart() {
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
    }
}
