# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Service Overview

`iot-platform-lte-backend` is the LTE protocol microservice (port 8087 in Docker, 8080 internally). It receives UDP packets from LTE water meters, parses binary frames, decodes meter readings, and forwards data to other platform services via REST (WebFlux `WebClient`). It has no database of its own — `spring.autoconfigure.exclude` disables `DataSourceAutoConfiguration`.

## Commands

```bash
./mvnw package            # Build JAR
./mvnw test               # Run tests
./mvnw package -DskipTests  # Build without tests (used in Dockerfile)
./mvnw spring-boot:run    # Run locally (UDP on port 16680, HTTP on 8080)
```

## Architecture: UDP Frame Processing Pipeline

The core flow is: **UDP socket → frame parser → cmd dispatcher → payload decoder → event → API calls**

```
UdpServerService          (udp/)           – raw byte ingestion, ACK response
  └─ FrameParserRegistry  (frame/)         – selects correct FrameParser by canParse()
       └─ BoveFrameParser (frame/bove/)    – parses BOVE binary protocol into ParsedFrame
  └─ CmdHandlerRegistry   (dispatcher/)   – routes ParsedFrame to CmdHandler by brand+cmd
       └─ MeterDataHandler                 – validates checksum, calls MeterApiService
            └─ PayloadDecoderFactory       – selects PayloadDecoder by brand+model+cmd
                 └─ BecoYMeterDecoder      – decodes 24-byte BECOY payload → DecodedReading
            └─ ApplicationEventPublisher   – fires MeterReadingEvent
                 └─ ConsumptionSaveListener – POSTs consumption to consumption service
                 └─ MessageSaveListener     – POSTs raw message to messages service
```

### Extension Points

To add a new **brand/device protocol**:
1. Implement `FrameParser` → register as `@Component` → `FrameParserRegistry` auto-discovers it
2. Implement `PayloadDecoder` → register as `@Component` → `PayloadDecoderFactory` auto-discovers it
3. Optionally implement `CmdHandler` with a specific `getBrand()` for brand-specific command handling

`CmdHandler.getBrand()` returning `null` means "applies to all brands" (see `MeterDataHandler`).

### Key Configuration (`application.properties`)

| Property | Default | Purpose |
|---|---|---|
| `udp.server.port` | `16680` | UDP listener port |
| `api.meter.base-url` | `http://iot-platform-meters-backend:8080/...` | Meters service |
| `api.message.base-url` | `http://iot-platform-messages-backend:8080/...` | Messages service |
| `api.consumption.base-url` | `http://iot-platform-consumption-backend:8080/...` | Consumption service |

For local development against Docker services, these URLs work as-is from inside Docker. If running the service locally outside Docker, change hostnames to `localhost` with the mapped ports (8085, 8088, 8084).

### BOVE Frame Format

```
[0x88][IMEI 7B BCD][COMM][ACK][FCNT][CMD][RESERVE 2B][DATA_LEN 2B LE][PAYLOAD N B][CHECKSUM][0x22]
```
- Checksum = sum of all bytes before it, mod 256
- ACK byte `0x00` means the device expects an ACK response
- IMEI is 7-byte BCD → 14 decimal digits

### BECOY Payload (24 bytes, CMD 0x00)

| Bytes | Field | Encoding |
|---|---|---|
| 0–3 | Serial (ADDR) | BCD little-endian |
| 4–7 | Totalizer | BCD little-endian |
| 8–9 | Uplink interval (min) | uint16 little-endian |
| 10 | Unit indicator | lookup → m³ factor |
| 11–12 | ST1/ST2 alarm bytes | bitmask |
| 13–22 | ICCID | BCD big-endian |
| 23 | RSSI | lookup: `−113 + (raw × 2)` dBm |
