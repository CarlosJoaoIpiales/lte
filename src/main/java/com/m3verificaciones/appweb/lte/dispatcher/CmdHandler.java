package com.m3verificaciones.appweb.lte.dispatcher;

import com.m3verificaciones.appweb.lte.dto.ParsedFrame;
import com.fasterxml.jackson.databind.JsonNode;

public interface CmdHandler {
    
    int getCmd();

    String getBrand();

    void handle(ParsedFrame frame, JsonNode meterDetails);
}
