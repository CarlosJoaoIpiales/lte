package com.m3verificaciones.appweb.lte.frame.bove;

import com.m3verificaciones.appweb.lte.dto.ParsedFrame;
import com.m3verificaciones.appweb.lte.frame.FrameParser;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BoveFrameParser implements FrameParser {
    private static final Logger log = LoggerFactory.getLogger(BoveFrameParser.class);

    private static final int START_BYTE = 0x88;
    private static final int END_BYTE = 0x22;
    public static final String BRAND = "BOVE";

    @Override
    public String getBrand() {
        return BRAND;
    }

    @Override
    public boolean canParse(byte[] raw) {
        return raw != null
                && raw.length >= 17
                && (raw[0] & 0xFF) == START_BYTE
                && (raw[raw.length - 1] & 0xFF) == END_BYTE;
    }

    @Override
    public ParsedFrame parse(byte[] raw) {
        if (!canParse(raw)) {
            throw new IllegalArgumentException("Invalid BOVE frame");
        }

        // Data length is at bytes 14-15 (little-endian)
        int dataLength = (raw[14] & 0xFF) | ((raw[15] & 0xFF) << 8);

        int expectedLen = 1 + 7 + 1 + 1 + 1 + 1 + 2 + 2 + dataLength + 1 + 1; // 17 + dataLength
        if (raw.length != expectedLen){
            throw new IllegalArgumentException("Invalid BOVE frame length. Expected " + expectedLen + " but got " + raw.length);
        }

        byte [] imeiRaw = Arrays.copyOfRange(raw, 1, 8); // bytes 1-7
        int comm = raw[8] & 0xFF; // byte 8
        int ack = raw[9] & 0xFF; // byte 9
        int fcnt = raw[10] & 0xFF; // byte 10
        int cmd = raw[11] & 0xFF; // byte 11
        byte[] reserve = Arrays.copyOfRange(raw, 12, 14); // bytes 12-13
        byte[] payload = Arrays.copyOfRange(raw, 16, 16 + dataLength); // bytes 16 to (16+dataLength-1)

        int checksumRx = raw[16 + dataLength] & 0xFF; // byte after payload
        int checksumCalc = calcChecksum(raw, 16 + dataLength); // calculate checksum from byte 0 to end of payload

        if(!isChecksumValid(checksumRx, checksumCalc)){
            log.warn("Checksum mismatch for BOVE frame. Received: 0x{}, Calculated: 0x{}", String.format("%02X", checksumRx), String.format("%02X", checksumCalc));
        }

        return BoveParsedFrame.builder()
                .rawBytes(raw)
                .brand(BRAND)
                .imei14(bcdBytesToDigits(imeiRaw))
                .imeiRaw(imeiRaw)
                .comm(comm)
                .is4g(comm == 0x00)
                .ack(ack)
                .ackEnabled(ack == 0x00)
                .fcnt(fcnt)
                .cmd(cmd)
                .dataLength(dataLength)
                .payload(payload)
                .checksumOk(checksumRx == checksumCalc)
                .reserveHex(toHex(reserve))
                .checksumReceivedHex(String.format("%02X", checksumRx))
                .checksumCalculatedHex(String.format("%02X", checksumCalc))
                .build();
    }

    // -- Utils static methods reuse for the decoders--

    public static String bcdBytesToDigits(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append((b >> 4) & 0x0F); // high nibble
            sb.append(b & 0x0F); // low nibble
        }
        return sb.toString();
    }

    public static String bcdLittleEndianToDigits(byte[] data) {
        byte[] reversed = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            reversed[i] = data[data.length - 1 - i];
        }
        return bcdBytesToDigits(reversed);
    }

    public static int littleEndianToInt(byte[] data) {
        int result = 0;
        for (int i = 0; i < data.length; i++) {
            result |= (data[i] & 0xFF) << (8 * i);
        }
        return result;
    }

    public static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    private static int calcChecksum(byte[] data, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum = (sum + (data[i] & 0xFF)) % 256;
        }
        return sum;
    }

    private static boolean isChecksumValid(int received, int calculated) {
        return received == calculated;
    }
}
