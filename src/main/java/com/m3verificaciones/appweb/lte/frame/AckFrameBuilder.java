package com.m3verificaciones.appweb.lte.frame;

import com.m3verificaciones.appweb.lte.dto.ParsedFrame;
import org.springframework.stereotype.Component;

@Component
public class AckFrameBuilder {
    private static final int START_BYTE = 0x88;
    private static final int END_BYTE = 0x22;
    private static final int COMM_SERVER = 0x01;
    private static final int ACK_CONFIRMATION = 0x01;

    public byte[] build(ParsedFrame frame){
        /*
        Estructure of ACK frame:
        (17 bytes, DataLength=0):
         * Start(1) + IMEI(7) + COMM(1) + ACK(1) + FCNT(1) + CMD(1)
         * + Reserve(2) + DataLength(2) + Checksum(1) + End(1)
        */

        byte[] ack = new byte[17];
        int i = 0;

        ack[i++] = (byte) START_BYTE;

        // IMEI (7 bytes, last 14 digits of the IMEI)
        byte[] imeiRaw = frame.getImeiRaw();
        for(byte b : imeiRaw){
            ack[i++] = b;
        }
        ack[i++] = (byte) COMM_SERVER;
        ack[i++] = (byte) ACK_CONFIRMATION;
        ack[i++] = (byte) frame.getFcnt();
        ack[i++] = (byte) frame.getCmd();
        ack[i++] = 0x00; // Reserve
        ack[i++] = 0x00; // Reserve
        ack[i++] = 0x00; // DataLength high byte
        ack[i++] = 0x00; // DataLength low byte

        // Checksum (simple sum of bytes modulo 256)
        int checksum = 0;
        for(int j = 0; j < 16; j++){
            checksum = (checksum + (ack[j] & 0xFF)) % 256;
        }
        ack[i++] = (byte) checksum;
        ack[i] = (byte) END_BYTE;

        return ack;
    }
}
