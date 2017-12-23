package com.nooga.lor1k;

public class MessageBus {
    public MessageBus() {
    }

    public void Debug(String s) {
        System.out.println("D   " + s);

    }

    public void Abort() {
        System.out.println("CPU Panic!");
    }

    public void Abort(CPU cpu) {
        cpu.stop();
        System.out.println("CPU Panic! Stopped Execution.");
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
