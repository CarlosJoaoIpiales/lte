package com.m3verificaciones.appweb.lte.decoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PayloadDecoderFactory {
    private static final Logger log = LoggerFactory.getLogger(PayloadDecoderFactory.class);

    // key: "BOVE.BECOY" -> List of decoders
    private final Map<String, List<PayloadDecoder>> decoderMap;

    public PayloadDecoderFactory(List<PayloadDecoder> decoders) {
        this.decoderMap = decoders.stream()
                .collect(Collectors.groupingBy(d -> d.getBrand().toUpperCase() + "." + d.getModel().toUpperCase()));
        
        decoders.forEach(
            d -> log.info("Registered PayloadDecoder for brand: {}, model: {}", d.getBrand(), d.getModel())
        );
    }

    public PayloadDecoder resolve(String brand,String model, int cmd, byte[] payload) {
        String key = brand.toUpperCase() + "." + model.toUpperCase();
        
        return Optional.ofNullable(decoderMap.get(key)).flatMap(list -> list.stream()
                .filter(d -> d.supports(cmd, payload))
                .findFirst())
            .orElseThrow(() -> new UnsupportedOperationException("No suitable PayloadDecoder found for brand=" + brand + " model=" + model + " cmd=0x" + String.format("%02X", cmd)));
    }
}
