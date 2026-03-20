package com.m3verificaciones.appweb.lte.frame;

import com.m3verificaciones.appweb.lte.dto.ParsedFrame;

public interface FrameParser {
    String getBrand();
    boolean canParse(byte[] raw);
    ParsedFrame parse(byte[] raw);
}
