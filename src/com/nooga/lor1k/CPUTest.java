package com.nooga.lor1k;

import com.nooga.lor1k.devices.Device;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class CPUTest {
    private Map<String, Path> tests;

    public CPUTest(String path) {
        tests = new HashMap<>();
        try(DirectoryStream<Path> ds = Files.newDirectoryStream(Paths.get(path))) {
            for(Path p : ds) {
                tests.put(p.getFileName().toString(), p);
               // System.out.println("test " + p.toString() + " " + p.getFileName().toString());
            }
        } catch(IOException ex) {
            System.out.println(ex);
        }
    }

    private void report(String s) {
        System.out.println("T   " + s);
    }

    public void run() {
        report("Running all tests");
        for(String s : tests.keySet()) {
            run(s);
        }
    }

    public void run(String name) {
        Path p = tests.get(name);

        //report("Setting up fresh test machine");

        MessageBus mb = new MessageBus();
        RAM ram = new RAM(32 * 0x100000, 0x100000);
        CPU cpu = new CPU(mb, ram);

        for(int i = 0; i < 256; i++) {
            ram.addDevice(new Device(mb, "dummydev" + i), i << 24, 0);
        }

        ELFLoader elfl = new ELFLoader(ram);

        //report("Loading test " + name);

        elfl.load(p);

        report("Running test " + name + "...");

        cpu.step(0x10000000,0);

        //report("R[3] = " + String.format("%x", cpu.r.get(3)));

        report("Test finished " + name);
    }
}
