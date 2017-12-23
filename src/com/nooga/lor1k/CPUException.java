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

    public static String toString(int ex_type) {
        switch(ex_type) {
            case EXCEPT_ITLBMISS: return "EXCEPT_ITLBMISS";
            case EXCEPT_IPF: return "EXCEPT_IPF";
            case EXCEPT_RESET: return "EXCEPT_RESET";
            case EXCEPT_DTLBMISS: return "EXCEPT_DTLBMISS";
            case EXCEPT_DPF: return "EXCEPT_DPF";
            case EXCEPT_BUSERR: return "EXCEPT_BUSERR";
            case EXCEPT_TICK: return "EXCEPT_TICK";
            case EXCEPT_INT: return "EXCEPT_INT";
            case EXCEPT_SYSCALL: return "EXCEPT_SYSCALL";
            case EXCEPT_TRAP: return "EXCEPT_TRAP";
            default:
                return "EXCEPT_UNKNOWN";
        }
    }
}
