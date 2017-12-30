package com.nooga.lor1k;

import java.util.Vector;

public class MessageBus {

    Vector<MessageBusListener> listeners;

    public MessageBus() {
        listeners = new Vector<>();
    }

    public void Debug(String s) {
        System.out.println("D   " + s);
        for(MessageBusListener l : listeners) {
            l.onMessage(s);
        }
    }

    public void registerBusListener(MessageBusListener l) {
        listeners.add(l);
    }

    public void Abort() {
        System.out.println("CPU Panic!");
    }

    public void Abort(CPU cpu) {
        cpu.stop();
        System.out.println("CPU Panic! Stopped Execution.");
        //System.exit(1);
    }

    public String addrToString(int i) {
        return String.format("%08x", i);
    }

    private String leftPad(String s) {
        return "00000000000000000000000000000000".substring(s.length()) + s;
    }

    public String instrToString(int ins) {
        return leftPad(Integer.toBinaryString(ins));
    }
}
