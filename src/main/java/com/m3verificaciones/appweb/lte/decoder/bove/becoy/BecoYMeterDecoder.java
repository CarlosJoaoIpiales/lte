package com.m3verificaciones.appweb.lte.decoder.bove.becoy;

import com.m3verificaciones.appweb.lte.decoder.PayloadDecoder;
import com.m3verificaciones.appweb.lte.dto.DecodedReading;
import com.m3verificaciones.appweb.lte.frame.bove.BoveFrameParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

@Component
public class BecoYMeterDecoder implements PayloadDecoder {

    private static final Logger log = LoggerFactory.getLogger(BecoYMeterDecoder.class);

    private static final String BRAND = "BOVE";
    private static final String MODEL = "BECOY";
    private static final int CMD_METER_DATA = 0x00;
    private static final int PAYLOAD_LENGTH = 24;

    private static final Map<Integer, Double> UNIT_MAP = Map.of(
        0x2B, 0.001,
        0x2C, 0.01,
        0x2D, 0.1,
        0x2E, 1.0,
        0x35, 0.0001
    );

    private static final Map<Integer, Integer> RSSI_MAP = buildRssiMap();

    @Override
    public String getBrand() {
        return BRAND;
    }

    @Override
    public String getModel() {
        return MODEL;
    }

    @Override
    public boolean supports(int cmd, byte[] payload) {
        // Normal Mode (water meter): CMD 0x00 with 24-byte payload
        return cmd == CMD_METER_DATA && payload != null && payload.length == PAYLOAD_LENGTH;
    }

    @Override
    public DecodedReading decode(byte[] payload) {
        // ADDR: bytes 0-3 (BCD, little-endian) -> serial number
        String serialMeter = BoveFrameParser.bcdLittleEndianToDigits(Arrays.copyOfRange(payload, 0, 4));

        // Totalizer: bytes 4-7 (BCD, little-endian)
        String totDigits = BoveFrameParser.bcdLittleEndianToDigits(Arrays.copyOfRange(payload, 4, 8));

        //Uplink interval: bytes 8-9 -> interval send messaes in minutes
        int intervalMin = BoveFrameParser.littleEndianToInt(Arrays.copyOfRange(payload, 8, 10));

        // Unit indicator: byte 10
        int unitRaw = payload[10] & 0xFF;
        Double unitFactor = UNIT_MAP.get(unitRaw);

        // ST1 and ST2: bytes 11-12
        int st1 = payload[11] & 0xFF;
        int st2 = payload[12] & 0xFF;

        // ICCID: bytes 13-22 (BCD, little-endian)
        String iccid = BoveFrameParser.bcdBytesToDigits(Arrays.copyOfRange(payload, 13, 23));

        // RSSI: byte 23
        int rssiRaw = payload[23] & 0xFF;

        // Totalizer water consumption in cubic meters with decimals based on unit factor
        Double consumption = null;
        if(unitFactor != null){
            try{
                consumption = Long.parseLong(totDigits) * unitFactor;
            }catch(NumberFormatException e){
                log.warn("Failed to parse totalizer digits '{}' for serial '{}'", totDigits, serialMeter);
            }
        }

        log.info("Decoded BecoY reading - Serial: {}, Consumption: {}, IntervalMin: {}, UnitFactor: {}, ICCID: {}, RSSI: {}",
                serialMeter, consumption, intervalMin, unitFactor, iccid, RSSI_MAP.get(rssiRaw));

        return DecodedReading.builder()
                .serialMeter(serialMeter)
                .consumption(consumption)
                .uplinkIntervalMinutes(intervalMin)
                .unitFactor(unitFactor)
                .iccId(iccid)
                .rssiDbm(RSSI_MAP.get(rssiRaw))
                // ST1 - alarms flow/fisics
                .leakageAlarm((st1 >> 2 & 1) == 1)
                .burstAlarm((st1 >> 3 & 1) == 1)
                .tamperAlarm((st1 >> 4 & 1) == 1)
                .freezingAlarm((st1 >> 5 & 1) == 1)
                // ST2 - alarms state of device
                .lowBatteryAlarm((st2 >> 0 & 1) == 1)
                .emptyPipeAlarm((st2 >> 1 & 1) == 1)
                .reverseFlowAlarm((st2 >> 2 & 1) == 1)
                .overRangeAlarm((st2 >> 3 & 1) == 1)
                .temperatureAlarm((st2 >> 4 & 1) == 1)
                .eerrorAlarm((st2 >> 5 & 1) == 1)
                .build();
    }

    private static Map<Integer, Integer> buildRssiMap() {
        java.util.HashMap<Integer, Integer> m = new java.util.HashMap<>();

        // RSSI mapping based on BecoY documentation: 0 -> -113 dBm, each increment of 1 adds 2 dBm
        for (int i = 0; i <= 0x1F; i++) {
            m.put(i, -113 + (i * 2));
        }
        return m;
    }
}
