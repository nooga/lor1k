package com.nooga.lor1k.devices;

import com.nooga.lor1k.CPU;
import com.nooga.lor1k.MessageBus;
import com.nooga.lor1k.io.IOListener;
import jdk.nashorn.internal.objects.NativeArray;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class UART extends Device {

    // Register offsets
    private static final byte UART_RXBUF = 0; /* R: Rx buffer, DLAB=0 */
    private static final byte UART_TXBUF = 0; /* W: Tx buffer, DLAB=0 (also called transmitter hoilding register */
    private static final byte UART_DLL   = 0; /* R/W: Divisor Latch Low, DLAB=1 */
    private static final byte UART_DLH   = 1; /* R/W: Divisor Latch High, DLAB=1 */
    private static final byte UART_IER   = 1; /* R/W: Interrupt Enable Register */
    private static final byte UART_IIR   = 2; /* R: Interrupt ID Register */
    private static final byte UART_FCR   = 2; /* W: FIFO Control Register */
    private static final byte UART_LCR   = 3; /* R/W: Line Control Register */
    private static final byte UART_MCR   = 4; /* W: Modem Control Register */
    private static final byte UART_LSR   = 5; /* R: Line Status Register */
    private static final byte UART_MSR   = 6; /* R: Modem Status Register */
    private static final byte UART_SCR   = 7; /* R/W: Scratch Register*/

    // Line Status register bits
    private static final byte UART_LSR_DATA_READY        = 0x01; // data available
    private static final byte UART_LSR_TX_EMPTY          = 0x20; // TX (THR) buffer is empty
    private static final byte UART_LSR_TRANSMITTER_EMPTY = 0x40; // TX empty and line is idle
    private static final byte UART_LSR_INT_ANY           = 0x1E; // Any of the lsr-interrupt-triggering status bits

    // Interrupt enable register bits
    private static final byte UART_IER_MSI  = 0x08; /* Modem Status Changed int. */
    private static final byte UART_IER_RLSI = 0x04; /* Enable Break int. Enable receiver line status interrupt */
    private static final byte UART_IER_THRI = 0x02; /* Enable Transmitter holding register int. */
    private static final byte UART_IER_RDI  = 0x01; /* Enable receiver data interrupt */

    // Interrupt identification register bits
    private static final byte UART_IIR_MSI    = 0x00; /* Modem status interrupt (Low priority). Reset by MSR read */
    private static final byte UART_IIR_NO_INT = 0x01;
    private static final byte UART_IIR_THRI   = 0x02; /* Transmitter holding register empty. Reset by IIR read or THR write */
    private static final byte UART_IIR_RDI    = 0x04; /* Receiver data interrupt. Reset by RBR read */
    private static final byte UART_IIR_RLSI   = 0x06; /* Receiver line status interrupt (High p.). Reset by LSR read */
    private static final byte UART_IIR_CTI    = 0x0c; /* Character timeout. Reset by RBR read */

    // Line control register bits
    private static final short UART_LCR_DLAB   = 0x80; /* Divisor latch access bit */

    // Modem control register bits
    private static final byte UART_MCR_DTR = 0x01; /* Data Terminal Ready - Kernel ready to receive */
    private static final byte UART_MCR_RTS = 0x02; /* Request To Send - Kernel ready to receive */

    // Modem status register bits
    private static final short UART_MSR_DCD       = 0x80; /* Data Carrier Detect */
    private static final byte UART_MSR_DSR       = 0x20; /* Data set Ready */
    private static final byte UART_MSR_DELTA_DSR = 0x2;
    private static final byte UART_MSR_CTS       = 0x10; /* Clear to Send */
    private static final byte UART_MSR_DELTA_CTS = 0x1;
    private final CPU intdev;
    private final int intno;
    private byte LSR;
    private byte LCR;
    private byte DLL;
    private byte DLH;
    private BlockingQueue<Byte> txbuf;
    private BlockingQueue<Byte> rxbuf;
    private byte IIR;
    private byte IER;
    private byte MSR;
    private byte MCR;
    private byte FCR;

    MessageBus message;
    private int ints;

    public UART(MessageBus message, CPU intdev, int intno) {
        this.message = message;
        this.intdev = intdev;
        this.intno = intno;
        this.reset();
    }

    @Override
    public void reset() {
        super.reset();
        this.LCR = 0x3; // Line Control, reset, character has 8 bits
        this.LSR = UART_LSR_TRANSMITTER_EMPTY | UART_LSR_TX_EMPTY; // Transmitter serial register empty and Transmitter buffer register empty
        this.MSR = (byte)(UART_MSR_DCD | UART_MSR_DSR | UART_MSR_CTS); // modem status register
        this.ints = 0x0; // internal interrupt pending register
        this.IIR = UART_IIR_NO_INT; // Interrupt Identification, no interrupt
        this.IER = 0x0; //Interrupt Enable
        this.DLL = 0x0;
        this.DLH = 0x0;
        this.FCR = 0x0; // FIFO Control;
        this.MCR = 0x0; // Modem Control
        
        this.rxbuf = new ArrayBlockingQueue<>(0x100000);
        this.txbuf = new ArrayBlockingQueue<>(0x100000);
    }

    public void write(byte[] bs) {
        for(byte b : bs) {
            try {
                this.rxbuf.put(b);
                this.LSR = (byte)((this.LSR | UART_LSR_DATA_READY) & 0xFF);
                this.ThrowInterrupt(UART_IIR_CTI);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void step(IOListener listener) {
        if(!this.txbuf.isEmpty()) {
            Byte b;
            byte[] bytes = new byte[this.txbuf.size()];
            int j = 0;
            while((b = this.txbuf.poll()) != null)
                bytes[j++] = b;

            if(listener != null)
                listener.put(bytes);

            System.out.print(new String(bytes));
        }
    }

    @Override
    public int readReg32(int addr) {
        return 0;
    }

    @Override
    public void writeReg32(int addr, int value) {
    }

    private void ThrowInterrupt(byte line) {
        this.ints |= (1 << line);
        this.CheckInterrupt();
    }

    private void ClearInterrupt(byte line) {
        this.ints &= (1 << line);
        this.CheckInterrupt();
    }

    private void CheckInterrupt() {
        if (0 != ((this.LSR & UART_LSR_INT_ANY)) && 0 != ((this.IER & UART_IER_RLSI))) {
            this.IIR = UART_IIR_RLSI;
            this.intdev.RaiseInterrupt(this.intno,0);
        } else
        if (0 != ((this.ints & (1 << UART_IIR_CTI))) && 0 != ((this.IER & UART_IER_RDI))) {
            this.IIR = UART_IIR_CTI;
            this.intdev.RaiseInterrupt(this.intno,0);
        } else
        if (0 != ((this.ints & (1 << UART_IIR_THRI))) && 0 != ((this.IER & UART_IER_THRI))) {
            this.IIR = UART_IIR_THRI;
            this.intdev.RaiseInterrupt(this.intno,0);
        } else
        if (0 != ((this.ints & (1 << UART_IIR_MSI)))  && 0 != ((this.IER & UART_IER_MSI))) {
            this.IIR = UART_IIR_MSI;
            this.intdev.RaiseInterrupt(this.intno,0);
        } else {
            this.IIR = UART_IIR_NO_INT;
            this.intdev.ClearInterrupt(this.intno,0);
        }
    }

    @Override
    public void writeReg8(int addr, byte value) {
//        if(addr != UART_TXBUF)
//            System.out.println("UART <- " + message.addrToString(addr) + " " +  String.format("%02x", value));

        value &= 0xFF;

        if (0 != (this.LCR & UART_LCR_DLAB)) {
            switch (addr) {

                case UART_DLL:
                    this.DLL = value;

                break;

                case UART_DLH:
                    this.DLH = value;

                break;
            }
        }

        switch (addr) {

            case UART_TXBUF:
                // we assume here, that the fifo is on

                // In the uart spec we reset UART_IIR_THRI now ...
                this.LSR &= ~UART_LSR_TRANSMITTER_EMPTY;
                this.LSR &= ~UART_LSR_TX_EMPTY;

                this.txbuf.add(value);

                // the data is sent immediately
                this.LSR |= UART_LSR_TRANSMITTER_EMPTY | UART_LSR_TX_EMPTY; // txbuffer is empty immediately
                this.ThrowInterrupt(UART_IIR_THRI);
                break;

            case UART_IER:
                message.Debug("UART_IER");
                // 2 = 10b ,5=101b, 7=111b
                this.IER = (byte)(value & 0x0F); // only the first four bits are valid
                this.ints &= ~(1 << UART_IER_THRI);
                // Check immediately if there is a interrupt pending
                this.CheckInterrupt();
                break;

            case UART_FCR:
                this.FCR = (byte)(value & 0xC9);
                if (0 != (value & 2)) {
                    this.ClearInterrupt(UART_IIR_CTI);
                    this.rxbuf.clear();  // clear receive fifo buffer
                }
                if (0 != (value & 4)) {
                    this.txbuf.clear();  // clear transmit fifo buffer
                    this.ClearInterrupt(UART_IIR_THRI);
                }
                break;

            case UART_LCR:
                if ((this.LCR & 3) != 3) {
                    message.Debug("Warning in UART: Data word length other than 8 bits are not supported");
                }
                this.LCR = value;
                break;

            case UART_MCR:
                this.MCR = value;
                break;

            default:
                message.Debug("Error in WriteRegister: not supported");
                message.Abort();
                break;
        }
    }

    @Override
    public byte readReg8(int addr) {

//        if(addr != UART_LSR)
//             System.out.println("UART -> " + message.addrToString(addr));

        if (0 != (this.LCR & UART_LCR_DLAB)) {  // Divisor latch access bit
            switch (addr) {
                case UART_DLL:
                    return this.DLL;

                case UART_DLH:
                    return this.DLH;
            }
        }

        short ret;
        switch (addr) {

            case UART_RXBUF:
                ret = 0x0; // if the buffer is empty, return 0
                if (!this.rxbuf.isEmpty()) {
                    ret = this.rxbuf.poll();
                }
                if (this.rxbuf.isEmpty()) {
                    this.LSR &= ~UART_LSR_DATA_READY;
                    this.ClearInterrupt(UART_IIR_CTI);
                }
                return (byte)(ret & 0xFF);


            case UART_IER:
                return (byte)(this.IER & 0x0F);


            case UART_MSR:
                ret = this.MSR;
                this.MSR &= 0xF0; // reset lowest 4 "delta" bits
                return (byte)(ret & 0xFF);


            case UART_IIR:
            {
                // the two top bits (fifo enabled) are always set
                ret = (byte)((this.IIR & 0x0F) | 0xC0);

                if (this.IIR == UART_IIR_THRI) {
                    this.ClearInterrupt(UART_IIR_THRI);
                }

                return (byte)(ret & 0xFF);

            }

            case UART_LCR:
                return this.LCR;


            case UART_LSR:
                return this.LSR;


            default:
                message.Debug("Error in ReadRegister: not supported");
                message.Abort();
                break;
        }
        return -1; //shouldn't ever happen
    }



    @Override
    public short readReg16(int addr) {
        return 0;
    }

    @Override
    public void writeReg16(int addr, short value) {

    }
}
