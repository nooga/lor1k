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

    public String addrToString(int i) {
        return String.format("%08x", i);
    }
}
