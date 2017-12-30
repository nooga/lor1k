package com.nooga.lor1k.gui;

import com.nooga.lor1k.CPU;

import javax.swing.table.AbstractTableModel;

public class RegisterTableModel extends AbstractTableModel {
    CPU cpu;

    public RegisterTableModel(CPU cpu) {
        this.cpu = cpu;
    }

    @Override
    public String getColumnName(int i) {
        if(i == 0)
            return "reg";
        else if(i == 1)
            return "val";
        else if(i == 2)
            return "x";
        else
            return null;
    }

    @Override
    public int getRowCount() {
        return cpu.r.capacity() + 3;
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public Object getValueAt(int i, int i1) {
        if(i >= 3) {
            i = i - 3;
            switch (i1) {
                case 0:
                    return String.format("r%d", i);
                case 1:
                    return String.format("%08x", cpu.r.get(i));
                case 2:
                    return cpu.r.get(i);
                default:
                    return null;
            }
        } else {
            if(i == 0) { // clock
                switch (i1) {
                    case 0:
                        return "clk";
                    case 1:
                        return cpu.clock;
                    default:
                        return null;
                }
            } else if(i == 1) { // pc
                switch (i1) {
                    case 0:
                        return "pc";
                    case 1:
                        return String.format("%08x", cpu.pc << 2);
                    case 2:
                        return String.format("%08x", cpu.getInstructionPhysicalAddr(cpu.pc << 2));
                    default:
                        return null;
                }
            } else if(i == 2) { // nextpc
                switch (i1) {
                    case 0:
                        return "nextpc";
                    case 1:
                        return String.format("%08x", cpu.nextpc << 2);
                    case 2:
                        return String.format("%08x", cpu.getInstructionPhysicalAddr(cpu.nextpc << 2));
                    default:
                        return null;
                }
            }
        }
        return null;
    }
}

// clockd
// pc (pce)
// npc (npce)
// di

// r0, r0d