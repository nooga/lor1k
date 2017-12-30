package com.nooga.lor1k;

public class Disasm {
    public static String disasm(int ins) {
        int rindex;
        switch((ins >> 26) & 0x3f) {

            // j
            case 0x0:
                return String.format("l.j 0x%x", ((ins << 6) >> 6));

                // jal
            case 0x1:
                return String.format("l.jal 0x%x", ((ins << 6) >> 6));

                // bnf
            case 0x3:
                return String.format("l.bnf 0x%x", ((ins << 6) >> 6));

                // bf
            case 0x4:
                return String.format("l.bf 0x%x", ((ins << 6) >> 6));

                // nop
            case 0x5:
                // nothing ;)
                return String.format("l.nop 0x%x", (ins & 0xff));

            // movhi & macrc
            case 0x6:
                // if 16th bit is set
                if(0 != (ins & 0x10000)) {
                    // macrc
                    return String.format("!! l.macrc r%d, 0x%x",(ins >> 21) & 0x1F ,  (ins & 0xffff));
                } else {
                    // movhi
                    return String.format("l.movhi r%d, 0x%x",(ins >> 21) & 0x1F ,  (ins & 0xffff));
                }

            // sys & trap
            case 0x8:
                if ((ins & 0xFFFF0000) == 0x21000000) {
                    // trap
                    return "l.trap";
                } else {
                    // sys
                    return "l.sys";
                }

            // rfe
            case 0x9:
                return "l.rfe";

                // jr
            case 0x11:
                return String.format("l.jr r%d", (ins >> 11) & 0x1F);

                // jalr
            case 0x12:
                return String.format("l.jalr r%d", (ins >> 11) & 0x1F);

                // lwa
            case 0x1B:
                return String.format("l.lwa r%d, %d(r%d)", (ins >> 21) & 0x1F, (ins << 16) >> 16, (ins >> 16) & 0x1F);

            // lwz
            case 0x21:
                return String.format("l.lwz r%d, %d(r%d)", (ins >> 21) & 0x1F, (ins << 16) >> 16, (ins >> 16) & 0x1F);


            // lbz
            case 0x23:
                return String.format("l.lbz r%d, %d(r%d)", (ins >> 21) & 0x1F, (ins << 16) >> 16, (ins >> 16) & 0x1F);


            // lbs
            case 0x24:
                return String.format("l.lbs r%d, %d(r%d)", (ins >> 21) & 0x1F, (ins << 16) >> 16, (ins >> 16) & 0x1F);


            // lhz
            case 0x25:
                return String.format("l.lhz r%d, %d(r%d)", (ins >> 21) & 0x1F, (ins << 16) >> 16, (ins >> 16) & 0x1F);


            // lhs
            case 0x26:
                return String.format("l.lhs r%d, %d(r%d)", (ins >> 21) & 0x1F, (ins << 16) >> 16, (ins >> 16) & 0x1F);


            // addi signed
            case 0x27:
                return String.format("l.addi r%d, r%d, 0x%x", (ins >> 21) & 0x1F, (ins >> 16) & 0x1F, (ins << 16) >> 16);

            // andi
            case 0x29:
                return String.format("l.andi r%d, r%d, 0x%x", (ins >> 21) & 0x1F, (ins >> 16) & 0x1F, ins & 0xFFFF);

            // ori
            case 0x2A:
                return String.format("l.ori r%d, r%d, 0x%x", (ins >> 21) & 0x1F, (ins >> 16) & 0x1F, ins & 0xFFFF);

            // xori
            case 0x2B:
                return String.format("l.xori r%d, r%d, 0x%x", (ins >> 21) & 0x1F, (ins >> 16) & 0x1F, ins & 0xFFFF);

            // mfspr
            case 0x2D:
                return String.format("l.mfspr r%d, r%d, 0x%x", (ins >> 21) & 0x1F, (ins >> 16) & 0x1F, ins & 0xFFFF);

            // slli & rori & srai
            case 0x2E:
                switch ((ins >> 6) & 0x3) {
                    case 0:
                        // slli
                        return String.format("l.slli r%d, r%d, 0x%x", (ins >> 21) & 0x1F, (ins >> 16) & 0x1F, ins & 0x1F);
                    case 1:
                        // rori
                        return String.format("l.rori r%d, r%d, 0x%x", (ins >> 21) & 0x1F, (ins >> 16) & 0x1F, ins & 0x1F);
                    case 2:
                        // srai
                        return String.format("l.srai r%d, r%d, 0x%x", (ins >> 21) & 0x1F, (ins >> 16) & 0x1F, ins & 0x1F);
                    default:
                        return "!! 2E opcode";
                }


            // sf...i
            case 0x2F:
                int imm = (ins << 16) >> 16;
                int r = (ins >> 16) & 0x1F;
                switch ((ins >> 21) & 0x1F) {
                    case 0x0:
                        // sfeqi
                        return String.format("l.sfeqi r%d, 0x%x", r, imm);
                    case 0x1:
                        // sfnei
                        return String.format("l.sfnei r%d, 0x%x", r, imm);
                    case 0x2:
                        // sfgtui
                        return String.format("l.sfgtui r%d, 0x%x", r, imm);
                    case 0x3:
                        // sfgeui
                        return String.format("l.sfgeui r%d, 0x%x", r, imm);
                    case 0x4:
                        // sfltui
                        return String.format("l.sfltui r%d, 0x%x", r, imm);
                    case 0x5:
                        // sfleui
                        return String.format("l.sfleui r%d, 0x%x", r, imm);
                    case 0xa:
                        // sfgtsi
                        return String.format("l.sfgtsi r%d, 0x%x", r, imm);
                    case 0xb:
                        // sfgesi
                        return String.format("l.sfgesi r%d, 0x%x", r, imm);
                    case 0xc:
                        // sfltsi
                        return String.format("l.sfltsi r%d, 0x%x", r, imm);
                    case 0xd:
                        // sflesi
                        return String.format("l.sflesi r%d, 0x%x", r, imm);
                    default:
                        return "!! l.s...i";
                }


            // mtspr
            case 0x30:
                imm = (ins & 0x7FF) | ((ins >> 10) & 0xF800);
                return String.format("l.mtspr r%d, r%d, 0x%x", (ins >> 16) & 0x1F, (ins >> 11) & 0x1F, imm);

                // floating point instructions
            case 0x32:
                int rA = (ins >> 16) & 0x1F;
                int rB = (ins >> 11) & 0x1F;
                int rD = (ins >> 21) & 0x1F;
                switch (ins & 0xFF) {
                    case 0x0:
                        // lf.add.s
                        return String.format("lf.add.s r%d, r%d, r%d", rD, rA, rB);
                    case 0x1:
                        // lf.sub.s
                        return String.format("lf.sub.s r%d, r%d, r%d", rD, rA, rB);
                    case 0x2:
                        // lf.mul.s
                        return String.format("lf.mul.s r%d, r%d, r%d", rD, rA, rB);
                    case 0x3:
                        // lf.div.s
                        return String.format("lf.div.s r%d, r%d, r%d", rD, rA, rB);
                    case 0x4:
                        // lf.itof.s
                        return String.format("lf.itof.s r%d, r%d", rD, rA);
                    case 0x5:
                        // lf.ftoi.s
                        return String.format("lf.ftoi.s r%d, r%d", rD, rA);
                    case 0x7:
                        // lf.madd.s
                        return String.format("lf.madd.s r%d, r%d, r%d", rD, rA, rB);
                    case 0x8:
                        // lf.sfeq.s
                        return String.format("lf.sfeq.s r%d, r%d, r%d", rD, rA, rB);
                    case 0x9:
                        // lf.sfne.s
                        return String.format("lf.sfne.s r%d, r%d, r%d", rD, rA, rB);
                    case 0xa:
                        // lf.sfgt.s
                        return String.format("lf.sfgt.s r%d, r%d, r%d", rD, rA, rB);
                    case 0xb:
                        // lf.sfge.s
                        return String.format("lf.sfge.s r%d, r%d, r%d", rD, rA, rB);
                    case 0xc:
                        // lf.sflt.s
                        return String.format("lf.sflt.s r%d, r%d, r%d", rD, rA, rB);
                    case 0xd:
                        // lf.sfle.s
                        return String.format("lf.sfle.s r%d, r%d, r%d", rD, rA, rB);
                    default:
                        return "!! lf....s";
                }

            // swa
            case 0x33:
                imm = ((((ins >> 10) & 0xF800) | (ins & 0x7FF)) << 16) >> 16;
                return String.format("l.swa  %d(r%d), r%d", imm, (ins >> 16) & 0x1F, (ins >> 11) & 0x1F);

            // sw
            case 0x35:
                imm = ((((ins >> 10) & 0xF800) | (ins & 0x7FF)) << 16) >> 16;
                return String.format("l.sw  %d(r%d), r%d", imm, (ins >> 16) & 0x1F, (ins >> 11) & 0x1F);


            // sb
            case 0x36:
                imm = ((((ins >> 10) & 0xF800) | (ins & 0x7FF)) << 16) >> 16;
                return String.format("l.sb  %d(r%d), r%d", imm, (ins >> 16) & 0x1F, (ins >> 11) & 0x1F);


            // sh
            case 0x37:
                imm = ((((ins >> 10) & 0xF800) | (ins & 0x7FF)) << 16) >> 16;
                return String.format("l.sh  %d(r%d), r%d", imm, (ins >> 16) & 0x1F, (ins >> 11) & 0x1F);


            // three operands instructions
            case 0x38:
                rA = (ins >> 16) & 0x1F;
                rB = (ins >> 11) & 0x1F;
                rindex = (ins >> 21) & 0x1F;
                //message.Debug("3op " + (ins & 0x3CF) + ": " + rindex + " <- " + rA + " . " + rB);
                switch (ins & 0x3CF) {
                    case 0x0:
                        // add signed
                        return String.format("l.add.s r%d, r%d, r%d", rindex, rA, rB);
                    case 0x2:
                        // sub signed
                        return String.format("l.sub r%d, r%d, r%d", rindex, rA, rB);
                    case 0x3:
                        // and
                        return String.format("l.and r%d, r%d, r%d", rindex, rA, rB);
                    case 0x4:
                        // or
                        return String.format("l.or r%d, r%d, r%d", rindex, rA, rB);
                    case 0x5:
                        // or
                        return String.format("l.or r%d, r%d, r%d", rindex, rA, rB);
                    case 0x8:
                        // sll
                        return String.format("l.sll r%d, r%d, r%d", rindex, rA, rB);
                    case 0x48:
                        // srl not signed
                        return String.format("l.srl r%d, r%d, r%d", rindex, rA, rB);
                    case 0xf:
                        // ff1
                        return String.format("l.ff1 r%d, r%d, r%d", rindex, rA, rB);
                    case 0x88:
                        // sra signed
                        return String.format("l.sra r%d, r%d, r%d", rindex, rA, rB);
                    case 0x10f:
                        // fl1
                        return String.format("l.fl1 r%d, r%d, r%d", rindex, rA, rB);
                    case 0x306:
                        // mul
                        return String.format("l.mul r%d, r%d, r%d", rindex, rA, rB);
                    case 0x30a:
                        // divu (specification seems to be wrong)
                        return String.format("l.divu r%d, r%d, r%d", rindex, rA, rB);
                    case 0x309:
                        // div (specification seems to be wrong)
                        return String.format("l.div r%d, r%d, r%d", rindex, rA, rB);
                    default:
                        return "!! op 38";
                }

            // sf....
            case 0x39:
                switch ((ins >> 21) & 0x1F) {
                    case 0x0:
                        // sfeq
                        return String.format("l.sfeq r%d, r%d", (ins >> 16) & 0x1F, (ins >> 11) & 0x1F);
                    case 0x1:
                        // sfne
                        return String.format("l.sfne r%d, r%d", (ins >> 16) & 0x1F, (ins >> 11) & 0x1F);
                    case 0x2:
                        // sfgtu
                        return String.format("l.sfgtu r%d, r%d", (ins >> 16) & 0x1F, (ins >> 11) & 0x1F);
                    case 0x3:
                        // sfgeu
                        return String.format("l.sfgeu r%d, r%d", (ins >> 16) & 0x1F, (ins >> 11) & 0x1F);
                    case 0x4:
                        // sfltu
                        return String.format("l.sfltu r%d, r%d", (ins >> 16) & 0x1F, (ins >> 11) & 0x1F);
                    case 0x5:
                        // sfleu
                        return String.format("l.sfleu r%d, r%d", (ins >> 16) & 0x1F, (ins >> 11) & 0x1F);
                    case 0xa:
                        // sfgts
                        return String.format("l.sfgts r%d, r%d", (ins >> 16) & 0x1F, (ins >> 11) & 0x1F);
                    case 0xb:
                        // sfges
                        return String.format("l.sfges r%d, r%d", (ins >> 16) & 0x1F, (ins >> 11) & 0x1F);
                    case 0xc:
                        // sflts
                        return String.format("l.sflts r%d, r%d", (ins >> 16) & 0x1F, (ins >> 11) & 0x1F);
                    case 0xd:
                        // sfles
                        return String.format("l.sfles r%d, r%d", (ins >> 16) & 0x1F, (ins >> 11) & 0x1F);
                    default:
                        return "!! sf....";
                }

            default:
                return "!!!!";
        }
    }
}
