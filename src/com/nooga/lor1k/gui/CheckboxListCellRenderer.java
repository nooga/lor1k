package com.nooga.lor1k.gui;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;

public class CheckboxListCellRenderer extends JCheckBox implements ListCellRenderer {

    HashMap hm;

    public CheckboxListCellRenderer(HashMap hm) {
        this.hm = hm;
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean sel, boolean focus) {
        setComponentOrientation(list.getComponentOrientation());
        setFont(list.getFont());
        setBackground(list.getBackground());
        setForeground(list.getForeground());
        setSelected(sel);
        setEnabled(list.isEnabled());

        setText(value == null ? "" : value.toString());

        return this;
    }
}
