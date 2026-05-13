package com.handbagstore.gui.panels;

import com.handbagstore.bll.AccountBLL;
import com.handbagstore.bll.DiscountBLL;
import com.handbagstore.dto.DiscountDTO;
import com.handbagstore.gui.components.DateChooser;
import com.handbagstore.utils.DateUtils;
import com.handbagstore.utils.CurrencyUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class DiscountManagerPanel extends JPanel {
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField txtCode, txtValue, txtMinOrder;
    private JComboBox<String> cmbType, cmbOccasion;
    private JTextField txtStartDate, txtEndDate;
    private JButton btnDeactivate;
    private final DiscountBLL discountBLL = new DiscountBLL();

    public DiscountManagerPanel() { initComponents(); refreshData(); }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel lblTitle = new JLabel("🎫 Quản lý Mã giảm giá");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        add(lblTitle, BorderLayout.NORTH);

        String[] cols = {"Mã", "Loại", "Giá trị", "Tối thiểu", "Bắt đầu", "Kết thúc", "Dịp", "Trạng thái"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(28);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.setFillsViewportHeight(true);
        table.getSelectionModel().addListSelectionListener(e -> loadSelectedRow());
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (table.rowAtPoint(e.getPoint()) == -1) {
                    clearForm();
                }
            }
        });
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Form
        JPanel bottom = new JPanel(new BorderLayout(10, 5));
        JPanel form = new JPanel(new GridLayout(3, 4, 10, 5));
        form.setBorder(BorderFactory.createTitledBorder("Tạo mã giảm giá mới"));

        txtCode = new JTextField(); txtValue = new JTextField(); txtMinOrder = new JTextField();
        cmbType = new JComboBox<>(new String[]{"PERCENT", "AMOUNT"});
        cmbOccasion = new JComboBox<>(new String[]{"MANUAL", "BIRTHDAY", "SPECIAL"});
        txtStartDate = new JTextField(); txtStartDate.setEditable(false);
        txtStartDate.putClientProperty("JTextField.placeholderText", "Chọn ngày bắt đầu...");
        txtStartDate.setCursor(new Cursor(Cursor.HAND_CURSOR));
        txtStartDate.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                LocalDate current = DateUtils.parseDate(txtStartDate.getText());
                LocalDate picked = DateChooser.showDialog(DiscountManagerPanel.this, current);
                if (picked != null) txtStartDate.setText(DateUtils.formatDate(picked));
            }
        });

        txtEndDate = new JTextField(); txtEndDate.setEditable(false);
        txtEndDate.putClientProperty("JTextField.placeholderText", "Chọn ngày kết thúc...");
        txtEndDate.setCursor(new Cursor(Cursor.HAND_CURSOR));
        txtEndDate.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                LocalDate current = DateUtils.parseDate(txtEndDate.getText());
                LocalDate picked = DateChooser.showDialog(DiscountManagerPanel.this, current);
                if (picked != null) txtEndDate.setText(DateUtils.formatDate(picked));
            }
        });

        form.add(new JLabel("Mã:")); form.add(txtCode);
        form.add(new JLabel("Loại:")); form.add(cmbType);
        form.add(new JLabel("Giá trị:")); form.add(txtValue);
        form.add(new JLabel("Tối thiểu:")); form.add(txtMinOrder);
        form.add(new JLabel("Bắt đầu:")); form.add(txtStartDate);
        form.add(new JLabel("Kết thúc:")); form.add(txtEndDate);
        bottom.add(form, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton btnCreate = new JButton("➕ Tạo mã");
        btnCreate.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCreate.setBackground(new Color(40, 167, 69)); btnCreate.setForeground(Color.WHITE);
        btnCreate.addActionListener(e -> createDiscount());

        btnDeactivate = new JButton("❌ Vô hiệu hóa");
        btnDeactivate.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnDeactivate.setBackground(new Color(220, 53, 69)); btnDeactivate.setForeground(Color.WHITE);
        btnDeactivate.addActionListener(e -> toggleDiscountStatus());

        JButton btnUpdate = new JButton("✏️ Cập nhật");
        btnUpdate.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnUpdate.setBackground(new Color(13, 110, 253)); btnUpdate.setForeground(Color.WHITE);
        btnUpdate.addActionListener(e -> updateDiscount());

        btnPanel.add(btnCreate); 
        btnPanel.add(btnUpdate);
        btnPanel.add(btnDeactivate);
        JPanel formWithOccasion = new JPanel(new BorderLayout());
        JPanel occasionRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        occasionRow.add(new JLabel("Dịp:")); occasionRow.add(cmbOccasion);
        formWithOccasion.add(form, BorderLayout.CENTER);
        formWithOccasion.add(occasionRow, BorderLayout.SOUTH);
        bottom.add(formWithOccasion, BorderLayout.CENTER);
        bottom.add(btnPanel, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);
    }

    public void refreshData() {
        try {
            tableModel.setRowCount(0);
            List<DiscountDTO> list = discountBLL.getAll();
            for (DiscountDTO d : list) {
                tableModel.addRow(new Object[]{
                    d.getCode(), d.getType(),
                    d.isPercentType() ? d.getValue() + "%" : CurrencyUtils.format(d.getValue()),
                    CurrencyUtils.format(d.getMinOrderAmt()), DateUtils.formatDateTime(d.getStartTime()),
                    DateUtils.formatDateTime(d.getEndTime()), d.getOccasion(),
                    d.isActive() ? "Hoạt động" : "Đã vô hiệu"
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
        }
    }

    private void createDiscount() {
        try {
            DiscountDTO d = getFormData();
            d.setActive(true);
            d.setCreatedBy(AccountBLL.getCurrentUser().getAccountId());

            discountBLL.createDiscount(d);
            JOptionPane.showMessageDialog(this, "Tạo mã giảm giá thành công!");
            clearForm();
            refreshData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateDiscount() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Chọn mã giảm giá!"); return; }
        try {
            String oldCode = (String) tableModel.getValueAt(row, 0);
            DiscountDTO existing = discountBLL.getByCode(oldCode);
            
            DiscountDTO d = getFormData();
            d.setDiscountId(existing.getDiscountId());
            d.setActive(existing.isActive());
            d.setCreatedBy(existing.getCreatedBy());

            discountBLL.updateDiscount(d);
            JOptionPane.showMessageDialog(this, "Cập nhật thành công!");
            refreshData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private DiscountDTO getFormData() {
        DiscountDTO d = new DiscountDTO();
        d.setCode(txtCode.getText().trim().toUpperCase());
        if (d.getCode().isEmpty()) throw new RuntimeException("Mã không được để trống!");
        
        d.setType((String) cmbType.getSelectedItem());
        String valStr = txtValue.getText().trim();
        if (valStr.isEmpty()) throw new RuntimeException("Giá trị không được để trống!");
        d.setValue(new BigDecimal(valStr));
        
        d.setMinOrderAmt(txtMinOrder.getText().trim().isEmpty() ? BigDecimal.ZERO : new BigDecimal(txtMinOrder.getText().trim()));
        
        String startStr = txtStartDate.getText().trim();
        if (startStr.isEmpty()) throw new RuntimeException("Vui lòng nhập ngày bắt đầu!");
        LocalDate start = DateUtils.parseDate(startStr);
        if (start == null) throw new RuntimeException("Ngày bắt đầu không đúng định dạng!");
        d.setStartTime(start.atStartOfDay());

        String endStr = txtEndDate.getText().trim();
        if (endStr.isEmpty()) throw new RuntimeException("Vui lòng nhập ngày kết thúc!");
        LocalDate end = DateUtils.parseDate(endStr);
        if (end == null) throw new RuntimeException("Ngày kết thúc không đúng định dạng!");
        d.setEndTime(end.atTime(23, 59, 59));
        
        d.setOccasion((String) cmbOccasion.getSelectedItem());
        return d;
    }

    private void loadSelectedRow() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        try {
            String code = (String) tableModel.getValueAt(row, 0);
            // We search in the full list or just fetch from DB. Since it's a manager panel, fetch is safer.
            DiscountDTO d = discountBLL.getByCode(code); 
            if (d != null) {
                txtCode.setText(d.getCode());
                cmbType.setSelectedItem(d.getType());
                txtValue.setText(d.getValue().toString());
                txtMinOrder.setText(d.getMinOrderAmt().toString());
                txtStartDate.setText(DateUtils.formatDate(d.getStartTime().toLocalDate()));
                txtEndDate.setText(DateUtils.formatDate(d.getEndTime().toLocalDate()));
                cmbOccasion.setSelectedItem(d.getOccasion());

                if (d.isActive()) {
                    btnDeactivate.setText("❌ Vô hiệu hóa");
                    btnDeactivate.setBackground(new Color(220, 53, 69));
                } else {
                    btnDeactivate.setText("🔄 Khôi phục");
                    btnDeactivate.setBackground(new Color(40, 167, 69));
                }
            }
        } catch (Exception ignored) {}
    }

    private void clearForm() {
        txtCode.setText(""); txtValue.setText(""); txtMinOrder.setText("");
        txtStartDate.setText(""); txtEndDate.setText("");
        cmbType.setSelectedIndex(0);
        cmbOccasion.setSelectedIndex(0);
        btnDeactivate.setText("❌ Vô hiệu hóa");
        btnDeactivate.setBackground(new Color(220, 53, 69));
        table.clearSelection();
    }

    private void toggleDiscountStatus() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Chọn mã giảm giá!"); return; }
        try {
            String code = (String) tableModel.getValueAt(row, 0);
            DiscountDTO d = discountBLL.getByCode(code);
            boolean newStatus = !d.isActive();
            discountBLL.toggleActive(d.getDiscountId(), newStatus);
            refreshData();
            loadSelectedRow(); // Update button text
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}
