package com.nooga.lor1k;


import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class CPU {

    private static final int SPR_UPR = 1; // unit present register
    private static final int SPR_SR = 17; // supervision register
    private static final int SPR_EEAR_BASE = 48; // exception ea register
    private static final int SPR_EPCR_BASE = 32; // exception pc register
    private static final int SPR_ESR_BASE = 64; // exception sr register
    private static final int SPR_IMMUCFGR = 4; // Instruction MMU Configuration register
    private static final int SPR_DMMUCFGR = 3; // Data MMU Configuration register
    private static final int SPR_ICCFGR = 6; // Instruction Cache configuration register
    private static final int SPR_DCCFGR = 5; // Data Cache Configuration register
    private static final int SPR_VR = 0; // Version register

    private static final long M_UINT32 = 0xFFFFFFFFL;

    private MessageBus message;

    private RAM ram;

    private long clock = 0;

    private int pc = 0;
    private int nextpc = 0;
    private boolean delayed_inst = false;

    public IntBuffer r; // TODO change to private later
    private FloatBuffer f;

    private int TTMR = 0;
    private int TTCR = 0;

    private int PICMR = 3;
    private int PICSR = 0;

    private int EA = -1; // hidden register for atomic lwa operation

    private boolean SR_SM = true; // supervisor mode
    private boolean SR_TEE = false; // tick timer Exception Enabled
    private boolean SR_IEE = false; // interrupt Exception Enabled
    private boolean SR_DCE = false; // Data Cache Enabled
    private boolean SR_ICE = false; // Instruction Cache Enabled
    private boolean SR_DME = false; // Data MMU Enabled
    private boolean SR_IME = false; // Instruction MMU Enabled
    private boolean SR_LEE = false; // Little Endian Enabled
    private boolean SR_CE = false; // CID Enabled ?
    private boolean SR_F = false; // Flag for l.sf... instructions
    private boolean SR_CY = false; // Carry Flag
    private boolean SR_OV = false; // Overflow Flag
    private boolean SR_OVE = false; // Overflow Flag Exception
    private boolean SR_DSX = false; // Delay Slot Exception
    private boolean SR_EPH = false; // Exception Prefix High
    private boolean SR_FO = true; // Fixed One, always set
    private boolean SR_SUMRA = false; // SPRS User Mode Read Access, or TRAP exception disable?
    private int SR_CID = 0x0; //Context ID


    private IntBuffer group0;
    private IntBuffer group1;
    private IntBuffer group2;
    private boolean stop_flag;
    private boolean print = false;

    public CPU(MessageBus message_bus, RAM ram) {
        this.message = message_bus;

        this.ram = ram;

        this.r = this.ram.int32Area(0, 34 << 2);
        this.f = this.ram.float32Area(0, 32 << 2);

        this.group0 = this.ram.int32Area(0x2000, 0x2000);
        this.group1 = this.ram.int32Area(0x4000, 0x2000);
        this.group2 = this.ram.int32Area(0x6000, 0x2000);

        this.Reset();
    }

    public void Reset() {
        this.stop_flag = false;

        this.TTMR = 0x0;
        this.TTCR = 0x0;
        this.PICMR = 0x3;
        this.PICSR = 0x0;

        this.group0.put(SPR_IMMUCFGR, 0x18); // 0 ITLB has one way and 64 sets
        this.group0.put(SPR_DMMUCFGR, 0x18); // 0 DTLB has one way and 64 sets
        this.group0.put(SPR_ICCFGR, 0x48);
        this.group0.put(SPR_DCCFGR, 0x48);
        this.group0.put(SPR_VR, 0x12000001);

        // UPR present
        // data mmu present
        // instruction mmu present
        // PIC present (architecture manual seems to be wrong here)
        // Tick timer present
        this.group0.put(SPR_UPR, 0x619);

        this.Exception(CPUException.EXCEPT_RESET, 0x0); // set pc values
        this.pc = this.nextpc;
        this.nextpc++;
    }

    // return instruction from RAM, pc is a word index, not address
    private int getInstruction(int addr) {
        if (!this.SR_IME) {
            return this.ram.Read32Big(addr);
        }
        // pagesize is 8192 bytes
        // nways are 1
        // nsets are 64

        int setindex = (addr >> 13) & 63;
        setindex &= 63; // number of sets
        int tlmbr = this.group2.get(0x200 | setindex);

        // test if tlmbr is valid
        if (((tlmbr & 1) == 0) || ((tlmbr >> 19) != (addr >> 19))) {
            this.Exception(CPUException.EXCEPT_ITLBMISS, this.pc<<2);
            return -1;
        }
        // set lru
        if (0 != (tlmbr & 0xC0)) {
            message.Debug("Error: LRU is not supported");
            message.Abort(this);
        }

        int tlbtr = this.group2.get(0x280 | setindex);
        //Test for page fault
        // check if supervisor mode
        if (this.SR_SM) {
            // check if user read enable is not set(URE)
            if (0 == (tlbtr & 0x40)) {
                this.Exception(CPUException.EXCEPT_IPF, this.pc<<2);
                return -1;
            }
        } else {
            // check if supervisor read enable is not set (SRE)
            if (0 == (tlbtr & 0x80)) {
                this.Exception(CPUException.EXCEPT_IPF, this.pc<<2);
                return -1;
            }
        }
        return this.ram.Read32Big((int)(((tlbtr & 0xFFFFE000) | (addr & 0x1FFF)) & 0xFFFFFFFFL));
    }


    public void step(int steps, int clock_speed) {
        int ins, jump, rindex, imm;
        int rA, rB, rD;

        long delta;

        do {
            if(this.stop_flag) {
                this.stop_flag = false;
                return;
            }

            this.clock++;

            // do this not so often
            if (0 != (steps & 1023)) {
                // ---------- TICK ----------
                // timer enabled
                if ((this.TTMR >> 30) != 0) {
                    delta = (this.TTMR & 0xFFFFFFFL) - (this.TTCR & 0xFFFFFFFL);
                    this.TTCR = (int)((this.TTCR + clock_speed) & 0xFFFFFFFFL);
                    if (delta < clock_speed) {
                        // if interrupt enabled
                        if (0 != (this.TTMR & (1 << 29))) {
                            this.TTMR |= (1 << 28); // set pending interrupt
                        }
                    }
                }

                // check if pending and check if interrupt must be triggered
                if ((this.SR_TEE) && (0 != (this.TTMR & (1 << 28)))) {
                    this.Exception(CPUException.EXCEPT_TICK, this.group0.get(SPR_EEAR_BASE));
                    this.pc = this.nextpc++;
                }
            }

            ins = this.getInstruction(this.pc << 2) ;

            //message.Debug("T   " + message.addrToString(this.pc << 2) + " : " + message.instrToString(ins) );

            if(ins == -1) {
                this.pc = this.nextpc++;
                continue;
            }

            switch((ins >> 26) & 0x3f) {

                // j
                case 0x0:
                    jump = this.pc + ((ins << 6) >> 6);
                    this.pc = this.nextpc;
                    this.nextpc = jump;
                    this.delayed_inst = true;
                    continue;

                // jal
                case 0x1:
                    jump = this.pc + ((ins << 6) >> 6);
                    this.r.put(9,  (this.nextpc << 2) + 4);
                    this.pc = this.nextpc;
                    this.nextpc = jump;
                    this.delayed_inst = true;
                    continue;

                // bnf
                case 0x3:
                    if(this.SR_F) {
                        break;
                    }

                    jump = this.pc + ((ins << 6) >> 6);
                    this.pc = this.nextpc;
                    this.nextpc = jump;
                    this.delayed_inst = true;
                    continue;

                // bf
                case 0x4:
                    if(!this.SR_F) {
                        break;
                    }

                    jump = this.pc + ((ins << 6) >> 6);
                    this.pc = this.nextpc;
                    this.nextpc = jump;
                    this.delayed_inst = true;
                    continue;

                // nop
                case 0x5:
                    // nothing ;)

                    //for running tests
                    if((ins & 0xff) == 1) {
                        message.Debug("test reports " + message.addrToString(this.r.get(3)));
                        this.stop_flag = true;
                    } else if((ins & 0xff) == 2) {
                       // message.Debug("test result r[3] = " + message.addrToString(this.r.get(3)));
                    }
                    break;

                // movhi & macrc
                case 0x6:
                    rindex = (ins >> 21) & 0x1F;
                    // if 16th bit is set
                    if(0 != (ins & 0x10000)) {
                        // macrc
                        message.Debug("Error: macrc not supported\n");
                        message.Abort(this);
                    } else {
                        // movhi
                        this.r.put(rindex,  ((ins & 0xFFFF) << 16));
                    }
                    break;

                // sys & trap
                case 0x8:
                    if ((ins & 0xFFFF0000) == 0x21000000) {
                        // trap
                        message.Debug("Trap at " + message.addrToString(this.pc<<2));
                        this.Exception(CPUException.EXCEPT_TRAP, this.group0.get(SPR_EEAR_BASE));
                    } else {
                        // sys
                        this.Exception(CPUException.EXCEPT_SYSCALL, this.group0.get(SPR_EEAR_BASE));
                    }
                    break;

                // rfe
                case 0x9:
                    this.nextpc = this.GetSPR(SPR_EPCR_BASE)>>2;
                    this.pc = this.nextpc++;
                    this.delayed_inst = false;
                    this.SetFlags(this.GetSPR(SPR_ESR_BASE)); // could raise an exception
                    continue;

                // jr
                case 0x11:
                    jump = this.r.get((ins >> 11) & 0x1F) >>2;
                    this.pc = this.nextpc;
                    this.nextpc = jump;
                    this.delayed_inst = true;
                    continue;

                // jalr
                case 0x12:
                    jump = this.r.get((ins >> 11) & 0x1F) >>2;
                    this.r.put(9,  (this.nextpc<<2) + 4);
                    this.pc = this.nextpc;
                    this.nextpc = jump;
                    this.delayed_inst = true;
                    continue;

                // lwa
                case 0x1B:
                    this.r.put(32,  this.r.get((ins >> 16) & 0x1F)  + ((ins << 16) >> 16));

                    if ((this.r.get(32)  & 3) != 0) {
                        message.Debug("Error in lwa: no unaligned access allowed");
                        message.Abort(this);
                    }

                   // message.Debug("lwa 32 " + message.addrToString(this.r.get(32)));

                    this.r.put(33,  (int)this.DTLBLookup(this.r.get(32) , false));

                    if (this.r.get(33)  == -1) {
                        break;
                    }

                    //message.Debug("lwa 33 " + message.addrToString(this.r.get(33)));

                    this.EA = this.r.get(33) ;
                    this.r.put((ins >> 21) & 0x1F,  (this.r.get(33)  > 0) ? ram.int32mem.get(this.r.get(33) >> 2) : ram.Read32Big(this.r.get(33)));
                    break;

                // lwz
                case 0x21:
                    this.r.put(32,  this.r.get((ins >> 16) & 0x1F)  + ((ins << 16) >> 16));

                    if ((this.r.get(32)  & 3) != 0) {
                        message.Debug("Error in lwz: no unaligned access allowed");
                        message.Abort(this);
                    }

                    //message.Debug("lwz 32 " + message.addrToString(this.r.get(32)));

                    this.r.put(33,  (int)this.DTLBLookup(this.r.get(32) , false));

                    if (this.r.get(33)  == -1) {
                        break;
                    }

                    //message.Debug("lwz 33 " + message.addrToString(this.r.get(33)));

                    this.r.put((ins >> 21) & 0x1F, (this.r.get(33) > 0) ? ram.int32mem.get(this.r.get(33) >> 2) : ram.Read32Big(this.r.get(33)));
                    break;

                // lbz
                case 0x23:
                    this.r.put(32,  this.r.get((ins >> 16) & 0x1F)  + ((ins << 16) >> 16));
                    this.r.put(33,  (int)this.DTLBLookup(this.r.get(32) , false));

                    if (this.r.get(33)  == -1) {
                        break;
                    }

                    this.r.put((ins >> 21) & 0x1F,  ram.Read8Big(this.r.get(33) ));
                    break;

                // lbs
                case 0x24:
                    this.r.put(32,  this.r.get((ins >> 16) & 0x1F)  + ((ins << 16) >> 16));
                    this.r.put(33,  (int)this.DTLBLookup(this.r.get(32) , false));
                    if (this.r.get(33)  == -1) {
                        break;
                    }
                    this.r.put((ins >> 21) & 0x1F,  (ram.Read8Big(this.r.get(33) ) << 24) >> 24);
                    break;

                // lhz
                case 0x25:
                    this.r.put(32,  this.r.get((ins >> 16) & 0x1F)  + ((ins << 16) >> 16));
                    this.r.put(33,  (int)this.DTLBLookup(this.r.get(32) , false));
                    if (this.r.get(33)  == -1) {
                        break;
                    }
                    this.r.put((ins >> 21) & 0x1F,  ram.Read16Big(this.r.get(33) ));
                    break;

                // lhs
                case 0x26:
                    this.r.put(32,  this.r.get((ins >> 16) & 0x1F)  + ((ins << 16) >> 16));
                    this.r.put(33,  (int)this.DTLBLookup(this.r.get(32) , false));
                    if (this.r.get(33)  == -1) {
                        break;
                    }
                    this.r.put((ins >> 21) & 0x1F,  (ram.Read16Big(this.r.get(33) ) << 16) >> 16);
                    break;

                // addi signed
                case 0x27:
                    imm = (ins << 16) >> 16;
                    rA = this.r.get((ins >> 16) & 0x1F) ;
                    rindex = (ins >> 21) & 0x1F;
                    this.r.put(rindex,  rA + imm);
                    this.SR_CY = this.r.get(rindex)  < rA;
                    this.SR_OV = 0 != ((rA ^ imm ^ -1) & (rA ^ this.r.get(rindex) ) & 0x80000000);
                    //TODO overflow and carry
                    // maybe wrong
                    break;
                // andi
                case 0x29:
                    this.r.put((ins >> 21) & 0x1F,  this.r.get((ins >> 16) & 0x1F)  & (ins & 0xFFFF));
                    break;

                // ori
                case 0x2A:
                    this.r.put((ins >> 21) & 0x1F,  this.r.get((ins >> 16) & 0x1F)  | (ins & 0xFFFF));
                    break;

                // xori
                case 0x2B:
                    rA = this.r.get((ins >> 16) & 0x1F) ;
                    this.r.put((ins >> 21) & 0x1F,  rA ^ ((ins << 16) >> 16));
                    break;

                // mfspr
                case 0x2D:
                    this.r.put((ins >> 21) & 0x1F,  this.GetSPR(this.r.get((ins >> 16) & 0x1F)  | (ins & 0xFFFF)));
                    break;

                // slli & rori & srai
                case 0x2E:
                    switch ((ins >> 6) & 0x3) {
                        case 0:
                            // slli
                            this.r.put((ins >> 21) & 0x1F,  this.r.get((ins >> 16) & 0x1F)  << (ins & 0x1F));
                            break;
                        case 1:
                            // rori
                            this.r.put((ins >> 21) & 0x1F,  this.r.get((ins >> 16) & 0x1F)  >>> (ins & 0x1F));
                            break;
                        case 2:
                            // srai
                            this.r.put((ins >> 21) & 0x1F,  this.r.get((ins >> 16) & 0x1F)  >> (ins & 0x1F));
                            break;
                        default:
                            message.Debug("Error: opcode 2E function not implemented");
                            message.Abort(this);
                            break;
                    }
                    break;

                // sf...i
                case 0x2F:
                    imm = (ins << 16) >> 16;
                    switch ((ins >> 21) & 0x1F) {
                        case 0x0:
                            // sfeqi
                            this.SR_F = this.r.get((ins >> 16) & 0x1F) ==  imm;
                            break;
                        case 0x1:
                            // sfnei
                            this.SR_F = this.r.get((ins >> 16) & 0x1F) !=  imm;
                            break;
                        case 0x2:
                            // sfgtui
                            this.SR_F = (this.r.get((ins >> 16) & 0x1F) & M_UINT32)  > (imm & M_UINT32);
                            break;
                        case 0x3:
                            // sfgeui
                            this.SR_F = (this.r.get((ins >> 16) & 0x1F) & M_UINT32) >=  (imm & M_UINT32);
                            break;
                        case 0x4:
                            // sfltui
                            this.SR_F = (this.r.get((ins >> 16) & 0x1F) & M_UINT32) < (imm & M_UINT32);
                            break;
                        case 0x5:
                            // sfleui
                            this.SR_F = (this.r.get((ins >> 16) & 0x1F) & M_UINT32) <=  (imm & M_UINT32);
                            break;
                        case 0xa:
                            // sfgtsi
                            this.SR_F = this.r.get((ins >> 16) & 0x1F)  > imm;
                            break;
                        case 0xb:
                            // sfgesi
                            this.SR_F = this.r.get((ins >> 16) & 0x1F) >=  imm;
                            break;
                        case 0xc:
                            // sfltsi
                            this.SR_F = this.r.get((ins >> 16) & 0x1F)  < imm;
                            break;
                        case 0xd:
                            // sflesi
                            this.SR_F = this.r.get((ins >> 16) & 0x1F) <=  imm;
                            break;
                        default:
                            message.Debug("Error: sf...i not supported yet");
                            message.Abort(this);
                            break;
                    }
                    break;

                // mtspr
                case 0x30:
                    imm = (ins & 0x7FF) | ((ins >> 10) & 0xF800);
                    this.pc = this.nextpc++;
                    this.delayed_inst = false;
                    int x = this.r.get((ins >> 11) & 0x1F);
                    this.SetSPR(this.r.get((ins >> 16) & 0x1F)  | imm, x ); // could raise an exception
                    continue;

                // floating point instructions
                case 0x32:
                    rA = (ins >> 16) & 0x1F;
                    rB = (ins >> 11) & 0x1F;
                    rD = (ins >> 21) & 0x1F;
                    switch (ins & 0xFF) {
                        case 0x0:
                            // lf.add.s
                            this.f.put(rD, this.f.get(rA) + this.f.get(rB));
                            break;
                        case 0x1:
                            // lf.sub.s
                            this.f.put(rD, this.f.get(rA) - this.f.get(rB));
                            break;
                        case 0x2:
                            // lf.mul.s
                            this.f.put(rD, this.f.get(rA) * this.f.get(rB));
                            break;
                        case 0x3:
                            // lf.div.s
                            this.f.put(rD, this.f.get(rA) / this.f.get(rB));
                            break;
                        case 0x4:
                            // lf.itof.s
                            this.f.put(rD, this.r.get(rA));
                            break;
                        case 0x5:
                            // lf.ftoi.s
                            this.r.put(rD, (int) this.f.get(rA));
                            break;
                        case 0x7:
                            // lf.madd.s
                            this.f.put(rD, this.f.get(rD) + this.f.get(rA) * this.f.get(rB));
                            break;
                        case 0x8:
                            // lf.sfeq.s
                            this.SR_F = this.f.get(rA) == this.f.get(rB);
                            break;
                        case 0x9:
                            // lf.sfne.s
                            this.SR_F = this.f.get(rA) != this.f.get(rB);
                            break;
                        case 0xa:
                            // lf.sfgt.s
                            this.SR_F = this.f.get(rA) > this.f.get(rB);
                            break;
                        case 0xb:
                            // lf.sfge.s
                            this.SR_F = this.f.get(rA) >= this.f.get(rB);
                            break;
                        case 0xc:
                            // lf.sflt.s
                            this.SR_F = this.f.get(rA) < this.f.get(rB);
                            break;
                        case 0xd:
                            // lf.sfle.s
                            this.SR_F = this.f.get(rA) <= this.f.get(rB);
                            break;
                        default:
                            message.Debug("Error: lf. function " + message.addrToString(ins & 0xFF) + " not supported yet");
                            message.Abort(this);
                            break;
                    }
                    break;

                // swa
                case 0x33:
                    imm = ((((ins >> 10) & 0xF800) | (ins & 0x7FF)) << 16) >> 16;
                    this.r.put(32,  this.r.get((ins >> 16) & 0x1F)  + imm);
                    if (0 != (this.r.get(32)  & 0x3)) {
                        message.Debug("Error in sw: no aligned memory access");
                        message.Abort(this);
                    }
                    this.r.put(33,  (int)this.DTLBLookup(this.r.get(32) , true));
                    if (this.r.get(33)  == -1) {
                        break;
                    }
                    this.SR_F = this.r.get(33) == this.EA;
                    this.EA = -1;
                    if (!this.SR_F) {
                        break;
                    }
                    if (this.r.get(33)  > 0) {
                        this.ram.int32mem.put(this.r.get(33) >> 2, this.r.get((ins >> 11) & 0x1F));
                    } else {
                        ram.Write32Big(this.r.get(33), this.r.get((ins >> 11) & 0x1F));
                    }
                    break;

                // sw
                case 0x35:
                    imm = ((((ins >> 10) & 0xF800) | (ins & 0x7FF)) << 16) >> 16;
                    this.r.put(32,  this.r.get((ins >> 16) & 0x1F)  + imm);
                    if (0 != (this.r.get(32) & 0x3)) {
                        message.Debug("Error in sw: no aligned memory access");
                        message.Abort(this);
                    }
                    this.r.put(33,  (int)this.DTLBLookup(this.r.get(32) , true));
                    if (this.r.get(33)  == -1) {
                        break;
                    }
                    if (this.r.get(33) > 0) {
                        this.ram.int32mem.put(this.r.get(33) >> 2,  this.r.get((ins >> 11) & 0x1F));
                    } else {
                        ram.Write32Big(this.r.get(33) , this.r.get((ins >> 11) & 0x1F) );
                    }
                    break;

                // sb
                case 0x36:
                    imm = ((((ins >> 10) & 0xF800) | (ins & 0x7FF)) << 16) >> 16;
                    this.r.put(32,  this.r.get((ins >> 16) & 0x1F)  + imm);
                    this.r.put(33,  (int)this.DTLBLookup(this.r.get(32) , true));
                    if (this.r.get(33)  == -1) {
                        break;
                    }
                    ram.Write8Big(this.r.get(33), (byte) this.r.get((ins >> 11) & 0x1F));
                    break;

                // sh
                case 0x37:
                    imm = ((((ins >> 10) & 0xF800) | (ins & 0x7FF)) << 16) >> 16;
                    this.r.put(32,  this.r.get((ins >> 16) & 0x1F)  + imm);
                    this.r.put(33,  (int)this.DTLBLookup(this.r.get(32) , true));
                    if (this.r.get(33)  == -1) {
                        break;
                    }
                    ram.Write16Big(this.r.get(33), (short) this.r.get((ins >> 11) & 0x1F));
                    break;

                // three operands instructions
                case 0x38:
                    rA = this.r.get((ins >> 16) & 0x1F) ;
                    rB = this.r.get((ins >> 11) & 0x1F) ;
                    rindex = (ins >> 21) & 0x1F;
                    //message.Debug("3op " + (ins & 0x3CF) + ": " + rindex + " <- " + rA + " . " + rB);
                    switch (ins & 0x3CF) {
                        case 0x0:
                            // add signed
                            this.r.put(rindex,  rA + rB);
                            this.SR_CY = this.r.get(rindex)  < rA;
                            this.SR_OV = 0 != (((rA ^ rB ^ -1) & (rA ^ this.r.get(rindex) )) & 0x80000000);
                            //TODO overflow and carry
                            break;
                        case 0x2:
                            // sub signed
                            this.r.put(rindex,  rA - rB);
                            //TODO overflow and carry
                            this.SR_CY = (rB > rA);
                            this.SR_OV = 0 != (((rA ^ rB) & (rA ^ this.r.get(rindex) )) & 0x80000000);
                            break;
                        case 0x3:
                            // and
                            this.r.put(rindex,  rA & rB);
                            break;
                        case 0x4:
                            // or
                            this.r.put(rindex,  rA | rB);
                            break;
                        case 0x5:
                            // or
                            this.r.put(rindex,  rA ^ rB);
                            break;
                        case 0x8:
                            // sll
                            this.r.put(rindex,  rA << (rB & 0x1F));
                            break;
                        case 0x48:
                            // srl not signed
                            this.r.put(rindex,  rA >>> (rB & 0x1F));
                            break;
                        case 0xf:
                            // ff1
                            this.r.put(rindex,  0);
                            for (int i = 0; i < 32; i++) {
                                if (0 != (rA & (1 << i))) {
                                    this.r.put(rindex,  i + 1);
                                    break;
                                }
                            }
                            break;
                        case 0x88:
                            // sra signed
                            this.r.put(rindex,  rA >> (rB & 0x1F));
                            break;
                        case 0x10f:
                            // fl1
                            this.r.put(rindex,  0);
                            for (int i = 31; i >= 0; i--) {
                                if (0 != (rA & (1 << i))) {
                                    this.r.put(rindex,  i + 1);
                                    break;
                                }
                            }
                            break;
                        case 0x306:
                        {
                            // mul signed (specification seems to be wrong)
                            // this is a hack to do 32 bit signed multiply. Seems to work but needs to be tested.
                            this.r.put(rindex,  (rA >> 0) * rB);
                            int rAl = rA & 0xFFFF;
                            int rBl = rB & 0xFFFF;
                            this.r.put(rindex,  this.r.get(rindex)  & 0xFFFF0000 | ((rAl * rBl) & 0xFFFF));
                            long result = rA * rB;
                            this.SR_OV = (result < (-2147483647 - 1)) || (result > (2147483647));
                            long uresult = rA * rB;
                            this.SR_CY = (uresult > (4294967295L));
                        }
                        break;
                        case 0x30a:
                            // divu (specification seems to be wrong)
                            this.SR_CY = rB == 0;
                            this.SR_OV = false;
                            if (!this.SR_CY) {
                                this.r.put(rindex,  (int)(((rA & M_UINT32) / (rB & M_UINT32)) & 0xFFFFFF));
                            }
                            break;
                        case 0x309:
                            // div (specification seems to be wrong)
                            this.SR_CY = rB == 0;
                            this.SR_OV = false;
                            if (!this.SR_CY) {
                                this.r.put(rindex,  rA / rB);
                            }

                            break;
                        default:
                            message.Debug("Error: op38 opcode not supported yet");
                            message.Abort(this);
                            break;
                    }
                    break;

                // sf....
                case 0x39:
                    switch ((ins >> 21) & 0x1F) {
                        case 0x0:
                            // sfeq
                            this.SR_F = this.r.get((ins >> 16) & 0x1F) == this.r.get((ins >> 11) & 0x1F) ;
                            break;
                        case 0x1:
                            // sfne
                            this.SR_F = this.r.get((ins >> 16) & 0x1F) != this.r.get((ins >> 11) & 0x1F) ;
                            break;
                        case 0x2:
                            // sfgtu
                            this.SR_F = (this.r.get((ins >> 16) & 0x1F) & M_UINT32) > (this.r.get((ins >> 11) & 0x1F) & M_UINT32);
                            break;
                        case 0x3:
                            // sfgeu
                            this.SR_F = (this.r.get((ins >> 16) & 0x1F) & M_UINT32) >= (this.r.get((ins >> 11) & 0x1F) & M_UINT32);
                            break;
                        case 0x4:
                            // sfltu
                            this.SR_F = (this.r.get((ins >> 16) & 0x1F) & M_UINT32) < (this.r.get((ins >> 11) & 0x1F) & M_UINT32);
                            break;
                        case 0x5:
                            // sfleu
                            this.SR_F = (this.r.get((ins >> 16) & 0x1F) & M_UINT32) <= (this.r.get((ins >> 11) & 0x1F) & M_UINT32);
                            break;
                        case 0xa:
                            // sfgts
                            this.SR_F = this.r.get((ins >> 16) & 0x1F) > this.r.get((ins >> 11) & 0x1F) ;
                            break;
                        case 0xb:
                            // sfges
                            this.SR_F = this.r.get((ins >> 16) & 0x1F) >= this.r.get((ins >> 11) & 0x1F);
                            break;
                        case 0xc:
                            // sflts
                            this.SR_F = this.r.get((ins >> 16) & 0x1F) < this.r.get((ins >> 11) & 0x1F) ;
                            break;
                        case 0xd:
                            // sfles
                            this.SR_F = this.r.get((ins >> 16) & 0x1F) <= this.r.get((ins >> 11) & 0x1F) ;
                            break;
                        default:
                            message.Debug("Error: sf.... function supported yet");
                            message.Abort(this);
                    }
                    break;

                default:
                    System.out.format("illegal instruction pc=%8x ins=%8x\n", this.pc << 2, ins);
                    message.Abort(this);
                    break;
            }

//            if(this.pc << 2 == 0x005501d4) this.print = true;
//            if(print) {
//                System.out.print(message.addrToString(this.pc << 2) + " regs ");
//                for (int i = 0; i < 8; i++)
//                    System.out.print(i + "=" + message.addrToString(this.r.get(i)) + " ");
//                System.out.println();
//            }

            this.pc = this.nextpc++;
            this.delayed_inst = false;

        } while(--steps != 0);
    }



    private void SetSPR(int idx, int x) {
        int address = idx & 0x7FF;
        int group = (idx >> 11) & 0x1F;

        switch (group) {
            case 0:
                if (address == SPR_SR) {
                    this.SetFlags(x);
                }
                this.group0.put(address, x);
                break;
            case 1:
                // Data MMU
               // x &= 0x0fffffff; // remove this
                this.group1.put(address, x);
                //message.Debug(message.addrToString(this.pc << 2)  + " set DTLB " + message.addrToString(address) + " = " + message.addrToString(x));
                break;
            case 2:
                // ins MMU
                this.group2.put(address, x);
               // message.Debug(message.addrToString(this.pc << 2)  + " set ITLB " + message.addrToString(address) + " = " + message.addrToString(x));
                break;
            case 3:
                // data cache, not supported
            case 4:
                // ins cache, not supported
                break;
            case 8:
                break;
            case 9:
                // pic
                switch (address) {
                    case 0:
                        this.PICMR = x | 0x3; // we use non maskable interrupt here
                        // check immediate for interrupt
                        if (this.SR_IEE) {
                            if (0 != (this.PICMR & this.PICSR)) {
                                message.Debug("Error in SetSPR: Direct triggering of interrupt exception not supported?");
                                message.Abort(this);
                            }
                        }
                        break;
                    case 2: // PICSR
                        break;
                    default:
                        message.Debug("Error in SetSPR: interrupt address not supported");
                        message.Abort(this);
                }
                break;
            case 10:
                //tick timer
                switch (address) {
                    case 0:
                        this.TTMR = x;
                        if(((this.TTMR >> 30)&3) != 0x3) {
                            // TODO Marcin was commented out in the original
                            // looks like it's not lethal

                            //message.Debug("Error in SetSPR: Timer mode other than continuous not supported");
                            //message.Abort(this);
                        }
                        break;
                    case 1:
                        this.TTCR = x;
                        break;
                    default:
                        message.Debug("Error in SetSPR: Tick timer address not supported");
                        message.Abort(this);
                        break;
                }
                break;

            default:
                message.Debug("Error in SetSPR: group " + group + " not found");
                message.Abort(this);
                break;
        }
    }

    private int GetSPR(int idx) {
        int address = idx & 0x7FF;
        int group = (idx >> 11) & 0x1F;

        switch (group) {
            case 0:
                if (address == SPR_SR) {
                    return this.GetFlags();
                }
                return this.group0.get(address);
            case 1:
                return this.group1.get(address);
            case 2:
                return this.group2.get(address);
            case 8:
                return 0x0;

            case 9:
                // pic
                switch (address) {
                    case 0:
                        return this.PICMR;
                    case 2:
                        return this.PICSR;
                    default:
                        message.Debug("Error in GetSPR: PIC address unknown");
                        message.Abort(this);
                        break;
                }
                break;

            case 10:
                // tick Timer
                switch (address) {
                    case 0:
                        return this.TTMR;
                    case 1:
                        return this.TTCR; // or clock
                    default:
                        message.Debug("Error in GetSPR: Tick timer address unknown");
                        message.Abort(this);
                        break;
                }
                break;
            default:
                message.Debug("Error in GetSPR: group " + group +  " unknown");
                message.Abort(this);
                break;
        }
        return -1; // shouldn't happen ever
    }

    private long DTLBLookup(int addr, boolean write) {
        if (!this.SR_DME) {
            return addr;
        }
        // pagesize is 8192 bytes
        // nways are 1
        // nsets are 64

        int setindex = (addr >> 13) & 63;
        int tlmbr = this.group1.get(0x200 | setindex); // match register
        if (((tlmbr & 1) == 0) || ((tlmbr >> 19) != (addr >> 19))) {
            this.Exception(CPUException.EXCEPT_DTLBMISS, addr);
            return -1;
        }
        // set lru
        if (0 != (tlmbr & 0xC0)) {
            message.Debug("Error: LRU ist not supported");
            message.Abort(this);
        }

        int tlbtr = this.group1.get(0x280 | setindex); // translate register

        // check if supervisor mode
        if (this.SR_SM) {
            if (((!write) && (0 == (tlbtr & 0x100))) || // check if SRE
                 ((write) && (0 == (tlbtr & 0x200)))) { // check if SWE
                this.Exception(CPUException.EXCEPT_DPF, addr);
                return -1;
            }
        } else {
            if (((!write) && (0 == (tlbtr & 0x40))) || // check if URE
                 ((write) && (0 == (tlbtr & 0x80)))) {     // check if UWE
                this.Exception(CPUException.EXCEPT_DPF, addr);
                return -1;
            }
        }
        return (((tlbtr & 0xFFFFE000) | (addr & 0x1FFF)) & 0xFFFFFFFFL);
    }

    private void Exception(int ex_type, int addr) {
        int except_vector = ex_type | (this.SR_EPH ? 0xf0000000 : 0x0);
        message.Debug("Info: Raising Exception " + CPUException.toString(ex_type) + "(" + message.addrToString(ex_type)+") at " + message.addrToString(addr));

        this.SetSPR(SPR_EEAR_BASE, addr);
        this.SetSPR(SPR_ESR_BASE, this.GetFlags());

        this.EA = -1;
        this.SR_OVE = false;
        this.SR_SM = true;
        this.SR_IEE = false;
        this.SR_TEE = false;
        this.SR_DME = false;

        this.nextpc = except_vector>>2;

        switch (ex_type) {
            case CPUException.EXCEPT_RESET:
                break;

            case CPUException.EXCEPT_ITLBMISS:
            case CPUException.EXCEPT_IPF:
            case CPUException.EXCEPT_DTLBMISS:
            case CPUException.EXCEPT_DPF:
            case CPUException.EXCEPT_BUSERR:
            case CPUException.EXCEPT_TICK:
            case CPUException.EXCEPT_INT:
            case CPUException.EXCEPT_TRAP:
                this.SetSPR(SPR_EPCR_BASE, (this.pc<<2) - (this.delayed_inst ? 4 : 0));
                break;

            case CPUException.EXCEPT_SYSCALL:
                this.SetSPR(SPR_EPCR_BASE, (this.pc<<2) + 4 - (this.delayed_inst ? 4 : 0));
                break;
            default:
                message.Debug("Error in Exception: exception type not supported");
                message.Abort(this);
        }

        // Handle restart mode timer
        if (ex_type == CPUException.EXCEPT_TICK && (this.TTMR >> 30) == 0x1) {
            this.TTCR = 0;
        }

        this.delayed_inst = false;
        this.SR_IME = false;    
    }

    private int GetFlags() {
        int x = 0x0;
        x |= this.SR_SM  ? (1     ) : 0;
        x |= this.SR_TEE ? (1 << 1) : 0;
        x |= this.SR_IEE ? (1 << 2) : 0;
        x |= this.SR_DCE ? (1 << 3) : 0;
        x |= this.SR_ICE ? (1 << 4) : 0;
        x |= this.SR_DME ? (1 << 5) : 0;
        x |= this.SR_IME ? (1 << 6) : 0;
        x |= this.SR_LEE ? (1 << 7) : 0;
        x |= this.SR_CE  ? (1 << 8) : 0;
        x |= this.SR_F   ? (1 << 9) : 0;
        x |= this.SR_CY  ? (1 << 10) : 0;
        x |= this.SR_OV  ? (1 << 11) : 0;
        x |= this.SR_OVE ? (1 << 12) : 0;
        x |= this.SR_DSX ? (1 << 13) : 0;
        x |= this.SR_EPH ? (1 << 14) : 0;
        x |= this.SR_FO  ? (1 << 15) : 0;
        x |= this.SR_SUMRA ? (1 << 16) : 0;
        x |= (this.SR_CID << 28);
        return x;
    }

    private void SetFlags(int x) {
        boolean old_SR_IEE = this.SR_IEE;
        boolean old_SR_DME = this.SR_DME;
        boolean old_SR_IME = this.SR_IME;

        this.SR_SM  = 0 != (x &   1);
        this.SR_TEE = 0 != (x & (1 << 1));
        this.SR_IEE = 0 != (x & (1 << 2));
        this.SR_DCE = 0 != (x & (1 << 3));
        this.SR_ICE = 0 != (x & (1 << 4));
        this.SR_DME = 0 != (x & (1 << 5));
        this.SR_IME = 0 != (x & (1 << 6));
        this.SR_LEE = 0 != (x & (1 << 7));
        this.SR_CE  = 0 != (x & (1 << 8));
        this.SR_F   = 0 != (x & (1 << 9));
        this.SR_CY  = 0 != (x & (1 << 10));
        this.SR_OV  = 0 != (x & (1 << 11));
        this.SR_OVE = 0 != (x & (1 << 12));
        this.SR_DSX = 0 != (x & (1 << 13));
        this.SR_EPH = 0 != (x & (1 << 14));
        this.SR_FO  = true;
        this.SR_SUMRA = 0 != (x & (1 << 16));
        this.SR_CID = (x >> 28) & 0xF;

        if (this.SR_LEE) {
            message.Debug("little endian not supported");
            message.Abort(this);
        }
        if (0 != this.SR_CID) {
            message.Debug("context id not supported");
            message.Abort(this);
        }
        if (this.SR_EPH) {
            message.Debug("exception prefix not supported");
            message.Abort(this);
        }
        if (this.SR_DSX) {
            message.Debug("delay slot exception not supported");
            message.Abort(this);
        }
        if (this.SR_IEE && old_SR_IEE) {
            this.CheckForInterrupt();
        }
    }

    private void CheckForInterrupt() {
        if (!this.SR_IEE) {
            return;
        }
        if (0 != (this.PICMR & this.PICSR)) {
            this.Exception(CPUException.EXCEPT_INT, this.group0.get(SPR_EEAR_BASE));
            this.pc = this.nextpc++;
        }
    }

    public void RaiseInterrupt(int line, int cpuid) {
        this.PICSR |= 1 << line;
        this.CheckForInterrupt();
    }

    public void ClearInterrupt(int line, int cpuid) {
        this.PICSR &= ~(1 << line);
    }

    public void stop() {
        this.stop_flag = true;
    }
}
