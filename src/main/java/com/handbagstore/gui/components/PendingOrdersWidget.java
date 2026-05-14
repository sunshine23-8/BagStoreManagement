package com.handbagstore.gui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.JFileChooser;

import com.handbagstore.bll.OrderBLL;
import com.handbagstore.bll.CustomerBLL;
import com.handbagstore.dto.InvoiceDTO;
import com.handbagstore.dto.InvoiceDetailDTO;
import com.handbagstore.dto.CustomerDTO;
import com.handbagstore.utils.PdfExporter;
import com.handbagstore.utils.CurrencyUtils;

/**
 * Widget hiển thị các đơn hàng PENDING với countdown timer.
 * Có nút Thanh toán / Hủy cho từng đơn.
 */
public class PendingOrdersWidget extends JPanel {
    private JPanel ordersListPanel;
    private Timer refreshTimer;
    private Timer countdownTimer;
    private final OrderBLL orderBLL = new OrderBLL();

    private java.util.function.Consumer<InvoiceDTO> onSelectOrder;
    private Runnable onDataChange;

    public PendingOrdersWidget(Runnable onDataChange, java.util.function.Consumer<InvoiceDTO> onSelectOrder) {
        this.onDataChange = onDataChange;
        this.onSelectOrder = onSelectOrder;
        initComponents();
        startAutoRefresh();
    }

