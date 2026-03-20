package com.m3verificaciones.appweb.lte.event;

import com.m3verificaciones.appweb.lte.dto.DecodedReading;
import com.m3verificaciones.appweb.lte.utils.api.ConsumptionApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ConsumptionSaveListener {
    private static final Logger log = LoggerFactory.getLogger(ConsumptionSaveListener.class);
    private final ConsumptionApiService consumptionApiService;

    public ConsumptionSaveListener(ConsumptionApiService consumptionApiService) {
        this.consumptionApiService = consumptionApiService;
    }

    @EventListener
    public void on(MeterReadingEvent event){
        DecodedReading reading = event.getReading();

        if(reading.getConsumption() == null){
            log.warn("Skipping consumption save for event with null consumption");
            return;
        }

        String imei = event.getFrame().getImei14();
        String serial = event.getMeterDetails().path("serial").asText("");
        String model = event.getMeterDetails().path("model").asText("");
        String diameter = event.getMeterDetails().path("diameter").asText("");

        consumptionApiService.createConsumption(imei, serial, reading.getConsumption(), model, diameter)
        .subscribe();
    }

}
