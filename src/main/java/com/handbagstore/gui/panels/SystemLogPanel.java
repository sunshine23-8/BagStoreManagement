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
import com.handbagstore.utils.TableUtils;

public class SystemLogPanel extends JPanel {
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField txtSearch;
    private final SystemLogBLL logBLL = new SystemLogBLL();

    public SystemLogPanel() {
        initComponents();
        refreshData();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JLabel lblIcon = new JLabel("📋");
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        JLabel lblText = new JLabel("Nhật ký Hệ thống");
        lblText.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titlePanel.add(lblIcon);
        titlePanel.add(lblText);
        topPanel.add(titlePanel, BorderLayout.WEST);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        txtSearch = new JTextField(20);
        txtSearch.putClientProperty("JTextField.placeholderText", "Tìm theo hành động...");
        JPanel searchTitlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JLabel sIcon = new JLabel("🔍");
        sIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        JLabel sText = new JLabel("Tìm kiếm:");
        sText.setFont(new Font("Segoe UI", Font.BOLD, 12));
        searchTitlePanel.add(sIcon);
        searchTitlePanel.add(sText);
        searchPanel.add(searchTitlePanel);
        JButton btnSearch = new JButton("<html><nobr><font face='Segoe UI Emoji'>🔍</font>&nbsp;Tìm</nobr></html>");
        btnSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSearch.addActionListener(e -> searchLogs());
        txtSearch.addActionListener(e -> searchLogs());
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                searchLogs();
            }

            public void removeUpdate(DocumentEvent e) {
                searchLogs();
            }

            public void changedUpdate(DocumentEvent e) {
                searchLogs();
            }
        });
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);
        topPanel.add(searchPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        String[] cols = { "Thời gian", "Người thực hiện", "Hành động", "Chi tiết" };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(28);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Align columns
        TableUtils.alignCenter(table, 0, 1, 2);
        TableUtils.alignLeft(table, 3);

        table.getColumnModel().getColumn(3).setPreferredWidth(400);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void refreshData() {
        try {
            tableModel.setRowCount(0);
            List<SystemLogDTO> logs = logBLL.getAll();
            for (SystemLogDTO log : logs) {
                tableModel.addRow(new Object[] {
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
        if (keyword.isEmpty()) {
            refreshData();
            return;
        }
        try {
            tableModel.setRowCount(0);
            List<SystemLogDTO> logs = logBLL.search(keyword, null, null);
            for (SystemLogDTO log : logs) {
                tableModel.addRow(new Object[] {
                        DateUtils.formatDateTime(log.getCreatedAt()),
                        log.getAccountName(), log.getAction(), log.getDetail()
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
        }
    }
}
