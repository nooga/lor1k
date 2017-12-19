package com.nooga.lor1k;
import com.nooga.lor1k.devices.Device;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class RAM {
    private ByteBuffer heap;
    public ShortBuffer int16mem;
    public IntBuffer int32mem;

    private Device[] devices;

    public RAM(int size, int offset) {
        this.heap = ByteBuffer.allocateDirect(size);
        this.int32mem = this.int32Area(offset, size - offset);
        this.int16mem = this.int16Area(offset, size - offset);

        this.devices = new Device[0x100];
    }

    public RAM(ByteBuffer heap, int offset) {
        this.heap = heap;
        this.int32mem = this.int32Area(offset, heap.limit() - offset);
        this.int16mem = this.int16Area(offset, heap.limit() - offset);

        this.devices = new Device[0x100];
    }


    public IntBuffer int32Area(int offset, int size) {
        return ((ByteBuffer)((ByteBuffer)this.heap.position(offset ^ Util.M_UINT32ZE)).slice().limit(size ^ Util.M_UINT32ZE)).asIntBuffer();
    }

    public ShortBuffer int16Area(int offset, int size) {
        return ((ByteBuffer)((ByteBuffer)this.heap.position(offset ^ Util.M_UINT32ZE)).slice().limit(size ^ Util.M_UINT32ZE)).asShortBuffer();
    }


    public FloatBuffer float32Area(int offset, int size) {
        return ((ByteBuffer)((ByteBuffer)this.heap.position(offset ^ Util.M_UINT32ZE)).slice().limit(size ^ Util.M_UINT32ZE)).asFloatBuffer();
    }

    public byte Read8Big(int addr) {
        if(addr >= 0) {
            return this.heap.get(addr);
        }
        return this.devices[(addr >> 24) & 0xFF].readReg8(addr & 0xFFFFFF);
    }

    public void Write8Big(int addr, byte value) {
        if(addr >= 0) {
            this.heap.put(addr, value);
        } else {
            this.devices[(addr >> 24) & 0xFF].writeReg8(addr & 0xFFFFFF, value);
        }
    }

    public int Read16Big(int addr) {
        if(addr >= 0) {
            return this.int16mem.get(addr >> 1);
        }
        return this.devices[(addr >> 24) & 0xFF].readReg16(addr & 0xFFFFFF);
    }

    public void Write16Big(int addr, short value) {
        if(addr >= 0) {
            this.int16mem.put(addr >> 1, value);
        } else {
            this.devices[(addr >> 24) & 0xFF].writeReg16(addr & 0xFFFFFF, value);
        }
    }

    public int Read32Big(int addr) {
        if(addr >= 0) {
            return this.int32mem.get(addr >> 2);
        }
        return this.devices[(addr >> 24) & 0xFF].readReg32(addr & 0xFFFFFF);
    }

    public void Write32Big(int addr, int value) {
        if(addr >= 0) {
            this.int32mem.put(addr >> 2, value);
        } else {
            this.devices[(addr >> 24) & 0xFF].writeReg32(addr & 0xFFFFFF, value);
        }
    }
}
