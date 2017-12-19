package com.nooga.lor1k;

public class CPUException {
    public static final int EXCEPT_ITLBMISS = 0xA00; // instruction translation lookaside buffer miss
    public static final int EXCEPT_IPF = 0x400; // instruction page fault
    public static final int EXCEPT_RESET = 0x100; // reset the processor
    public static final int EXCEPT_DTLBMISS = 0x900; // data translation lookaside buffer miss
    public static final int EXCEPT_DPF = 0x300; // instruction page fault
    public static final int EXCEPT_BUSERR = 0x200; // wrong memory access
    public static final int EXCEPT_TICK = 0x500; // tick counter interrupt
    public static final int EXCEPT_INT = 0x800; // interrupt of external devices
    public static final int EXCEPT_SYSCALL = 0xC00; // syscall, jump into supervisor mode
    public static final int EXCEPT_TRAP = 0xE00; // syscall, jump into supervisor mode
}
