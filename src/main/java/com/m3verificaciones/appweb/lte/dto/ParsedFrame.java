package com.m3verificaciones.appweb.lte.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class ParsedFrame {
    private byte[] rawBytes;
    private String brand;
    private String imei14;
    private byte[] imeiRaw;
    private int comm;
    private boolean is4g;
    private int ack;
    private boolean ackEnabled;
    private int fcnt;
    private int cmd;
    private int dataLength;
    private byte [] payload;
    private boolean checksumOk;
}
