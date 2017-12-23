package com.nooga.lor1k.devices;

import com.nooga.lor1k.MessageBus;

public class Device {

    private MessageBus message = null;
    private String name;

    public Device() {}

    public Device(MessageBus mb, String name) {
        this.name = name;
        this.message = mb;
    }

    public void reset() {

    }

    public int readReg32(int addr) {
        message.Debug("X   Abstract Device " + name + " read32: " + message.addrToString(addr));
        return 0;
    }


    public void writeReg32(int addr, int value) {
        message.Debug("X   Abstract Device " + name + " write32: " + message.addrToString(addr) + " = " + message.addrToString(value));
    }


    public void writeReg8(int addr, byte value) {
        message.Debug("X   Abstract Device " + name + " write8: " + message.addrToString(addr) + " = " + message.addrToString(value));
    }


    public byte readReg8(int addr) {
        message.Debug("X   Abstract Device " + name + " read8: " + message.addrToString(addr));
        return 0;
    }


    public short readReg16(int addr) {
        message.Debug("X   Abstract Device " + name + " read16: " + message.addrToString(addr));
        return 0;
    }


    public void writeReg16(int addr, short value) {
        message.Debug("X   Abstract Device write16: " + message.addrToString(addr) + " = " + message.addrToString(value));
    }
}
