package com.handbagstore.gui.panels;

import com.handbagstore.bll.SystemLogBLL;
import com.handbagstore.dto.SystemLogDTO;
import com.handbagstore.utils.DateUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class SystemLogPanel extends JPanel {
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField txtSearch;
    private final SystemLogBLL logBLL = new SystemLogBLL();

    public SystemLogPanel() { initComponents(); refreshData(); }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel lblTitle = new JLabel("📋 Nhật ký Hệ thống");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        topPanel.add(lblTitle, BorderLayout.WEST);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        txtSearch = new JTextField(20);
        txtSearch.putClientProperty("JTextField.placeholderText", "Tìm theo hành động...");
        JButton btnSearch = new JButton("🔍 Tìm");
        btnSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSearch.addActionListener(e -> searchLogs());
        txtSearch.addActionListener(e -> searchLogs());
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { searchLogs(); }
            public void removeUpdate(DocumentEvent e) { searchLogs(); }
            public void changedUpdate(DocumentEvent e) { searchLogs(); }
        });
        searchPanel.add(txtSearch); searchPanel.add(btnSearch);
        topPanel.add(searchPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        String[] cols = {"Thời gian", "Người thực hiện", "Hành động", "Chi tiết"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(28);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getColumnModel().getColumn(3).setPreferredWidth(400);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void refreshData() {
        try {
            tableModel.setRowCount(0);
            List<SystemLogDTO> logs = logBLL.getAll();
            for (SystemLogDTO log : logs) {
                tableModel.addRow(new Object[]{
                    DateUtils.formatDateTime(log.getCreatedAt()),
                    log.getAccountName(),
                    log.getAction(),
                    log.getDetail()
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
        }
    }

    private void searchLogs() {
        String keyword = txtSearch.getText().trim();
        if (keyword.isEmpty()) { refreshData(); return; }
        try {
            tableModel.setRowCount(0);
            List<SystemLogDTO> logs = logBLL.search(keyword, null, null);
            for (SystemLogDTO log : logs) {
                tableModel.addRow(new Object[]{
                    DateUtils.formatDateTime(log.getCreatedAt()),
                    log.getAccountName(), log.getAction(), log.getDetail()
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
        }
    }
}
