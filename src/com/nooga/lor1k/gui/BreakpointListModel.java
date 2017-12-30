package com.nooga.lor1k.gui;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import javax.swing.table.AbstractTableModel;
import java.util.HashMap;

public class BreakpointListModel extends AbstractListModel {
    HashMap hm;

    public BreakpointListModel(HashMap hm) {
        this.hm = hm;
    }

    @Override
    public int getSize() {
        return hm.size();
    }

    @Override
    public Object getElementAt(int i) {
        return hm.keySet().toArray()[i];
    }

    @Override
    public void addListDataListener(ListDataListener listDataListener) {

    }

    @Override
    public void removeListDataListener(ListDataListener listDataListener) {

    }

    void addBreakpoint(int addr) {
        hm.put(addr, true);
        fireIntervalAdded(this, 0, getSize());
        fireContentsChanged(this, 0, getSize());
    }

    void removeBreakpoint(int addr) {
        hm.remove(addr);
        fireIntervalRemoved(this, 0, getSize());
        fireContentsChanged(this, 0, getSize());
    }


}
