package com.m3verificaciones.appweb.lte.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.m3verificaciones.appweb.lte.dto.DecodedReading;
import com.m3verificaciones.appweb.lte.dto.ParsedFrame;
import com.m3verificaciones.appweb.lte.utils.api.MessageApiService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class MessageSaveListener {
    private static final Logger log = LoggerFactory.getLogger(MessageSaveListener.class);
    private final MessageApiService messageApiService;
    private final ObjectMapper objectMapper;

    public MessageSaveListener(MessageApiService messageApiService, ObjectMapper objectMapper) {
        this.messageApiService = messageApiService;
        this.objectMapper = objectMapper;
    }

    @EventListener
    public void on(MeterReadingEvent event) {
        try {
            ParsedFrame frame = event.getFrame();
            DecodedReading reading = event.getReading();

            String serial = event.getMeterDetails().path("serial").asText("");

            ObjectNode uplinkBody = objectMapper.createObjectNode();
            uplinkBody.put("typeMessage", "Uplink");
            uplinkBody.put("communication", "4G");
            uplinkBody.put("deviceId", frame.getImei14());
            uplinkBody.put("freq", "");
            uplinkBody.put("dr", "");
            uplinkBody.put("fcnt", frame.getFcnt());
            uplinkBody.put("port", "");
            uplinkBody.put("confirmed", false);
            uplinkBody.put("gws", "");
            uplinkBody.put("rawReading", frame.getRawBytes() != null ? bytesToHex(frame.getRawBytes()) : "");
            uplinkBody.put("serial", reading.getSerialMeter());
            messageApiService.createMessage(uplinkBody).subscribe();

            // Alarms
            ObjectNode boveBody = objectMapper.createObjectNode();
            boveBody.put("typeMessage", "Uplink");
            boveBody.put("controlCode", "");
            boveBody.put("dateLength", "");
            boveBody.put("datadeIntification1", "");
            boveBody.put("dataIdentification2", "");
            boveBody.put("countNumber", "");
            boveBody.put("unit", 0.0);
            boveBody.put("volumeData", 0.0);
            boveBody.put("meterStateSt1", "");
            boveBody.put("meterStateSt2", "");
            boveBody.put("batteryCapacity", "");
            boveBody.put("lowBatteryAlarm", reading.isLowBatteryAlarm());
            boveBody.put("emptyPipeAlarm", reading.isEmptyPipeAlarm());
            boveBody.put("reverseFlowAlarm", reading.isReverseFlowAlarm());
            boveBody.put("overRangeAlarm", reading.isOverRangeAlarm());
            boveBody.put("overTempratureAlarm", false); // No tienes este método, así que pon false o el valor que
                                                        // corresponda
            boveBody.put("eepromError", reading.isEerrorAlarm());
            boveBody.put("leakageAlarm", reading.isLeakageAlarm());
            boveBody.put("burstAlarm", reading.isBurstAlarm());
            boveBody.put("tamperAlarm", reading.isTamperAlarm());
            boveBody.put("freezingAlarm", reading.isFreezingAlarm());
            boveBody.put("rawData", frame.getRawBytes() != null ? bytesToHex(frame.getRawBytes()) : "");
            boveBody.put("deviceId", frame.getImei14() != null ? frame.getImei14() : "");
            boveBody.putNull("error");
            boveBody.put("valveState", "");

            messageApiService.createBoveMessage(boveBody).subscribe();
        } catch (Exception e) {
            log.error("Error processing MeterReadingEvent: {}", e.getMessage(), e);
        }
    }

    private String bytesToHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
