package com.m3verificaciones.appweb.lte.dispatcher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.m3verificaciones.appweb.lte.decoder.PayloadDecoder;
import com.m3verificaciones.appweb.lte.decoder.PayloadDecoderFactory;
import com.m3verificaciones.appweb.lte.dto.DecodedReading;
import com.m3verificaciones.appweb.lte.dto.ParsedFrame;
import com.m3verificaciones.appweb.lte.event.MeterReadingEvent;
import com.m3verificaciones.appweb.lte.frame.bove.BoveFrameParser;
import com.m3verificaciones.appweb.lte.utils.api.MeterApiService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class MeterDataHandler implements CmdHandler {
    private static final Logger log = LoggerFactory.getLogger(MeterDataHandler.class);

    private static final int CMD = 0x00;

    private final PayloadDecoderFactory decoderFactory;
    private final ApplicationEventPublisher eventPublisher;
    private final MeterApiService meterApiService;
    private final ObjectMapper objectMapper;

    public MeterDataHandler(PayloadDecoderFactory decoderFactory, ApplicationEventPublisher eventPublisher,
            MeterApiService meterApiService, ObjectMapper objectMapper) {
        this.decoderFactory = decoderFactory;
        this.eventPublisher = eventPublisher;
        this.meterApiService = meterApiService;
        this.objectMapper = objectMapper;
    }

    @Override
    public int getCmd() {
        return CMD;
    }

    @Override
    public String getBrand() {
        return null; // null = aplied to all brands, if specific brand handler exist, it will be used
                     // instead
    }

    @Override
    public void handle(ParsedFrame frame, JsonNode ignoredDetails) {
        if (!frame.isChecksumOk()) {
            log.warn("Checksum mismatch for frame from meterImei={}", frame.getImei14());
            return;
        }

        meterApiService.getMeterDetailsByImei(frame.getImei14())
                .subscribe(meterDetails -> {
                    if (isMeterInactive(meterDetails, frame.getImei14())) {
                        return;
                    }

                    String brand = frame.getBrand();
                    String model = meterDetails.path("model").asText("");
                    String serial = meterDetails.path("serial").asText("");
                    String uniqueKey = meterDetails.path("unique_key").asText("");

                    try {
                        PayloadDecoder decoder = decoderFactory.resolve(brand, model, frame.getCmd(),
                                frame.getPayload());
                        DecodedReading reading = decoder.decode(frame.getPayload());

                        logReading(frame.getImei14(), reading);

                        // Publish event for other components (e.g. to save reading in DB, trigger
                        // alerts, etc)
                        eventPublisher.publishEvent(new MeterReadingEvent(frame, reading, meterDetails));

                        // Update last seen and reading in meter details (optional, depends on your
                        // needs)
                        meterApiService.updateLastCommunication(uniqueKey).subscribe();
                    } catch (UnsupportedOperationException e) {
                        log.info("No decoder for brand={} model={} cmd=0x{}: {}", brand, model, frame.getCmd(),
                                e.getMessage());
                    } catch (Exception e) {
                        log.error("Error decoding payload for meterImei={}: {}", frame.getImei14(), e.getMessage(), e);
                    }
                });
    }

    private boolean isMeterInactive(JsonNode meterDetails, String imei) {
        if (meterDetails == null || meterDetails.isEmpty()) {
            log.warn("No details found for meterImei={}", imei);
            return true;
        }
        if (!"true".equals(meterDetails.path("state").asText("false"))) {
            log.warn("Meter with imei={} is inactive according to details", imei);
            return true;
        }
        return false;
    }

    private void logReading(String imei, DecodedReading reading) {
        log.info("Reading decodified: imei={} meterId={} consumption={} interval={}min rssi={}dbm", imei,
                reading.getSerialMeter(), reading.getConsumption(), reading.getUplinkIntervalMinutes(),
                reading.getRssiDbm());

        if (reading.hasActiveAlarms()) {
            log.warn("Activated Alarms: imei={} meterId={}", imei, reading.getSerialMeter());
        }
    }

}
