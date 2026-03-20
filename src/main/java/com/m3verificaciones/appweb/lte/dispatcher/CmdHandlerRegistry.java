package com.m3verificaciones.appweb.lte.dispatcher;

import com.fasterxml.jackson.databind.JsonNode;
import com.m3verificaciones.appweb.lte.dto.ParsedFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

@Component
public class CmdHandlerRegistry {
    private static final Logger log = LoggerFactory.getLogger(CmdHandlerRegistry.class);
    private final Map<String, CmdHandler> handlerMap = new HashMap<>();

    public CmdHandlerRegistry(List<CmdHandler> handlers) {
        handlers.forEach(h -> {
            String brand = h.getBrand() != null ? h.getBrand().toUpperCase() : "*";
            String key = brand + "." + String.format("%02X", h.getCmd());
            handlerMap.put(key, h);
            log.info("Registered CmdHandler for brand: {}, cmd: 0x{}", brand, String.format("%02X", h.getCmd()));
        });
    }

    public void dispatch(ParsedFrame frame){
        String brand = frame.getBrand().toUpperCase();
        String cmdHex = String.format("%02X", frame.getCmd());

        CmdHandler handler = Optional.ofNullable(handlerMap.get(brand + "." + cmdHex))
                .orElse(handlerMap.get("*." + cmdHex));
        
        if (handler == null) {
            log.warn("No handler for brand={} cmd=0x{}", brand, cmdHex);
            return;
        }

        handler.handle(frame, null);
    }
}
