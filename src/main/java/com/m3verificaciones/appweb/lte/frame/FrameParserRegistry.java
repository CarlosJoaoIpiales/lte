package com.m3verificaciones.appweb.lte.frame;

import com.m3verificaciones.appweb.lte.dto.ParsedFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FrameParserRegistry {
    private static final Logger log = LoggerFactory.getLogger(FrameParserRegistry.class);
    private final List<FrameParser> parsers;

    public FrameParserRegistry(List<FrameParser> parsers) {
        this.parsers = parsers;
        parsers.forEach(p -> log.info("Registered FrameParser for brand: {}", p.getBrand()));
    }

    public ParsedFrame parse(byte[] raw) {
        return parsers.stream()
                .filter(p -> p.canParse(raw))
                .findFirst()
                .map(p -> {
                    log.info("Parsing frame with parser for brand: {}", p.getBrand());
                    return p.parse(raw);
                })
                .orElseThrow(() -> new IllegalArgumentException("No suitable FrameParser found for the given raw data"));
    }

}
