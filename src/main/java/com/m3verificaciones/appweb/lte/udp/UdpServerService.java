package com.m3verificaciones.appweb.lte.udp;

import com.m3verificaciones.appweb.lte.dispatcher.CmdHandlerRegistry;
import com.m3verificaciones.appweb.lte.dto.ParsedFrame;
import com.m3verificaciones.appweb.lte.frame.AckFrameBuilder;
import com.m3verificaciones.appweb.lte.frame.FrameParserRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

import javax.xml.crypto.Data;

@Service
public class UdpServerService {
    private static final Logger log = LoggerFactory.getLogger(UdpServerService.class);

    @Value("${udp.server.port:16680}")
    private int port;

    private final FrameParserRegistry parserRegistry;
    private final CmdHandlerRegistry handlerRegistry;
    private final AckFrameBuilder ackBuilder;

    private volatile boolean running = false;
    private DatagramSocket socket;

    public UdpServerService(FrameParserRegistry parserRegistry, CmdHandlerRegistry handlerRegistry, AckFrameBuilder ackBuilder) {
        this.parserRegistry = parserRegistry;
        this.handlerRegistry = handlerRegistry;
        this.ackBuilder = ackBuilder;
    }

    @PostConstruct
    public void start(){
        running = true;
        Thread thread = new Thread(this::listen, "udp-server");
        thread.setDaemon(true);
        thread.start();
        log.info("UDP Server started on port {}", port);
    }

    @PreDestroy
    public void stop(){
        running = false;
        if(socket != null && !socket.isClosed()){
            socket.close();
        }
        log.info("UDP Server stopped");
    }

    private void listen(){
        try{
            socket = new DatagramSocket(port);
            byte[] buffer = new byte[4096];

            log.info("UDP Server is listening on port {}", port);

            while(running){
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());
                InetAddress senderAddr = packet.getAddress();
                int senderPort = packet.getPort();
                log.debug("Received packet from {}:{}", senderAddr, senderPort);

                processPacket(data, senderAddr, senderPort);
            }
        }catch (Exception e){
            if(running){
                log.error("Error in UDP server: {}", e.getMessage(), e);
            }
        }
    }

    private void processPacket(byte[] data, InetAddress addr, int senderPort){
        try{
            ParsedFrame frame = parserRegistry.parse(data);

            log.info("Parsed tram | brand={} imei={} cmd=0x{} checksu={}", frame.getBrand(), frame.getImei14(), String.format("%02X", frame.getCmd()),frame.isChecksumOk() ? "OK" : "FAIL");

            // Send ACK if the device wants it
            if(frame.isAckEnabled()){
                byte[] ack = ackBuilder.build(frame);
                socket.send(new DatagramPacket(ack, ack.length, addr, senderPort));
                log.debug("Sent ACK to {}:{}", addr, senderPort);
            }

            handlerRegistry.dispatch(frame);
        }catch (IllegalArgumentException e){
            log.warn("Failed to parse frame from {}: {}", addr, e.getMessage());
        }catch (Exception e){
            log.error("Error processing packet from {}: {}", addr, e.getMessage(), e);
        }
    }
}
