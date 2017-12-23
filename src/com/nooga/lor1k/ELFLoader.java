package com.nooga.lor1k;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

public class ELFLoader {

    RAM ram;
    ByteBuffer bf;
    private byte ei_class;
    private byte ei_data;
    private byte ei_version;
    private byte ei_osabi;
    private byte ei_abiver;
    private short e_type;
    private short e_machine;
    private int e_version;
    private int e_entry;
    private int e_phoff;
    private int e_shoff;
    private int e_flags;
    private short e_ehsize;
    private short e_phentsize;
    private short e_phnum;

    public ELFLoader(RAM ram) {
        this.ram = ram;
    }

    public void load(Path p) {
        RandomAccessFile f = null;
        FileChannel chan = null;
        try {
            f = new RandomAccessFile(p.toString(),"r");
            chan = f.getChannel();
            bf = ByteBuffer.allocate((int)chan.size());
            chan.read(bf);
            bf.flip();

            parse();
            link();

            chan.close();
            f.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void link() {

    }

    private void parse() {
        int magic = bf.getInt();

        if(magic != 0x7F454C46)
            return;

        this.ei_class = bf.get();
        this.ei_data = bf.get();
        this.ei_version = bf.get();
        this.ei_osabi = bf.get();
        this.ei_abiver = bf.get();

        bf.position(0x10); //skip ei_pad

        this.e_type = bf.getShort();
        this.e_machine = bf.getShort();

        this.e_version = bf.getInt();
        this.e_entry = bf.getInt();
        this.e_phoff = bf.getInt();
        this.e_shoff = bf.getInt();
        this.e_flags = bf.getInt();
        this.e_ehsize = bf.getShort();
        this.e_phentsize = bf.getShort();
        this.e_phnum = bf.getShort();

        int next = this.e_phoff;
        for(int i = 0; i < this.e_phnum; i++) {
            bf.position(next);
            int p_type = bf.getInt();
            int p_offset = bf.getInt();
            int p_vaddr = bf.getInt();
            int p_paddr = bf.getInt();
            int p_filesz = bf.getInt();
            int p_memsz = bf.getInt();
            int p_flags = bf.getInt();
            int p_align = bf.getInt();
            next = bf.position();

            if (p_type != 1) continue;

           System.out.format("ELF loading program: %x %x -> %x\n", p_offset, p_filesz, p_paddr);

            ByteBuffer bytes = (ByteBuffer) ((ByteBuffer)bf.position(p_offset)).slice().limit(p_filesz);

          //  BytePrinter.print(bytes, System.out);

            ram.heap.position(ram.offset + p_paddr);
            ram.heap.put(bytes);
            ram.heap.rewind();
        }
    }
}