    private void initComponents() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder("⏳ Đơn hàng đang chờ"));

        ordersListPanel = new JPanel();
        ordersListPanel.setLayout(new BoxLayout(ordersListPanel, BoxLayout.Y_AXIS));

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(ordersListPanel, BorderLayout.NORTH);

        JScrollPane sp = new JScrollPane(wrapper);
        sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(sp, BorderLayout.CENTER);

        JButton btnCancelAll = new JButton("🗑 Hủy tất cả");
        btnCancelAll.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCancelAll.setBackground(new Color(220, 53, 69));
        btnCancelAll.setForeground(Color.WHITE);
        btnCancelAll.addActionListener(e -> cancelAllOrders());
        add(btnCancelAll, BorderLayout.SOUTH);
    }

    public void refreshData() {
        try {
            ordersListPanel.removeAll();
            List<InvoiceDTO> pendingOrders = orderBLL.getPendingOrders();

            if (pendingOrders.isEmpty()) {
                JLabel lbl = new JLabel("Không có đơn chờ", SwingConstants.CENTER);
                lbl.setForeground(new Color(166, 173, 186));
                ordersListPanel.add(lbl);
            } else {
                for (InvoiceDTO inv : pendingOrders) {
                    ordersListPanel.add(createOrderCard(inv));
                    ordersListPanel.add(Box.createVerticalStrut(5));
                }
            }

            ordersListPanel.revalidate();
            ordersListPanel.repaint();
        } catch (Exception ex) {
            System.err.println("Lỗi refresh pending: " + ex.getMessage());
        }
    }

    private JPanel createOrderCard(InvoiceDTO inv) {
        JPanel card = new JPanel(new BorderLayout(8, 3));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 193, 7), 1, true),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        // Fetch customer name
        String custName = "Khách vãng lai";
        if (inv.getCustomerId() != null && inv.getCustomerId() > 0) {
            try {
                CustomerDTO customer = new CustomerBLL().getById(inv.getCustomerId());
                if (customer != null) {
                    custName = customer.getFullName();
                }
            } catch (Exception ignored) {
            }
        }

        // Fetch products summary
        StringBuilder sb = new StringBuilder("<html>📦 ");
        try {
            List<InvoiceDetailDTO> details = orderBLL.getInvoiceDetails(inv.getInvoiceId());
            if (details != null && !details.isEmpty()) {
                int count = 0;
                for (InvoiceDetailDTO d : details) {
                    if (count++ >= 5) {
                        sb.append("...<br/>");
                        break;
                    }
                    sb.append(d.getProductName()).append(" x").append(d.getQuantity()).append("<br/>");
                }
            } else {
                sb.append("Chưa có sản phẩm");
            }
        } catch (Exception ignored) {
            sb.append("Lỗi tải sản phẩm");
        }
        sb.append("</html>");

        // Info
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));

        JLabel lblCode = new JLabel("<html><nobr>🧾 " + inv.getInvoiceCode() + "</nobr></html>");
        infoPanel.add(lblCode);

        JLabel lblCust = new JLabel("<html>👤 " + custName + "</html>");
        infoPanel.add(lblCust);

        JLabel lblProd = new JLabel(sb.toString());
        lblProd.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblProd.setForeground(Color.GRAY);
        infoPanel.add(lblProd);

        JLabel lblTotal = new JLabel("<html><b>💰 " + CurrencyUtils.format(inv.getTotal()) + "</b></html>");
        infoPanel.add(lblTotal);

        // Countdown
        String remaining = getCountdownText(inv.getExpiresAt());
        JLabel lblCountdown = new JLabel("⏱ " + remaining);
        lblCountdown.setForeground(new Color(220, 53, 69));
        lblCountdown.setFont(new Font("Segoe UI", Font.BOLD, 12));
        infoPanel.add(lblCountdown);
        card.add(infoPanel, BorderLayout.CENTER);

        // Store data for real-time update
        card.putClientProperty("expiresAt", inv.getExpiresAt());
        card.putClientProperty("lblCountdown", lblCountdown);
        card.putClientProperty("invoice", inv);

        // Buttons
        JPanel btnPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        btnPanel.setPreferredSize(new Dimension(110, 0));

        JButton btnPay = new JButton("Thanh toán");
        btnPay.setBackground(new Color(40, 167, 69));
        btnPay.setForeground(Color.WHITE);
        btnPay.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnPay.addActionListener(e -> selectOrder(inv));

        JButton btnCancel = new JButton("Hủy");
        btnCancel.setBackground(new Color(220, 53, 69));
        btnCancel.setForeground(Color.WHITE);
        btnCancel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnCancel.addActionListener(e -> cancelOrder(inv));

        btnPanel.add(btnPay);
        btnPanel.add(btnCancel);
        card.add(btnPanel, BorderLayout.EAST);

        return card;
    }

    private String getCountdownText(LocalDateTime expiresAt) {
        if (expiresAt == null)
            return "N/A";
        Duration remaining = Duration.between(LocalDateTime.now(), expiresAt);
        if (remaining.isNegative())
            return "Hết hạn!";
        long minutes = remaining.toMinutes();
        long seconds = remaining.getSeconds() % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private void updateTimers() {
        boolean needRefresh = false;
        for (Component comp : ordersListPanel.getComponents()) {
            if (comp instanceof JPanel) {
                JPanel card = (JPanel) comp;
                LocalDateTime expiresAt = (LocalDateTime) card.getClientProperty("expiresAt");
                JLabel lblCountdown = (JLabel) card.getClientProperty("lblCountdown");
                InvoiceDTO inv = (InvoiceDTO) card.getClientProperty("invoice");

                if (expiresAt != null && lblCountdown != null) {
                    Duration remaining = Duration.between(LocalDateTime.now(), expiresAt);
                    if (remaining.isNegative()) {
                        if (inv != null) {
                            try {
                                orderBLL.cancelPendingOrder(inv.getInvoiceId());
                                needRefresh = true;
                            } catch (Exception ex) {
                                System.err.println("Lỗi tự động hủy đơn: " + ex.getMessage());
                            }
                        }
                    } else {
                        lblCountdown.setText("⏱ " + getCountdownText(expiresAt));
                    }
                }
            }
        }
        if (needRefresh) {
            refreshData();
        }
    }

    private void selectOrder(InvoiceDTO inv) {
        if (onSelectOrder != null) {
            onSelectOrder.accept(inv);
        }
    }

    private void exportPdf(int invoiceId) {
        try {
            InvoiceDTO inv = orderBLL.getInvoiceById(invoiceId);
            if (inv == null)
                return;
            List<InvoiceDetailDTO> details = orderBLL.getInvoiceDetails(invoiceId);
            CustomerDTO customer = null;
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

    private void cancelOrder(InvoiceDTO inv) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Hủy đơn " + inv.getInvoiceCode() + "?\nHàng sẽ được trả lại kho.",
                "Xác nhận hủy", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION)
            return;
        try {
            orderBLL.cancelPendingOrder(inv.getInvoiceId());
            JOptionPane.showMessageDialog(this, "Đã hủy đơn thành công!");
            refreshData();
            if (onDataChange != null) {
                onDataChange.run();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cancelAllOrders() {
        try {
            List<InvoiceDTO> pendingOrders = orderBLL.getPendingOrders();
            if (pendingOrders.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Không có đơn hàng chờ nào để hủy!");
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Bạn có chắc muốn hủy TẤT CẢ " + pendingOrders.size() + " đơn hàng đang chờ?\nHàng sẽ được trả lại kho.",
                    "Xác nhận hủy tất cả", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (confirm != JOptionPane.YES_OPTION)
                return;

            for (InvoiceDTO inv : pendingOrders) {
                orderBLL.cancelPendingOrder(inv.getInvoiceId());
            }

            JOptionPane.showMessageDialog(this, "Đã hủy tất cả đơn hàng chờ thành công!");
            refreshData();
            if (onDataChange != null) {
                onDataChange.run();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void startAutoRefresh() {
        // Countdown timer mỗi 1s
        countdownTimer = new Timer(1000, e -> updateTimers());
        countdownTimer.start();

        // Refresh DB mỗi 10s
        refreshTimer = new Timer(10000, e -> refreshData());
        refreshTimer.start();
    }
}
