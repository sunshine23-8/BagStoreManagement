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

        JScrollPane scrollPane = new JScrollPane(ordersListPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);

        JButton btnRefresh = new JButton("🔄 Làm mới");
        btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRefresh.addActionListener(e -> refreshData());
        add(btnRefresh, BorderLayout.SOUTH);
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
        JPanel card = new JPanel(new BorderLayout(5, 3));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 193, 7), 1, true),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        card.setMaximumSize(new Dimension(260, 120));

        // Info
        JPanel infoPanel = new JPanel(new GridLayout(3, 1));
        infoPanel.add(new JLabel("🧾 " + inv.getInvoiceCode()));
        infoPanel.add(new JLabel("💰 " + CurrencyUtils.format(inv.getTotal())));

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

        // Buttons
        JPanel btnPanel = new JPanel(new GridLayout(2, 1, 0, 3));
        JButton btnPay = new JButton("💰 TT");
        btnPay.setBackground(new Color(40, 167, 69));
        btnPay.setForeground(Color.WHITE);
        btnPay.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnPay.addActionListener(e -> selectOrder(inv));

        JButton btnCancel = new JButton("❌ Hủy");
        btnCancel.setBackground(new Color(220, 53, 69));
        btnCancel.setForeground(Color.WHITE);
        btnCancel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnCancel.addActionListener(e -> cancelOrder(inv));

        btnPanel.add(btnPay);
        btnPanel.add(btnCancel);
        card.add(btnPanel, BorderLayout.EAST);

        return card;
    }

    private String getCountdownText(LocalDateTime expiresAt) {
        if (expiresAt == null) return "N/A";
        Duration remaining = Duration.between(LocalDateTime.now(), expiresAt);
        if (remaining.isNegative()) return "Hết hạn!";
        long minutes = remaining.toMinutes();
        long seconds = remaining.getSeconds() % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private void updateTimers() {
        for (Component comp : ordersListPanel.getComponents()) {
            if (comp instanceof JPanel) {
                JPanel card = (JPanel) comp;
                LocalDateTime expiresAt = (LocalDateTime) card.getClientProperty("expiresAt");
                JLabel lblCountdown = (JLabel) card.getClientProperty("lblCountdown");
                if (expiresAt != null && lblCountdown != null) {
                    lblCountdown.setText("⏱ " + getCountdownText(expiresAt));
                }
            }
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
            if (inv == null) return;
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
                JOptionPane.showMessageDialog(this, "Đã xuất PDF thành công: " + path);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi xuất PDF: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cancelOrder(InvoiceDTO inv) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Hủy đơn " + inv.getInvoiceCode() + "?\nHàng sẽ được trả lại kho.",
                "Xác nhận hủy", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
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

    private void startAutoRefresh() {
        // Countdown timer mỗi 1s
        countdownTimer = new Timer(1000, e -> updateTimers());
        countdownTimer.start();

        // Refresh DB mỗi 10s
        refreshTimer = new Timer(10000, e -> refreshData());
        refreshTimer.start();
    }
}
