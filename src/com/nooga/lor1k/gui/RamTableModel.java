package com.nooga.lor1k.gui;

import com.nooga.lor1k.Disasm;
import com.nooga.lor1k.RAM;

import javax.swing.table.AbstractTableModel;

public class RamTableModel extends AbstractTableModel {

    private final int rows;
    RAM ram;

    public RamTableModel(RAM ram) {
        this.ram = ram;
        this.rows = ram.heap.capacity() - ram.offset;
    }

    private String formatInstruction(int ins) {
        return String.format("%02x %02x %02x %02x",
                (ins >> 24) & 0xFF,
                (ins >> 16) & 0xFF,
                (ins >> 8) & 0xFF,
                ins & 0xFF);
    }

    @Override
    public String getColumnName(int i) {
        if(i == 0)
            return "phys addr";
        else if(i == 1)
            return "instr";
        else if(i == 2)
            return "disasm";
        else
            return null;
    }

    @Override
    public int getRowCount() {
        return rows>>2;
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public Object getValueAt(int i, int i1) {
        if(i1 == 0)
            return String.format("%08X", i<<2);
        else if(i1 == 1)
            return formatInstruction(this.ram.int32mem.get(i));
        else if(i1 == 2)
            return Disasm.disasm(this.ram.int32mem.get(i));
        else
            return null;
    }
}
