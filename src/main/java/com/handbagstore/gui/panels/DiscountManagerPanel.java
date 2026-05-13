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
import com.handbagstore.utils.TableUtils;

public class DiscountManagerPanel extends JPanel {
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField txtCode, txtValue, txtMinOrder;
    private JComboBox<String> cmbType, cmbOccasion;
    private JTextField txtStartDate, txtEndDate;
    private JCheckBox chkStackable;
    private JButton btnDeactivate;
    private final DiscountBLL discountBLL = new DiscountBLL();

    public DiscountManagerPanel() {
        initComponents();
        refreshData();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JLabel lblIcon = new JLabel("🎫");
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        JLabel lblText = new JLabel("Quản lý Mã giảm giá");
        lblText.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titlePanel.add(lblIcon);
        titlePanel.add(lblText);
        add(titlePanel, BorderLayout.NORTH);

        String[] cols = { "Mã", "Loại", "Giá trị", "Đơn tối thiểu", "Bắt đầu", "Kết thúc", "Dịp", "Cộng dồn",
                "Trạng thái" };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int c) {
                if (c == 7)
                    return Boolean.class;
                return super.getColumnClass(c);
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(28);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Align columns
        TableUtils.alignCenter(table, 0, 1, 4, 5, 6);
        TableUtils.alignRight(table, 2, 3);

        table.setFillsViewportHeight(true);
        table.getSelectionModel().addListSelectionListener(e -> loadSelectedRow());

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row >= 0) {
                    if (col == 7) {
                        toggleStackableAt(row);
                    } else if (col == 8) {
                        toggleStatusAt(row);
                    }
                }
            }
        });

        table.getColumnModel().getColumn(7).setCellRenderer(new CheckBoxRenderer());
        table.getColumnModel().getColumn(8).setCellRenderer(new ButtonRenderer());

        add(new JScrollPane(table), BorderLayout.CENTER);

        // Form
        JPanel bottom = new JPanel(new BorderLayout(10, 5));
        JPanel form = new JPanel(new GridLayout(3, 4, 10, 5));
        form.setBorder(BorderFactory.createTitledBorder("Tạo mã giảm giá mới"));

        txtCode = new JTextField();
        txtValue = new JTextField();
        txtMinOrder = new JTextField();
        cmbType = new JComboBox<>(new String[] { "PERCENT", "AMOUNT" });
        cmbOccasion = new JComboBox<>(new String[] { "MANUAL", "BIRTHDAY", "SPECIAL" });
        txtStartDate = new JTextField();
        txtStartDate.putClientProperty("JTextField.placeholderText", "dd/MM/yyyy");
        JPanel startPanel = new JPanel(new BorderLayout(2, 0));
        startPanel.add(txtStartDate, BorderLayout.CENTER);
        JButton btnCalStart = new JButton("<html><font face='Segoe UI Emoji'>📅</font></html>");
        btnCalStart.addActionListener(e -> {
            LocalDate current = DateUtils.parseDate(txtStartDate.getText());
            LocalDate picked = DateChooser.showDialog(DiscountManagerPanel.this, current);
            if (picked != null)
                txtStartDate.setText(DateUtils.formatDate(picked));
        });
        startPanel.add(btnCalStart, BorderLayout.EAST);

        txtEndDate = new JTextField();
        txtEndDate.putClientProperty("JTextField.placeholderText", "dd/MM/yyyy");
        JPanel endPanel = new JPanel(new BorderLayout(2, 0));
        endPanel.add(txtEndDate, BorderLayout.CENTER);
        JButton btnCalEnd = new JButton("<html><nobr><font face='Segoe UI Emoji'>📅</font></nobr></html>");
        btnCalEnd.addActionListener(e -> {
            LocalDate current = DateUtils.parseDate(txtEndDate.getText());
            LocalDate picked = DateChooser.showDialog(DiscountManagerPanel.this, current);
            if (picked != null)
                txtEndDate.setText(DateUtils.formatDate(picked));
        });
        endPanel.add(btnCalEnd, BorderLayout.EAST);

        form.add(new JLabel("Mã:"));
        form.add(txtCode);
        form.add(new JLabel("Loại:"));
        form.add(cmbType);
        form.add(new JLabel("Giá trị:"));
        form.add(txtValue);
        form.add(new JLabel("Tối thiểu:"));
        form.add(txtMinOrder);
        form.add(new JLabel("Bắt đầu:"));
        form.add(startPanel);
        form.add(new JLabel("Kết thúc:"));
        form.add(endPanel);
        bottom.add(form, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton btnCreate = new JButton(
                "<html><nobr><font face='Segoe UI Emoji'>➕</font>&nbsp;Tạo&nbsp;mã</nobr></html>");
        btnCreate.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCreate.setBackground(new Color(40, 167, 69));
        btnCreate.setForeground(Color.WHITE);
        btnCreate.addActionListener(e -> createDiscount());

        btnDeactivate = new JButton(
                "<html><nobr><font face='Segoe UI Emoji'>❌</font>&nbsp;Vô&nbsp;hiệu&nbsp;hóa</nobr></html>");
        btnDeactivate.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnDeactivate.setBackground(new Color(220, 53, 69));
        btnDeactivate.setForeground(Color.WHITE);
        btnDeactivate.addActionListener(e -> toggleDiscountStatus());

        JButton btnUpdate = new JButton("<html><font face='Segoe UI Emoji'>✏️</font> Cập nhật</html>");
        btnUpdate.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnUpdate.setBackground(new Color(13, 110, 253));
        btnUpdate.setForeground(Color.WHITE);
        btnUpdate.addActionListener(e -> updateDiscount());

        btnPanel.add(btnCreate);
        btnPanel.add(btnUpdate);
        btnPanel.add(btnDeactivate);
        JPanel formWithOccasion = new JPanel(new BorderLayout());
        JPanel occasionRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        occasionRow.add(new JLabel("Dịp:"));
        occasionRow.add(cmbOccasion);
        chkStackable = new JCheckBox("Cộng dồn");
        occasionRow.add(chkStackable);
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
                tableModel.addRow(new Object[] {
                        d.getCode(), d.getType(),
                        d.isPercentType() ? d.getValue() + "%" : CurrencyUtils.format(d.getValue()),
                        CurrencyUtils.format(d.getMinOrderAmt()), DateUtils.formatDateTime(d.getStartTime()),
                        DateUtils.formatDateTime(d.getEndTime()), d.getOccasion(),
                        d.isStackable(),
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
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Chọn mã giảm giá!");
            return;
        }
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
        if (d.getCode().isEmpty())
            throw new RuntimeException("Mã không được để trống!");

        d.setType((String) cmbType.getSelectedItem());
        String valStr = txtValue.getText().trim();
        if (valStr.isEmpty())
            throw new RuntimeException("Giá trị không được để trống!");
        d.setValue(new BigDecimal(valStr));

        d.setMinOrderAmt(txtMinOrder.getText().trim().isEmpty() ? BigDecimal.ZERO
                : new BigDecimal(txtMinOrder.getText().trim()));

        String startStr = txtStartDate.getText().trim();
        if (startStr.isEmpty())
            throw new RuntimeException("Vui lòng nhập ngày bắt đầu!");
        LocalDate start = DateUtils.parseDate(startStr);
        if (start == null)
            throw new RuntimeException("Ngày bắt đầu không đúng định dạng!");
        d.setStartTime(start.atStartOfDay());

        String endStr = txtEndDate.getText().trim();
        if (endStr.isEmpty())
            throw new RuntimeException("Vui lòng nhập ngày kết thúc!");
        LocalDate end = DateUtils.parseDate(endStr);
        if (end == null)
            throw new RuntimeException("Ngày kết thúc không đúng định dạng!");
        d.setEndTime(end.atTime(23, 59, 59));

        d.setOccasion((String) cmbOccasion.getSelectedItem());
        d.setStackable(chkStackable.isSelected());
        return d;
    }

    private void loadSelectedRow() {
        int row = table.getSelectedRow();
        if (row < 0)
            return;
        try {
            String code = (String) tableModel.getValueAt(row, 0);
            // We search in the full list or just fetch from DB. Since it's a manager panel,
            // fetch is safer.
            DiscountDTO d = discountBLL.getByCode(code);
            if (d != null) {
                txtCode.setText(d.getCode());
                cmbType.setSelectedItem(d.getType());
                txtValue.setText(d.getValue().toString());
                txtMinOrder.setText(d.getMinOrderAmt().toString());
                txtStartDate.setText(DateUtils.formatDate(d.getStartTime().toLocalDate()));
                txtEndDate.setText(DateUtils.formatDate(d.getEndTime().toLocalDate()));
                cmbOccasion.setSelectedItem(d.getOccasion());
                chkStackable.setSelected(d.isStackable());

                if (d.isActive()) {
                    btnDeactivate.setText("❌ Vô hiệu hóa");
                    btnDeactivate.setBackground(new Color(220, 53, 69));
                } else {
                    btnDeactivate.setText("🔄 Khôi phục");
                    btnDeactivate.setBackground(new Color(40, 167, 69));
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void clearForm() {
        txtCode.setText("");
        txtValue.setText("");
        txtMinOrder.setText("");
        txtStartDate.setText("");
        txtEndDate.setText("");
        cmbType.setSelectedIndex(0);
        cmbOccasion.setSelectedIndex(0);
        chkStackable.setSelected(false);
        btnDeactivate.setText("❌ Vô hiệu hóa");
        btnDeactivate.setBackground(new Color(220, 53, 69));
        table.clearSelection();
    }

    private void toggleDiscountStatus() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Chọn mã giảm giá!");
            return;
        }
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

    private void toggleStackableAt(int row) {
        String code = (String) tableModel.getValueAt(row, 0);
        try {
            DiscountDTO d = discountBLL.getByCode(code);
            if (d != null) {
                boolean newVal = !d.isStackable();
                d.setStackable(newVal);
                discountBLL.updateDiscount(d);
                tableModel.setValueAt(newVal, row, 7);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
        }
    }

    private void toggleStatusAt(int row) {
        String code = (String) tableModel.getValueAt(row, 0);
        try {
            DiscountDTO d = discountBLL.getByCode(code);
            if (d != null) {
                boolean newStatus = !d.isActive();
                discountBLL.toggleActive(d.getDiscountId(), newStatus);
                tableModel.setValueAt(newStatus ? "Hoạt động" : "Đã vô hiệu", row, 8);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
        }
    }

    class CheckBoxRenderer extends JCheckBox implements javax.swing.table.TableCellRenderer {
        public CheckBoxRenderer() {
            setHorizontalAlignment(JLabel.CENTER);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            setSelected(value != null && (Boolean) value);
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }
            return this;
        }
    }

    class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            setText(value.toString());
            if ("Hoạt động".equals(value)) {
                setBackground(new Color(40, 167, 69));
                setForeground(Color.WHITE);
            } else {
                setBackground(new Color(220, 53, 69));
                setForeground(Color.WHITE);
            }
            return this;
        }
    }
}
