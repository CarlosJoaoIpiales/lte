package com.m3verificaciones.appweb.lte.frame.bove;

import com.m3verificaciones.appweb.lte.dto.ParsedFrame;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class BoveParsedFrame extends ParsedFrame {
    private String reserveHex;
    private String checksumReceivedHex;
    private String checksumCalculatedHex;
}
