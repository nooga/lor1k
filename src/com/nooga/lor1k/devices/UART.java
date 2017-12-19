package com.nooga.lor1k.devices;

public class UART implements Device {
    @Override
    public int readReg32(int addr) {
        return 0;
    }

    @Override
    public void writeReg32(int addr, int value) {

    }

    @Override
    public void writeReg8(int addr, byte value) {

    }

    @Override
    public byte readReg8(int addr) {
        return 0;
    }

    @Override
    public short readReg16(int addr) {
        return 0;
    }

    @Override
    public void writeReg16(int addr, short value) {

    }
}
