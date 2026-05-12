package com.handbagstore.gui.panels;

import com.handbagstore.bll.AccountBLL;
import com.handbagstore.bll.DiscountBLL;
import com.handbagstore.dto.DiscountDTO;
import com.handbagstore.utils.DateUtils;

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
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Form
        JPanel bottom = new JPanel(new BorderLayout(10, 5));
        JPanel form = new JPanel(new GridLayout(3, 4, 10, 5));
        form.setBorder(BorderFactory.createTitledBorder("Tạo mã giảm giá mới"));

        txtCode = new JTextField(); txtValue = new JTextField(); txtMinOrder = new JTextField();
        cmbType = new JComboBox<>(new String[]{"PERCENT", "AMOUNT"});
        cmbOccasion = new JComboBox<>(new String[]{"MANUAL", "BIRTHDAY", "SPECIAL"});
        txtStartDate = new JTextField(); txtStartDate.putClientProperty("JTextField.placeholderText", "dd/MM/yyyy");
        txtEndDate = new JTextField(); txtEndDate.putClientProperty("JTextField.placeholderText", "dd/MM/yyyy");

        form.add(new JLabel("Mã:")); form.add(txtCode);
        form.add(new JLabel("Loại:")); form.add(cmbType);
        form.add(new JLabel("Giá trị:")); form.add(txtValue);
        form.add(new JLabel("Tối thiểu:")); form.add(txtMinOrder);
        form.add(new JLabel("Bắt đầu:")); form.add(txtStartDate);
        form.add(new JLabel("Kết thúc:")); form.add(txtEndDate);
        bottom.add(form, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton btnCreate = new JButton("➕ Tạo mã");
        btnCreate.setBackground(new Color(40, 167, 69)); btnCreate.setForeground(Color.WHITE);
        btnCreate.addActionListener(e -> createDiscount());

        JButton btnDeactivate = new JButton("❌ Vô hiệu hóa");
        btnDeactivate.setBackground(new Color(220, 53, 69)); btnDeactivate.setForeground(Color.WHITE);
        btnDeactivate.addActionListener(e -> deactivateDiscount());

        btnPanel.add(btnCreate); btnPanel.add(btnDeactivate);
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
                    d.isPercentType() ? d.getValue() + "%" : d.getValue() + "đ",
                    d.getMinOrderAmt(), DateUtils.formatDateTime(d.getStartTime()),
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
            DiscountDTO d = new DiscountDTO();
            d.setCode(txtCode.getText().trim().toUpperCase());
            d.setType((String) cmbType.getSelectedItem());
            d.setValue(new BigDecimal(txtValue.getText().trim()));
            d.setMinOrderAmt(txtMinOrder.getText().trim().isEmpty() ? BigDecimal.ZERO : new BigDecimal(txtMinOrder.getText().trim()));
            LocalDate start = DateUtils.parseDate(txtStartDate.getText().trim());
            if (start != null) d.setStartTime(start.atStartOfDay());
            LocalDate end = DateUtils.parseDate(txtEndDate.getText().trim());
            if (end != null) d.setEndTime(end.atTime(LocalTime.MAX));
            d.setOccasion((String) cmbOccasion.getSelectedItem());
            d.setActive(true);
            d.setCreatedBy(AccountBLL.getCurrentUser().getAccountId());

            discountBLL.createDiscount(d);
            JOptionPane.showMessageDialog(this, "Tạo mã giảm giá thành công!");
            txtCode.setText(""); txtValue.setText(""); txtMinOrder.setText("");
            txtStartDate.setText(""); txtEndDate.setText("");
            refreshData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deactivateDiscount() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Chọn mã giảm giá!"); return; }
        try {
            String code = (String) tableModel.getValueAt(row, 0);
            DiscountDTO d = discountBLL.validateCode(code);
            discountBLL.deactivate(d.getDiscountId());
            refreshData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}
