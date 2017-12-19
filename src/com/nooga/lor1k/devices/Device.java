package com.nooga.lor1k.devices;

public interface Device {
    int readReg32(int addr);
    void writeReg32(int addr, int value);
    void writeReg8(int addr, byte value);
    byte readReg8(int addr);
    short readReg16(int addr);
    void writeReg16(int addr, short value);
}
