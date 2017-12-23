package com.nooga.lor1k;

import com.nooga.lor1k.devices.Device;
import com.nooga.lor1k.devices.UART;

import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        MessageBus mb = new MessageBus();
        RAM ram = new RAM(32 * 0x100000, 0x100000);
        CPU cpu = new CPU(mb, ram);

        //CPUTest test = new CPUTest("cputests/or1k/");
       // test.run("or1k-intloop");

        //test.run();
      //  IntBuffer heap = ram.int32Area(0,ram.heap.capacity());

        //cpu.Reset();

//        cpu.r.put(0, 0x00100000);
//        cpu.r.put(1, 0x000AAAA0);
//
//        int add =  0x3 << 30 | 0x8 << 26 |
//                0x2 << 21 | // rD
//                0x0 << 16 | // rA
//                0x1 << 11;  // rB
//
//        int nop = 0x5 << 26 | 0x1 << 24;
//
//        int initialPC = 0x40040;
//
//        heap.put(initialPC, add);
//        for(int i=0; i<20000; i++) {
//            heap.put(initialPC+i+1, nop);
//        }

        for(int i = 0; i < 256; i++) {
            ram.addDevice(new Device(mb, "dummydev" + i), i << 24, 0);
        }

        UART uart = new UART(mb, cpu,0x2);
        ram.addDevice(uart,0x90000000, 0x7);   // UART
//        ram.addDevice(new Device(mb),0x91000000, 0x1000); // FB
//        ram.addDevice(new Device(mb),0x9e000000, 0x1000); // ATA
//        ram.addDevice(new Device(mb),0x92000000, 0x1000); // Eth
//
        Path path = Paths.get("kernel/vmlinux.bin");
        ram.heap.position(ram.offset);
        try {
            ram.heap.put(Files.readAllBytes(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
        ram.heap.rewind();
        //ELFLoader eld = new ELFLoader(ram);

        //eld.load(path);

        cpu.Reset();
//
        while(true) {
            cpu.step(1000, 10);
            uart.step();

        }
//        System.out.format("%08x", cpu.r.get(2));
    }
}
