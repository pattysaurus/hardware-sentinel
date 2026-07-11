package com.cris.sysmonitor;

import com.fazecast.jSerialComm.SerialPort;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.time.LocalDateTime;

@Component
@EnableScheduling
public class SnapshotScheduler {

    @Autowired
    private DeviceSnapshotRepository repository;

    @Scheduled(fixedDelay = 30000)   // every 30 seconds
    public void captureSnapshot() {
        try {
            SystemInfo si  = new SystemInfo();
            HardwareAbstractionLayer hal = si.getHardware();

            int usbCount     = hal.getUsbDevices(false).size();
            int diskCount    = hal.getDiskStores().size();
            int comCount     = SerialPort.getCommPorts().length;
            int audioCount   = 0;   // PowerShell-sourced; use 0 until integrated
            int displayCount = 0;   // same
            int crisCount    = 0;   // populated by CRIS device scan below

            // Count CRIS devices (ports with "CH340" or "USB Serial" in description)
            SerialPort[] ports = SerialPort.getCommPorts();
            for (SerialPort p : ports) {
                String desc = p.getPortDescription().toLowerCase();
                if (desc.contains("ch340") || desc.contains("usb serial") || desc.contains("cris")) {
                    crisCount++;
                }
            }

            String ip = "unknown";
            try {
                ip = InetAddress.getLocalHost().getHostAddress();
            } catch (Exception ignored) {}

            DeviceSnapshot snap = new DeviceSnapshot(
                    ip, usbCount, diskCount, comCount,
                    audioCount, displayCount, crisCount,
                    "UP", LocalDateTime.now()
            );

            repository.save(snap);

        } catch (Exception e) {
            System.err.println("[SnapshotScheduler] Error capturing snapshot: " + e.getMessage());
        }
    }
}
