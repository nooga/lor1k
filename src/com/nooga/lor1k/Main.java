package com.nooga.lor1k;

import com.nooga.lor1k.devices.Device;
import com.nooga.lor1k.devices.UART;
import com.nooga.lor1k.gui.Debug;

import javax.swing.*;
import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
//        try {
//            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
//                if ("Nimbus".equals(info.getName())) {
//                    UIManager.setLookAndFeel(info.getClassName());
//                    break;
//                }
//            }
//        } catch (Exception e) {
//            // If Nimbus is not available, you can set the GUI to another look and feel.
//        }


        Debug debug = new Debug();

        JFrame frame = new JFrame("lor1k");

        frame.setContentPane(debug.main);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
//        MessageBus mb = new MessageBus();
//        RAM ram = new RAM(32 * 0x100000, 0x100000);
//        CPU cpu = new CPU(mb, ram);
//
//        CPUTest test = new CPUTest("cputests/or1k/");
//        test.run("or1k-basic");
//
//        if(false) {
//
//            for (int i = 0; i < 256; i++) {
//                ram.addDevice(new Device(mb, "dummydev" + i), i << 24, 0);
//            }
//
//            UART uart = new UART(mb, cpu, 0x2);
////        ram.addDevice(uart,0x90000000, 0x7);   // UART
////        ram.addDevice(new Device(mb),0x91000000, 0x1000); // FB
////        ram.addDevice(new Device(mb),0x9e000000, 0x1000); // ATA
////        ram.addDevice(new Device(mb),0x92000000, 0x1000); // Eth
////
//            Path path;
//
//            if (false) { //load jor1k kernel
//                path = Paths.get("kernel/vmlinux.bin");
//                ram.heap.position(ram.offset);
//                try {
//                    ram.heap.put(Files.readAllBytes(path));
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                ram.heap.rewind();
//            } else { //load lor1k kernel
//                path = Paths.get("kernel/vmlinux");
//                ELFLoader eld = new ELFLoader(ram);
//                eld.load(path);
//            }
//            cpu.Reset();
////
//            while (true) {
//                cpu.step(1000, 1000);
//                uart.step();
//            }
////        System.out.format("%08x", cpu.r.get(2));
//        }
    }
}
