package com.handbagstore.utils;

import javax.swing.*;
import java.awt.*;

public class ButtonUtils {
    public static void setupButton(JButton button, String icon, String text, Color bg, Color fg) {
        button.setLayout(new GridBagLayout());
        JPanel inner = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        inner.setOpaque(false);

        JLabel lblIcon = new JLabel(icon);
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));

        JLabel lblText = new JLabel(text);
        lblText.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        if (fg != null) {
            lblText.setForeground(fg);
            lblIcon.setForeground(fg);
            button.setForeground(fg);
        } else {
            // Default foreground if not specified
            lblText.setForeground(button.getForeground());
            lblIcon.setForeground(button.getForeground());
        }

        inner.add(lblIcon);
        inner.add(lblText);
        button.add(inner);

        if (bg != null) {
            button.setBackground(bg);
        }
        
        // Ensure the button doesn't wrap text by setting a reasonable preferred size if needed,
        // but usually GridBagLayout + FlowLayout inside will keep them together.
        // We can also disable the default text/icon of the button to avoid confusion.
        button.setText("");
        button.setIcon(null);
    }
}
