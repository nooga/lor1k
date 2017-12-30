package com.nooga.lor1k.gui;

import com.nooga.lor1k.*;
import com.nooga.lor1k.devices.Device;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class Debug implements MessageBusListener, BreakSource {
    private JButton runButton;
    private JButton stepButton;
    public JPanel main;
    private JTable table1;
    private JTabbedPane tabbedPane1;
    private JTextPane log;
    private JTable registert;
    private JFormattedTextField curAddr;
    private JButton jmpButton;
    private JButton brk;
    private JList list1;

    HashMap<Integer, Boolean> breakpoints;

    CPU cpu;
    RAM ram;
    MessageBus mb;

    RegisterTableModel registerm;
    RamTableModel ramm;
    DefaultListModel<Integer> breakpointm;


    Timer timer;
    private boolean updateSelection;
    private boolean stopflag;
    private int curBrk;

    public Debug() {
        breakpoints = new HashMap<>();

        mb = new MessageBus();

        mb.registerBusListener(this);

        ram = new RAM(32 * 0x100000, 0x100000);
        cpu = new CPU(mb, ram);
        cpu.setBreakSource(this);


        for (int i = 0; i < 256; i++) {
            ram.addDevice(new Device(mb, "dummydev" + i), i << 24, 0);
        }

        Path path;
        path = Paths.get("kernel/vmlinux");
        ELFLoader eld = new ELFLoader(ram);
        eld.load(path);

        ramm = new RamTableModel(ram);
        table1.setModel(ramm);
        table1.setFont(new Font("Monaco", Font.PLAIN, 10));
        table1.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        registerm = new RegisterTableModel(cpu);
        registert.setModel(registerm);
        registert.setFont(new Font("Monaco", Font.PLAIN, 10));


        breakpointm = new DefaultListModel<>();
       // list1.setCellRenderer(new CheckboxListCellRenderer(breakpoints));
        list1.setModel(breakpointm);

        refresh();

        stepButton.addActionListener(actionEvent -> step(1));

        runButton.addActionListener(actionEvent -> {

                if (timer == null) {
                    if(breakpointm.isEmpty())
                        startTimer(100);
                    else
                        startTimer(1000);
                    runButton.setText("stop");
                } else {
                    stopTimer();
                    runButton.setText("run");
                }
        });

        table1.getSelectionModel().addListSelectionListener(listSelectionEvent -> {
            if(updateSelection) {
                //System.out.println(String.format("%08x", listSelectionEvent.getLastIndex() << 2));
                curAddr.setValue(String.format("%08x", listSelectionEvent.getLastIndex() << 2));
            }
        });

        jmpButton.addActionListener(actionEvent -> {
            viewAddr(getCurAddr() >> 2);
        });

        brk.addActionListener(actionEvent -> {
            setBreakpoint(getCurAddr());
        });
    }

    private void setBreakpoint(int addr) {
        breakpointm.add(breakpointm.getSize(), addr);
    }

    private void deleteBreakpoint(int addr) {
        ((BreakpointListModel)list1.getModel()).removeBreakpoint(addr);
        System.out.println(breakpoints.keySet());
    }

    private int getCurAddr() {
        return Integer.parseInt(this.curAddr.getText(),16);
    }

    private void startTimer(int n) {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                step(n);
            }
        }, 0,100);
    }

    private void stopTimer() {
        timer.cancel();
        timer = null;
    }

    private void step(int n) {
        cpu.step(n,10);
        curBrk = -1;
        refresh();
    }

    private void viewAddr(int addr) {
        Rectangle cr = table1.getCellRect(addr, 0, true);
        table1.scrollRectToVisible(cr);
    }

    private void refresh() {
        int row = cpu.getInstructionPhysicalAddr(cpu.pc << 2) >> 2;

        updateSelection = false;

        table1.setRowSelectionInterval(row, row);

        updateSelection = true;

        viewAddr(row);

        registerm.fireTableDataChanged();
    }

    @Override
    public void onMessage(String m) {
       Document doc = log.getDocument();
        try {
            doc.insertString(doc.getLength(), m, null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean shouldStop(int addr) {
        return breakpointm.contains(addr) && curBrk != addr;
    }

    @Override
    public void executionStopped(int addr) {
        curBrk = addr;
        list1.setSelectedIndex(breakpointm.indexOf(addr));
        stopTimer();
        runButton.setText("run");
        refresh();
    }
}
