package com.m3verificaciones.appweb.lte.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.m3verificaciones.appweb.lte.dto.DecodedReading;
import com.m3verificaciones.appweb.lte.dto.ParsedFrame;
import com.m3verificaciones.appweb.lte.utils.api.ConsumptionApiService;
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
    public void on(MeterReadingEvent event){
        try{
            ParsedFrame frame = event.getFrame();
            DecodedReading reading = event.getReading();

            String serial = event.getMeterDetails().path("serial").asText("");

            ObjectNode body = objectMapper.createObjectNode();
            body.put("devEui", frame.getImei14());
            body.put("serialMeter", serial);
            body.put("typeMessage", "Uplink");
            body.put("communication", "4G");
            body.put("fcnt", frame.getFcnt());
            body.put("meterId", reading.getSerialMeter());
            body.put("consumption", reading.getConsumption());
            body.put("rssiDbm", frame.getCmd());
            body.put("iccId", frame.getRawBytes());
            body.put("rawReading", frame.getRawBytes() != null ? bytesToHex(frame.getRawBytes()) : "");

            // Alarms
            ObjectNode alarms = objectMapper.createObjectNode();
            alarms.put("leakageAlarm",    reading.isLeakageAlarm());
            alarms.put("burstAlarm",      reading.isBurstAlarm());
            alarms.put("tamperAlarm",     reading.isTamperAlarm());
            alarms.put("freezingAlarm",   reading.isFreezingAlarm());
            alarms.put("lowBatteryAlarm", reading.isLowBatteryAlarm());
            alarms.put("emptyPipeAlarm",  reading.isEmptyPipeAlarm());
            alarms.put("reverseFlowAlarm",reading.isReverseFlowAlarm());
            alarms.put("overRangeAlarm",  reading.isOverRangeAlarm());
            alarms.put("temperatureAlarm",reading.isTemperatureAlarm());
            alarms.put("eeErrorAlarm",    reading.isEerrorAlarm());
            body.set("alarms", alarms);

            messageApiService.createMessage(body).subscribe();
        }catch (Exception e){
            log.error("Error processing MeterReadingEvent: {}", e.getMessage(), e);
        }
    }

    private String bytesToHex(byte[] data){
        StringBuilder sb = new StringBuilder();
        for(byte b : data){
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
