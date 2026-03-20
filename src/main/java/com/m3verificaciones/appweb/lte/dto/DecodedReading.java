package com.m3verificaciones.appweb.lte.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DecodedReading {
    
    private String serialMeter;
    private Double consumption;
    private Integer uplinkIntervalMinutes;
    private Double unitFactor;
    private String iccId;
    private Integer rssiDbm;

    // Alarms
    private boolean leakageAlarm;
    private boolean burstAlarm;
    private boolean lowBatteryAlarm;
    private boolean tamperAlarm;
    private boolean freezingAlarm;
    private boolean emptyPipeAlarm;
    private boolean reverseFlowAlarm;
    private boolean overRangeAlarm;
    private boolean temperatureAlarm;
    private boolean eerrorAlarm;

    public boolean hasActiveAlarms() {
        return leakageAlarm || burstAlarm || lowBatteryAlarm || tamperAlarm || freezingAlarm ||
               emptyPipeAlarm || reverseFlowAlarm || overRangeAlarm || temperatureAlarm || eerrorAlarm;
    }
}
