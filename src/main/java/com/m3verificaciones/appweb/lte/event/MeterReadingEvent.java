package com.m3verificaciones.appweb.lte.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.m3verificaciones.appweb.lte.dto.DecodedReading;
import com.m3verificaciones.appweb.lte.dto.ParsedFrame;
import lombok.Getter;

@Getter
public class MeterReadingEvent {
    
    private final ParsedFrame frame;
    private final DecodedReading reading;
    private final JsonNode meterDetails;

    public MeterReadingEvent(ParsedFrame frame, DecodedReading reading, JsonNode meterDetails) {
        this.frame = frame;
        this.reading = reading;
        this.meterDetails = meterDetails;
    }
}
