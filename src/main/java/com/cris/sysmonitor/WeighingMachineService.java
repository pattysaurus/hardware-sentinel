package com.cris.sysmonitor;

import com.fazecast.jSerialComm.SerialPort;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class WeighingMachineService {

    // Latest raw reading from each monitored port
    private final List<WeighingReading> latestReadings = new CopyOnWriteArrayList<WeighingReading>();

    // Active port threads
    private final List<SerialPort> openPorts = new CopyOnWriteArrayList<SerialPort>();

    private volatile boolean running = true;

    @PostConstruct
    public void init() {
        Thread scanner = new Thread(new Runnable() {
            public void run() {
                scanAndListen();
            }
        });
        scanner.setDaemon(true);
        scanner.setName("weighing-scanner");
        scanner.start();
    }

    private void scanAndListen() {
        // Wait for app to fully start
        try { Thread.sleep(3000); } catch (InterruptedException ignored) {}

        SerialPort[] ports = SerialPort.getCommPorts();
        for (final SerialPort port : ports) {
            final String portName = port.getSystemPortName();

            // Skip ports already used by QR display detection
            // We try all ports; if it fails to open or sends nothing, we move on
            Thread listener = new Thread(new Runnable() {
                public void run() {
                    listenToPort(port);
                }
            });
            listener.setDaemon(true);
            listener.setName("weighing-" + portName);
            listener.start();
        }
    }

    private void listenToPort(SerialPort port) {
        // Default settings — most weighing machines use 9600,8,N,1
        port.setBaudRate(9600);
        port.setNumDataBits(8);
        port.setNumStopBits(SerialPort.ONE_STOP_BIT);
        port.setParity(SerialPort.NO_PARITY);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 2000, 0);

        if (!port.openPort()) return;

        openPorts.add(port);

        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(port.getInputStream())
            );

            while (running) {
                try {
                    String line = reader.readLine();
                    if (line != null) {
                        line = line.trim();
                        if (!line.isEmpty()) {
                            updateReading(port.getSystemPortName(), port.getDescriptivePortName(), line);
                        }
                    }
                } catch (Exception e) {
                    // Port disconnected or timeout — stop listening
                    break;
                }
            }
        } catch (Exception e) {
            // Could not get input stream
        } finally {
            port.closePort();
            openPorts.remove(port);
            removeReading(port.getSystemPortName());
        }
    }

    private void updateReading(String portName, String description, String rawLine) {
        WeighingReading existing = findReading(portName);
        if (existing != null) {
            existing.setRawData(rawLine);
            existing.setTimestamp(System.currentTimeMillis());
        } else {
            WeighingReading r = new WeighingReading();
            r.setPortName(portName);
            r.setDescription(description);
            r.setRawData(rawLine);
            r.setTimestamp(System.currentTimeMillis());
            latestReadings.add(r);
        }
    }

    private WeighingReading findReading(String portName) {
        for (WeighingReading r : latestReadings) {
            if (r.getPortName().equals(portName)) return r;
        }
        return null;
    }

    private void removeReading(String portName) {
        WeighingReading toRemove = findReading(portName);
        if (toRemove != null) latestReadings.remove(toRemove);
    }

    public List<WeighingReading> getLatestReadings() {
        return new ArrayList<WeighingReading>(latestReadings);
    }

    // Returns all available COM port names for configuration
    public List<String> getAvailablePorts() {
        List<String> names = new ArrayList<String>();
        for (SerialPort p : SerialPort.getCommPorts()) {
            names.add(p.getSystemPortName() + " — " + p.getDescriptivePortName());
        }
        return names;
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        for (SerialPort p : openPorts) {
            try { p.closePort(); } catch (Exception ignored) {}
        }
    }

    // ── Inner DTO ──────────────────────────────────────────────────────────────
    public static class WeighingReading {
        private String portName;
        private String description;
        private String rawData;
        private long timestamp;

        public String getPortName()               { return portName; }
        public void   setPortName(String v)       { this.portName = v; }
        public String getDescription()            { return description; }
        public void   setDescription(String v)    { this.description = v; }
        public String getRawData()                { return rawData; }
        public void   setRawData(String v)        { this.rawData = v; }
        public long   getTimestamp()              { return timestamp; }
        public void   setTimestamp(long v)        { this.timestamp = v; }
    }
}