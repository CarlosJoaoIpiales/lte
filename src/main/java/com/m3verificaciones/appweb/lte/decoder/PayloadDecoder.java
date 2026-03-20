package com.m3verificaciones.appweb.lte.decoder;

import com.m3verificaciones.appweb.lte.dto.DecodedReading;

public interface PayloadDecoder {
    String getBrand();
    String getModel();
    boolean supports(int cmd, byte[] payload);
    DecodedReading decode(byte[] payload);
}
